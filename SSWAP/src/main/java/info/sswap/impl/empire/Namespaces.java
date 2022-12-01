/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire;

/**
 * A placeholder to keep all the standard namespace URIs.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class Namespaces {
	/**
	 * The SSWAP namespace http://sswapmeet.sswap.info/sswap/
	 */
	public static final String SSWAPMEET_NS = "http://sswapmeet.sswap.info/";
	
	public static final String SSWAP_NS = SSWAPMEET_NS + "sswap/";
	
	public static final String SSWAP_AGAVE_NS = SSWAPMEET_NS + "agave/";
	
	public static final String SSWAP_DATA_NS = SSWAPMEET_NS + "data/";
	
	public static final String SSWAP_EXEC_NS = SSWAPMEET_NS + "exec/";
	
	public static final String SSWAP_MIME_NS = SSWAPMEET_NS + "mime/";
	
	/**
	 * The SSWAP Async namespace
	 */
	public static final String SSWAP_ASYNC_NS = SSWAP_NS + "async/";
	
	/**
	 * The SSWAP Util namespace
	 */
	public static final String SSWAP_UTIL_NS = SSWAP_NS + "util/";
	
	/**
	 * The RDF namespace http://www.w3.org/1999/02/22-rdf-syntax-ns#
	 */
	public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	
	/**
	 * The RDFS namespace http://www.w3.org/2000/01/rdf-schema#
	 */
	public static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
	
	/**
	 * The OWL namespace http://www.w3.org/2002/07/owl#
	 */
	public static final String OWL_NS = "http://www.w3.org/2002/07/owl#";
	
	/**
	 * The XSD namespace http://www.w3.org/2001/XMLSchema# 
	 */
	public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";
	
	/**
	 * Contains all the namespace prefixes defined in this class as a SPARQL fragment that
	 * can be used in a query.
	 */
	public static final String SPARQL_NS_PREFIXES = "PREFIX sswap: <" + SSWAP_NS + ">\n" +
	                                                "PREFIX rdf: <" + RDF_NS + ">\n" +
	                                                "PREFIX rdfs: <" + RDFS_NS + ">\n" +
	                                                "PREFIX owl: <" + OWL_NS + ">\n";
}
