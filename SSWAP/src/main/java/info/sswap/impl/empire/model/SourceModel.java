/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import info.sswap.api.model.Expressivity;
import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.ReasoningService;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPModel;

import javax.persistence.EntityManager;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Common interface for all SSWAP Models that have their own underlying Jena model.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public interface SourceModel extends SSWAPDocument {
	/**
	 * Gets the underlying Jena model.
	 * 
	 * @return the underlying Jena model.
	 */
	public Model getModel();
	
	/**
	 * Gets the Jena model containing the closure.
	 *  
	 * If the closure is not available, it will be computed.  
	 * 
	 * @return the closure
	 */
	public Model getClosureModel();

	/**
	 * Gets the model that is used to close the worlds for specific parts of this model
	 * (but the statements from the closed world model should not "pollute" the main
	 * model).
	 * 
	 * @return the model where statements for closing the world are stored
	 */
	public Model getClosedWorldModel();
	
	/**
	 * Replaces the underlying Jena model with another one.
	 * 
	 * @param model
	 *            the new model.
	 */
	public void setModel(Model model);

	/**
	 * Gets the Empire Entity manager that manages objects created based on the information from that Jena model
	 * 
	 * @return the Empire Entity manager
	 */
	public EntityManager getEntityManager();

	/**
	 * Sets the Empire Entity manager that manages objects created based on the information from that Jena model.
	 * 
	 * @param entityManager
	 *            the Empire entity manager for the Jena model
	 */
	public void setEntityManager(EntityManager entityManager);

	/**
	 * Adds a dependent SSWAP Model to this source model (i.e., a SSWAP model that is created based on the information
	 * read from the Jena model)
	 * 
	 * @param dependentModel
	 *            the dependent model
	 */
	public void addDependentModel(ModelImpl dependentModel);

	/**
	 * Removes a dependent SSWAP model from this source model
	 * 
	 * @param dependentModel
	 *            the dependent model to be removed
	 */
	public void removeDependentModel(ModelImpl dependentModel);

	/**
	 * Returns information whether the source model is dereferenced (i.e., was populated with data).
	 * 
	 * @return true if the model is dereferenced
	 */

	public boolean isDereferenced();

	/**
	 * Attempts to populate the model with data by retrieving it from the URI of the model.
	 */
	public void dereference();
	
	/**
	 * Attempts to populate the model with data by retrieving it from the given stream.
	 * 
	 * @param is the input stream to read the model contents
	 */
	public void dereference(InputStream is);
	
	/**
	 * Attempts to populate the model with data retrieved from the given Jena model.
	 *
	 * @param model the Jena model that contains the data
	 */
	public void dereference(Model model);

	/**
	 * Gets the reasoning service associated with the model. (In this implementation, there is only one reasoning
	 * service object for each SourceModel; all dependent models for that SourceModel share that reasoning service).
	 * 
	 * @return the reasoning service for the model.
	 */
	public ReasoningService getReasoningService();
	
	/**
	 * For every URI in the model, do an HTTP GET and read in the model. Assume RDF/XML. Iterate to 'degree' depth on
	 * new URIs thus read in. Closure obeys rules of self-definition. Returns the highest level of closure achieved.
	 *
	 * @return highest (deepest) level of closure performed
	 */
	public int doClosure();

	/**
	 * Serializes the contents of this object to the specified stream as RDF/XML.
	 *
	 * @param os
	 *            output stream
	 */
	public void serialize(OutputStream os);

	/**
	 * Serializes the contents of this object to the specified stream.
	 *
	 * @param os
	 *            output stream
	 * @param representation
	 *            the representation, in which the contents should be written (e.g., RDF/XML).
	 * @param commentedOutput
	 *            true, if the output should contain comments about various standard sections of an RDG (valid only for
	 *            RDF/XML)
	 */
	public void serialize(OutputStream os, RDFRepresentation representation, boolean commentedOutput);

	/**
	 * Sets a namespace prefix that will be used in serialization of this model.
	 * 
	 * @param prefix
	 *            the prefix
	 * @param uri
	 *            the corresponding URI
	 */
	public void setNsPrefix(String prefix, URI uri);

	/**
	 * Removes a namespace prefix. (The prefixes are used in serialization of this model.)
	 * 
	 * @param prefix
	 *            the prefix to be removed.
	 */
	public void removeNsPrefix(String prefix);

	/**
	 * Gets the map of currently defined namespace prefixes. (These prefixes are used in serialization of this model.)
	 * 
	 * @return the map mapping prefixes to corresponding URIs
	 */
	public Map<String, String> getNsPrefixMap();

	/**
	 * Gets the list of OWL imports in this model (the top-level only).
	 * 
	 * @return a collection of uris of the imports
	 */
	public Collection<String> getImports();

	/**
	 * Adds an import to the list of OWL imports of this model. Adding a URI to the list of imports does not trigger the
	 * import itself (use doClosure()) for that purpose.
	 * 
	 * @param uri
	 *            the URI of the import to be added
	 */
	public void addImport(URI uri);

	/**
	 * Removes an import from the list of imports
	 * 
	 * @param uri
	 *            the import to be removed
	 */
	public void removeImport(URI uri);
		
	/**
	 * Returns the dependent model, identified by its URI, for this source model
	 * 
	 * @param uri
	 * @return the dependent model
	 */
	public SSWAPModel getDependentModel(URI uri);
	
	/**
	 * Returns all dependent models with the specified uri
	 * 
	 * @param uri the uri
	 * @return collection of dependent models
	 */
	public Collection<SSWAPModel> getDependentModels(URI uri);
	
	
	/**
	 * Checks whether the model is in one of the expressivity profiles
	 * 
	 * @param expressivity the expressivity to be checked
	 * @return true if the model is in the given expressivity profile
	 */
	public boolean checkProfile(Expressivity expressivity);
	
	/**
	 * Sets a model-specific byte limit for retrieving closure for this source model, or
	 * restores the system-wide limit. 
	 * 
	 * Note: if closure has been computed for this model, calling this method has no effect
	 * 
	 * @param maxClosureBytes the new model-specific byte limit for closure for this model, or -1 to restore the 
	 * system-wide limit
	 */
	public void setMaxClosureBytes(long maxClosureBytes);
	
	/**
	 * Sets a model-specific time limit for retrieving closure for this source model, or
	 * restores the system-wide limit. 
	 * 
	 * Note: if closure has been computed for this model, calling this method has no effect
	 * 
	 * @param maxClosureTime the new model-specific time limit (in ms) for closure for this model, or -1 to restore the 
	 * system-wide limit
	 */
	public void setMaxClosureTime(long maxClosureTime);
	
	/**
	 * Sets a model-specific concurrent thread limit for retrieving closure for this source model, or
	 * restores the system-wide limit. 
	 * 
	 * Note: if closure has been computed for this model, calling this method has no effect
	 * 
	 * @param maxClosureThreads the new model-specific concurrent thread limit for closure for this model, or -1 to restore the 
	 * system-wide limit
	 */
	public void setMaxClosureThreads(int maxClosureThreads);
	
	/**
	 * Returns information whether this source model performs validation of values for properties.
	 * 
	 * @return true if the validation for the values is enabled.
	 */
	public boolean isValueValidationEnabled();
	
	/**
	 * Turns on or off validation of values for properties in this model 
	 * (i.e., values set via SSWAPIndividual.addProperty() and SSWAPIndividual.setProperty() methods)
	 * 
	 * @param validationEnabled true if the values should be validated, false if the validation should be turned off
	 */
	public void setValueValidationEnabled(boolean validationEnabled);
	
	/**
	 * Returns information whether this source model will perform the closure to 
	 * deliver terms to its reasoning service
	 * 
	 * @return true, if the source model will perform closure, false otherwise
	 */
	public boolean isClosureEnabled();

	/**
	 * Sets flag whether this source model will perform the closure to deliver terms to its reasoning service, or whether
	 * the reasoning service should solely rely on terms in this source model. Changing this flag has only effect
	 * if the reasoning service has not yet been initialized. 
	 * 
	 * @param enabled
	 */
	public void setClosureEnabled(boolean enabled);
}
