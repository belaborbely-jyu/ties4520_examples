/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.Expressivity;
import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.ReasoningService;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPModel;
import info.sswap.api.model.ValidationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.MappedSuperclass;

import org.mindswap.pellet.exceptions.InconsistentOntologyException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import com.clarkparsia.empire.SupportsRdfId;
import com.clarkparsia.empire.util.EmpireUtil;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Implements SSWAPModel. The base class of all implementations of objects in SSWAP API.
 * 
 * This class also implements SupportsRdfId, which is required for all Empire-managed classes. While actual
 * Empire-managed classes are only few, and low in the hierarchy (e.g., PDG, Protocol Graphs, and Individuals), this
 * interface provides an ability to relate this object with its definition in the RDF document (either via its URI or
 * BNode Id).
 * 
 * In general, the instances of this class will be a part of a SourceModel (aggregation). Since this object is
 * (potentially) populated by data coming from an RDF graph, it has to be somehow related to that graph. SourceModel
 * object is responsible for holding such a graph. Therefore, any dereferenced ModelImpl needs to have a reference to
 * its SourceModel (and since the link between ModelImpl and SourceModel is bi-directional, the SourceModel also has a
 * reference to the ModelImpl, as one of its dependent models).
 * 
 * Note: the only current implementation of SourceModel interface is SourceModelImpl, and it inherits from ModelImpl.
 * (For SourceModelImpl, the source model is the same as the object itself!)
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
@MappedSuperclass
public abstract class ModelImpl implements SSWAPModel, SupportsRdfId {
	/**
	 * Flag indicating whether the object has been dereferenced (true).
	 * 
	 * TODO: Verify whether this flag is really necessary, and maybe the information about the dereferencing should be
	 * taken from the source model, if one exist. (In the case, it does not exist, it is obvious that an object has not
	 * been dereferenced).
	 */
	private boolean dereferenced;

	/**
	 * The reference to the source model; that is, the object that manages the Jena model containing the RDF data for
	 * this object. Note: if this object is a source model, this reference may be identical with "this" reference. (Keep
	 * this in mind to avoid infinite calls.)
	 */
	private SourceModel sourceModel;
	
	/**
	 * Initializes this object. Since this is an abstract class, this constructor is protected -- this constructor
	 * should only be called when an actual implementation is created.
	 */
	protected ModelImpl() {
		this.dereferenced = false;
	}

	/**
	 * @inheritDoc
	 */
	public void dereference() {
		if (!isDereferenced()) {
			// re-read underlying RDF document and repopulate the fields of this object.
			refresh();
		}
	}
	
	/**
	 * @inheritDoc
	 */
	public void dereference(InputStream is) {
		if (!isDereferenced()) {
			refresh();
		}
	}

	/**
	 * @inheritDoc
	 */
	public int doClosure() {
		if (hasSourceModel()) {
			return sourceModel.doClosure();
		}
		
		return -1;
	}

