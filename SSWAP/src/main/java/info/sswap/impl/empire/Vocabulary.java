/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire;

import static info.sswap.impl.empire.Namespaces.SSWAP_NS;
import static info.sswap.impl.empire.Namespaces.SSWAP_ASYNC_NS;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * The class containing constants for vocabulary used in SSWAP.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class Vocabulary {
	/**
	 * The internal Jena model which is used to create the constants below
	 */
	private static final Model internalModel = ModelFactory.createDefaultModel();
	
	public static final Resource SSWAP = internalModel.getResource(SSWAP_NS + "SSWAP");
	
	/**
	 * Jena resource for sswap:Resource
	 */
	public static final Resource SSWAP_RESOURCE = internalModel.getResource(SSWAP_NS + "Resource");
	
	/**
	 * Jena resource for sswap:Graph
	 */
	public static final Resource SSWAP_GRAPH = internalModel.getResource(SSWAP_NS + "Graph");
	
	/**
	 * Jena resource for sswap:Subject
	 */
	public static final Resource SSWAP_SUBJECT = internalModel.getResource(SSWAP_NS + "Subject");
	
	/**
	 * Jena resource for sswap:Object
	 */
	public static final Resource SSWAP_OBJECT = internalModel.getResource(SSWAP_NS + "Object");
	
	/**
	 * Jena resource for sswap:Provider
	 */
	public static final Resource SSWAP_PROVIDER = internalModel.getResource(SSWAP_NS + "Provider");
	
	public static final Resource PROVIDED_BY = internalModel.getProperty(SSWAP_NS + "providedBy");
	
	public static final Resource PROVIDES_RESOURCE = internalModel.getProperty(SSWAP_NS + "providesResource");
	
	public static final Resource MAPS_TO = internalModel.getProperty(SSWAP_NS + "mapsTo");
	
	public static final Resource OPERATES_ON = internalModel.getProperty(SSWAP_NS + "operatesOn");
	
	public static final Resource HAS_MAPPING = internalModel.getProperty(SSWAP_NS + "hasMapping");
	
	/**
	 * Constant for sswap:name
	 */
	public static final Resource NAME = internalModel.getProperty(SSWAP_NS + "name");

	/**
	 * Constant for sswap:oneLineDescription URI
	 */
	public static final Resource ONE_LINE_DESCRIPTION = internalModel.getProperty(SSWAP_NS + "oneLineDescription");

	/**
	 * Constant for sswap:aboutURI URI
	 */
	public static final Resource ABOUT_URI = internalModel.getProperty(SSWAP_NS + "aboutURI");

	/**
	 * Constant for sswap:inputURI URI
	 */
	public static final Resource INPUT_URI = internalModel.getProperty(SSWAP_NS + "inputURI");

	/**
	 * Constant for sswap:outputURI URI
	 */
	public static final Resource OUTPUT_URI = internalModel.getProperty(SSWAP_NS + "outputURI");
	
	/**
	 * Constant for sswap:icon
	 */
	public static final Resource ICON = internalModel.getProperty(SSWAP_NS + "icon");

	/**
	 * Constant for sswap:metadata URI
	 */
	public static final Resource METADATA = internalModel.getProperty(SSWAP_NS + "metadata");

	/**
	 * The URI of SSWAP Ontology
	 */
	public static final String SSWAP_ONTOLOGY_URI = "http://sswapmeet.sswap.info/sswap/owlOntology";
	
	/**
	 * Non-standard HTTP Header to return error message
	 */
	public static final String SSWAP_HTTP_EXCEPTION_HEADER = "X-SSWAP-Exception-Msg";
	
	/**
	 * Non-standard HTTP header to return information about sswap:outputURI (e.g., to provide this information
	 * to clients that do not have capability to process RDF/XML)
	 */
	public static final String SSWAP_OUTPUT_URI_HEADER = "X-SSWAP-Output-URI";

	/**
	 * The token used to retrieve an RRG in an asynchronous version of SSWAP protocol.
	 */
	public static final Resource TOKEN = internalModel.getResource(SSWAP_ASYNC_NS + "token");
	
	/**
	 * A marker class to denote that a particular individual is an asynchronous RRG. 
	 */
	public static final Resource ASYNC_RRG = internalModel.getResource(SSWAP_ASYNC_NS + "RRG");
}
