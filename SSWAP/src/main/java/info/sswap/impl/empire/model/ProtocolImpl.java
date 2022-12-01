/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.MappingPattern;
import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.RDG;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPNode;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPProtocol;
import info.sswap.api.model.SSWAPProvider;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.ValidationException;
import info.sswap.impl.empire.model.ProtocolImpl.MappingValidator.MappingType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;

public abstract class ProtocolImpl extends SourceModelImpl implements SSWAPProtocol {
	
	private static final Logger LOGGER = LogManager.getLogger(ProtocolImpl.class);

	// cached version of SSWAPResource to ensure that we return the same object when there are
	// repeated calls. However, as with all caching:
	// TODO: when do we invalidate? (i.e., we need to intercept all sets for sure, but what with
	// changes that can trickle through refresh()? (is that possible?)
	private List<ResourceImpl> resources = null;
	
	/**
	 * Contains mappings from subjects in this protocol graph onto subjects in RDG.
	 * (The keys are subjects in this protocol graph, and values are subjects in RDG.)
	 * One subject in RDG can have multiple subjects from this graph mapped onto it.
	 * This structure is uninitialized until validateAgainstRDG() is called.
	 */
	private Map<SSWAPSubject,SSWAPSubject> subjectMappings = new HashMap<SSWAPSubject,SSWAPSubject>();
	
	/**
	 * Contains mappings from objects in this protocol graph onto objects in RDG.
	 * (The keys are objects in this protocol graph, and values are objects in RDG.)
	 * One object in RDG can have multiple objects from this graph mapped onto it.
	 * This structure is uninitialized until validateAgainstRDG() is called.
	 */
	private Map<SSWAPObject,SSWAPObject> objectMappings = new HashMap<SSWAPObject,SSWAPObject>();

	/**
	 * Flag to determine if default subsumption, or special case equivalency, mapping
	 * should be performed in validation against an RDG.
	 */
	private boolean isEquivalentMapper = false;