	/**
	 * @inheritDoc
	 */
	public Expressivity getExpressivity() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean checkProfile(Expressivity expressivity) {
		SourceModel sourceModel = assertSourceModel();
		return sourceModel.checkProfile(expressivity);
	}
	
	
	/**
	 * @inheritDoc
	 */
	public InputStream getInputStream() {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		serialize(byteArrayOutputStream);
	
		return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());	
	}

	
	/**
	 * @inheritDoc
	 */
	public URI getURI() {
		@SuppressWarnings("rawtypes")
		RdfKey rdfKey = getRdfId();

		if ((rdfKey != null) && rdfKey instanceof URIKey) {
			return ((URIKey) rdfKey).value();
		}

		return null;
	}

	/**
	 * Sets the URI of this object. This method should be called only once in the lifetime of the object.
	 * 
	 * @param uri
	 *            the new uri of the object
	 */
	public void setURI(URI uri) {
		// if the RdfId is not set it, set it then (setting RdfId more than once is not allowed under Empire rules)
		if (getRdfId() == null) {
			setRdfId(new URIKey(uri));
		}
	}

	/**
	 * @inheritDoc
	 */
	public boolean isDereferenced() {
		return dereferenced;
	}

	/**
	 * Sets the dereference flag. This method will be called by the more concrete classes.
	 * 
	 * @param dereferenced
	 *            true if the object has been dereference, false otherwise
	 */
	protected void setDereferenced(boolean dereferenced) {
		this.dereferenced = dereferenced;
	}

	/**
	 * @inheritDoc
	 */
	public void serialize(OutputStream os) {
		serialize(os, RDFRepresentation.RDF_XML, false /* commentedOutput */);
	}

	/**
	 * @inheritDoc
	 */
	public void serialize(OutputStream os, RDFRepresentation representation, boolean commentedOutput) {
		if (hasSourceModel()) {
			((SourceModelImpl) getSourceModel()).persist();

			Model fullModel = assertModel();
			Model partitionedModel = ModelUtils.partitionModel(fullModel, getURI().toString(), false);
			
			ModelUtils.serializeModel(partitionedModel, os, representation, /* commentedOutput */ commentedOutput);			
		}
	}

	/**
	 * @inheritDoc
	 */
	public void validate() throws ValidationException {
		try {
			((ReasoningServiceImpl) getReasoningService()).validateConsistency();
		}
		catch (InconsistentOntologyException e) {
			throw new ValidationException("The ontology is inconsistent", e);
		}
	}

	/**
	 * Sets the source model for this object (i.e., the object that actually holds the Jena model with RDF data). If
	 * this object has currently a model, it will be removed from it.
	 * 
	 * @param sourceModel
	 *            the new source model
	 */
	public void setSourceModel(SourceModel sourceModel) {
		// if there is a current source model, remove this object from its dependent models
		if (this.sourceModel != null) {
			this.sourceModel.removeDependentModel(this);
		}

		this.sourceModel = sourceModel;

		// add this object to the new source model, as a dependent model
		if (sourceModel != null) {
			sourceModel.addDependentModel(this);
			
			this.dereferenced = sourceModel.isDereferenced();
		}
	}

	/**
	 * Gets a reference to the current source model of this object (i.e., the object that holds the RDF graph that is
	 * the source of data for this object). If this object is the source model for itself, this method will return
	 * "this".
	 * 
	 * @return source model for this object. (Will be null for non-dereferenced objects.)
	 */
	public SourceModel getSourceModel() {
		return sourceModel;
	}

	/**
	 * Creates an object based on the data from the same data source as this one (e.g., if this object represents a PDG,
	 * it allows to get a SSWAPProvider from this data source; or a SSWAPResource from RDG). This method assumes that
	 * there is only one suitable object of the given type (in case there is more, it will return the first one).
	 * 
	 * @param <T>
	 *            parameterized type of an Empire-annotated class to define what kind of data has to be retrieved
	 * @param clazz
	 *            the Empire-annotated class
	 * @return an object retrieved from the underlying RDF graph or null if there is no data in the RDF (or RDF at all,
	 *         because the object is not dereferenced or connected to any SourceModel).
	 */
	protected <T extends ModelImpl> T getDependentObject(Class<T> clazz) {
		// retrieve only if there is a SourceModel and Empire EntityManager is properly initialized
		// (the lack of entity manager indicates that the SourceModel is not dereferenced)
		if (hasSourceModelWithEntityManager()) {
			// retrieve the actual object
			T object = ImplFactory.get().readSSWAPDataObject(getSourceModel().getEntityManager(), clazz);

			// if the object was retrieved successfully do proper initialization (URI, setting the source model)
			if (object != null) {
				ImplFactory.initURI(object);
				object.setSourceModel(getSourceModel());
			}

			return object;
		}

		return null;
	}

	protected <T extends ModelImpl> Collection<T> getAllDependentObjects(Class<T> clazz) {
		List<T> result = new LinkedList<T>();
		
		// retrieve only if there is a SourceModel and Empire EntityManager is properly initialized
		// (the lack of entity manager indicates that the SourceModel is not dereferenced)
		if (hasSourceModelWithEntityManager()) {
			for (T object: EmpireUtil.all(getSourceModel().getEntityManager(), clazz)) {
				if (object != null) {
					ImplFactory.initURI(object);
					object.setSourceModel(getSourceModel());
										
					result.add(object);
				}				
			}
			
			return result;
		}

		return null;
	}

	
	/**
	 * Refreshes the data of this object (should be called, if the underlying RDF graph changed). The current
	 * implementation does nothing, and it is expected to be overriden by the more concrete implementations of this
	 * class.
	 */
	public void refresh() {
		// nothing
	}

	/**
	 * Writes back the changes to the underlying RDF graph. The current implementation does nothing
	 */
	public void persist() {
		// nothing
	}

	/**
	 * Refreshes SSWAPModels that are considered siblings of this one (i.e., models that have the same URI).
	 * Calling this method will not cause refresh of this object.
	 */
	protected void refreshSiblings() {
		assertSourceModel();
		
		for (SSWAPModel model : getSourceModel().getDependentModels(getURI())) {
			if (this == model) {
				// do not refresh itself
				continue;
			}
			
			if (model instanceof ModelImpl) {
				((ModelImpl) model).refresh();
			}
		}
	}
	
	/**
	 * @inheritDoc
	 */
	public ReasoningService getReasoningService() {
		return getSourceModel().getReasoningService();
	}
	
	protected boolean rdfIdEquals(ModelImpl other) {
		if (getURI() != null) {
			// uris are the same
			return getURI().equals(other.getURI());
		}
		else if (getRdfId() != null) {
			// rdf ids are the same
			return getRdfId().equals(other.getRdfId());
		}
		else {
			return false;
		}	
	}
	
	protected int rdfIdHashCode() {
		if (getURI() != null) {
			return getURI().hashCode();
		}
		else if (getRdfId() != null) {
			return getRdfId().hashCode();
		}
		else {
			return 0;
		}	
	}

	/**
	 * @inheritDoc
	 */
	public void setNsPrefix(String prefix, URI uri) {
		if (hasSourceModel()) {
			getSourceModel().setNsPrefix(prefix, uri);
		}
	}

	/**
	 * @inheritDoc
	 */
	public void removeNsPrefix(String prefix) {
		if (hasSourceModel()) {
			getSourceModel().removeNsPrefix(prefix);
		}
	}

	/**
	 * @inheritDoc
	 */
	public Map<String, String> getNsPrefixMap() {
		if (hasSourceModel()) {
			return getSourceModel().getNsPrefixMap();
		}

		return null;
	}

	/**
	 * Gets the list of OWL imports in this model (the top-level only).
	 * 
	 * @return a collection of uris of the imports
	 */
	public Collection<String> getImports() {
		if (hasSourceModel()) {
			return getSourceModel().getImports();
		}

		return Collections.emptyList();
	}

	/**
	 * Adds an import to the list of OWL imports of this model. Adding a URI to the list of imports does not trigger the
	 * import itself (use doClosure()) for that purpose.
	 * 
	 * @param uri
	 *            the URI of the import to be added
	 */
	public void addImport(URI uri) {
		if (hasSourceModel()) {
			getSourceModel().addImport(uri);
		}
	}

	/**
	 * Removes an import from the list of imports
	 * 
	 * @param uri
	 *            the import to be removed
	 */
	public void removeImport(URI uri) {
		if (hasSourceModel()) {
			getSourceModel().removeImport(uri);
		}
	}

	/**
	 * Checks whether this model has a source model associated with it
	 * 
	 * @return true if the model has a source model associated with it
	 */
	protected boolean hasSourceModel() {
		return (sourceModel != null);
	}

	/**
	 * Checks whether this model has a source model associated with it that has an entity manager.
	 * 
	 * @return true if the model has a source model associated with it, and that source model has an entity manager.
	 */
	protected boolean hasSourceModelWithEntityManager() {
		return (hasSourceModel() && (getSourceModel().getEntityManager() != null));
	}

	/**
	 * Gets the source model of a dereferenced ModelImpl
	 * 
	 * @return a source model of the passed model (never null)
	 * @throws IllegalArgumentException
	 *             if the model has not been dereferenced, and therefore does not have a source model
	 */
	protected SourceModel assertSourceModel() throws IllegalArgumentException {
		SourceModel result = getSourceModel();

		if (result == null) {
			throw new IllegalArgumentException("The model has not been dereferenced");
		}

		return result;
	}
	
	protected Model assertModel() throws IllegalArgumentException {
		Model result = assertSourceModel().getModel();
		
		if (result == null) {
			throw new IllegalArgumentException("This model does not have an associated Jena model!");
		}
		
		return result;
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPDocument getDocument() {
		return getSourceModel();
	}

	@Override
	public Collection<URI> getTypeSignature(URI type) {
		if (!ModelUtils.isBNodeURI(type.toString())) {
			return Collections.singletonList(type);
		} else {
			//Anonymous type, so "we need to go deeper" (c)
			Resource typeRes = sourceModel.getModel().getResource(type.toString());

			if (typeRes == null) {
				return Collections.emptyList();
			} else {
				Collection<URI> termPairs = Sets.newHashSet();
				// TODO
				// Filter out RDF and OWL vocabulary
				// Account for rdfs:isDefinedBy and create proper ontology
				// descriptors
				for (Statement stmt : ReasoningServiceImpl.extractTypeDefinition(sourceModel.getModel(), typeRes)) {
					if (stmt.getSubject().isURIResource()) {
						addTermToSignature(termPairs, stmt.getSubject().getURI());
					}
					if (stmt.getPredicate().isURIResource()) {
						addTermToSignature(termPairs, stmt.getPredicate().getURI());
					}
					if (stmt.getObject().isURIResource()) {
						addTermToSignature(termPairs, stmt.getObject().asResource().getURI());
					}
				}

				return termPairs;
			}
		}
	}
	
	private void addTermToSignature(Collection<URI> terms, String uri) {
		if (!belongs2RDForOWLVocabulary(uri)) {
			terms.add(URI.create(uri));
		}
	}

	private boolean belongs2RDForOWLVocabulary(String term) {
		return OWLRDFVocabulary.BUILT_IN_VOCABULARY_IRIS.contains(IRI.create(term));
	}
}
