/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RQG;
import info.sswap.api.model.RRG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPNode;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPProtocol;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.SSWAPType;
import info.sswap.api.model.ValidationException;
import info.sswap.impl.empire.Namespaces;
import info.sswap.impl.empire.Vocabulary;
import info.sswap.impl.empire.model.ProtocolImpl.MappingValidator.MappingType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of RRG interface
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public abstract class RRGImpl extends ProtocolImpl implements RRG {
	/**
	 * A flag indicating whether this RRG is mutable. In general, RRGs should be immutable 
	 * only on the client-side, and mutable on the server-side.
	 */
	private boolean mutable = true;
	
	/**
	 * sswap/util/Null marker class.
	 * Used on a sswapSubject to designate that a RDG/RIG requires no input.
	 */
	private static final URI SSWAP_UTIL_NULL = URI.create(Namespaces.SSWAP_UTIL_NS + "Null");
	
	private Map<SSWAPNode,SSWAPNode> translatedNodes = new HashMap<SSWAPNode,SSWAPNode>();
	
	private void assertMutable() {
		if (!mutable) {
			throw new IllegalArgumentException("This RRG is immutable");
		}
	}
	
    @Override
    protected boolean supportsTranslation() {
    	return true;
    }
    
    @Override
    protected Map<SSWAPNode,SSWAPNode> getTranslationMap() {
    	return translatedNodes;
    }
	
	/**
	 * Sets whether this object is mutable.
	 * 
	 * @param mutable true, if the object should be mutable, false otherwise 
	 */
	void setMutable(boolean mutable) {
		this.mutable = mutable;
	}
	
	/**
	 * Checks whether this object is mutable.
	 * 
	 * @return mutable true, if the object should be mutable, false otherwise
	 */
	public boolean isMutable() {
		return mutable;
	}
	
	@Override
	public void setNsPrefix(String prefix, URI uri) {
		assertMutable();
		
		super.setNsPrefix(prefix, uri);
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public void addImport(URI uri) {
		assertMutable();
		
		super.addImport(uri);
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public SSWAPGraph createGraph() {
		assertMutable();
		
		return super.createGraph();
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public SSWAPSubject createSubject() {
		assertMutable();
		
		return super.createSubject();
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public SSWAPSubject createSubject(URI uri) {
		assertMutable();
		
		return super.createSubject(uri);
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public SSWAPObject createObject() {
		assertMutable();
		
		return super.createObject();
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public SSWAPObject createObject(URI uri) {
		assertMutable();
		
		return super.createObject(uri);
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public SSWAPResource createResource(URI uri) {
		assertMutable();
		
		return super.createResource(uri);
	}
	
	private SSWAPSubject createSubject(SSWAPObject object, RIG rig, Map<URI,SSWAPSubject> subjectCache) {
		if (object.isAnonymous()) {
			return rig.createSubject();
		}
		
		SSWAPSubject result = subjectCache.get(object.getURI());
		
		if (result == null) {
			result = rig.createSubject(object.getURI());
			subjectCache.put(object.getURI(), result);
		}
		
		return result;
	}
	
	private SSWAPSubject selectTemplateSubject(Collection<SSWAPSubject> subjects, SSWAPObject rrgObject) {
		if (subjects.isEmpty()) {
			return null;
		}
		
		for (SSWAPSubject subject : subjects) {
			if (((ReasoningServiceImpl) getReasoningService()).isMappingValid(rrgObject, MappingType.SUB, subject)) {
				return subject;
			}
		}
		
		return subjects.iterator().next();
	}
	
	private Collection<SSWAPObject> createRIGObjects(RIG rig, Collection<SSWAPObject> templateObjects) {
		List<SSWAPObject> result = new LinkedList<SSWAPObject>();
		
		for (SSWAPObject templateObject : templateObjects) {
			if (templateObject.isAnonymous()) {
				SSWAPObject newObject = rig.createObject();
				copyObjectToObject(templateObject, newObject);
				
				result.add(newObject);
			}
			else {
				result.add(templateObject);
			}
		}
		
		return result;
	}
	
	private Collection<SSWAPObject> getAllObjects() {
		List<SSWAPObject> result = new LinkedList<SSWAPObject>();
		
		for (SSWAPGraph graph : getResource().getGraphs()) {
			for (SSWAPSubject subject : graph.getSubjects()) {
				for (SSWAPObject object : subject.getObjects()) {
					result.add(object);
				}
			}
		}
		
		return result;
	}
	
	private Collection<SSWAPSubject> getAllRIGSubjects(RIG rig) {
		List<SSWAPSubject> subjects = new LinkedList<SSWAPSubject>();
		
		for (SSWAPGraph graph : rig.getResource().getGraphs()) {
			for (SSWAPSubject subject : graph.getSubjects()) {
				subjects.add(subject);
			}
		}
		
		return subjects;
	}
	
	public RIG createRIG(RDG rdg) throws DataAccessException {
		ReasoningServiceImpl reasoningService = (ReasoningServiceImpl) getReasoningService();		
		
		boolean automaticTermRetrieval = reasoningService.isAutomaticTermRetrieval();
		boolean crossDocumentTermRetrieval = reasoningService.isCrossDocumentTermRetrieval();
		
		reasoningService.setAutomaticTermRetrieval(false);
		reasoningService.setCrossDocumentTermRetrieval(false);	

		RIG rig = rdg.getRIG();
		
		try {
			RIG rigWithAnonymizedNodes = (RIG) ((ProtocolImpl) rig).anonymizeSSWAPNodes();
			ReasoningServiceImpl anonRIGReasoningService = (ReasoningServiceImpl) rigWithAnonymizedNodes.getReasoningService();
			anonRIGReasoningService.setAutomaticTermRetrieval(false);
			anonRIGReasoningService.setCrossDocumentTermRetrieval(false);	

			// add the information from the RDG (to the already existing information from this Protocol Graph) to be taken
			// into account when reasoning. This is done to ensure that all the types (including anonymous types) that are
			// defined
			// in the RDG can be accessed by the reasoning service.
			reasoningService.addModel(rigWithAnonymizedNodes);
			
			Map<URI,SSWAPSubject> subjectCache = new HashMap<URI,SSWAPSubject>();
			Collection<SSWAPSubject> templateSubjects = getAllRIGSubjects(rig);
			
			Map<SSWAPGraph,List<SSWAPSubject>> rigSubjectsByGraph = new HashMap<SSWAPGraph,List<SSWAPSubject>>();
			
			for (SSWAPGraph graph : rig.getResource().getGraphs()) {
				rigSubjectsByGraph.put(graph, new LinkedList<SSWAPSubject>());
			}		
			
			for (SSWAPObject rrgObject : getAllObjects()) {
				
				if (rrgObject.getDeclaredTypes() != null) {
					if (rrgObject.getDeclaredTypes().contains(getType(SSWAP_UTIL_NULL))) {
						continue;
					}
				}
				
				SSWAPSubject templateSubject = selectTemplateSubject(templateSubjects, rrgObject);
				
				if (templateSubject == null) {
					continue;
				}
				
				Collection<SSWAPObject> rigObjects = createRIGObjects(rig, templateSubject.getObjects());
				
				SSWAPSubject newSubject = createSubject(rrgObject, rig, subjectCache);
				
				newSubject.setObjects(rigObjects);
				copyObjectToSubject(rrgObject, newSubject, rig);
				
				rigSubjectsByGraph.get(templateSubject.getGraph()).add(newSubject);
			}
			
			boolean graphHasNonEmptyObjects = false;
			for (SSWAPGraph graph : rig.getResource().getGraphs()) {
				List<SSWAPSubject> newSubjects = rigSubjectsByGraph.get(graph);
				
				if (!newSubjects.isEmpty()) {
					graphHasNonEmptyObjects = true;
					graph.setSubjects(newSubjects);
				}
			}
			if (!graphHasNonEmptyObjects && rig.getResource().getGraphs().size() > 0) {
				throw new DataAccessException("Graph has no non-null Objects");
			}
								
		    reasoningService.removeModel(rigWithAnonymizedNodes);
		}
		finally {
			reasoningService.setAutomaticTermRetrieval(automaticTermRetrieval);
			reasoningService.setCrossDocumentTermRetrieval(crossDocumentTermRetrieval);			
		}
		
		return rig;
	}
	
	private static Collection<SSWAPObject> getAllObjects(SSWAPGraph graph) {
		List<SSWAPObject> objects = new LinkedList<SSWAPObject>();

		for (SSWAPSubject subject : graph.getSubjects()) {
			objects.addAll(subject.getObjects());
		}		
		
		return objects;
	}
	
	private void copyObjectToObject(SSWAPObject src, SSWAPObject dst) {
		ImplFactory.get().copy(src, dst, dst.getDocument(), new HashSet<String>());
	}
	
	private void copyObjectToSubject(SSWAPObject object, SSWAPSubject subject, SSWAPProtocol protocolDoc) {
		Set<String> sswapObjectTypes = new HashSet<String>();
		sswapObjectTypes.add(Vocabulary.SSWAP_OBJECT.toString());
		sswapObjectTypes.add(Vocabulary.SSWAP_SUBJECT.toString());
		sswapObjectTypes.add(Vocabulary.SSWAP_GRAPH.toString());
		sswapObjectTypes.add(Vocabulary.SSWAP_RESOURCE.toString());
		sswapObjectTypes.add(Vocabulary.SSWAP_PROVIDER.toString());
		
		ImplFactory.get().copy(object, subject, protocolDoc, sswapObjectTypes);		
	}
	
	public String getGraphType() {
		return "RRG";
	}
	
	/**
	 * In addition to the regular validation (provided by the superclass), it also
	 * performs computation of type triples for objects in RRG.
	 * 
	 * @param rdg RDG against which to validate
	 */
	@Override
	public void validateAgainstRDG(RDG rdg) throws ValidationException {
		super.validateAgainstRDG(rdg);
		
		// get object mappings
		Map<SSWAPObject,SSWAPObject> objectMappings = getObjectMappings();
		
		for (SSWAPObject rrgObject : objectMappings.keySet()) {
			SSWAPObject rdgObject = objectMappings.get(rrgObject);
		
			// check for null -- some objects may not be mappable onto RDG objects
			// (i.e., they are extra objects)
			if (rdgObject != null) {
				copyDeclaredTypes(rdgObject, rrgObject);
			}
		}
	}
	
	/**
	 * Copy types from one SSWAPObject onto another SSWAPObject
	 * 
	 * @param srcObject source object
	 * @param dstObject destination object
	 */
	private void copyDeclaredTypes(SSWAPObject srcObject, SSWAPObject dstObject) {
		Collection<SSWAPType> dstDeclaredTypes = dstObject.getDeclaredTypes();
		
		for (SSWAPType srcDeclaredType : srcObject.getDeclaredTypes()) {
			if (ModelUtils.isBNodeURI(srcDeclaredType.getURI().toString())) {
				// do not copy anonymous types
				continue;
			}
			
			// only copy, if the destination object does not contain the type
			if (!dstDeclaredTypes.contains(srcDeclaredType)) {
				// remember to obtain the type from the dstObject's document
				// (srcObject and dstObject may not be in the same document!)
				dstObject.addType(dstObject.getDocument().getType(srcDeclaredType.getURI()));
			}
		}
	}
	
	public RQG createRQG() {
		RQG result = SSWAP.createRQG(null);
		
		for (String importURI : getImports()) {
			try {
	            result.addImport(new URI(importURI));
            }
            catch (URISyntaxException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
		}
		
		
		SSWAPResource rrgResource = getResource();
		SSWAPResource rqgResource = result.getResource();
		
		List<SSWAPGraph> rqgGraphs = new LinkedList<SSWAPGraph>();
		
		for (SSWAPGraph rrgGraph : rrgResource.getGraphs()) {
			SSWAPGraph rqgGraph = result.createGraph();
			List<SSWAPSubject> rqgSubjects = new LinkedList<SSWAPSubject>();
			
			for (SSWAPSubject rrgSubject : rrgGraph.getSubjects()) {
				for (SSWAPObject rrgObject : rrgSubject.getObjects()) {
					SSWAPSubject rqgSubject = result.createSubject();
					
					copyObjectToSubject(rrgObject, rqgSubject, result);
					
					SSWAPObject rqgObject = result.createObject();
					rqgSubject.setObject(rqgObject);
					
					rqgSubjects.add(rqgSubject);
				}
			}
			
			rqgGraph.setSubjects(rqgSubjects);
			rqgGraphs.add(rqgGraph);
		}
		
		rqgResource.setGraphs(rqgGraphs);
		//All the properties above will be set without triggering
		//ME because there're is no graph set and, thus, no signature to do ME
		//Trigger it now and refresh the reasoning service
		rqgResource.doClosure();
		
		return result;
	}
	
	@Override
	protected boolean validatesObjects() {
		return false;
	}
	
	@Override
	protected MappingValidator<SSWAPObject> getObjectMappingValidator() {
		return new DefaultMappingValidator<SSWAPObject>(MappingType.SUB);
	}
}
