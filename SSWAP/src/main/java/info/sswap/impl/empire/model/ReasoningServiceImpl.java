/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.Config;
import info.sswap.api.model.ModelResolver;
import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.ReasoningService;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPModel;
import info.sswap.api.model.SSWAPNode;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPType;
import info.sswap.api.model.ValidationException;
import info.sswap.api.spi.ExtensionAPI;
import info.sswap.impl.empire.Namespaces;
import info.sswap.impl.empire.Vocabulary;
import info.sswap.impl.empire.io.ClosureModelResolver;
import info.sswap.impl.empire.model.ProtocolImpl.MappingValidator.MappingType;
import info.sswap.ontologies.modularity.ModularityModelResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mindswap.pellet.DependencySet;
import org.mindswap.pellet.Edge;
import org.mindswap.pellet.Individual;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.Node;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.exceptions.InconsistentOntologyException;
import org.mindswap.pellet.jena.JenaUtils;
import org.mindswap.pellet.jena.PelletInfGraph;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.jena.vocabulary.OWL2;
import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;

import com.google.common.collect.Lists;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Implementation of the reasoning service for a Jena model.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class ReasoningServiceImpl implements ReasoningService {
	
	private static final Logger LOGGER = LogManager.getLogger(ReasoningServiceImpl.class);
		
	public static ExplanationSyntax EXPLANATION_SYNTAX = ExplanationSyntax.RDFXML;
	
	/**
	 * A set of URIs of properties that should not be followed during translation/validation process (essentially an exclusion list) 
	 * 
	 * Reasons why the predicates are included:
	 * 
	 * - sswap:providedBy - in a typical RDG/RIG/RRG it points to a provider individual, but that provider individual does not contain (in RDG/RIG/RRG) any
	 *                      of the required properties for SSWAPProvider (e.g., name). Moreover, closure does not dereference individuals, so we won't get
	 *                      that information. This would cause any translation to fail, so we are making an exception for sswap:providedBy
	 * 
	 * - sswap:operatesOn - prevents translation of sswap:Resource from proceeding too far (onto sswap:Graph); the elements in the graph (e.g., sswap:Subject)
	 *                      are translated separately
	 *                      
	 * - sswap:mapsTo     - prevents translation of sswap:Subject from proceeding too far (to sswap:Object); objects are currently not translated
	 */
	private static final Set<String> TRANSLATION_NO_FOLLOW_PROPS = new HashSet<String>(Arrays.asList( Vocabulary.PROVIDED_BY.toString(), Vocabulary.MAPS_TO.toString(), Vocabulary.OPERATES_ON.toString() ));
	
	/**
	 * Controls whether reasoning service should retrieve terms (esp. properties and named classes), it does not know about.
	 */
	private boolean automaticTermRetrieval = true;
	
	/**
	 * Controls whether reasoning service should monitor attempts to use terms from a different source model, and try to temporarily
	 * retrieve information from that different model to answer the query.
	 */
	private boolean crossDocumentTermRetrieval = true;
	
	/**
	 * The Jena OntModel created from the regular Jena model.
	 */
	private final OntModel ontModel;
	
	/**
	 * The source models for which this reasoning service operates. This lists is guaranteed to be non-empty at all 
	 * times and the first entry is always the original source model used to create this reasoning service. 
	 */
	private final List<SourceModel> sourceModels;
	
	/**
	 * Temporarily added dependent SSWAPDocuments (e.g., if a user used a term from a different document in a query)
	 * The keys are the SSWAPDocuments, and the values are Jena Models (with TBox of that SSWAPDocument) that have been temporarily
	 * added as submodels to this ontModel.
	 */
	private Map<SSWAPDocument,Model> crossDocumentDependencies = new IdentityHashMap<SSWAPDocument,Model>();
	
	/**
	 * A cache for property information so that we do not have repeatedly query the reasoner while validating values.
	 * 
	 * The keys are URIs of the properties (as strings), and the values are PropertyInformation entries
	 */
	private Map<String,PropertyInformation> propertyInformation = new HashMap<String,PropertyInformation>();
	
	/**
	 * Used to resolve externally defined terms
	 */
	private final ModelResolver mResolver = Boolean.valueOf(Config.get().getProperty(Config.MODULE_EXTRACTION_ENABLED_KEY)) 
											? new ModularityModelResolver()
											: new ClosureModelResolver();
	
	
	/**
	 * Creates a reasoning service based on Pellet reasoner. It also creates a corresponding, internal ontology model
	 * (which triggers classification, and it may take some time).
	 * 
	 * @param sourceModel
	 *           the Jena model to be reasoned over
	 */
	
	/* (old javadocs):
	 * 
	 * @param baseModel
	 *            the Jena model containing the raw (original) set of statements
	 * @param closureModel
	 *            the associated Jena model containing statements derived from performing a closure URIs in the base model
	 */
	public ReasoningServiceImpl(SourceModel sourceModel) {
		// Workaround for an issue with incremental operation of the reasoner: 
		// if set to true (default), some of the updates to the submodels of the OntModel do not propagate in
		// time, and can cause random, non-deterministic InconsistentOntologyExceptions
		PelletOptions.USE_TRACING = true;
		PelletOptions.PROCESS_JENA_UPDATES_INCREMENTALLY = false;
		
		sourceModels = Lists.newArrayList();
		
		ontModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		ontModel.getDocumentManager().setProcessImports(false);
		
		PelletInfGraph pellet = (PelletInfGraph) ontModel.getGraph();
		
		// required to ensure that the changes from the submodels propagate to the OntModel
		pellet.setAutoDetectChanges(true);
		
		addSourceModel(sourceModel);
	}
	
	public void setAutoDetectChanges(boolean autodetect) {
		((PelletInfGraph) ontModel.getGraph()).setAutoDetectChanges(autodetect);
	}
	
	private boolean containsSourceModel(SSWAPDocument sourceModel) {
		for (SourceModel model : sourceModels) {
			if (sourceModel == model) {
				return true;
			}
		}
		
		return false;
	}

	private void addSourceModel(SourceModel sourceModel) {		
		if (containsSourceModel(sourceModel)) {
			return;
		}
		
		sourceModels.add(sourceModel);
		
		try {
			Model closureModel = sourceModel.getClosureModel();
			if (closureModel != null) {
				ontModel.addSubModel(closureModel);
			}

			Model jenaModel = sourceModel.getModel();
			if (jenaModel == null) {
				throw new IllegalArgumentException("The SSWAP model does not have an associated Jena model");
			}
			
			ontModel.addSubModel(jenaModel); 
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);
		}
	}

	private void removeSourceModel(SourceModel sourceModel) {
		try {
			if (sourceModel == sourceModels.get(0)) {
				return;
			}
			
			for (Iterator<SourceModel> it = sourceModels.iterator(); it.hasNext(); ) {
				SourceModel subModel = it.next();
				
				if (subModel == sourceModel) {
					it.remove();
					return;
				}
			}
			
			Model closureModel = sourceModel.getClosureModel();
			if (closureModel != null) {
				ontModel.removeSubModel(closureModel);
			}

			Model jenaModel = sourceModel.getModel();
			if (jenaModel == null) {
				throw new IllegalArgumentException("The SSWAP model does not have an associated Jena model");
			}
			
			ontModel.removeSubModel(jenaModel); 
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);
		}
	}
	
	public void setAutomaticTermRetrieval(boolean automaticTermRetrieval) {
		this.automaticTermRetrieval = automaticTermRetrieval;
	}
	
	public boolean isAutomaticTermRetrieval() {
		return automaticTermRetrieval;
	}
	
	public void setCrossDocumentTermRetrieval(boolean crossDocumentTermRetrieval) {
		this.crossDocumentTermRetrieval = crossDocumentTermRetrieval;
	}
	
	public boolean isCrossDocumentTermRetrieval() {
		return crossDocumentTermRetrieval;
	}
			
	/**
	 * Verifies the consistency of the underlying ontologies (together). If the method
	 * returns without throwing an exception, the ontologies are considered consistent 
	 * 
	 * @throws ValidationException if the ontologies are not consistent
	 */
	public void validateConsistency() throws ValidationException {
		try {
			ontModel.prepare();
			PelletInfGraph pellet = (PelletInfGraph) ontModel.getGraph();
			// try to query the underlying ontModel to check inconsistency
			if (!pellet.isConsistent()) {
				// if the check succeeds, we have inconsistency
				throw new ValidationException("The document is inconsistent (owl:Thing rdfs:subClassOf owl:Nothing)");
			}
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);
		}
	}
	
	private void handleInconsistentOntologyException(InconsistentOntologyException e) throws InconsistentOntologyException {
		if (ExplanationSyntax.PELLET.equals(EXPLANATION_SYNTAX)) {
			throw e;
		}
		
		throw new InconsistentOntologyException("The ontology is inconsistent. The set of statements capturing the inconsistency is:\n\n" + renderInconsistencyExplanation());
	}
	
	private String renderInconsistencyExplanation() {		
		PelletInfGraph pellet = (PelletInfGraph) ontModel.getGraph();
			
		Model explanationModel = pellet.explainInconsistency();
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		RDFRepresentation representation = RDFRepresentation.RDF_XML;
		
		if (ExplanationSyntax.TURTLE.equals(EXPLANATION_SYNTAX)) {
			representation = RDFRepresentation.TURTLE;
		}
		
		ModelUtils.serializeModel(explanationModel, os, representation, false /* commentedOutput */);
		
		String result = new String(os.toByteArray());
		
		try {
	        os.close();
        }
        catch (IOException e) {
        	// nothing
        }
		
		return result;
	}
	
	/**
	 * Retrieves additional term (by dereferencing it). Note: this method will return an empty model, if a URI of a bnode is given
	 * (no dereferencing possible)
	 * 
	 * @param termURI the URI of the term to be retrieved.
	 * @return new closure model for the term
	 */
	private Model retrieveAdditionalTerm(URI termURI) {		
		if (!ModelUtils.isBNodeURI(termURI.toString())) {
			//System.out.println("+++ Resolving the term " + termURI);
			
			return mResolver.resolveTerm(termURI);
		}
		else {
			return JenaModelFactory.get().createEmptyModel();
		}			
	}
		
	/**
	 * Checks whether we know anything about the given term (i.e., whether it is used anywhere in the ontModel).
	 * The assumption here is that if the term is mentioned, we know the definition of this term (based on the fact
	 * that closure should have retrieved it in such a case, or it is an individual).
	 * 
	 * @param termURI the URI of the term that should be checked
	 * @return true if the term is used anywhere
	 */
	private boolean isTermKnown(URI termURI) {
		if (isBuiltInVocabulary(termURI.toString())) {
			return true;
		}
		
		Resource termResource = ontModel.getResource(termURI.toString());
		
		return ontModel.containsResource(termResource);	
	}
	
	/**
	 * Ensures that we know the definition of the term (i.e., if the term is not known, we try to dereference it)
	 * 
	 * @param termURI the URI of the term
	 */
	private void assertTermKnown(URI termURI) {
		// TODO: caching of terms we already know
		// can ever terms cease to be known (if no, caching is easy; if yes, cache invalidation would become an issue)
		if (automaticTermRetrieval  && !isTermKnown(termURI)) {
			Model additionalTerm = retrieveAdditionalTerm(termURI);
			
			if (additionalTerm != null) {
				
				ontModel.add(additionalTerm, true);
			}
		}		
	}
	
	/**
	 * Ensures that we know all the terms from another document (source model) of the passed model(i.e., separate closure etc), if it happens to 
	 * be passed to this reasoning service (can happen for two-parameter methods in reasoning service like isSubTypeOf, 
	 * where one of the arguments (typically the second one) belongs to a different model). 
	 * 
	 * @param sswapModel the term passed to a method where cross-document query is possible. If the document of this model is different
	 * than the document for this reasoning service, this method will add the document to crossDocumentDependencies (remember to use 
	 * releaseCrossModelTerms later!)
	 */
	private void assertCrossModelTerms(SSWAPModel sswapModel) {
		if (!crossDocumentTermRetrieval) {
			return;
		}
		
		SSWAPDocument sourceDocument = sswapModel.getDocument();
		
		// only add the document if it is different than the current document and it is not there already
		if (!containsSourceModel(sourceDocument) && !crossDocumentDependencies.containsKey(sourceDocument)) {
			// extract TBox of the closure of the other document
			SSWAPDocument closureDocument = ExtensionAPI.getClosureDocument(sourceDocument);
			
			Model closureModel = ExtensionAPI.asJenaModel(closureDocument);
			
			Model tboxModel = extractTBox(closureModel);
						
			// add TBox as a submodel of the OntModel (we are only adding TBox to minimize risk of inconsistent ontologies,
			// and ABox is the most typical place for inconsistency; esp. that cross-document queries typically occur between
			// RDG and RIG/RRG which contain sswap:Resources with the same URI and many of the properties on sswap:Resources are
			// functional properties (i.e., any difference in values for these properties between RDG and RIG/RRG will trigger
			// inconsistent ontology exception))
			ontModel.addSubModel(tboxModel, true);
			
			crossDocumentDependencies.put(sourceDocument, tboxModel);
		}
	}

	/**
	 * Releases all the terms that were temporarily imported by assertCrossModelTerms() for each model. If the given
	 * invocation of assertCrossModelTerms() did not cause any term additions (or they were already removed), the
	 * invocation of this method is harmless.
	 * 
	 * @param sswapModels variable list (array) of SSWAPModels from which to remove TBox models
	 */
	private void releaseCrossModelTerms(SSWAPModel... sswapModels) {
		if (!crossDocumentTermRetrieval) {
			return;
		}		
		
		for (SSWAPModel sswapModel : sswapModels) {
			SSWAPDocument sourceDocument = sswapModel.getDocument();
			
			Model tboxModel = crossDocumentDependencies.remove(sourceDocument);
			if (tboxModel != null) {
				ontModel.removeSubModel(tboxModel);
			}
        }
	}
	
	public boolean isSameAs(SSWAPIndividual ind1, SSWAPIndividual ind2) {
		try {
			assertCrossModelTerms(ind1);
			assertCrossModelTerms(ind2);
			
			Resource ind1Resource = ontModel.getResource(ind1.getURI().toString());
			Resource ind2Resource = ontModel.getResource(ind2.getURI().toString());
			
			Statement sameAsStatement = ontModel.createStatement(ind1Resource, OWL.sameAs, ind2Resource);
			
			return ontModel.contains(sameAsStatement);
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);
			
			return false; // this line won't be reached
		}
	}
	
	public boolean isDifferentFrom(SSWAPIndividual ind1, SSWAPIndividual ind2) {
		try {
			assertCrossModelTerms(ind1);
			assertCrossModelTerms(ind2);
			
			Resource ind1Resource = ontModel.getResource(ind1.getURI().toString());
			Resource ind2Resource = ontModel.getResource(ind2.getURI().toString());
			
			Statement differentFrom = ontModel.createStatement(ind1Resource, OWL.differentFrom, ind2Resource);
			
			return ontModel.contains(differentFrom);
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);
			
			return false; // this line won't be reached
		}
	}
	
	/**
	 * @inheritDoc
	 */
	public boolean isStrictSubTypeOf(SSWAPType sub, SSWAPType sup) {		
		try {
			// a strict sub type is a sub type of the other type, and is neither identical
			// with sup nor equivalent (subTypeOf(sub, sup) and subTypeOf(sup, sub)
			// indicates equivalent type

			// for performance reasons, we test for identical types first
			return !sub.equals(sup) && isSubTypeOf(sub, sup) && !isSubTypeOf(sup, sub);
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);
			
			return false; // this line won't be reached
		}
	}

	/**
	 * @inheritDoc
	 */
	public boolean isSubTypeOf(SSWAPType sub, SSWAPType sup) {
		try {
			boolean result = false;

			try {
				// since sub (less typically) and sup (more likely) may belong to a different SSWAPDocument
				// (cross document reasoning), assert that we know about the terms from another model
				// (if there are one)
				assertCrossModelTerms(sub);
				
				assertCrossModelTerms(sup);
				
				assertTermKnown(sub.getURI());
				assertTermKnown(sup.getURI());
				
				TypeImpl subType = (TypeImpl) sub;
				TypeImpl supType = (TypeImpl) sup;

				Statement subClassOfStatement = ontModel.createStatement(subType.getResource(), RDFS.subClassOf, supType
								.getResource());
				
				result = ontModel.contains(subClassOfStatement);
				
			}
			finally {
				// release any cross document terms, if they were imported by assertCrossModel()
				releaseCrossModelTerms(sup, sub);				
			}

			return result;

		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return false; // this line won't be reached
		}
	}

	/**
	 * @inheritDoc
	 */
	public boolean isSubPredicateOf(SSWAPPredicate sub, SSWAPPredicate sup) {
		try {
			boolean result = false;

			try {
				// since sub (less typically) and sup (more likely) may belong to a different SSWAPDocument
				// (cross document reasoning), assert that we know about the terms from another model
				// (if there are one)
				assertCrossModelTerms(sub);
				assertCrossModelTerms(sup);

				assertTermKnown(sub.getURI());
				assertTermKnown(sup.getURI());

				Resource subResource = ontModel.getResource(sub.getURI().toString());
				Resource supResource = ontModel.getResource(sup.getURI().toString());

				//System.err.println("Checking rdfs:subPropertyOf for: " + subResource.getURI() + " and " + supResource.getURI());
				
				Statement subPropertyOfStatement = ontModel.createStatement(subResource, RDFS.subPropertyOf, supResource);

				result = ontModel.contains(subPropertyOfStatement); 
			}
			finally {
				// release any cross document terms, if they were imported by assertCrossModel()
				releaseCrossModelTerms(sup, sub);
			}

			return result;
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return false; // this line won't be reached
		}
	}
	
	/**
	 * Checks whether one predicate is a strict sub predicate of another.
	 * 
	 * @param sub the candidate for the sub predicate
	 * @param sup the candidate for the super predicate
	 * @return true if sub is a strict sub predicate of sup
	 */
	public boolean isStrictSubPredicateOf(SSWAPPredicate sub, SSWAPPredicate sup) {
		try {
			return !sub.getURI().equals(sup.getURI()) && isSubPredicateOf(sub, sup) && !isSubPredicateOf(sup, sub);
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return false; // this line won't be reached
		}
	}
	
	public boolean isIntersection(String type) {
		try {
			return ontModel.contains(ontModel.getResource(type), OWL.intersectionOf, (RDFNode) null);
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);
			
			return false; // this line won't be reached
		}
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPType getDomain(SSWAPPredicate predicate) {
		try {
			assertTermKnown(predicate.getURI());

			// get the object in "property rdfs:domain ?o" query
			String domain =  getSinglePredicateInformation(predicate, RDFS.domain);

			if (domain == null) {
				return null;
			}

			return predicate.getDocument().getType(URI.create(domain));
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return null; // this line won't be reached
		}
	}
	
	public Collection<SSWAPType> getDomains(SSWAPPredicate predicate) {
		try {
			List<SSWAPType> result = new LinkedList<SSWAPType>();
			assertTermKnown(predicate.getURI());

			// get the objects in "property rdfs:domain ?o" query
			for (String domain :  getPredicateInformation(predicate, RDFS.domain)) {
				result.add(predicate.getDocument().getType(URI.create(domain)));
			}		

			return result;
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return null; // this line won't be reached
		}
	}


	/**
	 * @inheritDoc
	 */
	public String getRange(SSWAPPredicate predicate) {
		try {
			// do not perform assertTermKnown (it is quite costly) if we already have information about the 
			// property in the cache (i.e., the term is known for sure)
			if (!containsPropertyInformation(predicate)) {
				assertTermKnown(predicate.getURI());
			}
				
			// get the object in "property rdfs:range ?o" query
			return getPropertyInformation(predicate).getRange();
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return null; // this line won't be reached
		}
	}
	
	public Collection<String> getRanges(SSWAPPredicate predicate) {
		try {
			assertTermKnown(predicate.getURI());

			// get the objects in "property rdfs:range ?o" query
			return getPredicateInformation(predicate, RDFS.range);
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return null; // this line won't be reached
		}
	}
 
	private String getEnumType(Resource anonDataRange) {
		/*if (!ontModel.contains(anonDataRange, RDF.type, OWL.DataRange)) {
			return null;			
		}*/
		
		RDFNode enumList = ModelUtils.getFirstObjectValue(ontModel, anonDataRange, OWL.oneOf);
		Set<String> datatypeURIs = new HashSet<String>();
		
		if ((enumList != null) && enumList.isResource()) {
			RDFList list = ModelUtils.createRDFList(ontModel, enumList.asResource());
			
			for (RDFNode listElement : list.asJavaList()) {
				if (listElement.isLiteral()) {
					datatypeURIs.add(listElement.asLiteral().getDatatypeURI());
				}
			}
		}
		
		if (datatypeURIs.size() == 1) {
			return datatypeURIs.iterator().next();
		}
		
		return null;
	}
	
	public String getTypeForEnumRange(SSWAPPredicate predicate) {
		try {
			assertTermKnown(predicate.getURI());

			Resource resource = ontModel.getResource(predicate.getURI().toString());

			StmtIterator it = ontModel.listStatements(resource, RDFS.range, (RDFNode) null);

			while (it.hasNext()) {
				Statement statement = it.next();

				if (statement.getObject().isResource() && statement.getObject().isAnon()) {
					Resource anonRange = statement.getObject().asResource();

					String type = getEnumType(anonRange);

					if (type != null) {
						return type;
					}
				}
			}

			return null;
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return null; // this line won't be reached
		}
	}
	
	/**
	 * @inheritDoc
	 */
	public boolean isObjectPredicate(SSWAPPredicate predicate) {
		try {
			// do not perform assertTermKnown (it is quite costly) if we already have information about the 
			// property in the cache (i.e., the term is known for sure)
			if (!containsPropertyInformation(predicate)) {
				assertTermKnown(predicate.getURI());
			}

			// check whether the ontModel contains the following triple
			// property rdf:type owl:ObjectProperty
			return (getPropertyInformation(predicate).getType() == PropertyType.OBJECT);			
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return false; // this line won't be reached
		}
	}

	/**
	 * @inheritDoc
	 */
	public boolean isDatatypePredicate(SSWAPPredicate predicate) {
		try {
			// do not perform assertTermKnown (it is quite costly) if we already have information about the 
			// property in the cache (i.e., the term is known for sure)
			if (!containsPropertyInformation(predicate)) {
				assertTermKnown(predicate.getURI());
			}
			
			// property rdf:type owl:DatatypeProperty
			return (getPropertyInformation(predicate).getType() == PropertyType.DATATYPE);
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return false; // this line won't be reached
		}
	}
	
	public boolean isAnnotationPredicate(SSWAPPredicate predicate) {
		try {
			// do not perform assertTermKnown (it is quite costly) if we already have information about the 
			// property in the cache (i.e., the term is known for sure)
			if (!containsPropertyInformation(predicate)) {
				assertTermKnown(predicate.getURI());
			}

			// property rdf:type owl:AnnotationProperty
			return getPropertyInformation(predicate).isAnnotation();
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return false; // this line won't be reached
		}
	}
	
	/**
	 * Gets information about the property. If the property is already in the cache, the cached information is returned.
	 * In case of a cache miss, we fetch the information from the reasoner, and store it in the cache before
	 * returning it from this method 
	 * 
	 * @param predicate the predicate describing the information requested
	 * @return the property information (should not be null)
	 */
	private synchronized PropertyInformation getPropertyInformation(SSWAPPredicate predicate) {
		PropertyInformation result = propertyInformation.get(predicate.getURI().toString());
		
		if (result == null) {
			result = createPropertyInformation(predicate);
			propertyInformation.put(predicate.getURI().toString(), result);			
		}
		
		return result;
	}	
	
	/**
	 * Checks whether this reasoning service has already cached information about the particular property/predicate
	 * 
	 * @param predicate the predicate that should be checked
	 * @return true, if there is information about this property in the cache, false otherwise
	 */
	private synchronized boolean containsPropertyInformation(SSWAPPredicate predicate) {
		return propertyInformation.containsKey(predicate.getURI().toString());
	}

	/**
	 * Creates a PropertyInformation entry for the predicate. This method 
	 * accesses the reasoner/OntModel to inspect available information about the property.
	 * 
	 * @param predicate the predicate for which the entry is being created.
	 * @return the property information record
	 */
	private PropertyInformation createPropertyInformation(SSWAPPredicate predicate) {
		// check for annotation property
		boolean annotation = containsPredicateInformation(predicate, RDF.type, OWL.AnnotationProperty);
		
		// now, first assume that the type of the property is undefined
		PropertyType type = PropertyType.UNDEFINED;
		
		if (containsPredicateInformation(predicate, RDF.type, OWL.DatatypeProperty)) {
			// if we see that a property is a datatype property, store that information
			type = PropertyType.DATATYPE;
		}
		else if (containsPredicateInformation(predicate, RDF.type, OWL.ObjectProperty)) {
			// if we see that a property is an object property, store that information
			type = PropertyType.OBJECT;
		}
		
		// get information about the range
		String range = getSinglePredicateInformation(predicate, RDFS.range);
		
		return new PropertyInformation(type, annotation, range);
	}
	
	/**
	 * Gets all inferred named types for an individual (anonymous classes are not included).
	 * 
	 * @param individual the individual
	 * @return a collection of Strings containing the URIs of types
	 */
	public Collection<String> getInferredNamedTypes(SSWAPIndividual individual) {
		try {
			List<String> result = new LinkedList<String>();

			Resource resource = ontModel.getResource(individual.getURI().toString());

			StmtIterator it = ontModel.listStatements(resource, RDF.type, (RDFNode) null);

			while (it.hasNext()) {
				Statement statement = it.next();

				if (statement.getObject().isURIResource() 
								&& !ModelUtils.isBNodeURI(statement.getObject().asResource().getURI())) {
					result.add(((Resource) statement.getObject()).getURI());
				}
			}

			return result;
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return null; // this line won't be reached
		}
	}
	
	/**
	 * Gets all inferred types for the individual.
	 * 
	 * @param individual the individual
	 * @return a collection of String containing the URIs of types (for anonymous types, these contain the internal URIs (tag:sswap.info:bnode:...)
	 */	
	public Collection<String> getInferredTypes(SSWAPIndividual individual) {
		try {
			List<String> result = new LinkedList<String>();

			Resource resource = ontModel.getResource(individual.getURI().toString());

			StmtIterator it = ontModel.listStatements(resource, RDF.type, (RDFNode) null);
			
			while (it.hasNext()) {
				Statement statement = it.next();

				if (statement.getObject().isURIResource()) {
					result.add(((Resource) statement.getObject()).getURI());
				}
			}
			
			return result;
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return null; // this line won't be reached
		}
	}
	
	public Collection<String> getSuperClasses(String uri) {
		try {
			assertTermKnown(URI.create(uri));

			List<String> result = new LinkedList<String>();

			Resource resource = ontModel.getResource(uri);

			StmtIterator it = ontModel.listStatements(resource, RDFS.subClassOf, (RDFNode) null);

			while (it.hasNext()) {
				Statement statement = it.next();

				if (statement.getObject().isURIResource()) {
					result.add(((Resource) statement.getObject()).getURI());
				}
			}

			return result;
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return null; // this line won't be reached
		}
	}

	/**
	 * Gets information about the predicate. It searches the underlying ontModel for the first statement that has the
	 * predicate URI as its subject, and the specified informationProperty as the property. The method returns the URI
	 * of the object (if the object has a URI).
	 * 
	 * @param predicate
	 *            the SSWAPPredicate to be queried about
	 * @param informationProperty
	 *            the Jena property (e.g., rdfs:domain) that describes the SSWAPPredicate
	 * @return the URI of the object in the first statement found, or null (if no statements were found, or the object
	 *         was not a resource)
	 */
	private String getSinglePredicateInformation(SSWAPPredicate predicate, Property informationProperty) {
		String result = null;

		Resource resource = ontModel.getResource(predicate.getURI().toString());

		StmtIterator it = ontModel.listStatements(resource, informationProperty, (RDFNode) null);

		if (it.hasNext()) {
			Statement statement = it.next();

			if (statement.getObject().isURIResource()) {
				result = ((Resource) statement.getObject()).getURI();
			}
		}

		it.close();

		return result;
	}

	private Collection<String> getPredicateInformation(SSWAPPredicate predicate, Property informationProperty) {
		List<String> result = new LinkedList<String>();

		Resource resource = ontModel.getResource(predicate.getURI().toString());

		StmtIterator it = ontModel.listStatements(resource, informationProperty, (RDFNode) null);

		while (it.hasNext()) {
			Statement statement = it.next();

			if (statement.getObject().isURIResource()) {
				result.add(((Resource) statement.getObject()).getURI());
			}
		}

		it.close();

		return result;
	}

	
	/**
	 * Checks whether the ontModel contains a specific information about the predicate. For example, it can check whether
	 * the given SSWAPPredicate is an object predicate.
	 * 
	 * @param predicate
	 *            the SSWAPPredicate to be queried about
	 * @param informationProperty
	 *            the Jena property (e.g., rdf:type) that describes the SSWAPPredicate
	 * @param expectedResource
	 *            the expected object (e.g., owl:ObjectProperty)
	 * @return true, if the ontModel contains the information, false otherwise
	 */
	private boolean containsPredicateInformation(SSWAPPredicate predicate, Property informationProperty,
	                Resource expectedResource) {
		Resource resource = ontModel.getResource(predicate.getURI().toString());

		return ontModel.contains(resource, informationProperty, expectedResource);
	}

	/**
	 * @inheritDoc
	 */
	public void addModel(SSWAPModel model) {
		try {
			ModelImpl modelImpl = ImplFactory.get().assertImplementation(model, ModelImpl.class);
			SourceModel sourceModel = ImplFactory.get().assertSourceModel(modelImpl);
			
			addSourceModel(sourceModel);
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return; // this line won't be reached
		}
	}	

	/**
	 * @inheritDoc
	 */
	public void removeModel(SSWAPModel model) {
		ModelImpl modelImpl = ImplFactory.get().assertImplementation(model, ModelImpl.class);
		SourceModel sourceModel = ImplFactory.get().assertSourceModel(modelImpl);

		removeSourceModel(sourceModel);
	}

	/**
	 * Translates the individual into statements that are required and used by the specified class.
	 * 
	 * @param type
	 *            the URI of the class
	 * @param individual
	 *            the URI of the individual
	 * @throws ValidationException
	 *             if the individual is not of the specified class
	 */
	public SourceModel translate(SSWAPType type, SSWAPElement individual) throws ValidationException {
		try {
			IndividualTranslator translator = new IndividualTranslator();

			SourceModel result = ImplFactory.get().createEmptySSWAPDataObject(individual.getURI(), SourceModelImpl.class);

			Model translatedJenaModel = translator.getTranslatedModel(ATermUtils.makeTermAppl(type.getURI().toString()),
							ATermUtils.makeTermAppl(individual.getURI().toString()));
			result.dereference(translatedJenaModel);

			return result;
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return null; // this line won't be reached
		}
	}
	
	/**
	 * Extracts the TBox from the current OntModel
	 * 
	 * @return the extracted TBox
	 */
	public Model extractTBox() {
		try {
			return extractTBox(ontModel);
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return null; // this line won't be reached
		}
	}
	
	public static Model extractTBox(Model model) {
		Model result = JenaModelFactory.get().createEmptyModel();
		
		for (StmtIterator it = model.listStatements(); it.hasNext(); ) {
			Statement s = it.next();

			// add all statements that we classified as being TBox statements
			// NOTE: the statement before is NOT equivalent to !isABoxStatement
			// (some triples belong to BOTH ABox and TBox -- these are usually
			// encoding anonymous types)
			if (isTBoxStatement(s) && !isBlacklistedForExtraction(s)) {
				result.add(s);
			}
		}	
		
		return result;
	}
	
	public Model extractInferredIndividualModel(URI individualURI) {
		try {			
			Model intermediate = ModelUtils.partitionModel(ontModel, individualURI.toString(), false);
			
			Model result = JenaModelFactory.get().createEmptyModel();
			
			for (StmtIterator it = intermediate.listStatements(); it.hasNext(); ) {
				Statement s = it.next();

				if (!isBlacklistedForExtraction(s)) {
					result.add(s);
				}
			}							 
			
			return result;

		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);
			
			return null; // this line won't be reached
		}
	}
	
	
	
	/**
	 * Check whether the given URI is a built-in vocabulary (i.e., the vocabulary in owl:, rdf: or rdfs: namespaces
	 * @param uri the URI to be checked
	 * @return true, if the URI is a built-in vocabulary
	 */
	private static boolean isBuiltInVocabulary(String uri) {
		return (uri.startsWith(Namespaces.OWL_NS) || uri.startsWith(Namespaces.RDF_NS) || uri.startsWith(Namespaces.RDFS_NS));
	}
	
	/**
	 * Check whether the statement is not meant to be extracted. 
	 * 
	 * Some inferences cause problems if they are extracted and then loaded back; they usually relate to standard 
	 * OWL vocabulary that is not meant to be defined/re-defined in the ontology (for example an attempt to define types
	 * for owl:topObjectProperty via rdf:type causes problems because no other property is allowed to have that set of types). 
	 * 
	 * @param s
	 * @return true if the statement should not be extracted; false otherwise
	 */
	private static boolean isBlacklistedForExtraction(Statement s) {
		// do not extract owl:propertyDisjointWith properties (a workaround for a Pellet bug)
		if (OWL2.propertyDisjointWith.getURI().equals(s.getPredicate().getURI())) {
			return true;
		}
		
		// do not extract information about built in vocabulary
		if (s.getSubject().isURIResource() && isBuiltInVocabulary(s.getSubject().getURI())) {
			return true;
		}
		
		// do not extract certain inferences for owl:topObjectProperty (in the subject position)
		if (s.getSubject().isURIResource() 
			&& (OWL2.topObjectProperty.getURI().equals(s.getSubject().getURI()) 
			   || OWL2.topDataProperty.getURI().equals(s.getSubject().getURI()))) {
			
			return (OWL2.propertyDisjointWith.getURI().equals(s.getPredicate().getURI())
					|| RDFS.subPropertyOf.getURI().equals(s.getPredicate().getURI())
					|| OWL2.equivalentProperty.getURI().equals(s.getPredicate().getURI())
					|| RDF.type.getURI().equals(s.getPredicate().getURI()));
		}
		
		// do not extract certain inferences for owl:bottomObjectProperty (in the subject position)
		if (s.getSubject().isURIResource() 
			&& (OWL2.bottomObjectProperty.getURI().equals(s.getSubject().getURI())
				|| OWL2.bottomDataProperty.getURI().equals(s.getSubject().getURI()))) {
			
			return (OWL2.propertyDisjointWith.getURI().equals(s.getPredicate().getURI())
					|| RDFS.subPropertyOf.getURI().equals(s.getPredicate().getURI())
					|| OWL2.equivalentProperty.getURI().equals(s.getPredicate().getURI())
					|| RDF.type.getURI().equals(s.getPredicate().getURI()));
		}
		
		// do not extract inferences that a property is an owl:subPropertyOf owl:topObjectProperty (every property satisfies that condition)
		// and disjoint with that property (no property should satisfy this condition except for owl:bottomObjectProperty)  
		if (s.getObject().isURIResource() 
			&& (OWL2.topObjectProperty.getURI().equals(s.getObject().asResource().getURI())
			    || OWL2.topDataProperty.getURI().equals(s.getObject().asResource().getURI()))) {
			return (RDFS.subPropertyOf.getURI().equals(s.getPredicate().getURI())
					|| OWL2.propertyDisjointWith.getURI().equals(s.getPredicate().getURI()));
		}		
		
		return false;
	}
	
	/**
	 * Checks whether a triple belongs to a TBox
	 * 
	 * @param s Jena statement (triple)
	 * 
	 * @return true if the statement belongs to TBox
	 */
	private static boolean isTBoxStatement(Statement s) {
		Property p = s.getPredicate();
		
		if (p.getURI().equals(RDF.type.getURI())) {
			// all rdf:type triples are TBox if they reference a type in the built in vocabulary; e.g., 
			// ?x rdf:type owl:Class
			if (s.getObject().isURIResource() && isBuiltInVocabulary(s.getObject().asResource().getURI())) {
				return true;
			}
			else {
				return false;
			}
		}
		else if (p.getURI().equals(OWL.sameAs.getURI())) {
			// owl:sameAs triples belong to ABox
			return false;
		}
		else if (p.getURI().equals(OWL.differentFrom.getURI())) {
			// owl:differentFrom triples belong to ABox
			return false;
		} 
		else if (isBuiltInVocabulary(p.getURI())) {
			// all other predicates in the built-in vocabulary are TBox
			return true;
		}
		
		// an ABox statement otherwise
		return false;
	}
	
	/**
	 * Checks whether a triple belongs to ABox. 
	 * 
	 * Note: for triples that belong to both TBox and ABox, this method returns false (not ABox statement). Such cases require 
	 * additional processing/detection outside of this method.
	 * 
	 * Such triples encode anonymous types of individuals in the ABox. An example of construct containing such triples:
	 * 
	 * 	:x rdf:type [
	 *    owl:intersectionOf ( :A :B )
	 *  ]
	 *	
	 * The anonymous type above belongs to both TBox (it defines a class; an anonymous one), and to ABox
	 * (it is an assertion of a type of an individual). Since the anonymous type does not have a valid URI (only a Bnode Id/internal URI,
	 * which cannot be used cross-document/cross-model), it cannot be just put in TBox, and be referenced from ABox; instead, it has
	 * to be put in both TBox and ABox.
	 * 
	 * Since detection of this corner-case requires knowledge of a full context how the given triple/anonymous type is being used,
	 * this method cannot make such distinction (it has access only to a single triple). 
	 * Therefore, it maps all triples for the anonymous type as not belonging into ABox. 
	 * 
	 * 
	 * @param s the triple to be classified as belong to ABox
	 * @return true, if the triple belongs to ABox, false otherwise
	 */
	public static boolean isABoxStatement(Statement s) {
		Property p = s.getPredicate();
		
		if (p.getURI().equals(RDF.type.getURI())) {
			// all rdf:type triples are TBox if they reference a type in the built in vocabulary; e.g., 
			// ?x rdf:type owl:Class, all other rdf:type triples are ABox
			if (s.getObject().isURIResource() && isBuiltInVocabulary(s.getObject().asResource().getURI())) {
				return false;
			}
			else {
				return true;
			}
		}
		else if (p.getURI().equals(OWL.sameAs.getURI())) {
			// owl:sameAs triples belong to ABox
			return true;
		}
		else if (p.getURI().equals(OWL.differentFrom.getURI())) {
			// owl:differentFrom triples belong to ABox
			return true;
		} 
		else if (isBuiltInVocabulary(p.getURI())) {
			// all other predicates in the built-in vocabulary are *generally* TBox
			// (see discussion in Javadoc for this method)
			return false;
		}
		
		return true;
	}

	/**
	 * Checks whether the given triple asserts that an individual belongs to an anonymous class
	 * (i.e., x rdf:type [ ]) 
	 * 
	 * @param s statement to be checked
	 * @return true if the given statement is a triple that asserts that an individual belongs to an anonymous class
	 */
	private static boolean isAnonymousTypeAssertionTriple(Statement s) {
		if (s.getPredicate().getURI().equals(RDF.type.getURI())) {
			// it is an rdf:type triple
			if (s.getObject().isURIResource() && ModelUtils.isBNodeURI(s.getObject().asResource().getURI())) {
				// the resource in the object position is a bnode using our bnode encoding scheme
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Extracts all the statements that make up an anonymous type. This method supports anonymous types that use the following OWL constructs:
	 * 
	 * owl:intersectionOf, owl:unionOf, owl:complementOf, and owl:Restrictions (including properties like owl:onProperty, owl:cardinality,
	 * owl:minCardinality, owl:maxCardinality, owl:allValuesFrom, owl:someValuesFrom, owl:hasValue, owl:oneOf).
	 * 
	 *  This method is used to extract anonymous classes to be put in ABox (see discussion for isABoxStatement())
	 * 
	 * @param m the model containing the statements
	 * @param type the resource that represents the anonymous type
	 * @return the list of statements that make up the anonymous type
	 */
	public static List<Statement> extractTypeDefinition(Model m, Resource type) {
		List<Statement> result = new LinkedList<Statement>();
		
		// get all triples for the specified resource
		for (StmtIterator it = m.listStatements(type, null, (RDFNode) null); it.hasNext(); ) {
			Statement s = it.next();
			
			// handle owl:intersectionOf and owl:unionOf which link this type to multiple sub types using an RDFList. Moreover
			// some of these types may be anonymous types, and as such their definition should be recursively included
			if (s.getPredicate().getURI().equals(OWL.intersectionOf.getURI()) || s.getPredicate().getURI().equals(OWL.unionOf.getURI())) {
				// add the owl:intersectionOf/owl:unionOf triple
				result.add(s);
				
				if (s.getObject().isURIResource()) {
					// extract and add all the statements for the RDFList in the object position of the statement
					Collection<Statement> listStatements = ModelUtils.getAllStatementsForList(m, s.getObject().asResource());
					result.addAll(listStatements);
					
					RDFList rdfList = ModelUtils.createRDFList(m, s.getObject().asResource());
					
					// now for each element in the RDFList check whether it is not an anonymous class, if so, include its statements in
					// the output
					for (RDFNode listElement : rdfList.asJavaList()) {
						if (listElement.isURIResource() && ModelUtils.isBNodeURI(listElement.asResource().getURI())) {
							result.addAll(extractTypeDefinition(m, listElement.asResource()));
						}
					}
				}
			} 
			else if (s.getPredicate().getURI().equals(OWL.complementOf.getURI())) {
				// handle owl:complementOf
				result.add(s);
				
				// if the object is an anonymous type, include it in the output
				if (s.getObject().isURIResource() && ModelUtils.isBNodeURI(s.getObject().asResource().getURI())) {
					result.addAll(extractTypeDefinition(m, s.getObject().asResource()));
				}
			}
			else if (s.getPredicate().getURI().equals(RDF.type.getURI()) 
							&& (s.getObject().isURIResource()) 
							&& (s.getObject().asResource().getURI().equals(OWL.Restriction.getURI()))) {
				// for rdf:type owl:Restriction, include that triple
				result.add(s);
			}
			else if (s.getPredicate().getURI().equals(OWL.onProperty.getURI())) {
				// include owl:onProperty (part of owl:Restriction) 
				result.add(s);
			}
			else if (s.getPredicate().getURI().equals(OWL.cardinality.getURI())) {
				// include owl:cardinality (part of owl:Restriction)
				result.add(s);
			}
			else if (s.getPredicate().getURI().equals(OWL.minCardinality.getURI())) {
				// include owl:minCardinality (part of owl:Restriction)
				result.add(s);
			}
			else if (s.getPredicate().getURI().equals(OWL.maxCardinality.getURI())) {
				// include owl:maxCardinality (part of owl:Restriction)
				result.add(s);
			}
			else if (s.getPredicate().getURI().equals(OWL.allValuesFrom.getURI())) {
				// include owl:allValuesFrom (part of owl:Restriction)
				result.add(s);
			}
			else if (s.getPredicate().getURI().equals(OWL.someValuesFrom.getURI())) {
				// include owl:someValuesFrom (part of owl:Restriction)
				result.add(s);
			}
			else if (s.getPredicate().getURI().equals(OWL.hasValue.getURI())) {
				// include owl:hasValue (part of owl:Restriction)
				result.add(s);
			}
			else if (s.getPredicate().getURI().equals(OWL.oneOf.getURI())) {
				// include owl:oneOf (part of owl:Restriction)
				result.add(s);
				
				if (s.getObject().isURIResource()) {
					// extract and add all the statements for the RDFList in the object position of the statement
					Collection<Statement> listStatements = ModelUtils.getAllStatementsForList(m, s.getObject().asResource());
					result.addAll(listStatements);					
				}
			}
		}
		
		return result;
	}

	/**
	 * Extracts the ABox from the current OntModel
	 * 
	 * @return the extracted ABox
	 */
	public Model extractABox() {
		try {
			return extractABox(ontModel);
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);
			
			return null; // this line won't be reached
		}		
	}
	
	public static Model extractABox(Model model) {
		Model result = JenaModelFactory.get().createEmptyModel();
		
		for (StmtIterator it = model.listStatements(); it.hasNext(); ) {
			Statement s = it.next();

			// add all statements that we classified as being ABox statements
			// NOTE: the statement before is NOT equivalent to !isTBoxStatement
			// (some triples belong to BOTH ABox and TBox -- these are usually
			// encoding anonymous types)
			if (isABoxStatement(s)) {
				result.add(s);
				
				// if the statement is an assertion that the individual is of an anonymous type
				// we need to include the information about that type in the ABox
				if (isAnonymousTypeAssertionTriple(s)) {
					result.add(extractTypeDefinition(model, s.getObject().asResource()));
				}
			}
		}	
		
		return result;
	}
	
	public Model extractInferredModel() {
		try {
			Model result = JenaModelFactory.get().createEmptyModel();

			result.add(ontModel);

			return result;
		}
		catch (InconsistentOntologyException e) {
			handleInconsistentOntologyException(e);

			return null; // this line won't be reached
		}
	}
	
	public void resetKB() {
		((PelletInfGraph) ontModel.getGraph()).reload();
		((PelletInfGraph) ontModel.getGraph()).rebind();
	}
		
	public KnowledgeBase getPelletKB() {						
		((PelletInfGraph) ontModel.getGraph()).prepare();
		return ((PelletInfGraph) ontModel.getGraph()).getKB();
	}
		
	public OntModel getOntModel() {		
		return ontModel;
	}

	
	private abstract class ReasoningTaskBase {
		/**
		 * Lazily initialized map of classes onto relevant TBox axioms for the specified class.
		 * Because of lazy initialization, most of the code should use getTBoxAxioms(ATermAppl)
		 * method to retrieve information from this data structure.
		 */
		private Map<ATermAppl,Collection<ATermAppl>> tboxAxioms;
				
		/**
		 * Gets/initializes a list for given class in tboxAxioms.
		 * 
		 * @param clazz the class
		 * @return collection that contains corresponding TBox axioms for that class
		 * (this collection is never null, even if the class is not known)
		 */
		private Collection<ATermAppl> getTBoxAxiomList(ATermAppl clazz) {
			Collection<ATermAppl> list = tboxAxioms.get(clazz);
			
			if (list == null) {
				list = new HashSet<ATermAppl>();
				tboxAxioms.put(clazz, list);
			}
			
			return list;
		}
		
		/**
		 * Lazily initializes the tboxAxiomsMap
		 */
		private void initTBoxAxiomMap() {
			tboxAxioms = new HashMap<ATermAppl,Collection<ATermAppl>>();
			
			// iterate over all asserted axioms, filter the relevant ones,
			// and store them in the map (indexed by class name)
			for (ATermAppl axiom : getPelletKB().getTBox().getAssertedAxioms()) {
				
				// rdfs:subClassOf -- index it for both subclass and superclass
				if (ATermUtils.SUBFUN.equals(axiom.getAFun())) {
					if (axiom.getArgument(0) instanceof ATermAppl) {
						getTBoxAxiomList((ATermAppl) axiom.getArgument(0)).add(axiom);
					}
					
					if (axiom.getArgument(1) instanceof ATermAppl) {
						getTBoxAxiomList((ATermAppl) axiom.getArgument(1)).add(axiom);
					}
				}
				// owl:equivalentClass -- index it for both equivalent classes
				else if (ATermUtils.EQCLASSFUN.equals(axiom.getAFun())) {
					if (axiom.getArgument(0) instanceof ATermAppl) {
						getTBoxAxiomList((ATermAppl) axiom.getArgument(0)).add(axiom);
					}
					
					if (axiom.getArgument(1) instanceof ATermAppl) {
						getTBoxAxiomList((ATermAppl) axiom.getArgument(1)).add(axiom);
					}					
				}
			}
		}
		
		/**
		 * Gets relevant TBox axioms for the specified class (currently, the only relevant
		 * axioms are sub-/super-/equivalent-class axioms).
		 * 
		 * @param clazz the class for which the relevant TBox axioms should be retrieved
		 * @return a list of relevant axioms (may be empty but never null)
		 */
		protected Collection<ATermAppl> getTBoxAxioms(ATermAppl clazz) {
			if (null == tboxAxioms) {
				initTBoxAxiomMap();
			}
			
			return getTBoxAxiomList(clazz);
		}
		
		@SuppressWarnings("unused")
        protected Collection<ATermAppl> getDomains(ATermAppl role) {
			return getPelletKB().getRBox().getDefinedRole(role).getDomains();
		}
		
		protected Collection<ATermAppl> getRanges(ATermAppl role) {
			return getPelletKB().getRanges(role);
		}

		/**
		 * Gets all super classes of the specified class
		 * 
		 * @param clazz
		 *            the ATerm for the class
		 * @return a collection of ATerms for the super classes
		 */
		protected Collection<ATermAppl> getSuperClasses(ATermAppl clazz) {
			List<ATermAppl> result = new LinkedList<ATermAppl>();

			for (ATermAppl tboxAxiom : getTBoxAxioms(clazz)) {
				if (ATermUtils.SUBFUN.equals(tboxAxiom.getAFun())) {
					if (tboxAxiom.getArgument(1) instanceof ATermAppl) {
						result.add((ATermAppl) tboxAxiom.getArgument(1));
					}
					else {
						throw new IllegalArgumentException(
						                "Found an subClass axiom which does not contain an ATermAppl on the right-hand side");
					}
				}
			}

			return result;
		}

		/**
		 * Gets all equivalent classes to the specified class
		 * 
		 * @param clazz
		 *            the ATerm for the class
		 * @return a collection of ATerms for the equivalent classes
		 */
		protected Collection<ATermAppl> getEquivalentClasses(ATermAppl clazz) {
			List<ATermAppl> result = new LinkedList<ATermAppl>();

			for (ATermAppl tboxAxiom : getTBoxAxioms(clazz)) {
				if (ATermUtils.EQCLASSFUN.equals(tboxAxiom.getAFun())) {
					if (tboxAxiom.getArgument(1) instanceof ATermAppl) {
						result.add((ATermAppl) tboxAxiom.getArgument(1));
					}
					else {
						throw new IllegalArgumentException(
						                "Found an equivalentClass axiom which does not contain an ATermAppl on the right-hand side");
					}
				}
			}

			return result;
		}

		/**
		 * Represents a pair that consists of a class and an individual belonging to this class.
		 * 
		 * @author Blazej Bulka <blazej@clarkparsia.com>
		 */
		class ClassIndividualMapping {
			/**
			 * The class in the mapping
			 */
			private ATermAppl clazz;

			/**
			 * The individual in the mapping
			 */
			private ATermAppl individual;

			/**
			 * Creates a pair that contains the mapping between the class and the individual.
			 * 
			 * @param clazz
			 *            the class
			 * @param individual
			 *            the individual
			 */
			public ClassIndividualMapping(ATermAppl clazz, ATermAppl individual) {
				this.clazz = clazz;
				this.individual = individual;
			}

			/**
			 * Gets the class.
			 * 
			 * @return the class
			 */
			@SuppressWarnings("unused")
			public ATermAppl getClazz() {
				return clazz;
			}

			/**
			 * Gets the individual
			 * 
			 * @return the individual
			 */
			@SuppressWarnings("unused")
			public ATermAppl getIndividual() {
				return individual;
			}

			/**
			 * Overridden equals() method. The pair is equal to another object if and only if the other object is a
			 * ClassIndividualMapping and it contains the same class and individual.
			 * 
			 * @param o
			 *            object to be compared for equality
			 * @return true if the other object is equal to this one
			 */
			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}

				if (o instanceof ClassIndividualMapping) {
					ClassIndividualMapping other = (ClassIndividualMapping) o;

					// intentional comparison with == (instead of equals()); this relies
					// on the intrinsic property of ATerms for which it is guaranteed
					// that if a.equals(b) then a == b
					return (clazz == other.clazz) && (individual == other.individual);
				}

				return false;
			}

			/**
			 * Overrided hashCode() method to maintain consistency with equals().
			 * 
			 * @return the hash code for that pair
			 */
			@Override
			public int hashCode() {
				int prime = 31;
				return (prime * clazz.hashCode()) + individual.hashCode();
			}
		}
	}

	/**
	 * Translates an individual into using the vocabulary used by the specified class. (The individual must belong to
	 * that class.) This kind of translation is performed when the individual belongs to a more specific class, which
	 * could have created its own vocabulary (e.g., by creating subProperties or by using subclasses for values of
	 * properties), and we want to express that individual using the concepts/classes of a more generic class.
	 * 
	 * An example of a use case when such a situation happens is when a client sends an RIG to the provider, and that
	 * RIG can use concepts different than the original RDG. This class will validate and translate all properties for
	 * that individual back into using the RDG terminology.
	 * 
	 * @author Blazej Bulka <blazej@clarkparsia.com>
	 * 
	 */
	private class IndividualTranslator extends ReasoningTaskBase {
		/**
		 * The model where the translated concepts will be stored.
		 */
		private Model model = ModelFactory.createDefaultModel();

		/**
		 * A cache for already translated concepts. The cache not only includes efficiency (e.g., when the subclass
		 * relationships do not form a tree, and a super class can be visited multiple times for an individual), but
		 * also prevents infinite loops (e.g., when two classes list each other as equivalent).
		 */
		private TranslationCache cache = new TranslationCache();
		
		/**
		 * Stack of named classes that represents the dependencies between named classes being translated.
		 * When a class C is being translated, it is pushed on the stack, when its superclass B is translated (as a part of C's translation),
		 * it is then pushed on the stack, and popped when its translation is finished. Similar push/pop will also occur for every
		 * equivalent class of the currently validated class. 
		 * 
		 * The purpose of this stack is to provide more meaningful explanations in ValidationExceptions. If we are to
		 * generate an exception at any place (due to restriction violation), the name on the top of the stack (if there is any) 
		 * identifies the class where the violated restriction was defined (either directly in this class or in one of 
		 * its anonymous super-/equivalent- classes).
		 */
		private Stack<String> classExplanationStack = new Stack<String>();

		/**
		 * Creates a Jena model with translated information for the individual
		 * 
		 * @param clazz
		 *            the class to which the individual should belong (and whose terms should be used)
		 * @param individual
		 *            the individual whose information should be translated.
		 * @return the model with the translated information
		 * @throws ValidationException
		 *             if the individual does not conform to the restrictions defined in the specified class
		 */
		public Model getTranslatedModel(ATermAppl clazz, ATermAppl individual) throws ValidationException {
			((PelletInfGraph) ontModel.getGraph()).getLoader().clear();
			((PelletInfGraph) ontModel.getGraph()).prepare();
			Collection<Statement> statements = translate(clazz, individual);

			for (Statement statement : statements) {
				model.add(statement);
			}

			return model;
		}

		
		/**
		 * Translates the individual into the vocabulary used by the specified class
		 * 
		 * @param clazz
		 *            the class to which the individual should belong
		 * @param individual
		 *            the individual to be translated
		 * @return a collection of statements that are the result of translation
		 * @throws ValidationException
		 *             if the individual does not belong to the class (e.g., it violates a restriction on a property)
		 */
		@SuppressWarnings("unchecked")
		private Collection<Statement> translate(ATermAppl clazz, ATermAppl individual) throws ValidationException {
			LOGGER.trace("entering translate(" + clazz + ", " + individual + ")");			

			if (cache.isCached(clazz, individual)) {
				LOGGER.trace("exiting translate(" + clazz + ", " + individual + ") - " + cache.getCached(clazz, individual) + " (cached)");
				return cache.getCached(clazz, individual);
			}

			cache.cache(clazz, individual, Collections.EMPTY_LIST);

			try {
				Collection<Statement> result = Collections.EMPTY_LIST;

				// For owl:Thing we do not need to perform any validation -- every individual belongs to this class
				if (clazz == ATermUtils.TOP) {
					// nothing
				}
				else if (ATermUtils.isPrimitive(clazz) && !ATermUtils.isLiteral(individual)) {
					// this also includes class expressions that have bnode URIs (validateNamedClass can handle these)
					result = translateNamedClass(clazz, individual);
				}
				else if (ATermUtils.isAnd(clazz)) {
					// owl:intersectionOf
					result = translateIntersectionOf(clazz, individual);
				}
				else if (ATermUtils.isOr(clazz)) {
					// owl:unionOf
					result = translateUnionOf(clazz, individual);
				}
				else if (ATermUtils.isNot(clazz)) {
					// owl:complementOf
					result = translateComplementOf(clazz, individual);
				}
				else if (ATermUtils.isSomeValues(clazz)) {
					// owl:someValuesFrom
					result = translateSomeValues(clazz, individual);
				}
				else if (ATermUtils.isAllValues(clazz)) {
					// owl:allValuesFrom
					result = translateAllValues(clazz, individual);
				}
				else if (ATermUtils.isMin(clazz)) {
					// owl:minCardinality
					result = translateMin(clazz, individual);
				}
				else if (ATermUtils.isMax(clazz)) {
					// owl:maxCardinality
					result = translateMax(clazz, individual);
				}
				else if (ATermUtils.CARDFUN.equals(clazz.getAFun())) {
					// owl:cardinality
					result = translateCard(clazz, individual);
				}
				else if (ATermUtils.isSelf(clazz)) {
					// owl:hasSelf
					result = translateSelf(clazz, individual);
				}
				else {
					// mostly nominals which we do not validate/translate (should we?)
				}

				// store the information so that we do not have to repeat executing this method anymore
				cache.cache(clazz, individual, result);
				return result;
			}
			catch (ValidationException e) {
				cache.cacheValidationException(clazz, individual, e);
				throw e;
			}
			finally {
				LOGGER.trace("exiting translate(" + clazz + ", " + individual + ") - " + cache.getCached(clazz, individual) + " (computed)");
			}									
		}

		/**
		 * Translates a named class expression (this includes both named classes and intersections/unions/complements
		 * that got assigned a special bnode URI).
		 * 
		 * @param namedClass
		 *            the named class expression
		 * @param individual
		 *            the individual that should belong that class expression
		 * @return a collection of statements that are the result of translation
		 * @throws ValidationException
		 *             if the individual does not belong to this class
		 */
		private Collection<Statement> translateNamedClass(ATermAppl namedClass, ATermAppl individual)
		                throws ValidationException {
			
			boolean namedClassExplanationMod = false;
			List<Statement> result = new LinkedList<Statement>();
			
			try {				
				if (!ModelUtils.isBNodeURI(namedClass.toString())) {
					classExplanationStack.push(namedClass.toString());
					namedClassExplanationMod = true;
					// the only difference between named classes and other types of class expressions is that for the former
					// the individual should contain rdf:type triple

					if (containsTriple(individual.toString(), RDF.type.toString(), namedClass.toString())) {
						result.add(createTriple(individual.toString(), RDF.type.toString(), namedClass.toString()));
					}
				}

				LOGGER.trace("superClasses for " + namedClass + " " + getSuperClasses(namedClass));

				for (ATermAppl superClass : getSuperClasses(namedClass)) {
					// recursively validate whether the individual satisfies the requirements of the super class
					result.addAll(translate(superClass, individual));
				}

				LOGGER.trace("equivalentClasses for " + namedClass + " " + getEquivalentClasses(namedClass));

				// equivalent classes check (for intersection/union/complement class expressions they are represented as
				// ATerms that are equivalent to
				// to an and(), or() or not() statement
				for (ATermAppl equivalentClass : getEquivalentClasses(namedClass)) {
					// recursively validate whether the individual satisfies the requirements of the equivalent class
					result.addAll(translate(equivalentClass, individual));
				}
			}
			finally {
				if (namedClassExplanationMod) {
					classExplanationStack.pop();
				}
			}
			
			return result;
		}

		/**
		 * Translates an owl:intersectionOf
		 * 
		 * @param intersection
		 *            the intersection of class expressions
		 * @param individual
		 *            the individual that should satisfy all expressions in the intersection
		 * @return a collection of statements that are the result of translation
		 * @throws ValidationException
		 *             if the individual does not satisfy one or more class expressions
		 */
		private Collection<Statement> translateIntersectionOf(ATermAppl intersection, ATermAppl individual)
		                throws ValidationException {
			// owl:intersectionOf
			List<Statement> result = new LinkedList<Statement>();

			if (intersection.getArgument(0) instanceof ATermList) {
				ATermList list = (ATermList) intersection.getArgument(0);

				for (int i = 0; i < list.getLength(); i++) {
					if (list.elementAt(i) instanceof ATermAppl) {
						result.addAll(translate((ATermAppl) list.elementAt(i), individual));
					}
				}
			}
			else {
				throw new IllegalArgumentException("The argument to an and() is not an ATermList");
			}

			return result;
		}

		/**
		 * Translates an owl:unionOf
		 * 
		 * @param union
		 *            the union of class expressions
		 * @param individual
		 *            the individual that should satisfy all expressions in the intersection
		 * @return a collection of statements that are the result of translation
		 * @throws ValidationException
		 *             if the individual does not satisfy one or more class expressions
		 */
		private Collection<Statement> translateUnionOf(ATermAppl union, ATermAppl individual)
		                throws ValidationException {
			// owl:unionOf
			List<Statement> result = new LinkedList<Statement>();

			boolean satisfied = false;
			List<String> validationExceptionMessages = new LinkedList<String>();
						
			if (union.getArgument(0) instanceof ATermList) {
				ATermList list = (ATermList) union.getArgument(0);

				for (int i = 0; i < list.getLength(); i++) {
					if (list.elementAt(i) instanceof ATermAppl) {
						try {
							result.addAll(translate((ATermAppl) list.elementAt(i), individual));

							// note that we found one successful class expression
							satisfied = true;

							// do not terminate the loop prematurely 
							// (although at this point we know that the whole union passes validation,
							// we should translate all the branches).
						}
						catch (ValidationException e) {
							validationExceptionMessages.add(e.getMessage());
						}
					}
				}
			}
			else {
				throw new IllegalArgumentException("The argument to an or() is not an ATermList");
			}

			// TODO how do we handle empty unions? (right now they will always generate ValidationException, which is
			// probably right)

			if (!satisfied) {
				StringBuffer msg = new StringBuffer("Could not find one satisfied alternative in owl:unionOf");
				if (!classExplanationStack.isEmpty()) {
					msg.append(" (defined in " + classExplanationStack.peek() + " or in one of its anonymous super classes or equivalent classes)");
				}
				
				if (!validationExceptionMessages.isEmpty()) {
					msg.append(". Validation failures for alternatives: [");
					
					for (String validationMessage : validationExceptionMessages) {
						msg.append("{ ");
						msg.append(validationMessage);
						msg.append(" }");
					}
					
					msg.append("]");
				}
				
				throw new ValidationException(msg.toString());
			}

			return result;
		}

		/**
		 * Translates an owl:complementOf
		 * 
		 * @param complement
		 *            the complement
		 * @param individual
		 *            the individual that should belong to the complement
		 * @return a collection of statements that are the result of translation
		 * @throws ValidationException
		 *             if the individual does not belong to the complement
		 */
		@SuppressWarnings("unchecked")
		private Collection<Statement> translateComplementOf(ATermAppl complement, ATermAppl individual)
		                throws ValidationException {
			boolean satisfied = false;

			if (complement.getArgument(0) instanceof ATermAppl) {
				try {
					translate((ATermAppl) complement.getArgument(0), individual);
					satisfied = true;
				}
				catch (ValidationException e) {
					// nothing -- correct behavior
				}
			}
			else {
				throw new IllegalArgumentException("The argument to a not() is not an ATermAppl");
			}

			if (satisfied) {				
				String className = "[unknown]";
				
				if (!classExplanationStack.isEmpty()) {
					className = classExplanationStack.peek();
				}
			
				throw new ValidationException(String.format("The individual belongs to class %s (or its anonymous super class or equivalent class), while it is supposed to belong to its complement", className));
			}

			return Collections.EMPTY_LIST;
		}

		/**
		 * Translates an owl:someValuesFrom restriction and the property referenced in it.
		 * 
		 * @param someValues
		 *            the ATerm describing the restriction
		 * @param individual
		 *            the individual that should satisfy the restriction
		 * @return a collection of statements that are the result of translation
		 * @throws ValidationException
		 *             if the individual does not satisfy the restriction
		 */
		private Collection<Statement> translateSomeValues(ATermAppl someValues, ATermAppl individual)
		                throws ValidationException {
			List<Statement> result = new LinkedList<Statement>();
			ATermAppl property = (ATermAppl) someValues.getArgument(0);
			ATermAppl range = (ATermAppl) someValues.getArgument(1);

			result.addAll(translateDomain(property, individual));
			
			// verify that the individual has at least one value for that property, and the value matches the specified
			// range
			result.addAll(translateCardinality(individual, property, 1, range, /* enforceRange */false));
			
			return result;
		}

		/**
		 * Translates an owl:allValuesFrom restriction and the property referenced in it.
		 * 
		 * @param allValues
		 *            the ATerm describing the restriction
		 * @param individual
		 *            the individual that should satisfy the restriction
		 * @return a collection of statements that are the result of translation
		 * @throws ValidationException
		 *             if the individual does not satisfy the restriction
		 */
		private Collection<Statement> translateAllValues(ATermAppl allValues, ATermAppl individual)
		                throws ValidationException {
			List<Statement> result = new LinkedList<Statement>();
			ATermAppl property = (ATermAppl) allValues.getArgument(0);
			ATermAppl range = (ATermAppl) allValues.getArgument(1);

			result.addAll(translateDomain(property, individual));
			
			result.addAll(translateCardinality(individual, property, 0, range, /* enforceRange */true));
			return result;
		}

		/**
		 * Translates an owl:minCardinality restriction and the property referenced in it
		 * 
		 * @param min
		 *            the ATerm describing the restriction
		 * @param individual
		 *            the individual that should satisfy the restriction
		 * @return a collection of statements that are the result of translation
		 * @throws ValidationException
		 *             if the individual does not satisfy the restriction
		 */
		private Collection<Statement> translateMin(ATermAppl min, ATermAppl individual) throws ValidationException {
			List<Statement> result = new LinkedList<Statement>();
			ATermAppl property = (ATermAppl) min.getArgument(0);
			ATermInt minValueTerm = (ATermInt) min.getArgument(1);

			result.addAll(translateDomain(property, individual));
			
			result.addAll(translateCardinality(individual, property, minValueTerm.getInt(), null, /* enforceRange */true));
			return result;
		}

		/**
		 * Translates owl:maxCardinality restriction for the individual and the property referenced in it
		 * 
		 * @param max
		 *            the ATerm describing the restriction
		 * @param individual
		 *            the individual that should satisfy the restriction
		 * @return a collection of statements that are the result of translation
		 * @throws ValidationException
		 *             if the individual does not satisfy the restriction
		 */
		private Collection<Statement> translateMax(ATermAppl max, ATermAppl individual) throws ValidationException {
			List<Statement> result = new LinkedList<Statement>();
			ATermAppl property = (ATermAppl) max.getArgument(0);
			ATermInt maxValueTerm = (ATermInt) max.getArgument(1);
			
			result.addAll(translateDomain(property, individual));
			
			// max cardinality is enforced only if it is set to 0
			// in such a case, we have to ensure that there are no values for the property
			if (maxValueTerm.getInt() == 0) {
				validateNone(individual, property);
			}
			else {
				result.addAll(translateCardinality(individual, property, 0, null, /* enforceRange */false));
			}
			
			return result;
		}

		/**
		 * Translates an owl:cardinality restriction for the individual, and the property referenced in it
		 * 
		 * @param card
		 *            the ATerm describing the restriction
		 * @param individual
		 *            the individual that should satisfy the restriction
		 * @return a collection of statements that are the result of translation
		 * @throws ValidationException
		 *             if the individual does not satisfy the restriction
		 */
		private Collection<Statement> translateCard(ATermAppl card, ATermAppl individual) throws ValidationException {
			List<Statement> result = new LinkedList<Statement>();

			result.addAll(translateMin(card, individual));
			result.addAll(translateMax(card, individual));

			return result;
		}

		/**
		 * Translates owl:hasSelf restriction for the individual and the property referenced in it
		 * 
		 * @param self
		 *            the ATerm describing the restriction
		 * @param individual
		 *            the individual that should satisfy the restriction
		 * @return a collection of statements that are the result of translation
		 * @throws ValidationException
		 *             if the individual does not satisfy the restriction
		 */
		private Collection<Statement> translateSelf(ATermAppl self, ATermAppl individual) throws ValidationException {
			List<Statement> result = new LinkedList<Statement>();

			ATermAppl property = (ATermAppl) self.getArgument(0);

			result.addAll(translateDomain(property, individual));
			
			assertContainsTriple(individual.toString(), property.toString(), individual.toString());
			result.add(createTriple(individual.toString(), property.toString(), individual.toString()));

			return result;
		}
		
		private Collection<Statement> translateDomain(ATermAppl role, ATermAppl individual) throws ValidationException {
			List<Statement> result = new LinkedList<Statement>();
			
			// TODO: temporarily disabled until all the ontologies are fixed
			
			//for (ATermAppl domain : getDomains(role)) {
			//	result.addAll(translate(domain, individual));
			//}
			
			return result;
		}

		/**
		 * Translates a minimum cardinality constraint, and the property referenced in it
		 * 
		 * @param individual
		 *            the individual for which the cardinality is verified
		 * @param property
		 *            the property on which the cardinality restriction is placed
		 * @param n
		 *            the minimum cardinality
		 * @param rangeRestriction
		 *            the range of the property
		 * @param enforceRange
		 *            if true, throw Validation exception whenever there is any value that does not match the range,
		 *            otherwise only throw exception whenever the count of valid values (matching the range) violates
		 *            the cardinality restriction
		 * @return a collection of statements that are the result of translation
		 * @throws ValidationException
		 *             if the individual does not satisfy the restriction
		 */
		private Collection<Statement> translateCardinality(ATermAppl individual, ATermAppl property, int n,
		                ATermAppl rangeRestriction, boolean enforceRange) throws ValidationException {
			List<Statement> result = new LinkedList<Statement>();

			Resource subjectResource = ontModel.getResource(individual.toString());
			Property propertyResource = ontModel.getProperty(property.toString());

			StmtIterator it = ontModel.listStatements(subjectResource, propertyResource, (RDFNode) null);

			int statementCount = 0;

			try {
				// iterate for each statement that matches the pattern: ?individual ?property *
				// and verify/translate the values in the object position, as well as count
				// how many valid values there were (to be used later to enforce the min cardinality restriction)
				while (it.hasNext()) {
					Statement statement = it.next();
					
					RDFNode object = statement.getObject();
					ATermAppl objectATerm = JenaUtils.makeATerm(object);
					
					try {
						if (!TRANSLATION_NO_FOLLOW_PROPS.contains(statement.getPredicate().toString())) {
							for (ATermAppl range : getRanges(property)) {
								result.addAll(translate(range, objectATerm));
							}

							if (rangeRestriction != null) {
								result.addAll(translate(rangeRestriction, objectATerm));
							}
						}

						// increase the counter only if the value was of expected type
						statementCount++;
					}
					catch (ValidationException e) {
						if (enforceRange) {
							// if the range has to be enforced, we throw the exception immediately
							throw e;
						}
					}

					result.add(statement);
				}
			}
			finally {
				it.close();
			}

			// enforce the minCardinality restriction (using closed-world assumption)
			if (statementCount < n) {
				throw new ValidationException(
                    String.format("Cardinality restriction violation for property %s. The minimum number is %d but encountered %d",
				                  property.toString(), 
				                  n, 
				                  statementCount));
			}

			return result;
		}

		/**
		 * Validate that there are no values for the property and individual.
		 * 
		 * @param individual
		 *            the individual
		 * @param property
		 *            the property
		 * @throws ValidationException
		 *             if there are values for that property and individual
		 */
		private void validateNone(ATermAppl individual, ATermAppl property) throws ValidationException {
			Resource subjectResource = ontModel.getResource(individual.toString());
			Property propertyResource = ontModel.getProperty(property.toString());

			StmtIterator it = ontModel.listStatements(subjectResource, propertyResource, (RDFNode) null);

			try {
				if (it.hasNext()) {
					throw new ValidationException(
                        String.format("Violated maximum cardinality restriction for property %s. The max is 0 but there are values for this property",
					                  property.toString()));
				}
			}
			finally {
				it.close();
			}
		}

		/**
		 * Asserts that the underlying ont model contains the specified triple.
		 * 
		 * @param subject
		 *            the subject of the triple
		 * @param predicate
		 *            the predicate of the triple
		 * @param object
		 *            the object of the triple
		 * @throws ValidationException
		 *             if there is no such a triple in the ont model.
		 */
		private void assertContainsTriple(String subject, String predicate, String object) throws ValidationException {
			if (!containsTriple(subject, predicate, object)) {
				throw new ValidationException("Expected to find the following triple: " + subject + ", " + predicate
				                + ", " + object);
			}
		}

		/**
		 * Checks whether the underlying ontModel contains the specified triple
		 * 
		 * @param subject
		 *            the subject of the triple
		 * @param predicate
		 *            the predicate of the triple
		 * @param object
		 *            the object of the triple
		 * @return true if the ont model contains the triple, false if it does not
		 */
		private boolean containsTriple(String subject, String predicate, String object) {
			Resource subjectResource = ontModel.getResource(subject);
			Property predicateProperty = ontModel.getProperty(predicate);
			Resource objectResource = ontModel.getResource(object);

			return ontModel.contains(subjectResource, predicateProperty, objectResource);
		}

		/**
		 * Creates a triple.
		 * 
		 * @param subject
		 *            subject URI
		 * @param predicate
		 *            predicate URI
		 * @param object
		 *            object URI
		 * @return a new Jena Statement from the subject, predicate, and object
		 */
		private Statement createTriple(String subject, String predicate, String object) {
			Resource subjectResource = model.getResource(subject);
			Property predicateProperty = model.getProperty(predicate);
			Resource objectResource = model.getResource(object);

			return model.createStatement(subjectResource, predicateProperty, objectResource);
		}

		/**
		 * Contains cache of translation results.
		 * 
		 * @author Blazej Bulka <blazej@clarkparsia.com>
		 */
		private class TranslationCache {
			/**
			 * Maps a pair (class + individual) onto the result of its translation (collection of statements)
			 */
			private Map<ClassIndividualMapping, Collection<Statement>> contents = 
				new HashMap<ClassIndividualMapping, Collection<Statement>>();
			
			private Map<ClassIndividualMapping, ValidationException> validationExceptions =
				new HashMap<ClassIndividualMapping, ValidationException>();

			/**
			 * Checks whether the results for the given class and individual are already cached
			 * 
			 * @param clazz
			 *            the class
			 * @param individual
			 *            the individual
			 * @return true if the results are cached
			 */
			public boolean isCached(ATermAppl clazz, ATermAppl individual) {
				ClassIndividualMapping key = new ClassIndividualMapping(clazz, individual);
				
				return contents.containsKey(key) || validationExceptions.containsKey(key);
			}

			/**
			 * Gets the cached translation results for the given class and individual
			 * 
			 * @param clazz
			 *            the class
			 * @param individual
			 *            the individual
			 * @return the cached translation results (if available), or null (if not available; i.e.,
			 *         isCached(ATermAppl, AtermAppl) returns false).
			 */
			public Collection<Statement> getCached(ATermAppl clazz, ATermAppl individual) throws ValidationException {
				ClassIndividualMapping key = new ClassIndividualMapping(clazz, individual);
				
				if (validationExceptions.containsKey(key)) {
					throw validationExceptions.get(key);
				}
				
				return contents.get(key);
			}

			/**
			 * Stores the results of translation of the given class and individual.
			 * 
			 * @param clazz
			 *            the class
			 * @param individual
			 *            the individual
			 * @param translation
			 *            the translation results
			 */
			public void cache(ATermAppl clazz, ATermAppl individual, Collection<Statement> translation) {
				contents.put(new ClassIndividualMapping(clazz, individual), translation);
			}
			
			public void cacheValidationException(ATermAppl clazz, ATermAppl individual, ValidationException exception) {
				validationExceptions.put(new ClassIndividualMapping(clazz, individual), exception);
			}						
		}
	}
	
	public enum ExplanationSyntax {
		RDFXML, PELLET, TURTLE
	}
	
	public <T extends SSWAPNode> boolean isMappingValid(T protocolIndividual, MappingType type, T rdgIndividual)  {
        boolean isValid = false;
		try {
	        KnowledgeBase kb = getPelletKB();
	        
	        boolean assignProtocolTypeValues = !type.equals(MappingType.SUPER);
	        
	        ATermAppl protocolType = createClassExpression(kb, protocolIndividual, assignProtocolTypeValues /* assertIndirectValues */);
	        ATermAppl rdgType = createClassExpression(kb, rdgIndividual, false /* assertIndirectValues */);
	        
	        if (protocolType == null) {
	        	LOGGER.warn("protocolType is null");
	        	return false;
	        }
	        
	        if (rdgType == null) {
	        	LOGGER.warn("rdgType is null");
	        	return false;
	        }
	        
	        if (LOGGER.isDebugEnabled()) {
	        	LOGGER.debug("Mapping type: " + type);
	        	LOGGER.debug("Protocol ind: " + protocolIndividual);
	        	LOGGER.debug("Protocol type: " + ATermUtils.toString(protocolType));
	        	LOGGER.debug("RDG ind: " + rdgIndividual);
	        	LOGGER.debug("RDG type: " + ATermUtils.toString(rdgType));
	        }
	        
			switch (type) {
				case SUB:
					isValid = kb.isSubClassOf(protocolType, rdgType);
					
					/*if (!isValid && !protocolType.equals(ATermUtils.TOP)) {
						System.out.println(kb.getTBox().getAssertedAxioms());	
					}*/
					
					break;
				case SUB_IF_NOT_TOP:
					isValid = protocolType.equals(ATermUtils.TOP) || kb.isSubClassOf(protocolType, rdgType);
					break;
				case SUPER:
					isValid = kb.isSubClassOf(rdgType, protocolType);
					break;
				case EQUIVALENT:
					isValid = kb.isEquivalentClass(protocolType, rdgType);
					break;
				case ANY:
					isValid = true;
					break;
				default:
					throw new AssertionError();
			}
	        
        }
        catch (InconsistentOntologyException e) {
	        handleInconsistentOntologyException(e);
        }
        
        LOGGER.debug(isValid);
        
        return isValid;
	}
	
	/**
	 * Creates a class expression from an individual based on asserted types and properties. Asserted properties are
	 * interpreted as existential restrictions. SSWAP terms (both types and properties from SSWAP namespace) are 
	 * ignored.
	 */
	private ATermAppl createClassExpression(KnowledgeBase kb, SSWAPNode node, boolean assertIndirectValues) {
		ATermAppl term = ATermUtils.makeTermAppl(node.getURI().toString());		
		Individual ind = kb.getABox().getIndividual(term);
		
		if (ind == null) {
			return null;
		}
		
		return createClassExpression(ind, false /* assertDirectValue */, assertIndirectValues, new HashSet<Edge>());
	}
	
	private ATermAppl createClassExpression(Node ind, boolean assertDirectValue, boolean assertIndirectValues, Set<Edge> visitedEdges) {		
		// every individual contains a nominal for its own name, this should be ignored
		ATermAppl nominal = ATermUtils.makeValue(ind.getName()); 
		List<ATermAppl> types = Lists.newArrayList();
		for (Entry<ATermAppl,DependencySet> entry : ind.getDepends().entrySet()) {
			ATermAppl type = entry.getKey();
			DependencySet ds = entry.getValue();
	        if (isAsserted(ds) && !type.equals(nominal) && !isSSWAPTerm(type)) {
	        	types.add(entry.getKey());
	        }
        }
		
		if (ind.isIndividual()) {
			for (Edge edge : ((Individual) ind).getOutEdges()) {
				ATermAppl p = edge.getRole().getName();
		        if (isAsserted(edge.getDepends()) && !isSSWAPTerm(p)) {
		        	// make sure we won't introduce an infinite loop
		        	if (!visitedEdges.contains(edge)) {
		        		visitedEdges.add(edge);
		        		ATermAppl c = createClassExpression(edge.getTo(), assertIndirectValues, assertIndirectValues, visitedEdges);		        	
		        		types.add(ATermUtils.makeSomeValues(p, c));
		        	}
		        }
	        }
			
			if (assertDirectValue && !ModelUtils.isBNodeURI(ind.getName().toString())) {
				types.add(ATermUtils.makeValue(ind.getName()));
			}								
		}
		
		// if we have at least one type, TOP is unnecessary
		if (types.size() > 1) {
			types.remove(ATermUtils.TOP);
			types.remove(ATermUtils.TOP_LIT);
		}
		
		ATermAppl result = ATermUtils.makeAnd( ATermUtils.makeList( types ) );
		
		return result;
	}
	
	private static boolean isAsserted(DependencySet ds) {
		return ds.getBranch() == DependencySet.NO_BRANCH;
	}
	
	private static boolean isSSWAPTerm(ATermAppl term) {
		return term.getName().startsWith(Namespaces.SSWAP_NS);
	}
	
	/**
	 * An enum for storing a type of a property (datatype vs object or undefined)
	 * 
	 * @author Blazej Bulka <blazej@clarkparsia.com>
	 */
	public enum PropertyType {
		DATATYPE, OBJECT, UNDEFINED
	}
	
	/**
	 * An entry for caching information about a property so that we do not have to query the reasoner repeatedly 
	 * about properties. 
	 * 	
	 * @author Blazej Bulka <blazej@clarkparsia.com>
	 */
	static class PropertyInformation {
		/**
		 * A type of the property (object/datatype/unknown) 
		 */
		private PropertyType type;
		
		/**
		 * Whether a property is mark as an annotation property. 
		 */
		private boolean annotation;
		
		/**
		 * A string containing URI of a range of the property (if known), or null
		 */
		private String range;
		
		/**
		 * Creates a new property information entry
		 * 
		 * @param type type of the property
		 * @param annotation information whether the property is an annotation property
		 * @param range range of the property or null
		 */
		public PropertyInformation(PropertyType type, boolean annotation, String range) {
			this.type = type;
			this.annotation = annotation;
			this.range = range;
		}

		/**
		 * Gets the type of the property
		 * 
		 * @return the type of the property
		 */
		public PropertyType getType() {
			return type;
		}
		
		/**
		 * Gets information whether the property is an annotation property
		 * 
		 * @return true if the property is known to be an annotation property, false otherwise
		 */
		public boolean isAnnotation() {
			return annotation;
		}
		
		/**
		 * Gets the range of the property if known
		 * 
		 * @return string containing the URI of the range (XSD datatype or owl:Class) if a range is known, false otherwise
		 */
		public String getRange() {
			return range;
		}
	}
}