	/**
	 * @inheritDoc
	 */
	public SSWAPGraph getGraph() {
		SSWAPResource resource = getResource();

		if (resource != null) {
			return resource.getGraph();
		}

		return null;
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPObject getObject() {
		SSWAPSubject subject = getSubject();

		if (subject != null) {
			return subject.getObject();
		}

		return null;
	}

	/**
	 * @inheritDoc
	 */
	public MappingPattern getPattern() {
		if (isMultiGraphs()) {
			// TODO: How are multi graph patterns treated?
			return MappingPattern.NONE;
		}
		
		Map<SSWAPGraph,Collection<SSWAPSubject>> subjectMap = getMappings();
		Collection<SSWAPSubject> subjects = subjectMap.get(getGraph());
		
		if (subjects == null) {
			// TODO -- is this the best way to handle this (maybe exception would be better?)
			return MappingPattern.NONE;
		}
		
		if (subjects.size() == 1) {
			SSWAPSubject subject = subjects.iterator().next();
			Collection<SSWAPObject> objects = subject.getObjects();
			
			if (objects.size() == 1) {
				// one subject and one object, this means that we have a pair mapping
				return MappingPattern.PAIR;
			}
			else if (objects.size() > 1) {
				// one subject and multiple objects
				return MappingPattern.ONE_TO_MANY;
			}
		}
		else {
			// this data structure is used to verify how many unique URIs of subjects are there
			Set<URI> objectURIs = new HashSet<URI>();
			
			for (SSWAPSubject subject : subjects) {
				Collection<SSWAPObject> objects = subject.getObjects();
				
				if (objects.size() > 1) {
					// multiple objects -> many to many mapping
					return MappingPattern.MANY_TO_MANY;
				}
				else if (objects.size() == 1) {
					// there is one object -- add its URI to the set of objectURIs
					objectURIs.add(objects.iterator().next().getURI());
				}
				
				// if there are more than one unique object URIs then this is many to many mapping
				if (objectURIs.size() > 1) {
					return MappingPattern.MANY_TO_MANY;
				}
			}
			
			if (objectURIs.size() == 1) {
				// all the subjects were mapped to the same object (there is only one unique URI of an object)
				// this is many to one mapping
				return MappingPattern.MANY_TO_ONE;
			}
		}
		
		// could not detect any known pattern 
		// this return statement can only be reached if there was a subject with no mapped object		
		return MappingPattern.NONE;
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPProvider getProvider() {
		SSWAPResource resource = getResource();

		if (resource != null) {
			return resource.getProvider();
		}

		return null;
	}

	Collection<ResourceImpl> getResources() {
		if (resources == null) {
			this.resources = new LinkedList<ResourceImpl>(getAllDependentObjects(ResourceImpl.class));
		}
		
		return resources;
	}	
		
	/**
	 * @inheritDoc
	 */
	public SSWAPResource getResource() {
		Collection<ResourceImpl> resources = getResources();
		
		if (resources.isEmpty()) {
			return null;
		}
		else {
			if (getURI() == null) {
				// if there is no specific URI on this protocol -- return the first sswap:Resource
				return resources.iterator().next();	
			}
			else {
				// if possible, return the sswap:Resource whose URI matches the URI of the graph
				for (SSWAPResource resource : resources) {
					if (resource.getURI().equals(getURI())) {
						return resource;
					}
				}
				
				// if the resource has not been found, return first resource
				return resources.iterator().next();
			}			
		}
	}

	/**
	 * Creates a new ResourceImpl in this protocol graph
	 * 
	 * @param resourceURI
	 *            the URI of the resource to be created
	 * @return the created SSWAPResource
	 */
	public SSWAPResource createResource(URI resourceURI) {
		if (resourceURI == null) {
			resourceURI = URI.create(ModelUtils.generateBNodeId());
		}
		
		Collection<ResourceImpl> resources = getResources();

		ResourceImpl resource = ImplFactory.get().createDependentObject(this, resourceURI, ResourceImpl.class);
		
		if (!resources.contains(resource)) {
			resources.add(resource);
		}

		return resource;
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPSubject getSubject() {
		SSWAPGraph graph = getGraph();

		if (graph != null) {
			return graph.getSubject();
		}

		return null;
	}

	/**
	 * @inheritDoc
	 */
	public Map<SSWAPGraph, Collection<SSWAPSubject>> getMappings() {
		Map<SSWAPGraph, Collection<SSWAPSubject>> result = new HashMap<SSWAPGraph, Collection<SSWAPSubject>>();
		SSWAPResource resource = getResource();

		if (resource != null) {
			for (SSWAPGraph graph : resource.getGraphs()) {
				result.put(graph, graph.getSubjects());

			}
		}

		return result;
	}

	/**
	 * @inheritDoc
	 */
	public boolean isMultiGraphs() {
		SSWAPResource resource = getResource();

		if (resource != null) {
			return (resource.getGraphs().size() > 1);
		}

		return false;
	}

	/**
	 * @inheritDoc
	 */
	public boolean isPattern(MappingPattern pattern) {
		return getPattern().equals(pattern);
	}	
	
	/**
	 * Populates this graph (must not be dereferenced yet) with the contents of another graph. The
	 * implementation of this method serializes the other graph, and then dereferences this graph from
	 * the serialization of that other graph.
	 * 
	 * @param otherGraph the source graph for the contents
	 * @throws DataAccessException in the unlikely case there is an I/O issue when populating the new graph with the contents of the old graph
	 */
	protected void dereference(ProtocolImpl otherGraph) throws DataAccessException {
		ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
		otherGraph.serialize(intermediateOutputStream);		
		ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream
		                .toByteArray());
		
		dereference(intermediateInputStream);
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPGraph createGraph() {
		SourceModel sourceModel = assertSourceModel();

		return ImplFactory.get().createDependentObject(sourceModel, URI.create(ModelUtils.generateBNodeId()),
		                GraphImpl.class);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPSubject createSubject() {
		return createSubject(null);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPSubject createSubject(URI uri) {
		SourceModel sourceModel = assertSourceModel();

		URI subjectURI = (uri == null) ? URI.create(ModelUtils.generateBNodeId()) : uri;

		return ImplFactory.get().createDependentObject(sourceModel, subjectURI, SubjectImpl.class);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPObject createObject() {
		return createObject(null);		
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPObject createObject(URI uri) {
		SourceModel sourceModel = assertSourceModel();

		URI objectURI = (uri == null) ? URI.create(ModelUtils.generateBNodeId()) : uri;

		return ImplFactory.get().createDependentObject(sourceModel, objectURI, ObjectImpl.class);
	}
	
	@Override
	public void validate() throws ValidationException {
		validate(false /* strict */);
	}
	
	@Override
	public void validate(boolean strict) throws ValidationException {
		super.validate(strict);
		
		SSWAPResource resource = getResource();
		
		if (resource == null) {
			throw new ValidationException("sswap:Resource missing from the protocol graph");
		}
		else {
			try {
				resource.validate();
				
				/*
				if ((resource.getURI() != null) && !resource.getURI().equals(getURI())) {
					throw new ValidationException("URI of sswap:Resource does not match the URI of the protocol graph");
				}
				*/
			}
			catch (IllegalArgumentException e) {
				throw new ValidationException("Invalid data found: " + e.getMessage());
			}
		}
	}	
	
	/**
	 * Generates a copy of this Protocol document, where all SSWAPNodes (i.e., nodes with special meaning to SSWAP Protocol; e.g., sswap:Resource)
	 * have been converted into BNodes.
	 * 
	 * This method is typically used when reasoning is needed when using information from multiple documents (e.g., Protocol graph and RDG), and when it
	 * is necessary to treat corresponding objects from these documents as different individuals. For example, sswap:Resources frequently
	 * have their own URIs, which are identical in RDG/RIG/RRG, and this will cause the reasoner to treat all these resources as being the same individual;
	 * in the cases when these individuals differ (e.g., sswap:inputURI is different), this may lead to undesired effects (e.g., inconsistent ontology). 
	 * 
	 * @return a copy of this Protocol document but with all SSWAPNodes turned into blank nodes.
	 */
	protected ProtocolImpl anonymizeSSWAPNodes() {		
    	URI uri = URI.create(ModelUtils.generateBNodeId());
    	
    	// create the instance of the implementation class (i.e., this method will create RDG object, if this is an RDG object)
		ProtocolImpl result = ImplFactory.get().createEmptySSWAPDataObject(uri, getClass());
	
		Model newModel = JenaModelFactory.get().createEmptyModel();
		newModel.add(getModel());
		ModelUtils.convertSSWAPNodesToBNodes(newModel);
		
		result.dereference(newModel);
		
		return result;
	}
	
	/**
	 * Checks whether the particular implementation of Protocol Graph supports translation of terms (e.g., RIG)
	 * 
	 * @return true if the implementation supports translation of terms, false otherwise
	 */
	protected boolean supportsTranslation() {
		// by default we do not support it, the classes that support it have to override this method
		return false;
	}
	
	protected boolean validatesObjects() {
		return false;
	}
	
	/**
	 * Returns information whether this object needs default values of parameters set during validation against RDG
	 * (i.e., default values for sswap:Resource or sswap:Subject).
	 * Most protocol types require values of default parameters.
	 * 
	 * @return true if this object requires default values of parameters
	 */
	protected boolean needsDefaultParametersSet() {
		return true;
	}
	
	protected boolean validatesResourceURIMatch() {
		return true;
	}
	
	/**
	 * Gets the translation map for implementations of Protocol Graphs that support translation.
	 * The translation map maps a resource from this graph onto its translated counterpart.
	 * The map returned by this method is modifiable (validateAgainstRDG() method will attempt
	 * to store the mappings) 
	 * 
	 * @return a map whose keys are the individuals from this graph, and the values are translated individuals
	 */
	protected Map<SSWAPNode,SSWAPNode> getTranslationMap() {
		// by default we return null, because supportsTranslation() returns false
		return null;
	}
	
	protected void setMatchingGraphs(Collection<SSWAPGraph> matchingGraphs) {
		// nothing
	}
	
	/**
	 * Gets the human readable name of the particular graph implementation (e.g., RDG, RQG, RIG, RRG)
	 * This method is mostly provided to generate meaningful messages in exceptions (rather than to 
	 * include class name, which may not be familiar to most users)
	 * 
	 * @return the name of this protocol graph implementation
	 */
	public abstract String getGraphType();//Pavel: made public to be accessible to model resolvers who do different things for different kinds of graphs
	
	protected boolean needsClosedWorldForValidation() {
		return true;
	}
	
	public void closeWorld() {
		ResourceImpl resource = (ResourceImpl) getResource();
		
		resource.closeWorld();
		
		for (SSWAPGraph graph : getResource().getGraphs()) {
			for (SSWAPSubject subject : graph.getSubjects()) {
				((SubjectImpl) subject).closeWorld();
			}
		}
	}
	
	public void uncloseWorld() {
		this.getClosedWorldModel().removeAll();
	}
	
	/**
	 * Validates this Protocol message against an RDG.
	 * 
	 * @param rdg
	 *            the RDG against which the protocol graph should be validated
	 * @throws ValidationException
	 *             if validation fails
	 */
	public void validateAgainstRDG(RDG rdg) throws ValidationException {
		ReasoningServiceImpl reasoningService = (ReasoningServiceImpl) getReasoningService();		
				
		if (needsClosedWorldForValidation()) {
			closeWorld();
		}
		
		boolean automaticTermRetrieval = reasoningService.isAutomaticTermRetrieval();
		boolean crossDocumentTermRetrieval = reasoningService.isCrossDocumentTermRetrieval();
		
		reasoningService.setAutomaticTermRetrieval(false);
		reasoningService.setCrossDocumentTermRetrieval(false);
		
		try {
			SSWAPResource thisResource = getResource();
			SSWAPResource rdgResource = rdg.getResource();

			if (validatesResourceURIMatch()) {
				
				URI thisProtocolURI = getURI(),
					thisResourceURI = thisResource.getURI(),
					rdgURI = rdgResource.getURI();
				
				if (!thisProtocolURI.equals(rdgURI)) {
					LOGGER.warn("Accepting " + getGraphType() + " (" + thisProtocolURI + ") whose URI does not match the URI of the RDG (" + rdgURI + ")");
					//throw new ValidationException(String.format("The URIs of the RDG and %s graphs do not match", getGraphType()));
				}

				if (!thisResourceURI.equals(rdgURI)) {
					//throw new ValidationException(String.format("The URI of the sswap:Resource in the RDG (" + rdgURI + ") does not match the URI of sswap:Resource in the %s (%s)", getGraphType(),thisURI.toString()));
					throw new ValidationException("Rejecting " + getGraphType() + " (" + thisProtocolURI + "): URI of its sswap:Resource (" + thisResourceURI + ") does not match the URI of the RDG (" + rdgURI + ")");
				}
				
				if (!thisProtocolURI.equals(thisResourceURI)) {
					LOGGER.warn("Accepting " + getGraphType() + " (" + thisProtocolURI + ") whose URI does not match the URI of its own sswap:Resource (" + thisResourceURI + ")");
					// throw new ValidationException(String.format("The URIs of the SSWAPResource and its %s graph do not match", getGraphType()));
				}
			}

			// at this point, after all the URIs have been compared, an "anonymized" version of the RDG is prepared
			// to prevent any clashes with URIs in this Protocol Graph while reasoning (esp. sswap:Resources from RDG and Protocol Graph
			// are likely to clash)
			RDG rdgWithAnonymizedNodes = (RDG) ((ProtocolImpl) rdg).anonymizeSSWAPNodes();
			ReasoningServiceImpl anonRdgReasoningService = (ReasoningServiceImpl) rdgWithAnonymizedNodes.getReasoningService();
			anonRdgReasoningService.setAutomaticTermRetrieval(false);
			anonRdgReasoningService.setCrossDocumentTermRetrieval(false);	

			rdgResource = rdgWithAnonymizedNodes.getResource();

			// add the information from the RDG (to the already existing information from this Protocol Graph) to be taken
			// into account when reasoning. This is done to ensure that all the types (including anonymous types) that are
			// defined
			// in the RDG can be accessed by the reasoning service.
			reasoningService.addModel(rdgWithAnonymizedNodes);
			
			if (needsDefaultParametersSet()) {
				((ResourceImpl) thisResource).setDefaultParameterValues((ResourceImpl) rdgResource);
			}

			if (!this.getResourceMappingValidator().isMappingValid(thisResource, rdgResource)) {
				throw new ValidationException(String.format("The sswap:Resource in the %s does not meet the restrictions listed in the RDG", getGraphType()));
			}

			if (supportsTranslation()) {
				SourceModelImpl translatedResourceSourceModel = (SourceModelImpl) reasoningService.translate(
								rdgResource.getDeclaredType(), thisResource);
				SSWAPResource translatedResource = translatedResourceSourceModel.getDependentObject(ResourceImpl.class);

				if (translatedResource == null) {
					throw new ValidationException(String.format("The sswap:Resource in the %s does not meet the restrictions listed in the RDG", getGraphType()));
				}

				copyNonTranslatedProperties(translatedResource, thisResource, rdgResource); 
				getTranslationMap().put(thisResource, translatedResource);
				((ResourceImpl) translatedResource).setOriginalResource((ResourceImpl) thisResource); 
			}		
			
			Collection<SSWAPGraph> rdgGraphs = rdgResource.getGraphs();
			List<SSWAPGraph> matchingGraphs = new LinkedList<SSWAPGraph>();
			ValidationException lastValidationException = null;
			
			for (SSWAPGraph protocolGraph : thisResource.getGraphs()) {
				try {
					validateGraph(protocolGraph, rdgGraphs, reasoningService);
					matchingGraphs.add(protocolGraph);
				}
				catch (ValidationException e) {
					lastValidationException = e;
				}
			}
			
			if ((lastValidationException != null) && (matchingGraphs.isEmpty())) {
				throw lastValidationException;
			}
			
			setMatchingGraphs(matchingGraphs);
			
			// remove the RDG's information from the reasoning service for this Protocol Graph (so that the callers of
			// this method can reuse it without any side-effects)
			reasoningService.removeModel(rdgWithAnonymizedNodes);		
		}
		finally {
			reasoningService.setAutomaticTermRetrieval(automaticTermRetrieval);
			reasoningService.setCrossDocumentTermRetrieval(crossDocumentTermRetrieval);
		
			if (needsClosedWorldForValidation()) {
				uncloseWorld();
			}
		}
	}
	
	private void validateGraph(SSWAPGraph protocolGraph, Collection<SSWAPGraph> rdgGraphs, ReasoningServiceImpl reasoningService) throws ValidationException {
		ValidationException lastValidationException = null;
		
		for (SSWAPGraph rdgGraph : rdgGraphs) {
			lastValidationException = null;
			
			try {
				// compute an acceptable mapping between subjects in the Protocol graph and the subjects in RDG
				Map<SSWAPSubject, SSWAPSubject> subjectMapping = mapIndividuals(protocolGraph.getSubjects(), rdgGraph.getSubjects(), getSubjectMappingValidator());

				if (subjectMapping == null) {
					throw new ValidationException(String.format("Unable to produce any valid mapping of subjects between the %s and the RDG", getGraphType()));
				}

				subjectMappings.putAll(subjectMapping);

				// for each subject pair (one from RDG and one from Protocol graph) try to map their objects (one object from Protocol graph to the
				// corresponding
				// object in the RDG).
				ValidationException lastSubjectValidationException = null;
				boolean anySubjectPassedValidation = false;
				
				for (SSWAPSubject protocolSubject : subjectMapping.keySet()) {
					boolean thisSubjectPassedValidation = false;
					SSWAPSubject rdgSubject = subjectMapping.get(protocolSubject);

					if (rdgSubject != null) {
						if (needsDefaultParametersSet()) {
							((SubjectImpl) protocolSubject).setDefaultParameterValues((SubjectImpl) rdgSubject);
						}

						if (supportsTranslation()) {
							try {
								SourceModelImpl sourceModel = (SourceModelImpl) reasoningService.translate(
												rdgSubject.getDeclaredType(), protocolSubject);

								SSWAPSubject translatedSubject = sourceModel.getDependentObject(TranslatedSubjectImpl.class);
								((TranslatedSubjectImpl) translatedSubject).setOriginalSubject(protocolSubject);
								copyNonTranslatedProperties(translatedSubject, protocolSubject, rdgSubject);
								getTranslationMap().put(protocolSubject, translatedSubject);
								anySubjectPassedValidation = true;
								thisSubjectPassedValidation = true;
							}
							catch (ValidationException e) {							
								lastSubjectValidationException = e;
							}
						}
						else {
							// if there is no translation/validation then every subject at this point is considered to have passed
							// (after all, it has passed subject mapping validator)
							anySubjectPassedValidation = true;
							thisSubjectPassedValidation = true;
						}

						Collection<SSWAPObject> rdgObjects = rdgSubject.getObjects();
						Collection<SSWAPObject> protocolObjects = protocolSubject.getObjects();
						
						Map<SSWAPObject,SSWAPObject> objectMapping = mapIndividuals(protocolObjects, rdgObjects, getObjectMappingValidator());

						if (thisSubjectPassedValidation && (objectMapping == null)) {
							throw new ValidationException(
											String.format("Unable to produce any valid mapping of objects between the %s and the RDG", getGraphType()));
						}
						
						if (thisSubjectPassedValidation && validatesObjects()) {
							validateMappedObjects(reasoningService, objectMapping);
						}

						if (objectMapping != null) {
							objectMappings.putAll(objectMapping);
						}
					}					
				}
				
				if (!anySubjectPassedValidation) {
					if (lastSubjectValidationException != null) {
						throw lastSubjectValidationException;
					}
					else {
						throw new ValidationException("No subject passed cardinality restrictions specified in the RDG");
					}
				}
				
				break; // at this point we verified that the protocolGraph's sswapGraph matched one of RDG graphs
			}
			catch (ValidationException e) {
				lastValidationException = e;
			}
		}
		
		if (lastValidationException != null) {
			throw lastValidationException;
		}
	}
	
	private void validateMappedObjects(ReasoningServiceImpl reasoningService, Map<SSWAPObject,SSWAPObject> mappedObjects) throws ValidationException {
		ValidationException lastValidationException = null;
		boolean objectPassedValidation = false;
		
		for (SSWAPObject protocolObject : mappedObjects.keySet()) {
			SSWAPObject rdgObject = mappedObjects.get(protocolObject);
			
			if (rdgObject != null) {
				try {
					reasoningService.translate(rdgObject.getType(), protocolObject);
					objectPassedValidation = true;
				}
				catch (ValidationException e) {
					lastValidationException = e;
				}
			}
		}
 		
		if (!objectPassedValidation) {
			if (lastValidationException != null) {
				throw lastValidationException;
			}
			else {
				throw new ValidationException("No object passed cardinality restrictions specified in the RDG");
			}
		}
	} 
	
	/**
	 * Gets the computed subject mappings during the recent validateAgainstRDG() invocation.
	 * 
	 * @return a map where keys are subjects in this graph and values are subjects in RDG (value may be null, if there is no mapping)
	 */
	protected Map<SSWAPSubject,SSWAPSubject> getSubjectMappings() {
		return subjectMappings;
	}

	/**
	 * Gets the computed object mappings during the recent validateAgainstRDG() invocation.
	 * 
	 * @return a map where keys are objects in this graph and values are objects in RDG (value may be null, if there is no mapping)
	 */
	protected Map<SSWAPObject,SSWAPObject> getObjectMappings() {
		return objectMappings;
	}
		
	/**
	 * Attempts to map individuals from this Protocol graph onto their corresponding individuals from RDG. Since it is expected that individuals in the RDG
	 * express a requirement for the Protocol graph, every individual in the RDG needs to have at least one individual mapped from the Protocol graph (required mapping).
	 * If it is not possible to compute such a required mapping, this method will return null (to indicate failure). 
	 * 
	 * Once every individual in RDG has an associated individual in Protocol graph associated with it, this method will attempt to find mapping for 
	 * any other remaining individuals in Protocol graph (if any; this is optional mapping). Because of the optional mapping, it is possible that an 
	 * individual in RDG will have more than one individual in the Protocol graph mapped onto it. Similarly, it is possible that an individual in the Protocol Graph may not
	 * have a corresponding element in RDG.
	 * 
	 * @param <T> the type of individuals being mapped (e.g., SSWAPSubject)
	 * @param protocolIndividuals the individuals in the Protocol Graph to be mapped
	 * @param rdgIndividuals the individuals in RDG to be mapped
	 * @param individualMapper an object that can decide whether an individual from Protocol Graph is a valid mapping for the individual from RDG
	 * @return a map whose keys are individuals in Protocol Graph, and the values are individuals in RDG (a value may be null, if there is no mapping
	 * for that particular Protocol graph individual). The map returned may be null, if it was not possible to establish the required mappings 
	 */
	private <T extends SSWAPIndividual> Map<T, T> mapIndividuals(Collection<T> protocolIndividuals, Collection<T> rdgIndividuals,
	                MappingValidator<T> individualMapper) {
		// the required mappings -- the keys are individuals in RDG and values are individuals in the Protocol graph (reverse of what the final result will be!)
		Map<T,T> requiredMappings = requiredMappings(rdgIndividuals, protocolIndividuals, individualMapper);
		
		if (requiredMappings == null) {
			// unable to find any mapping for the required mappings
			return null;
		}
				
		// prepare result -- keys here are individuals in this Protocol graph and the values are individuals in RDG
		Map<T,T> result = new HashMap<T,T>();
		
		// for required mappings just store them in the result map -- swapping keys and values
		for (T rdgIndividual : requiredMappings.keySet()) {
			result.put(requiredMappings.get(rdgIndividual), rdgIndividual);
		}
		
		// map now all the Protocol graph individuals which were not involved in required mappings		
		for (T protocolIndividual : protocolIndividuals) {
			if (!result.containsKey(protocolIndividual)) {
				result.put(protocolIndividual, optionalMapping(protocolIndividual, rdgIndividuals, individualMapper));				
			}
		}
				
		return result;		
	}
	
	/**
	 * Computes required mappings from RDG individuals to individuals in this protocol graph. Since an
	 * individual in the Protocol graph can be potentially mapped to multiple corresponding individuals in the RDG, in order to compute a
	 * full mapping (involving all individuals in RDG), backtracking may be necessary, as well multiple proper solutions may be
	 * possible. This method returns the first full mapping.
	 * 
	 * While this method uses a brute-force search of all possible mappings, the typical amounts of subjects or objects
	 * in the RDG tend to be fairly small (is that true?). If this is too slow, three solutions are possible: since many
	 * calls share the parts of the solution caching/memoization may be possible; a more advanced Constraint
	 * Satisfaction Solver may also be useful; and more intelligent techniques for backtracking may also be useful)
	 * 
	 * This method calls requiredMapping() method, which in turn recursively calls this method. First, this method picks the
	 * first element from the remaining rdg individuals, and calls requiredMapping(), so that it finds a mapping (an individual in the protocol graph)
	 * for that rdg individual. After a potential mapping is found, requiredMapping() calls requiredMappings() to find the mappings for
	 * the remaining individuals. If at any step a mapping cannot be produced, a null map is returned, and the methods
	 * backtrack to the next potential mapping.
	 * 
	 * @param <T>
	 * @param rdgIndividuals the RDG individuals
	 * @param protocolIndividuals the Protocol individuals
	 * @param individualMapper an object that can decide whether an individual in RDG can be mapped onto a particular individual in the protocol graph
	 * @return a map whose keys are RDG individuals, and the values are Protocol graph individuals
	 */
	private <T extends SSWAPIndividual> Map<T, T> requiredMappings(Collection<T> rdgIndividuals, Collection<T> protocolIndividuals,
	                MappingValidator<T> individualMapper) {
		// if there are no RDG individuals to map, we got a successful (empty mapping). This is the base case of the recursion.
		if (rdgIndividuals.isEmpty()) {
			return new HashMap<T, T>();
		}

		if (protocolIndividuals.size() < rdgIndividuals.size()) {
			return null; // there are just not enough Protocol graph individuals to produce a mapping
		}

		
		// try to select a specific RDG individual to find a mapping for it
		for (T rdgIndividual : rdgIndividuals) {
			// compute the list of all other RDG individuals (that still have to be mapped).
			Collection<T> remainingRdgIndividuals = filter(new HashSet<T>(), rdgIndividuals, rdgIndividual);

			// call requiredMapping to find a mapping for that RDG individual; requiredMapping will call this (requiredMappings) method back
			// to find mappings for remaining RDG individuals
			Map<T, T> result = requiredMapping(rdgIndividual, remainingRdgIndividuals, protocolIndividuals, individualMapper);			
			
			if (result != null) {
				// a mapping was found -- finish recursion
				return result;
			}
			
			// no mapping could be found, if that rdgIndividual had his mapping assigned at this time; backtrack by picking another one to
			// find mapping first
			
			// TODO: is the split into two methods required anymore? if a mapping cannot be produced when selecting a particular RDG individual
			// as first (without selecting any corresponding Protocol Graph individual), then the mapping is impossible (?). The ordering of assignment
			// influences only the performance in CSPs, after all, not the completeness. (and we are not optimizing here the ordering in any way yet
			// since in most cases it is probably more costly than the benefit ...)
		}

		// no mappings found
		return null;
	}

	/**
	 * Finds a mapping for a selected RDG individual. This method is a second step in the recursive backtracking algorithm for finding a 
	 * mapping. See the comment for requiredMappings() (the first step). 
	 * 
	 * @param <T>
	 * @param rdgIndividual the selected rdgIndividual
	 * @param remainingRdgIndividuals the remaining RDG individuals for which mappings still have to be found
	 * @param protocolIndividuals individuals in this protocol graph which have not yet been used in a mapping
	 * @param individualMapper an object that can decide whether an individual in RDG can be mapped onto a particular individual in the protocol graph
	 * @return a map whose keys are RDG individuals, and the values are individuals in this protocol graph
	 */
	private <T extends SSWAPIndividual> Map<T, T> requiredMapping(T rdgIndividual, Collection<T> remainingRdgIndividuals,
	                Collection<T> protocolIndividuals, MappingValidator<T> individualMapper) {
		// iterate for every possible protocol individual
		for (T protocolIndividual : protocolIndividuals) {
			// check whether the protocol individual is a valid mapping for the RDG individual
			if (individualMapper.isMappingValid(protocolIndividual, rdgIndividual)) {
				// a map of all other protocol individuals (they may still be mapped)
				Collection<T> remainingProtocolIndividuals = filter(new HashSet<T>(), protocolIndividuals, protocolIndividual);
				
				// invoke recursively requiredMappings to compute mappings between remaining (unmapped) RDG individuals and protocol individuals			
				Map<T, T>  result = requiredMappings(remainingRdgIndividuals, 
								                     remainingProtocolIndividuals,
								                     individualMapper);

				if (result != null) {
					Map<T,T> resultCopy = new HashMap<T,T>(result);
					
					// a full mapping was found -- record the current pair (RDG individual->Protocol individual), and return
					resultCopy.put(rdgIndividual, protocolIndividual);

					return resultCopy;
				}
				
				// no mapping was found with this assignment, so try another Protocol individual				
			}
		}

		// no mappings found		
		return null;
	}

	/**
	 * Finds an optional mapping between a single Protocol graph individual and an RDG individual (which may be already involved in some mapping;
	 * for optional mappings this is allowed).
	 * 
	 * @param <T>
	 * @param protocolIndividual the Protocol graph individual for which the mapping should be found
	 * @param rdgIndividuals candidate RDG individuals
	 * @param individualMapper an object that can decide whether an individual in RDG can be mapped onto a particular individual in this protocol graph
	 * @return an individual from RDG that can be mapped onto the given protocol graph individual, null if there are no RDG individuals that can be mapped
	 */
	private <T extends SSWAPIndividual> T optionalMapping(T protocolIndividual, Collection<T> rdgIndividuals, MappingValidator<T> individualMapper) {
		for (T rdgIndividual : rdgIndividuals) {			
			if (individualMapper.isMappingValid(protocolIndividual, rdgIndividual)) {				
				return rdgIndividual;
			}
		}
		
		return null;
	}
	
	/**
	 * A convenience method that filters a specified element from the collection: i.e., it adds items to a new
	 * collection without the specified element
	 * 
	 * @param <T>
	 *            the type of objects in the collections
	 * @param newCollection
	 *            the collection to which the non-filtered items are to be added
	 * @param oldCollection
	 *            the collections that contains elements to be filtered
	 * @param filteredElement
	 *            the element to be filtered
	 * @return the newCollection (i.e., the copy of oldCollection but without any occurrences of filteredElement)
	 */
	private static <T> Collection<T> filter(Collection<T> newCollection, Collection<T> oldCollection, T filteredElement) {
		for (T element : oldCollection) {
			if (!element.equals(filteredElement)) {
				newCollection.add(element);
			}
		}

		return newCollection;
	}
	
	/**
	 * Copies the properties that were not translated for a Protocol graph node but they are both mentioned in RDG (default property), and the original
	 * (untranslated) Protocol graph node. Such properties are typically not translated because they were not defined in subject's type, but
	 * they were still mentioned in RDG.
	 * 
	 * This method should be invoked on the translated individual (e.g., SSWAPSubject).
	 * 
	 * @param nonTranslatedIndividual the node in the protocol graph (before translation)
	 * @param rdgIndividual the corresponding node in RDG
	 */
	private void copyNonTranslatedProperties(SSWAPIndividual translatedIndividual, SSWAPIndividual nonTranslatedIndividual, SSWAPIndividual rdgIndividual) {
		Set<SSWAPProperty> originalProperties = new HashSet<SSWAPProperty>(nonTranslatedIndividual.getProperties());
		Set<SSWAPProperty> rdgProperties = new HashSet<SSWAPProperty>(rdgIndividual.getProperties());
		
		for (SSWAPProperty originalProperty : originalProperties) {
			SSWAPProperty rdgProperty = getCorrespondingRDGProperty(originalProperty, rdgProperties);
			
			if ((rdgProperty != null) && (translatedIndividual.getProperty(originalProperty.getPredicate()) == null)) {
				copyPropertyValues(rdgProperty.getURI(), translatedIndividual, originalProperty.getURI(), nonTranslatedIndividual);
			}
		}
	}
	
	/**
	 * Performs a deep copy of all values for the given property from the specified individual to this individual.
	 * 
	 * @param dstPropertyURI the URI of the property in this (destination) individual to where the values should be copied
	 * @param srcPropertyURI the URI of the property in the source individual
	 * @param srcOriginal the object from which the values should be copied
	 */
	private void copyPropertyValues(URI dstPropertyURI, SSWAPIndividual dstIndividual, URI srcPropertyURI, SSWAPIndividual srcOriginal) {
		for (SSWAPProperty srcProperty : srcOriginal.getProperties(srcOriginal.getDocument().getPredicate(srcPropertyURI))) {
			SSWAPPredicate dstPredicate = dstIndividual.getDocument().getPredicate(dstPropertyURI);
			
			SSWAPElement value = ImplFactory.get().deepCopyElement(getSourceModel(), srcProperty.getValue(), new HashSet<URI>());
			
			if (value.isIndividual()) {
				dstIndividual.addProperty(dstPredicate, value.asIndividual());
			}
			else if (value.isLiteral()) {
				dstIndividual.addProperty(dstPredicate, value.asLiteral());
			}
			
			persist();
		}
	}
	
	/**
	 * Returns the corresponding property in the RDG individual for the specified Protocol graph property.
	 * A corresponding property is a property that is a super-property of the given property.
	 * 
	 * @param protocolProperty Protocol graph property
	 * @param rdgProperties all properties mentioned in the RDG individual
	 * @return the property that is corresponding to the given Protocol graph property or null, if there is no such property
	 */
	private static SSWAPProperty getCorrespondingRDGProperty(SSWAPProperty protocolProperty, Collection<SSWAPProperty> rdgProperties) {
		for (SSWAPProperty rdgProperty : rdgProperties) {
			if (protocolProperty.getPredicate().isSubPredicateOf(rdgProperty.getPredicate())) {
				return rdgProperty;
			}
		}
		
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public void serialize(OutputStream os, RDFRepresentation rdfRepresentation, boolean commentedOutput) {
		SSWAPResource resource = getResource();
		
		if ((resource == null) || (resource.getURI() == null)) {
			super.serialize(os, rdfRepresentation, commentedOutput);
		}
		else {
			persist();

			Model fullModel = assertModel();
			Model partitionedModel = ModelUtils.partitionModel(fullModel, resource.getURI().toString(), true);
			
			ModelUtils.serializeModel(partitionedModel, os, rdfRepresentation, /* commentedOutput */ commentedOutput);						
		}		
	}


	/**
	 * When validating one protocol graph against another, usually some combination of subsumption checking
	 * is performed (e.g., that the RIG:sswapResource is a subType of the target RDG:sswapResource, etc.).
	 * By setting isEquivalentMapper == true, type equivalency, rather than subsumption is enforced. This is
	 * used to determine if a data-rich, upstream service can be converted into an RQG based solely on its
	 * published RDG, or if the actual upstream data must be considered in reasoning.
	 *  
	 * @param isEquivalentMapper true to enforce equivalency; false otherwise. Default is false.
	 * @see #validateAgainstRDG(RDG)
	 * @see RQGImpl
	 * @see info.sswap.pipeline.ui.controllers.DiscoveryServerController#getDataSetAsRDG(URI)
	 */
	
	/*
	 * One cannot easily extend ProtocolImpl and use casting and @Override methods because Empire handles
	 * implementation instantiation. Extending ProtocolImpl requires various hard code changes.
	 * Thus equivalency mapping is implemented here directly in ProtocolImpl.
	 */
	
	public void setEquivalentMapper(boolean isEquivalentMapper) {
		this.isEquivalentMapper = isEquivalentMapper;
	}
	
	public boolean isEquivalentMapper() {
		return isEquivalentMapper;
	}
	
	public void resetEquivalentMapper() {
		setEquivalentMapper(false);
	}
	
	protected MappingValidator<SSWAPResource> getResourceMappingValidator() {
		return isEquivalentMapper() ? new DefaultMappingValidator<SSWAPResource>(MappingType.EQUIVALENT) 
						            : new DefaultMappingValidator<SSWAPResource>(MappingType.SUB);
	}
	
	protected MappingValidator<SSWAPSubject> getSubjectMappingValidator() {
		return isEquivalentMapper() ? new DefaultMappingValidator<SSWAPSubject>(MappingType.EQUIVALENT) 
		                            : new DefaultMappingValidator<SSWAPSubject>(MappingType.SUB);
	}

	protected MappingValidator<SSWAPObject> getObjectMappingValidator() {
		return isEquivalentMapper() ? new DefaultMappingValidator<SSWAPObject>(MappingType.EQUIVALENT) 
		                            : new DefaultMappingValidator<SSWAPObject>(MappingType.SUPER);
	}
	
	@Override
	public int doClosure() {		
		//System.out.println("+++Computing closure for " + getGraphType() + "+++");
		//resetReasoningService();
		setClosureModel(getModelResolver().resolveProtocolModel(this));

		return 0;
	}	
	
	@Override
	protected <T extends SSWAPIndividual> T createCopyObject(T original, URI copyURI) {
		if (original instanceof SSWAPSubject) {
			return (T) createSubject(copyURI);
		}
		else if (original instanceof SSWAPObject) {
			return (T) createObject(copyURI);
		}
		else if (original instanceof SSWAPGraph) {
			return (T) createGraph();
		}
		else if (original instanceof SSWAPResource) {
			return (T) createResource(copyURI);
		}
		else {
			return super.createCopyObject(original, copyURI);
		}
	}

	/**
	 * Performs a check whether an individual from ProtocolGraph can correspond to another individual from RDG. 
	 * The concrete implementations of this class will perform specific checks for individuals (e.g.,
	 * for subjects they will check whether the Protocol graph's subject is a subclass of the RDG's subject).
	 * 
	 * @author Blazej Bulka <blazej@clarkparsia.com>
	 *
	 * @param <T> the type that should be checked
	 */
	public interface MappingValidator<T> {
		public enum MappingType {
			/**
			 * Protocol type should be subclass of RDG type
			 */
			SUB,
			/**
			 * Protocol type should be subclass of RDG type or protocol type should be equal to TOP (owl:Thing)
			 */
			SUB_IF_NOT_TOP,
			/**
			 * Protocol type should be a super class of RDG type
			 */
			SUPER,
			/**
			 * Protocol type should be equivalent to RDG type
			 */
			EQUIVALENT,
			/**
			 * Protocol type  
			 */
			ANY
			
		}
		
		/**
		 * Checks whether it is valid to associate the individual in the Protocol graph with the individual in the RDG.
		 * 
		 * @param protocolIndividual the individual from the Protocol graph that should be checked
		 * @param rdgIndividual the individual from the RDG that should be checked
		 * @return true if the two individuals can be associated
		 */
		public boolean isMappingValid(T protocolIndividual, T rdgIndividual);
	}
	
	public class DefaultMappingValidator<T extends SSWAPNode> implements MappingValidator<T> {
		private final MappingType mappingType;
		
		public DefaultMappingValidator() {
			this(MappingType.EQUIVALENT);
		}
		
        public DefaultMappingValidator(MappingType mappingType) {
	        this.mappingType = mappingType;
        }

		public boolean isMappingValid(T protocolIndividual, T rdgIndividual) {
			return ((ReasoningServiceImpl) protocolIndividual.getReasoningService()).isMappingValid(protocolIndividual,
			                                                                                        mappingType,
			                                                                                        rdgIndividual);
        }
	}	
}
