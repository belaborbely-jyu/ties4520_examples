/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.io;

import info.sswap.api.input.Input;
import info.sswap.api.input.InputValue;

/**
 * Constants used for the JSON representation of {@link Input} and {@link InputValue} objects. Since input values
 * correspond to RDF values, the JSON representation of RDF values as defined in <a
 * href="http://www.w3.org/TR/rdf-sparql-json-res/">SPARQL query results in JSON</a> is adopted.
 * 
 * @author Evren Sirin
 */
public class JSONConstants {
	// constants to serialize Input objects
	public static final String TYPE = "type";
	public static final String VALUE = "value";
	public static final String LABEL = "label";
	public static final String DESCRIPTION = "description";
	public static final String INPUTS = "inputs";
	public static final String VALUES = "values";
	public static final String VALUE_INDEX = "valueIndex";
	public static final String VALUE_TYPES = "valueTypes";
	public static final String PROPERTY = "property";
	public static final String MIN = "min";
	public static final String MAX = "max";
	public static final String RANGE = "range";
	// constants to serialize values (adopted from SPARQL JSON format)
	public static final String URI = "uri";
	public static final String BNODE = "bnode";
	public static final String LITERAL = "literal";
	public static final String DATATYPE = "datatype";
	public static final String LANG = "xml:lang";
}
