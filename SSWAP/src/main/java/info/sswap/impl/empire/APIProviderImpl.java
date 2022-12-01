/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire;

import info.sswap.api.model.Cache;
import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.PDG;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RQG;
import info.sswap.api.model.RRG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPModel;
import info.sswap.api.model.SSWAPNode;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPProvider;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.SSWAPType;
import info.sswap.api.spi.APIProvider;
import info.sswap.api.spi.ExtensionAPI;
import info.sswap.impl.empire.io.ClosureBuilderFactory;
import info.sswap.impl.empire.model.ImplFactory;
import info.sswap.impl.empire.model.IndividualImpl;
import info.sswap.impl.empire.model.JenaModelFactory;
import info.sswap.impl.empire.model.ModelImpl;
import info.sswap.impl.empire.model.ModelUtils;
import info.sswap.impl.empire.model.PDGImpl;
import info.sswap.impl.empire.model.ProviderImpl;
import info.sswap.impl.empire.model.RDGImpl;
import info.sswap.impl.empire.model.RIGImpl;
import info.sswap.impl.empire.model.RQGImpl;
import info.sswap.impl.empire.model.RRGImpl;
import info.sswap.impl.empire.model.ReasoningServiceImpl;
import info.sswap.impl.empire.model.ResourceImpl;
import info.sswap.impl.empire.model.SourceModel;
import info.sswap.impl.empire.model.SourceModelImpl;
import info.sswap.impl.empire.model.TranslatedSubjectImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.ClassClassPath;
import javassist.ClassPool;

