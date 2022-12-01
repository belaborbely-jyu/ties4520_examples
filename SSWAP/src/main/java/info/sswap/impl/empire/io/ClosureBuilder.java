/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.io;

import info.sswap.api.model.Config;
import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.Expressivity;
import info.sswap.impl.empire.Namespaces;
import info.sswap.impl.empire.model.ExpressivityChecker;
import info.sswap.impl.empire.model.JenaModelFactory;
import info.sswap.impl.empire.model.ModelUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Builds a closure of statements that are contained in a particular model by recursively following the URIs of
 * Resources (until a certain depth is reached, a time/byte limit is reached, or no new statements could be added by
 * this method).
 * 
 * TODO: Add a note about reusing this object
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class ClosureBuilder {
	private static final Logger LOGGER = LogManager.getLogger(ClosureBuilder.class);
	
	/**
	 * A set of types of that are dereferenced during the closure computation.
	 */
	private static Set<Resource> DEREFERENCED_TYPES = new HashSet<Resource>(Arrays.asList(new Resource[] {
	                OWL.AnnotationProperty, OWL.Class, OWL.DataRange, OWL.DatatypeProperty, OWL.FunctionalProperty,
	                OWL.ObjectProperty, OWL.Ontology, OWL.OntologyProperty, OWL.Restriction, OWL.SymmetricProperty,
	                OWL.TransitiveProperty, OWL2.Annotation, OWL2.ReflexiveProperty }));
	
	private static Set<Property> HIERARCHY_PROPERTIES = new HashSet<Property>(Arrays.asList(new Property[] {
					RDFS.subClassOf, RDFS.subPropertyOf, OWL.equivalentClass, OWL.equivalentProperty
	}));

	/**
	 * The maximum number of bytes transferred over the network this builder should not exceed while computing the
	 * closure Please note: the byte limits from separate threads are synchronized at the beginning and the end of each
	 * dereference operation. Therefore, this limit should be treated as a soft limit, because it is possible that
	 * multiple concurrent threads will exceed it and it will be noticed only after they finished their invidual
	 * transfers (no new transfers will occur, however).
	 */
	private long maxBytes;

	/**
	 * The maximum amount of time this builder is allowed to spend while computing the closure.
	 */
	private long maxTime;

	/**
	 * Maximum amount of concurrent threads (in addition to the caller's thread) for concurrent downloads.
	 */
	private int maxThreads;

	/**
	 * The counter of bytes read by this closure builder.
	 */
	private long bytesRead;

	private long startTime;

	/**
	 * The set of URIs that were marked during closure computation. A marked URI is considered as processed, and will
	 * never be enqueued for future dereference.
	 */
	private Set<String> markedURIs;

	/**
	 * The current list of URIs to be dereferenced for the current depth.
	 */
	private List<String> dereferenceQueue;

	private ModelCache modelCache;

	private ExpressivityChecker expressivityChecker;

	private int connectTimeout;

	private int readTimeout;
	
	private List<String> ignoredNamespaces;

	/**
	 * Creates a new closure builder. This method is intentionally package private to encourage the use of
	 * ClosureBuilderFactory for creating the objects of this type.
	 * 
	 * @param maxBytes
	 *            the maximum amount of bytes this closure builder may transfer while computing the closure (soft limit)
	 * @param maxTime
	 *            the maximum amount of time this closure builder can spend computing the closure
	 * @param maxThreads
	 *            the maximum amount of concurrent threads (for concurrent downloads)
	 */
	ClosureBuilder(long maxBytes, long maxTime, int maxThreads, ModelCache modelCache, List<String> ignoredNamespaces) {
		this.maxBytes = maxBytes;
		this.maxTime = maxTime;
		this.maxThreads = maxThreads;
		this.modelCache = modelCache;

		try {
			connectTimeout = Integer.parseInt(Config.get().getProperty(Config.CLOSURE_CONNECT_TIMEOUT_KEY, Config.CLOSURE_CONNECT_TIMEOUT_DEFAULT));
		}
		catch (NumberFormatException e) {
			connectTimeout = Integer.parseInt(Config.CLOSURE_CONNECT_TIMEOUT_DEFAULT);
		}
		
		try {
			readTimeout = Integer.parseInt(Config.get().getProperty(Config.CLOSURE_READ_TIMEOUT_KEY, Config.CLOSURE_READ_TIMEOUT_DEFAULT));
		}
		catch (NumberFormatException e) {
			readTimeout = Integer.parseInt(Config.CLOSURE_READ_TIMEOUT_DEFAULT);
		}

		this.markedURIs = new HashSet<String>();
		this.dereferenceQueue = new LinkedList<String>();
		this.expressivityChecker = new ExpressivityChecker();
		this.ignoredNamespaces = ImmutableList
				.<String>builder()
				.add(Namespaces.RDF_NS, Namespaces.RDFS_NS, Namespaces.OWL_NS, Namespaces.XSD_NS)
				.addAll(ignoredNamespaces)
				.build();
	}

	/**
	 * Gets the connect timeout for a single HTTP connection
	 * 
	 * @return the connect timeout in milliseconds
	 */
	public int getConnectTimeout() {
		return connectTimeout;
	}

	/**
	 * Sets the connect timeout for a single HTTP connection
	 * 
	 * @param connectTimeout
	 *            the connect timeout in milliseconds
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * Gets the read timeout for a single HTTP connection
	 * 
	 * @return the readTimeout the read timeout in milliseconds
	 */
	public int getReadTimeout() {
		return readTimeout;
	}

	/**
	 * Sets the read timeout for a single HTTP connection
	 * 
	 * @param readTimeout
	 *            the read timeout in milliseconds
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	/**
	 * Build closure for the given model using default values for max closure parameters.
	 * 
	 * @param baseModel
	 *            the model whose closure should be computed (may be null, in such a case the model will be first
	 *            dereferenced from the given URI)
	 * @param modelURI
	 *            the URI of the model (may be null). If the passed model is null, this URI will be used to dereference
	 *            the initial model. In all other cases, this URI will just prevent an unnecessary re-download of the
	 *            initial contents of the model
	 * @return the computed closure
	 */

	public Closure build(Model baseModel, String modelURI) {
		int degree;
		
		try {
			degree = Integer.parseInt(Config.get().getProperty(Config.MAX_CLOSURE_DEGREE_KEY, Config.MAX_CLOSURE_DEGREE_DEFAULT));
		} catch (NumberFormatException e) {
			degree = Integer.parseInt(Config.MAX_CLOSURE_DEGREE_DEFAULT);
		}
		
		return build(baseModel, modelURI, degree);
	}
	
	/**
	 * Build closure for the given model
	 * 
	 * @param baseModel
	 *            the model whose closure should be computed (may be null, in such a case the model will be first
	 *            dereferenced from the given URI)
	 * @param modelURI
	 *            the URI of the model (may be null). If the passed model is null, this URI will be used to dereference
	 *            the initial model. In all other cases, this URI will just prevent an unnecessary re-download of the
	 *            initial contents of the model
	 * @param degree
	 *            the maximum degree for the closure
	 * @return the computed closure
	 */
	public Closure build(Model baseModel, String modelURI, int degree) {
		int hierarchyDegree;
		
		try {
			hierarchyDegree = Integer.parseInt(Config.get().getProperty(Config.MAX_HIERARCHY_CLOSURE_DEGREE_KEY, Config.MAX_HIERARCHY_CLOSURE_DEGREE_DEFAULT));
		} catch (NumberFormatException e) {
			hierarchyDegree = Integer.parseInt(Config.MAX_HIERARCHY_CLOSURE_DEGREE_DEFAULT);
		}
		
		
		return build(baseModel, modelURI, degree, hierarchyDegree);
	}
	
	public Closure build(Model baseModel, String modelURI, int degree, int hierarchyDegree) {
		String closureURI = (modelURI == null) ? null : ModelUtils.normalizeURI(modelURI);
		
		LOGGER.info("Building closure for " + modelURI + " up to a max degree of " + degree);
		startTime = System.currentTimeMillis();
		
		if ((closureURI != null) && (baseModel != null)) {
			// we already know this model, so mark that it is not retrieved more than once
			markedURIs.add(closureURI);
		}

		Model[] intermediateModels = new Model[degree + hierarchyDegree + 1];
		Model newModel = null;

		newModel = JenaModelFactory.get().createEmptyModel();

		intermediateModels[0] = newModel;
		int largestDegreeRetrieved = 0;

		// main loop, repeated for each degree
		for (int currentDegree = 1; currentDegree <= degree + hierarchyDegree; currentDegree++) {
			//System.out.println("Retrieving degree " + currentDegree);
			Model currentModel = newModel;

			// clear the queue from the previous degree (if any)
			dereferenceQueue.clear();

			if (getBytesRemaining() <= 0) {
				LOGGER.error("Byte limit exceeded during closure computation");
				// byte limit exceeded -- we break here
				break;
			}

			if (getTimeRemaining() <= 0) {
				LOGGER.error("Time limit exceeded during closure computation");
				// time limit exceeded -- we break here
				break;
			}

			if ((modelURI != null) && (baseModel == null)) {
				enqueueURI(modelURI);
			}

			if (currentDegree == 1 && baseModel != null) {
				if (currentDegree > degree) {
					enqueueHierarchyURIs(baseModel);
				}
				else {					
					enqueueURIs(baseModel);
				}
			}
			else {
				// process the current model and enqueue any new concepts
				if (currentDegree > degree) {
					enqueueHierarchyURIs(currentModel);
				}
				else {
					enqueueURIs(currentModel);
				}
			}

			if (dereferenceQueue.isEmpty()) {
				LOGGER.trace("Closure levels off. No new statements found at degree " + currentDegree);
				// we reached the point when no new statements could be dereferenced
				// we can terminate the search early
				break;
			}

			// actual computation for the degree
			newModel = doClosure(currentModel, /* typeStatementsOnly */false);

			intermediateModels[currentDegree] = newModel;
			largestDegreeRetrieved = currentDegree;
			LOGGER.trace("Finished retrieval of terms for degree " + currentDegree);
		}


		LOGGER.debug("Max degree retrieved from the network (before OWL DL determination): " + largestDegreeRetrieved);
		
		int finalDegree = largestDegreeRetrieved;
		
		// Now, try to determine the highest closure degree that is OWL DL (after applying the type-retrieving step)
		for (int currentDegree = largestDegreeRetrieved; currentDegree >= 0; currentDegree--) {
			Set<String> markedURIsCopy = new HashSet<String>(markedURIs);
			markedURIs = new HashSet<String>();
			LOGGER.trace("Performing a type retrieving step for degree " + currentDegree);
			Model degreeWithTypes = doTypeRetrievingStep(baseModel, intermediateModels[currentDegree]);
			
			if (isOWLDL(baseModel, degreeWithTypes, currentDegree)) {
				newModel = degreeWithTypes;
				finalDegree = largestDegreeRetrieved;
				LOGGER.debug("Found a closure degree that is OWL DL: " + currentDegree);
				break;
			}
			else if (currentDegree == 0) {
				LOGGER.debug("Reached the degree of 0 (with type retrieval) and still could not find a degree that is OWL DL");
				finalDegree = 0;
				newModel = null;
			}
			else {
				LOGGER.trace("The degree is not OWL DL, reducing by one: " + currentDegree); 
			}

			markedURIs = markedURIsCopy;
		}

		markedURIs.clear();

		if (newModel != null) {
			// remove all the bnodes into the internal representation
			ModelUtils.removeBNodes(newModel);

			// clear the marked URIs (in case this object is reused)
			return new Closure(baseModel, newModel, finalDegree);
		}
		else {
			return new Closure(baseModel, JenaModelFactory.get().createEmptyModel(), finalDegree);
		}
	}

	private Model doTypeRetrievingStep(Model baseModel, Model sourceModel) {
		// clear the queue from the previous degree (if any)
		dereferenceQueue.clear();

		if (getBytesRemaining() <= 0) {
			// byte limit exceeded -- we break here
			return sourceModel;
		}

		if (getTimeRemaining() <= 0) {
			// time limit exceeded -- we break here
			return sourceModel;
		}

		// process the current model and enqueue any new concepts
		if (baseModel != null) {
			enqueueURIs(baseModel);
		}
		enqueueURIs(sourceModel);

		if (dereferenceQueue.isEmpty()) {
			LOGGER.trace("Dereference queue is empty for type retrieving step. Everything has already been retrieved. Quitting type retrieval step");
			// we reached the point when no new statements could be dereferenced
			// we can terminate the search early
			return sourceModel;
		}

		// actual computation for the degree
		return doClosure(sourceModel, /* typeStatementsOnly */true);
	}

	private boolean isOWLDL(Model baseModel, Model model, int currentDegree) {
		OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
		try {
			OWLOntology ontology = null;

			if (baseModel == null) {
				ontology = ModelUtils.createOWLOntology(ontologyManager, ModelUtils.generateBNodeId(), model);
			}
			else {
				ontology = ModelUtils
				                .createOWLOntology(ontologyManager, ModelUtils.generateBNodeId(), baseModel, model);
			}

			boolean owlDl = expressivityChecker.checkProfile(Expressivity.DL, ontology);
			
			if (!owlDl) {
				LOGGER.debug("Closure found not to be OWL-DL at degree " + currentDegree);
				LOGGER.trace("Violations of OWL-DL for the computed closure");
				LOGGER.trace("===== Begin violations =======================");
				for (String violationDescription : expressivityChecker.getViolationExplanations(Expressivity.DL, ontology)) {
					LOGGER.trace(violationDescription);					
				}
				LOGGER.trace("===== End violations =========================");
			}
			
			return owlDl;
		}
		catch (OWLOntologyCreationException e) {
			return false;
		}
	}

	/**
	 * Computes one degree of closure on the passed model
	 * 
	 * @param sourceModel
	 *            the model whose closure (one level) should be computed
	 * @return the computed closure
	 */
	private Model doClosure(Model sourceModel, boolean typeStatementsOnly) {
		// copy all the currently known facts into the result
		Model closureModel = JenaModelFactory.get().createEmptyModel();
		addStatements(closureModel, sourceModel);

		// prepare the thread pool for the degree
		ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);

		// create the dereference tasks
		List<Callable<DereferenceTask>> dereferenceTaskCallables = new LinkedList<Callable<DereferenceTask>>();

		for (final String queueElement : dereferenceQueue) {
			Callable<DereferenceTask> dereferenceTask = new DereferenceTask(queueElement, threadPool);
			dereferenceTaskCallables.add(dereferenceTask);
		}

		try {
			// ask the thread pool to invoke all the dereference tasks concurrently (using the amount of threads
			// specified by maxThreads
			// this method waits till all the tasks are finished (or terminated by the timeout)
			List<Future<DereferenceTask>> dereferencedTaskFutures = threadPool.invokeAll(dereferenceTaskCallables,
			                getTimeRemaining(), TimeUnit.MILLISECONDS);

			for (Future<DereferenceTask> future : dereferencedTaskFutures) {
				try {
					DereferenceTask dereferenceTask = future.get();

					// if we were able to retrieve the model, just add the contents
					// of the retrieved model to the result
					if (dereferenceTask.getModel() != null) {
						if (typeStatementsOnly) {
							addTypeStatements(dereferenceTask.getURL(), closureModel, dereferenceTask.getModel());
						}
						else {
							addStatements(closureModel, dereferenceTask.getModel());
						}
					}
				}
				catch (CancellationException e) {
					LOGGER.debug("Closure computation cancelled", e);
					
					// nothing -- just start next iteration (another task could have
					// been finished before the cancellation)
				}
				catch (ExecutionException e) {
					LOGGER.error("Exception while retrieving a single document within the closure", e);
					
					// nothing -- just start next iteration (another task could have
					// been finished without execution exception)
				}
				catch (RejectedExecutionException e) {
					LOGGER.error("Exception while retrieving a single document within the closure", e);
					
					// nothing -- just start next iteration (another task could have
					// been finished without execution exception)
				}
				catch (InterruptedException e) {
					LOGGER.error("Exception while retrieving a single document within the closure", e);
					
					// nothing -- just start next iteration (another task could have
					// been finished before the interruption)
				}
			}
		}
		catch (InterruptedException e) {
			LOGGER.error("Closure computation cancelled", e);
						
			// nothing -- we are about to leave the method anyway
		}catch (RejectedExecutionException e) {
			LOGGER.error("Closue computation rejected", e);
			
			// nothing -- we are about to leave the method anyway
		}

		// shutdown the thread pool
		threadPool.shutdownNow();

		return closureModel;
	}

	/**
	 * Gets the amount of bytes that can still be transferred before the limit is reached
	 * 
	 * @return the amount of bytes that can still be transferred (0 or more)
	 */
	private synchronized long getBytesRemaining() {
		long result = maxBytes - bytesRead;
		return (result > 0l) ? result : 0;
	}

	/**
	 * Gets the amount of milliseconds that were already used while computing the closure
	 * 
	 * @return the amount of milliseconds since the startTime
	 */
	private synchronized long getTimeUsed() {
		return System.currentTimeMillis() - startTime;
	}

	/**
	 * Gets the amount of milliseconds that can still be used for closure computation before the time limit is reached.
	 * 
	 * @return the amount of milliseconds (0 or more)
	 */
	private synchronized long getTimeRemaining() {
		long result = maxTime - getTimeUsed();

		return (result > 0l) ? result : 0;
	}

	/**
	 * Retrieves a document at the specified URL and parses it into the Jena model. This method obeys the byte limits
	 * while downloading the URL and updates the byte counters appropriately.
	 * 
	 * This method is invoked by the concurrent worker threads. Note: in case of concurrent downloads, while the
	 * closure-wide byte counters will be updated correctly, the exceeding of the byte limit may not be noticed until a
	 * new concurrent download starts. (Every stream has its own internal counter, and these are not synchronized with
	 * each other during the download.)
	 * 
	 * @param urlString
	 *            the string containing the URL to be retrieved
	 * @return the downloaded model
	 * @throws ByteLimitExceededException
	 *             if the byte limit has been exceeded during the dereferencing.
	 * @throws IOException
	 *             if a generic I/O error should occur
	 */
	private Model dereferenceURL(String urlString) throws IOException, DataAccessException {
		LOGGER.debug("Attempting to dereference " + urlString);
		Model result = modelCache.getModel(urlString);

		// if we got a hit from the cache, return it right away
		if (result != null) {
			LOGGER.debug("Cache hit for " + urlString);
			return result;
		}
		
		URL url = null;
		
		try {
			 url = new URL(urlString);
		} 
		catch (MalformedURLException e) {
			LOGGER.debug("The URI is not a valid URL; skipping its dereferencing: " + urlString + "; " + e.getMessage());
			return JenaModelFactory.get().createEmptyModel();
		}
		
		long bytesRemaining = 0l;

		// check whether there are any bytes left for download
		synchronized (this) {
			bytesRemaining = getBytesRemaining();

			if (bytesRemaining <= 0) {
				LOGGER.info("Closure-wide byte transfer limit exceeded while attempting to retrieve " + urlString);
				throw new ByteLimitExceededException("Transfer limit exceeded");
			}
		}

		// create an input stream with the limit read
		// (It is possible that another thread will create a stream with the exactly same limit,
		// and therefore the overall byte limit may be exceeded by up to (N-1) * bytesRemaining,
		// where N is the number of concurrent threads)
		URLConnection urlConnection = url.openConnection();
		urlConnection.setConnectTimeout(connectTimeout);
		urlConnection.setReadTimeout(readTimeout);
		
		// support content negotiation
		urlConnection.setRequestProperty("accept", "application/rdf+xml, application/xml; q=0.8, text/xml; q=0.7, application/rss+xml; q=0.3, */*; q=0.2");
		
		try {
			InputStream connInput = ModelUtils.executeRequest(urlConnection).getContent();
			ByteLimitInputStream in = new ByteLimitInputStream(connInput, bytesRemaining);

			// parse the model
			try {
				result = JenaModelFactory.get().getModel(in);
			}
			finally {	
				try {
					in.close();
				}
				catch (IOException e) {
					// intentionally ignored
				}
			}

			LOGGER.debug("Succesfully retrieved " + urlString);

			// update the closure-wide byte counters
			synchronized (this) {
//				bytesRead += in.getBytesRead();
			}

			modelCache.setModel(urlString, result);
		}
		catch (DataAccessException e) {
			// do not cache data access exceptions
			
			throw e;
		}
		catch (ByteLimitExceededException e) {
			// do not cache failure when we exceeded the byte limit
			throw e;
		}
		catch (SocketTimeoutException e) {
			modelCache.setAsInaccessible(urlString);
			
			throw e;
		}
		catch (IOException e) {
			if (urlConnection instanceof HttpURLConnection) {
				int responseCode = ((HttpURLConnection) urlConnection).getResponseCode();
				
				if (responseCode == -1 || (responseCode == 404)) {
					modelCache.setAsInaccessible(urlString);									
				}
			}
			
			throw e;
		}
		
		return result;
	}

	/**
	 * Add a URI to the dereference queue, unless the URI has been already processed (i.e., has either been dereferenced
	 * or is awaiting dereference).
	 * 
	 * @param uri
	 *            the URI to be enqueued
	 */
	private void enqueueURI(String uri) {
		// if the uri is in markedURI set, this means that has already been processed,
		// otherwise we are enqueuing it
		String normalizedURI = ModelUtils.normalizeURI(uri);
		if (markedURIs.add(normalizedURI)) {
			// add to the queue
			dereferenceQueue.add(uri);

			LOGGER.trace(String.format("Scheduling %s for retrieval", uri));
		}
		else {
			LOGGER.trace(String.format("Skipping %s (already attempted its retrieval during this closure computation)", uri));
		}
	}

	/**
	 * Add URIs of all resources to a dereference queue (unless they have already been processed).
	 * 
	 * @param model
	 *            the model whose URIs should be added
	 */
	private synchronized void enqueueURIs(Model model) {
		for (String uri : getResourceURIs(model)) {			
			enqueueURI(uri);
		}
	}
	
	private synchronized void enqueueHierarchyURIs(Model model) {
		for (String uri : getUpHierarchyResourceURIs(model)) {			
			enqueueURI(uri);
		}
	}

	private boolean modelContainsPattern(Model m, Resource s, Property p, RDFNode o) {
		StmtIterator it = m.listStatements(s, p, o);
		
		try {
			if (it.hasNext()) {
				return true;
			}
		}
		finally {
			it.close();
		}

		return false;
	}
	
	private boolean isOWLMembersListOfType(Model model, Resource listResource, Resource typeResource) {
		StmtIterator it = model.listStatements(null, OWL2.members, listResource);
		try {
			if (it.hasNext()) {
				Statement s = it.next();
				Resource subject = s.getSubject();
				
				if (modelContainsPattern(model, subject, RDF.type, typeResource)) {
					return true;
				}
			}
		} 
		finally {
			it.close();
		}
		
		return false;
	}
	
	/**
	 * Attempts to guess whether a resource is a type definition that should be dereferenced (as opposed to an
	 * individual). This method is used before reasoning (which would give us a straightforward answer to this
	 * question).
	 * 
	 * @param model
	 *            the model where the Resource is mentioned
	 * @param resource
	 *            the resource that should be checked
	 * @return true if the resource belongs to a type that should be dereferenced.
	 */
	private boolean belongsToDereferencedType(Model model, Resource resource) {
		if (modelContainsPattern(model, null, RDF.type, resource)) {
			return true;
		}

		StmtIterator it = model.listStatements(resource, RDF.type, (RDFNode) null);

		try {
			while (it.hasNext()) {
				Statement s = it.next();

				Resource type = s.getResource();

				if (DEREFERENCED_TYPES.contains(type)) {
					return true;
				}
			}
		}
		finally {
			it.close();
		}

		// NOTE: given previous experiences with ontologies partitioned according to SSWAP protocol rules,
		// the checks below tend not to identify any additional types (when a resource is used within these 
		// constructs, typically has explicit typing)
	
		if (modelContainsPattern(model, resource, RDFS.subClassOf, null) 
						|| modelContainsPattern(model, null, RDFS.subClassOf, resource) 
						|| modelContainsPattern(model, null, OWL.allValuesFrom, resource) 
						|| modelContainsPattern(model, null, OWL.someValuesFrom, resource)
						|| modelContainsPattern(model, null, OWL.complementOf, resource)
						|| modelContainsPattern(model, resource, OWL.complementOf, null)
						|| modelContainsPattern(model, null, OWL2.onClass, resource)
						|| modelContainsPattern(model, null, OWL2.onDataRange, resource)
						|| modelContainsPattern(model, resource, OWL.equivalentClass, null)
						|| modelContainsPattern(model, null, OWL.equivalentClass, resource)
						|| modelContainsPattern(model, resource, OWL.disjointWith, null)
						|| modelContainsPattern(model, null, OWL.disjointWith, resource)
						|| modelContainsPattern(model, resource, OWL2.hasKey, null)
						|| modelContainsPattern(model, null, RDFS.domain, resource)
						|| modelContainsPattern(model, null, RDFS.range, resource)) {
			return true;
		}
		
		for(Resource listMembership : ModelUtils.getListURIs(model, resource)) {
			if (modelContainsPattern(model, null, OWL.intersectionOf, listMembership) 
				|| modelContainsPattern(model, null, OWL.unionOf, listMembership)				
				|| modelContainsPattern(model, null, OWL2.disjointUnionOf, listMembership)) {
				return true;
			}		
			
			if (isOWLMembersListOfType(model, listMembership, OWL2.AllDisjointClasses)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Attempts to guess whether a resource is a property mentioning (in a position other than predicate position)
	 * that should be dereferenced (as opposed to an individual). This method is used before reasoning 
	 * (which would give us a straightforward answer to this
	 * question).
	 * 
	 * @param model
	 *            the model where the Resource is mentioned
	 * @param resource
	 *            the resource that should be checked
	 * @return true if the resource belongs to a property that should be dereferenced.
	 */
	
	private boolean belongsToDereferencedProperty(Model model, Resource resource) {
		// owl:onProperty -- check for it first, as the properties are mentioned 
		// in this predicate without explicitly being typed
		if (modelContainsPattern(model, null, OWL.onProperty, resource)) {
			return true;
		}
		
		// NOTE: given previous experiences with ontologies partitioned according to SSWAP protocol rules,
		// the checks below tend not to identify any additional properties (when a resource is used within these 
		// constructs, typically has explicit typing)
		
		if (modelContainsPattern(model, resource, RDFS.subPropertyOf, null)
			|| modelContainsPattern(model, null, RDFS.subPropertyOf, resource)
			|| modelContainsPattern(model, null, OWL.equivalentProperty, resource)
			|| modelContainsPattern(model, resource, OWL.equivalentProperty, null)
			|| modelContainsPattern(model, resource, OWL2.propertyDisjointWith, null)
			|| modelContainsPattern(model, null, OWL2.propertyDisjointWith, resource)
			|| modelContainsPattern(model, resource, RDFS.domain, null)
			|| modelContainsPattern(model, resource, RDFS.range, null)) {
			return true;
		}
	
		for(Resource listMembership : ModelUtils.getListURIs(model, resource)) {
			if (modelContainsPattern(model, null, OWL2.propertyChainAxiom, listMembership)
				|| modelContainsPattern(model, null, OWL2.hasKey, listMembership)) {
				return true;
			}
			
			if (isOWLMembersListOfType(model, listMembership, OWL2.AllDisjointProperties)) {
				return true;
			}
		}
	
		return false;
	}


	/**
	 * Checks whether the URI of an RDFNode from the Jena model should be enqueued for retrieval. We only enqueue
	 * non-anonymous nodes that do not belong to standard ontologies (e.g., RDF, OWL etc) -- the concepts in these
	 * ontologies do not follow the SSWAP convention of having every URI dereferenceable.
	 * 
	 * This method takes into account the fact that this implementation of the API represents anonymous nodes internally
	 * as nodes with a special URI (these nodes are not enqueued as any other anonymous nodes).
	 * 
	 * @param node
	 *            the node that should be checked
	 * @return true, if the node should be enqueued
	 */
	private boolean shouldBeEnqueued(Model model, RDFNode node, boolean property) {
		
		boolean result = node.isURIResource() // not an anonymous node in Jena
		                && !ModelUtils.isBNodeURI(node.asResource().getURI()) // not internal representation of
		                // anonymous nodes
		                && !isIgnoredNamespace(node.asResource().getURI()) // does not belong to the standard ontologies
		                && (property 
		                	|| belongsToDereferencedType(model, node.asResource()) 
		                	|| belongsToDereferencedProperty(model, node.asResource()));
				
		return result;
	}

	/**
	 * Extract URIs of all resources that are non-anonymous resources and do not belong to standard ontologies (e.g.,
	 * OWL, RDF etc.)
	 * 
	 * @param model
	 *            the model from which the URIs should be extracted
	 * @return a collection of uris
	 */
	private Collection<String> getResourceURIs(Model model) {
		Set<String> result = new HashSet<String>();

		// process all statements
		for (StmtIterator it = model.listStatements(); it.hasNext();) {
			Statement statement = it.next();

			Resource subject = statement.getSubject();

			if (subject.isURIResource() 
				&& !result.contains(subject.getURI()) 
				&& shouldBeEnqueued(model, subject, false /* property */)) {
				result.add(subject.getURI());
			}

			Property predicate = statement.getPredicate();

			if (predicate.isURIResource()
				&& !result.contains(predicate.getURI())
				&& shouldBeEnqueued(model, predicate, true /* property */)) {
				result.add(predicate.getURI());
			}

			RDFNode object = statement.getObject();

			if (object.isURIResource() 
				&& !result.contains(object.asResource().getURI())
				&& shouldBeEnqueued(model, object, false /* property */)) {
				result.add(object.asResource().getURI());
			}
		}

		return result;
	}

	private Collection<String> getUpHierarchyResourceURIs(Model model) {
		Set<String> result = new HashSet<String>();

		// process all statements
		for (StmtIterator it = model.listStatements(); it.hasNext();) {
			Statement statement = it.next();

			Property property = statement.getPredicate();
			
			if (HIERARCHY_PROPERTIES.contains(property)) {
				RDFNode object = statement.getObject();
				
				if (object.isURIResource() 
								&& !result.contains(object.asResource().getURI())
								&& shouldBeEnqueued(model, object, false /* property */)) {
					
					LOGGER.debug(String.format("Enqueuing %s for retrieval because it is referenced as an object in a hierarchy property %s", object.asResource().getURI(), property.getURI()));									
					result.add(object.asResource().getURI());
				}
			}
			
		}

		return result;
	}

	
	/**
	 * Adds all statements from a source model to the destination model. Additionally, it copies other relevant
	 * information (e.g., namespace prefixes from the source model to the destination model)
	 * 
	 * @param destModel
	 *            the destination model
	 * @param sourceModel
	 *            the source model
	 */
	private void addStatements(Model destModel, Model sourceModel) {
		destModel.add(sourceModel);

		Map<String, String> nsMap = sourceModel.getNsPrefixMap();
		for (String prefix : nsMap.keySet()) {
			destModel.setNsPrefix(prefix, nsMap.get(prefix));
		}
	}

	/**
	 * Adds only type statements from the source model that describe the specified resource. This method is used for the
	 * final iteration of the closure computation, when there may be still untyped resources (effectively making the
	 * underlying ontology OWL Full). In an attempt to fill the type information, the final iteration is performed by
	 * dereferencing the URI of the untyped resource, and then this method is called to retrieve the type information.
	 * 
	 * @param url
	 *            the URL of the concept in destination model whose type should be completed with information from the
	 *            source model
	 * @param dest
	 *            the destination model
	 * @param source
	 *            the source model for the type information
	 */
	private void addTypeStatements(String url, Model dest, Model source) {
		Resource resource = ResourceFactory.createResource(url);

		// look for all rdf:type statements for the url
		for (StmtIterator it = source.listStatements(resource, RDF.type, (RDFNode) null); it.hasNext();) {
			Statement stmt = it.nextStatement();
			RDFNode type = stmt.getObject();

			// we only add type information that uses a small set of concepts (owl:Class,
			// owl:ObjectProperty, owl:DatatypeProperty or owl:AnnotationProperty).
			// One reason is that we do not want to add non-standard types, which may
			// be yet undefined. Another reason, we do not want to add types which
			// are not legal in OWL (e.g., most of RDFS vocabulary like rdfs:Class)
			if (type.equals(OWL.Class) || type.equals(OWL.ObjectProperty) || type.equals(OWL.DatatypeProperty)
			                || type.equals(OWL.AnnotationProperty) || type.equals(OWL2.DataRange)) {
				dest.add(stmt);
			}
		}
	}

	/**
	 * Checks whether a concept belongs to an ignored namespace. These concepts are not enqueued/dereferenced
	 * 
	 * @param uri
	 *            the URI to be checked
	 * @return true if this is a standard concept
	 */
	private boolean isIgnoredNamespace(String uri) {
		for (String ignoredNamespace : ignoredNamespaces) {
			if (uri.startsWith(ignoredNamespace)) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * A dereference task that is executed in the separate thread.
	 * 
	 * @author Blazej Bulka <blazej@clarkparsia.com>
	 */
	private class DereferenceTask implements Callable<DereferenceTask> {
		/**
		 * The URL to be dereferenced
		 */
		private String url;

		/**
		 * The executor service that will execute this task
		 */
		private ExecutorService executorService;

		/**
		 * The model retrieved (may be null, if no data could be retrieved)
		 */
		private Model model;

		/**
		 * True if byte limit was exceeded
		 */
		private boolean byteLimitExceeded;

		/**
		 * Creates a dereferencing task to retrieve the given URL with the specified executor service.
		 * 
		 * @param url
		 *            the URL
		 * @param executorService
		 *            the executor service
		 */
		public DereferenceTask(String url, ExecutorService executorService) {
			this.url = url;
			this.executorService = executorService;
			this.byteLimitExceeded = false;
		}

		/**
		 * The method that will be called by the ExecutorService to actually execute the task in its thread.
		 */
		public DereferenceTask call() {
			try {
				// attempt to dereference the task
				model = dereferenceURL(url);
			}
			catch (ByteLimitExceededException e) {
				byteLimitExceeded = true;

				// if byte limit is exceeded, it makes no sense to
				// execute any further tasks -- shutdown the executor service
				executorService.shutdown();
			}
			catch (FileNotFoundException e) {
				LOGGER.warn("For URL: " + url + " : " + e.toString());  // common, so just log a WARN and suppress the stack trace
				// nothing (the model is just null)
			}
			catch (IOException e) {
				LOGGER.warn("For URL: " + url + " : " + e.toString());
				// nothing (the model is just null)
			}
			catch (DataAccessException e) {
				LOGGER.warn("For URL: " + url + " : " + e.toString());
				// nothing (the model is just null)
			}

			return this;
		}

		/**
		 * Gets the dereferenced model (if any).
		 * 
		 * @return the dereferenced model (may be null, if the task was unsuccessful in dereferencing the model)
		 */
		public Model getModel() {
			return model;
		}

		/**
		 * Gets the URL that is dereferenced
		 * 
		 * @return URL
		 */
		public String getURL() {
			return url;
		}

		/**
		 * Checks whether the byte limit was exceeded
		 * 
		 * @return true if the byte limit is exceeded; false otherwise
		 */
		@SuppressWarnings("unused")
		public boolean isByteLimitExceeded() {
			return byteLimitExceeded;
		}
	}
}