import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.PelletOptions.MonitorType;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Provides implementation of the API that is called by SSWAP factory class. This class is implemented as a singleton.
 * Normal access to the instance is provided via the get() static method.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class APIProviderImpl implements APIProvider {
	/**
	 * The instance singleton instance of this class.
	 */
	private static final APIProviderImpl instance = new APIProviderImpl();

	/**
	 * Private constructor (following singleton design pattern).
	 */
	private APIProviderImpl() {
		// turn off the classification/realization progress messages printed by Pellet to standard output
		PelletOptions.USE_CLASSIFICATION_MONITOR = MonitorType.NONE;
		
		// Fix that is required for Javassist within Empire to be able to load all our classes
		// Javassist by default only loads classes from the class loader for the JVM, which
		// won't work in a servlet container, which uses its own class loaders
		// to load the classes of the application
		ClassPool.getDefault().insertClassPath(new ClassClassPath(getClass()));
	}

	/**
	 * The getter method for the singleton instance.
	 * 
	 * @return the instance of the implementation
	 */
	public static APIProviderImpl get() {
		return instance;
	}

	/**
	 * @inheritDoc
	 */
	public PDG getPDG(URI uri) throws DataAccessException {
		return ImplFactory.get().createEmptySSWAPDataObject(uri, PDGImpl.class);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPProvider createProvider(URI uri) throws DataAccessException {
		// since Providers do not exist on their own, but their description is always within a PDG,
		// we have to get the PDG first
		PDGImpl pdg = ImplFactory.get().createEmptySSWAPDataObject(uri, PDGImpl.class);

		ProviderImpl provider = ImplFactory.get().createEmptySSWAPDataObject(uri, ProviderImpl.class);
		provider.setSourceModel(pdg);

		return provider;
	}

	/**
	 * @inheritDoc
	 */
	public RDG getRDG(URI uri) throws DataAccessException {
		RDG result = ImplFactory.get().createEmptySSWAPDataObject(uri, RDGImpl.class);
		
		if (!result.isDereferenced()) {
			result.dereference();
		}
		
		return result;
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPResource createResource(URI uri) throws DataAccessException {
		RDGImpl rdg = ImplFactory.get().createEmptySSWAPDataObject(uri, RDGImpl.class);

		ResourceImpl resource = ImplFactory.get().createEmptySSWAPDataObject(uri, ResourceImpl.class);
		resource.setSourceModel(rdg);

		return resource;
	}

	/**
	 * @inheritDoc
	 */
	public RDG createRDG(URI resourceURI, String name, String oneLineDescription, URI providerURI) throws DataAccessException {
		RDGImpl rdg = ImplFactory.get().createEmptySourceModel(resourceURI, RDGImpl.class);

		ResourceImpl resource = (ResourceImpl) rdg.createResource(resourceURI);		
		resource.setName(name);
		resource.setOneLineDescription(oneLineDescription);
		
		ProviderImpl provider = ImplFactory.get().createDependentObject(rdg, providerURI, ProviderImpl.class);
		resource.setProvider(provider);

		return rdg;
	}

	/**
	 * @inheritDoc
	 */
	public RQG getRQG(InputStream is) throws DataAccessException {
		return getResourceGraph(is, RQG.class, null);
	}
	
	/**
	 * @inheritDoc
	 */
	public RQG createRQG(URI resourceURI) throws DataAccessException {
		if (resourceURI == null) {
			resourceURI = URI.create(ModelUtils.generateBNodeId());
		}
		
		RQGImpl rqg = ImplFactory.get().createEmptySourceModel(resourceURI, RQGImpl.class);
		
		rqg.createResource(resourceURI);
		
		return rqg;
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPDocument createSSWAPDocument(URI uri) throws DataAccessException {
		if (uri == null) {
			uri = URI.create(ModelUtils.generateBNodeId());
		}
		
		return ImplFactory.get().createEmptySourceModel(uri, SourceModelImpl.class);
	}

    /**
     * @inheritDoc
     */
    public PDG createPDG(URI providerURI, String name, String oneLineDescription) throws DataAccessException {
		PDGImpl pdg = ImplFactory.get().createEmptySourceModel(providerURI, PDGImpl.class);

		SSWAPProvider provider = pdg.createProvider(providerURI);
		provider.setName(name);
		provider.setOneLineDescription(oneLineDescription);
	
		return pdg;
    }

    /**
     * Gets the implementation of a specific SSWAPProtocol interface
     * 
     * @param <S> the interface 
     * @param interfaceClass the interface class
     * @return the class of the implementation
     */
    private <S extends SSWAPDocument> Class<? extends SourceModelImpl> getImplementationClass(Class<S> interfaceClass) {
    	if (RDG.class.equals(interfaceClass)) {
    		return RDGImpl.class;
    	}
    	else if (RIG.class.equals(interfaceClass)) {
    		return RIGImpl.class;
    	}
    	else if (RRG.class.equals(interfaceClass)) {
    		return RRGImpl.class;
    	}
    	else if (RQG.class.equals(interfaceClass)) {
    		return RQGImpl.class;
    	}
    	else if (PDG.class.equals(interfaceClass)) {
    		return PDGImpl.class;
    	}
    	else {
    		return SourceModelImpl.class;
    	}
    }
    
    /**
     * @inheritDoc
     */
    @SuppressWarnings("unchecked")
    public <T extends SSWAPDocument> T getResourceGraph(InputStream is, Class<T> clazz, URI uri) throws DataAccessException {
    	// implementationClass contains the Java Class of the specific implementation of the 
    	// requested interface
    	Class<? extends SourceModelImpl> implementationClass = getImplementationClass(clazz);
    	
    	if (uri == null) {
    		uri = URI.create(ModelUtils.generateBNodeId());
    	}
    	
    	// create the instance of the implementation class
		T result = (T) ImplFactory.get().createEmptySSWAPDataObject(uri, implementationClass);
		
		result.dereference(is);
		
		return result;
    }


    public Model asJenaModel(SSWAPModel model) throws UnsupportedOperationException {
    	if (model instanceof ModelImpl) {
    		// for implementations from this provider, we can efficiently process this using internal data structures
    		SourceModel sourceModel = ((ModelImpl) model).getSourceModel(); 
    		
    		// make sure we have source model
    		if (sourceModel != null) {
    			// if the model is the source model, we can return its Jena model directly
    			if (sourceModel == model) {
    				return sourceModel.getModel();	
    			}
    			else {
    				// else we need to compute partition and return that as a Jena model
    				return ModelUtils.partitionModel(sourceModel.getModel(), model.getURI().toString(), false);
    			}    			
    		}
    		else {
    			// if there is no source model -- then we can return an empty Jena model
    			return JenaModelFactory.get().createEmptyModel();
    		}
    	}
    	else {
    		// we were passed a model created by a different implementation. Technically, we could throw
    		// UnsupportedOperationException, but in this case, we can resort to slow-but-tried method
    		// of serializing that model and reading it back into a Jena model
    		
    		try {
    			ByteArrayOutputStream bos = new ByteArrayOutputStream();    		
    			model.serialize(bos);
    			
    			ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());    			
    			bos.close();
    			
    			Model jenaModel = JenaModelFactory.get().createEmptyModel();
    			jenaModel.read(bis, "RDF/XML");    			
    			bis.close();
    			
    			return jenaModel;
    		}
    		catch (IOException e) {
    			throw new UnsupportedOperationException("We cannot convert this model into Jena model because it was created by a different implementation");
    		}
    	}
    }

    public SSWAPDocument getInferredABox(SSWAPDocument document) throws UnsupportedOperationException {
    	if (document instanceof SourceModelImpl) {
    		return ((SourceModelImpl) document).getInferredABox();
    	}
    	
    	throw new UnsupportedOperationException("This document was created by a different API implementation");
    }

    public SSWAPDocument getInferredTBox(SSWAPDocument document) throws UnsupportedOperationException {
    	if (document instanceof SourceModelImpl) {
    		return ((SourceModelImpl) document).getInferredTBox();
    	}
    	
    	throw new UnsupportedOperationException("This document was created by a different API implementation");
    }

    public void setMaxClosureBytes(SSWAPModel model, long byteLimit) throws UnsupportedOperationException {
		if (model instanceof ModelImpl) {
			SourceModel sourceModel = ((ModelImpl) model).getSourceModel();
			
			if (sourceModel != null) {
				sourceModel.setMaxClosureBytes(byteLimit);
			}
		}
		else {
			throw new UnsupportedOperationException("This model was created by a different API implementation");
		}
    }

    public void setMaxClosureThreads(SSWAPModel model, int threads) throws UnsupportedOperationException {
		if (model instanceof ModelImpl) {
			SourceModel sourceModel = ((ModelImpl) model).getSourceModel();
			
			if (sourceModel != null) {
				sourceModel.setMaxClosureThreads(threads);
			}
		}
		else {
			throw new UnsupportedOperationException("This model was created by a different API implementation");
		}    
	}

    public void setMaxClosureTime(SSWAPModel model, long timeLimit) throws UnsupportedOperationException {
		if (model instanceof ModelImpl) {
			SourceModel sourceModel = ((ModelImpl) model).getSourceModel();
			
			if (sourceModel != null) {
				sourceModel.setMaxClosureTime(timeLimit);
			}
		}
		else {
			throw new UnsupportedOperationException("This model was created by a different API implementation");
		}    
	}

    public <T extends SSWAPDocument> T createDocument(Model model, Class<T> clazz) throws UnsupportedOperationException {
    	return createDocument(model, clazz, null);
    }
    
    @SuppressWarnings("unchecked")	// for createEmptySSWAPDataObject()
	public <T extends SSWAPDocument> T createDocument(Model model, Class<T> clazz, URI uri) throws UnsupportedOperationException {
    	// implementationClass contains the Java Class of the specific implementation of the 
    	// requested interface
    	Class<? extends SourceModelImpl> implementationClass = getImplementationClass(clazz);
    	
    	if (uri == null) {
    		uri = URI.create(ModelUtils.generateBNodeId());
    	}
    	
    	// create the instance of the implementation class
		T result = (T) ImplFactory.get().createEmptySSWAPDataObject(uri, implementationClass);
		
		Map<String,String> nsPrefixMap = model.getNsPrefixMap();
		
		((SourceModelImpl) result).dereference(model);
		
		for (String prefix : nsPrefixMap.keySet()) {
			try {
	            result.setNsPrefix(prefix, new URI(nsPrefixMap.get(prefix)));
            }
            catch (URISyntaxException e) {
            	// ignoring prefixes that are not legal URIs
            }
		}		
		
		return result;
    }

    public SSWAPDocument getClosureDocument(SSWAPDocument document) throws UnsupportedOperationException {
		if (document instanceof SourceModelImpl) {
			SourceModelImpl sourceModel = (SourceModelImpl) document;
			
			Model baseModel = sourceModel.getModel();
			sourceModel.doClosure();
			Model closureModel = sourceModel.getClosureModel();
			
			Model resultModel = JenaModelFactory.get().createEmptyModel();
			resultModel.add(baseModel);
			resultModel.add(closureModel);
			
			return createDocument(resultModel, SourceModelImpl.class, document.getURI());
		}
		else {
			throw new UnsupportedOperationException("This document was created by a different API implementation");
		}
    }

    public SSWAPDocument getInferredDocument(SSWAPDocument document) throws UnsupportedOperationException {
		if (document instanceof SourceModelImpl) {
			SourceModelImpl sourceModel = (SourceModelImpl) document;
			ReasoningServiceImpl reasoningService = (ReasoningServiceImpl) sourceModel.getReasoningService();
			Model inferredModel = reasoningService.extractInferredModel();
						
			return createDocument(inferredModel, SourceModelImpl.class, document.getURI());
		}
		else {
			throw new UnsupportedOperationException("This document was created by a different API implementation");
		}
    }

	@Override
    public void setExplanationSyntax(String explanationSyntax) throws UnsupportedOperationException {
	    if ("RDF/XML".equals(explanationSyntax) || ("RDF/XML-ABBREV".equals(explanationSyntax))) {
	    	ReasoningServiceImpl.EXPLANATION_SYNTAX = ReasoningServiceImpl.ExplanationSyntax.RDFXML;
	    }
	    else if ("TURTLE".equals(explanationSyntax)) {
	    	ReasoningServiceImpl.EXPLANATION_SYNTAX = ReasoningServiceImpl.ExplanationSyntax.TURTLE;
	    }
	    else if ("PELLET".equals(explanationSyntax)) {
	    	ReasoningServiceImpl.EXPLANATION_SYNTAX = ReasoningServiceImpl.ExplanationSyntax.PELLET;	    
	    }
	    else {
	    	ReasoningServiceImpl.EXPLANATION_SYNTAX = ReasoningServiceImpl.ExplanationSyntax.RDFXML;
	    }
    }
	
	public RQG generateRQG(RDG upstreamService, RDG downstreamService) throws UnsupportedOperationException {
		return generateRQG(upstreamService, downstreamService, null);
	}
	
	public RQG generateRQG(RDG upstreamService, RDG downstreamService, URI resultURI) throws UnsupportedOperationException {
		RQG rqg = this.createRQG(resultURI);
		ExtensionAPI.setValueValidation(rqg, false /* enabled */);
				
		if (upstreamService != null) {
			for (String importURI : upstreamService.getImports()) {
				try {
	                rqg.addImport(new URI(importURI));
                }
                catch (URISyntaxException e) {
                	// ignored
                }
			}
		}
		
		if (downstreamService != null) {
			for (String importURI : downstreamService.getImports()) {
				try {
	                rqg.addImport(new URI(importURI));
                }
                catch (URISyntaxException e) {
                	// ignored
                }
			}
		}
		
		SSWAPResource rqgResource = rqg.getResource();
		SSWAPGraph rqgGraph = rqg.createGraph();
		
		if (upstreamService != null) {
			rqgGraph.setSubjects(generateRQGSubjects(upstreamService, downstreamService, rqg));
		}
		else {
			SSWAPSubject rqgSubject = rqg.createSubject();
			
			if (downstreamService != null) {
				rqgSubject.setObjects(generateRQGObjects(downstreamService, rqg));
			}
			else {
				SSWAPObject rqgObject = rqg.createObject();				
				rqgSubject.setObject(rqgObject);
			}
			
			rqgGraph.setSubject(rqgSubject);
		}
		
		rqgResource.setGraph(rqgGraph);
		
		ExtensionAPI.setValueValidation(rqg, true /* enabled */);
		
		return rqg;
	}
	
	private Collection<SSWAPSubject> generateRQGSubjects(RDG upstreamService, RDG downstreamService, RQG rqg) {
		List<SSWAPSubject> result = new LinkedList<SSWAPSubject>();
		
		Set<String> sswapObjectTypes = new HashSet<String>();
		sswapObjectTypes.add(Vocabulary.SSWAP_OBJECT.toString());		
		
		for (SSWAPGraph upstreamGraph : upstreamService.getResource().getGraphs()) {
			for (SSWAPSubject upstreamSubject : upstreamGraph.getSubjects()) {
				for (SSWAPObject upstreamObject : upstreamSubject.getObjects()) {
					SSWAPSubject rqgSubject = rqg.createSubject();
					
					ImplFactory.get().copy(upstreamObject, rqgSubject, rqg, sswapObjectTypes);
					
					if (downstreamService != null) {
						rqgSubject.setObjects(generateRQGObjects(downstreamService, rqg));
					}
					else {
						SSWAPObject rqgObject = rqg.createObject();				
						rqgSubject.setObject(rqgObject);						
					}
					
					result.add(rqgSubject);
				}
			}
		}
		
		return result;
	}

	private Collection<SSWAPObject> generateRQGObjects(RDG downstreamService, RQG rqg) {
		List<SSWAPObject> result = new LinkedList<SSWAPObject>();
		
		Set<String> sswapSubjectTypes = new HashSet<String>();
		sswapSubjectTypes.add(Vocabulary.SSWAP_SUBJECT.toString());
		
		for (SSWAPGraph downstreamGraph : downstreamService.getResource().getGraphs()) {
			for (SSWAPSubject downstreamSubject : downstreamGraph.getSubjects()) {
				SSWAPObject rqgObject = rqg.createObject();
				
				ImplFactory.get().copy(downstreamSubject, rqgObject, rqg, sswapSubjectTypes);
				
				result.add(rqgObject);
			}
		}
		
		return result;
	}
	

	public RIG getAsyncRIG(URI serviceURI, URI upstreamRRG) throws DataAccessException {
		RDG rdg = null;
		
		try {
			rdg = SSWAP.getRDG(serviceURI);
		}
		catch (DataAccessException e) {
			throw new DataAccessException(String.format("Unable to get RDG for the service %s: %s", serviceURI.toString(), e.getMessage()));
		}
		
		RIG rig = rdg.getRIG();
		
		SSWAPResource resource = rig.getResource();
		
		SSWAPGraph graph = rig.createGraph();
		
		SSWAPSubject subject = rig.createSubject(upstreamRRG);
		subject.addType(rig.getType(URI.create(Vocabulary.ASYNC_RRG.toString())));
		
		SSWAPObject object = rig.createObject();
		object.addType(rig.getType(URI.create(Vocabulary.ASYNC_RRG.toString())));
		
		subject.setObject(object);
		graph.setSubject(subject);
		resource.setGraph(graph);
		
		return rig;
	}
	
	public SSWAPElement copyElement(SSWAPDocument dstDocument, SSWAPElement element) throws UnsupportedOperationException {
		if (dstDocument instanceof SourceModel) {
			SourceModel dstModel = (SourceModel) dstDocument;
			boolean valueValidationCopy = dstModel.isValueValidationEnabled();
			dstModel.setValueValidationEnabled(false);
			
			try {
				return ImplFactory.get().deepCopyElement(dstModel, element, new HashSet<URI>());
			}
			finally {
				dstModel.setValueValidationEnabled(valueValidationCopy);
			}
		}
		
		throw new UnsupportedOperationException("The destination document has not been created by this API implementation");
	}
	
	@Override
	public Cache getCache() {
		return ClosureBuilderFactory.getDefaultModelCache();
	}

	@Override
    public void setValueValidation(SSWAPDocument document, boolean enabled) throws UnsupportedOperationException {
	    if (document instanceof SourceModel) {
	    	((SourceModel) document).setValueValidationEnabled(enabled);
	    }
	    else {
	    	throw new UnsupportedOperationException("The document has not been created by this API implementation");	
	    }	    
    }
	
	@Override
	public void setClosureEnabled(SSWAPDocument document, boolean enabled) throws UnsupportedOperationException {
		if (document instanceof SourceModel) {
	    	((SourceModel) document).setClosureEnabled(enabled);
	    }
	    else {
	    	throw new UnsupportedOperationException("The document has not been created by this API implementation");	
	    }
	}

	@Override
    public Collection<String> getInferredTypeURIs(SSWAPIndividual individual) throws UnsupportedOperationException {
		if (individual instanceof IndividualImpl) {
	    	return ((ReasoningServiceImpl) individual.getReasoningService()).getInferredTypes(individual);
	    }
	    else {
	    	throw new UnsupportedOperationException("The individual has not been created by this API implementation");	
	    }
    }

	@Override
    public RDG createCompositeService(URI serviceURI, String name, String description, URI providerURI, RDG firstService, RDG lastService) throws UnsupportedOperationException {
		RDG result = SSWAP.createRDG(serviceURI, name, description, providerURI);
		
		ExtensionAPI.setValueValidation(result, false /* enabled */);
		
		SSWAPResource resource = result.getResource();
		SSWAPGraph graph = result.createGraph();
		
		if (firstService != null) {
			graph.setSubjects(generateCompositeServiceSubjects(firstService, lastService, result));
		}
		else {
			SSWAPSubject subject = result.createSubject();
			
			if (lastService != null) {
				subject.setObjects(generateCompositeServiceObjects(lastService, result));
			}
			else {
				SSWAPObject object = result.createObject();				
				subject.setObject(object);
			}
			
			graph.setSubject(subject);
		}
		
		resource.setGraph(graph);
		
		ExtensionAPI.setValueValidation(result, true /* enabled */);
	    return result;
    }
		
	private Collection<SSWAPSubject> generateCompositeServiceSubjects(RDG firstService, RDG lastService, RDG result) {
		Set<String> ignoredTypes = new HashSet<String>();
		ignoredTypes.add(Vocabulary.SSWAP_OBJECT.toString());
		ignoredTypes.add(Vocabulary.SSWAP_SUBJECT.toString());
		ignoredTypes.add(Vocabulary.SSWAP_GRAPH.toString());
		ignoredTypes.add(Vocabulary.SSWAP_RESOURCE.toString());
		
		List<SSWAPSubject> subjects = new LinkedList<SSWAPSubject>();
				
		for (SSWAPGraph firstServiceGraph : firstService.getResource().getGraphs()) {
			for (SSWAPSubject firstServiceSubject : firstServiceGraph.getSubjects()) {
				SSWAPSubject resultSubject = null;
				
				if (firstServiceSubject.isAnonymous()) {
					resultSubject = result.createSubject();
				}
				else {
					resultSubject = result.createSubject(firstServiceSubject.getURI());
				}
				
				ImplFactory.get().copy(firstServiceSubject, resultSubject, result, ignoredTypes);
				
				if (lastService != null) {
					resultSubject.setObjects(generateCompositeServiceObjects(lastService, result));
				}
				else {
					resultSubject.setObject(result.createObject());
				}
					
				subjects.add(resultSubject);
			}
		}
		
		return subjects;
	}
	
	private Collection<SSWAPObject> generateCompositeServiceObjects(RDG lastService, RDG result) {
		Set<String> ignoredTypes = new HashSet<String>();
		ignoredTypes.add(Vocabulary.SSWAP_OBJECT.toString());
		ignoredTypes.add(Vocabulary.SSWAP_SUBJECT.toString());
		ignoredTypes.add(Vocabulary.SSWAP_GRAPH.toString());
		ignoredTypes.add(Vocabulary.SSWAP_RESOURCE.toString());

		List<SSWAPObject> objects = new LinkedList<SSWAPObject>();
		
		for (SSWAPGraph lastServiceGraph : lastService.getResource().getGraphs()) {
			for (SSWAPSubject lastServiceSubject : lastServiceGraph.getSubjects()) {
				for (SSWAPObject lastServiceObject : lastServiceSubject.getObjects()) {
					SSWAPObject resultObject = null;
					
					if (lastServiceObject.isAnonymous()) {
						resultObject = result.createObject();
					}
					else {
						resultObject = result.createObject(lastServiceObject.getURI());
					}
				
					ImplFactory.get().copy(lastServiceObject, resultObject, result, ignoredTypes);
				
					objects.add(resultObject);
				}
			}
		}
		
		return objects;
	}
	
	protected <T extends SSWAPDocument> T clone(T original, Class<T> clazz) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();		
		original.serialize(bos);
		
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		
		return SSWAP.getResourceGraph(bis, clazz, original.getURI());
	}
	
	private void clearIndividual(SSWAPIndividual individual) {
		for (SSWAPType type : new LinkedList<SSWAPType>(individual.getDeclaredTypes())) {
			individual.removeType(type);
		}
		
		for (SSWAPProperty property : new LinkedList<SSWAPProperty>(individual.getProperties())) {
			individual.removeProperty(property);
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
    public RQG inputOnlyRQG(RQG rqg) throws UnsupportedOperationException {
		RQG result = clone(rqg, RQG.class);
		
		for (SSWAPGraph graph : result.getResource().getGraphs()) {
			for (SSWAPSubject subject : graph.getSubjects()) {
				for (SSWAPObject object : subject.getObjects()) {
					clearIndividual(object);
				}
			}
		}
		
	    return result;
    }
	
	
	/**
	 * @inheritDoc
	 */
	@Override
    public RQG outputOnlyRQG(RQG rqg) throws UnsupportedOperationException {
		RQG result = clone(rqg, RQG.class);
		
		for (SSWAPGraph graph : result.getResource().getGraphs()) {
			for (SSWAPSubject subject : graph.getSubjects()) {
				clearIndividual(subject);
			}
		}
		
	    return result;
    }
	
	@Override
	public boolean isUnrestricted(RQG rqg) throws UnsupportedOperationException {
		for (SSWAPGraph graph : rqg.getResource().getGraphs()) {
			for (SSWAPSubject subject : graph.getSubjects()) {
				if (!isEmptySubject(subject)) {
					return false;
				}
				
				for (SSWAPObject object : subject.getObjects()) {
					if (!isEmptyObject(object)) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	private static boolean isEmptySubject(SSWAPSubject subject) {
		return !subject.getType().isStrictSubTypeOf(subject.getDocument().getType(URI.create(Vocabulary.SSWAP_SUBJECT.toString())));
	}
	
	private static boolean isEmptyObject(SSWAPObject object) {
		return !object.getType().isStrictSubTypeOf(object.getDocument().getType(URI.create(Vocabulary.SSWAP_OBJECT.toString())));
	}

	@SuppressWarnings("unchecked")
    @Override
    public <T extends SSWAPNode> T getUntranslatedNode(T translatedNode) throws UnsupportedOperationException {
		if (translatedNode instanceof TranslatedSubjectImpl) {
			SSWAPSubject originalSubject = ((TranslatedSubjectImpl) translatedNode).getOriginalSubject();
			
			return (T) ((originalSubject == null)? translatedNode 
							                     : originalSubject);
		}
		else if (translatedNode instanceof ResourceImpl) {
			SSWAPResource originalResource = ((ResourceImpl) translatedNode).getOriginalResource();
			
			return (T) ((originalResource == null)? translatedNode
							                      : originalResource);
		}
		
	    return translatedNode;
    }
}
