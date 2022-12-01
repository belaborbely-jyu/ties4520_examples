/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import java.util.UUID;

/**
 * Configuration properties for SSWAP Services. These properties may be set in a
 * Java properties file and loaded at startup by entering a path to the file in
 * web.xml as in this snippet:
 * 
 * <pre>
 * {@code
 * <servlet>
 *   <servlet-name>MyServlet</servlet-name>
 * 
 *     <servlet-class>org.mySite.sswap.MyServlet</servlet-class>
 * 
 *     <!-- if not defined, defaults to values in info.sswap.api.model.Config <url-pattern>
 *     <init-param>
 *       <param-name>ConfigPath</param-name>
 *       <param-value>/pathTo/config.properties</param-value>
 *     </init-param>
 *     -->
 * </servlet>
 * }
 * </pre>
 * 
 * Sample sswap.properties file:
 * 
 * <pre>
 * {@code
 * info.sswap.impl.empire.io.CACHE_DIR = /opt/tomcat/cache
 * info.sswap.impl.empire.model.RIG_INVOCATION_TIMEOUT = 120000
 * }
 * </pre>
 * The keys in the property file (<i>e.g.</i> 'info.sswap.impl.empire.io.CACHE_DIR')
 * are the values of their respective _KEY fields in this class (<i>e.g.</i> CACHE_DIR_KEY)
 * as listed in Constant Field Values link for each field.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class Config {	
	/**
	 * Key for the property that defines how many closure degrees will be performed during closure computation.
	 */
	public static String MAX_CLOSURE_DEGREE_KEY = "info.sswap.impl.empire.io.MAX_CLOSURE_DEGREE";
	
	/**
	 * The default closure degree if not otherwise specified (must be a parseable integer)
	 */
	public static final String MAX_CLOSURE_DEGREE_DEFAULT = "3";
	
	/**
	 * Key for the property that defines how many additional "hierarchy retrieving" closure degrees will be performed
	 * (in addition to the closure degrees defined by MAX_CLOSURE_DEGREE_KEY) during closure computation.
	 */
	public static String MAX_HIERARCHY_CLOSURE_DEGREE_KEY = "info.sswap.impl.empire.io.MAX_HIERARCHY_CLOSURE_DEGREE";
	
	/**
	 * The default hierarchy retrieving closure degree if not otherwise specified (must be a parseable integer)
	 */
	public static final String MAX_HIERARCHY_CLOSURE_DEGREE_DEFAULT = "10";
	
	/**
	 * Key for the property that defines the connect timeout for a single HTTP connection during closure computation
	 * (i.e., if an HTTP connection cannot be established in that many milliseconds, it will be interrupted,
	 * and a retrieval failure will be declared for that term.)
	 */
	public static String CLOSURE_CONNECT_TIMEOUT_KEY = "info.sswap.impl.empire.io.CLOSURE_CONNECT_TIMEOUT";
	
	/**
	 * The default connect timeout for closure computation (in milliseconds), if not otherwise specified.
	 * Must be a parseable integer
	 */
	public static final String CLOSURE_CONNECT_TIMEOUT_DEFAULT = "10000"; // 10 seconds in milliseconds
	
	/**
	 * Key for the property that defines the read timeout for a single HTTP connection during closure computation
	 * (i.e., if data transmission for an HTTP connection should stall past that many milliseconds, it will be 
	 * interrupted, and a retrieval failure will be declared for that term).
	 */
	public static String CLOSURE_READ_TIMEOUT_KEY = "info.sswap.impl.empire.io.CLOSURE_READ_TIMEOUT";
	
	/**
	 * The default read timeout for closure computation (in milliseconds), if not otherwise specified.
	 * Must be a parseable integer
	 */
	public static final String CLOSURE_READ_TIMEOUT_DEFAULT = "10000"; // 10 seconds in milliseconds
	
	/**
	 * Key for the property that defines a closure-wide bytes limit (i.e., if that many bytes are transmitted
	 * during a single closure computation, no further HTTP connections will be initiated, and existing connections
	 * will be terminated as soon as possible).
	 */
	public static String CLOSURE_BYTES_LIMIT_KEY = "info.sswap.impl.empire.io.CLOSURE_BYTES_LIMIT";
	
	/**
	 * The default byte limit for closure computation in bytes, if not otherwise specified. Must be a parseable integer
	 */
	public static final String CLOSURE_BYTES_LIMIT_DEFAULT = "524288"; // 512 KB in bytes
	
	/**
	 * Key for the property that defines a closure-wide time limit (i.e., if an attempt to retrieve a closure
	 * should exceed that many milliseconds, no further HTTP connections will be initiated, and existing connections
	 * will be terminated as soon as possible).
	 */
	public static String CLOSURE_TIME_LIMIT_KEY = "info.sswap.impl.empire.io.CLOSURE_TIME_LIMIT";
	
	/**
	 * The default time limit for closure computation in milliseconds, if not otherwise specified. Must be a parseable integer
	 */
	public static final String CLOSURE_TIME_LIMIT_DEFAULT = "30000"; // 30 seconds in milliseconds
	
	/**
	 * Key for the property that defines how many threads/concurrent connections should be used in a single closure computation.
	 * Setting higher value of this property may allow to utilize more bandwidth for faster closure computation.
	 * Setting lower value of this property can reduce the bandwidth utilization but it may also stall closure computation
	 * if some terms are to be retrieved from a slow HTTP server.
	 */
	public static String CLOSURE_THREADS_KEY = "info.sswap.impl.empire.io.CLOSURE_THREADS";
	
	/**
	 * The default number of threads for closure retrieval, if not otherwise specified. Must be a parseable integer.
	 */
	public static final String CLOSURE_THREADS_DEFAULT = "2"; 
	
	/**
	 * Key for the property that defines how long an entry cached in model cache should be stored (time-to-live; TTL).
	 * Whenever the closure computation process successfully a definition of a term, it is stored in
	 * the model cache (which allows subsequent requests to dereference this term not to generate an HTTP connection,
	 * which is not only faster, but also does not exhaust the byte limit for a particular closure).
	 * 
	 * Note: model cache does not store information about failed retrievals; that is, if a closure computation fails
	 * to retrieve a term (e.g., due to network issue or because the term is not available at the address),
	 * next closure computations will still attempt to dereference the term (because the failure could have been
	 * transient).
	 */
	public static String MODEL_CACHE_ENTRY_TTL_KEY = "info.sswap.impl.empire.io.MODEL_CACHE_ENTRY_TTL";
	
	/**
	 * The default TTL for a model cache entry, if not otherwise specified. Must be a parseable long.
	 */
	public static final String MODEL_CACHE_ENTRY_TTL_DEFAULT = "14400000"; // 4 hours in milliseconds
	
	public static String MODEL_CACHE_NEGATIVE_ENTRY_TTL_KEY = "info.sswap.impl.empire.io.MODEL_CACHE_NEGATIVE_ENTRY_TTL";
	
	public static final String MODEL_CACHE_NEGATIVE_ENTRY_TTL_DEFAULT = "1800000"; // 30 minutes in milliseconds
	
	/**
	 * The URI of the publicly accessible instance of HTTP/JSON API (used by classes in info.sswap.api.http).
	 */
	public static final String HTTP_API_URI_KEY = "info.sswap.api.http.HTTP_API_URI";
	
	/**
	 * The default URI for HTTP API, if not otherwise specified. Must also be a parseable URL 
	 */
	public static final String HTTP_API_URI_DEFAULT = "http://sswap.info/api/";
	
	/**
	 * The URI of the discovery server for queries (used to answer RQGs)
	 */
	public static final String DISCOVERY_SERVER_QUERY_URI_KEY = "info.sswap.api.model.DISCOVERY_SERVER_URI";
	public static final String DISCOVERY_SERVER_QUERY_URI_ALT_KEY = "discovery.server.url";

	/**
	 * The default URI of the discovery server, if not otherwise specified. Must also be a parseable URL.
	 */
	public static final String DISCOVERY_SERVER_QUERY_URI_DEFAULT = "http://sswap.info/query";

	/**
	 * Property name for setting the connect timeout (in milliseconds) during RIG invocation.
	 */
	public static final String RIG_INVOCATION_TIMEOUT_KEY = "info.sswap.impl.empire.model.RIG_INVOCATION_TIMEOUT";
		
	/**
	 * The default connect timeout (in milliseconds) during RIG invocation. (Must be a parseable integer.)
	 */
	public static final String RIG_INVOCATION_TIMEOUT_DEFAULT = "60000"; // 60 seconds
	
	/**
	 * Key for the cache directory which contains the index and cached files.
	 */
	public static final String CACHE_DIR_KEY = "info.sswap.impl.empire.io.CACHE_DIR";

	/**
	 * The default cache directory.
	 */
	public static final String CACHE_DIR_DEFAULT = "cache";
	
	/**
	 * Key for the name of the index file for the cache that maps URIs to local file paths.
	 */
	public static final String CACHE_INDEX_FILE_KEY = "info.sswap.impl.empire.io.CACHE_INDEX_FILE";
	
	/**
	 * The default name of the cache index file.
	 */
	public static final String CACHE_INDEX_FILE_DEFAULT = "cache.properties";

	/**
	 * Key for enabling the disk cache (vs. memory-only caching)
	 */
	public static final String DISK_CACHE_ENABLED_KEY = "info.sswap.impl.empire.io.DISK_CACHE_ENABLED";
	
	/**
	 * The default value for enabling (or disabling) the disk cache (value is 'true' or 'false').
	 */
	public static final String DISK_CACHE_ENABLED_DEFAULT = "true";
	
	/**
	 * Key for enabling any caching (disk or memory)
	 */
	public static final String CACHE_ENABLED_KEY = "info.sswap.impl.empire.io.CACHE_ENABLED";
	
	/**
	 * The default value for enabling (or disabling) caching (value is 'true' or 'false').
	 */
	public static final String CACHE_ENABLED_DEFAULT = "true";
	
	/**
	 * Property name for controlling Unique Name Assumption (UNA) when closing the world during service invocations.
	 * 
	 * If set to true, the individuals referred in sswap:Subjects/sswap:Resource will be assumed different if they
	 * have different URIs and cannot be proven to be the same by the reasoner. If set to false (default), the "regular"
	 * rules of OWL will apply; i.e., there may not be enough information to prove individuals be the same or different,
	 * and subsequently some cardinality restrictions may fail (e.g., if the service requires two values for a certain property,
	 * it may not be provable that the two values in the request are different, and therefore the request will fail).  
	 */
	public static final String DISABLE_UNA_WHEN_CLOSING_WORLD_KEY = "info.sswap.impl.empire.DISABLE_UNA_WHEN_CLOSING_WORLD";
	
	/**
	 * We are not disabling UNA by default when closing the world during service invocations.
	 */
	public static final String DISABLE_UNA_WHEN_CLOSING_WORLD = "false";
	
	/**
	 * Property name for the path to EHCache configuration file for storing RRGs.
	 * 
	 * This cache is a persistent cache that is used to store RRGs on the server side during asynchronous invocations of sswap
	 * services. There is no default value for this property (if there is no value, SSWAP will use built-in 
	 * configuration file)
	 */
	public static final String RRG_EHCACHE_FILE_KEY = "info.sswap.api.servlet.RRG_EHCACHE_FILE";
	
	/**
	 * Property name for the name of the EHCache cache for storing RRGs (produced by the SSWAP service and available for
	 * asynchronous retrieval)
	 */
	public static final String RRG_EHCACHE_CACHE_NAME_KEY = "info.sswap.api.servlet.RRG_EHCACHE_CACHE_NAME";
	
	/**
	 * Default value of the EHCache cache that stores RRGs (produced by the SSWAP service and available for asynchronous retrieval)
	 */
	public static final String RRG_EHCACHE_CACHE_NAME_DEFAULT = "sswap-rrg-cache";

	/**
	 * Property name for the path to EHCache configuration file for storing content/output of the service (e.g., referenced in an RRG).
	 * 
	 * This cache is a persistent cache that is used to store contentoutput of the service on the server side during asynchronous invocations of sswap
	 * services. There is no default value for this property (if there is no value, SSWAP will use built-in 
	 * configuration file)
	 */
	public static final String CONTENT_EHCACHE_FILE_KEY = "info.sswap.api.servlet.CONTENT_EHCACHE_FILE";

	/**
	 * Property name for the name of the EHCache cache for storing content/output of the service (produced by the SSWAP service)
	 */
	public static final String CONTENT_EHCACHE_CACHE_NAME_KEY = "info.sswap.api.servlet.CONTENT_EHCACHE_CACHE_NAME";

	/**
	 * Default value of the EHCache cache that stores content/output data (produced by the SSWAP service)
	 */
	public static final String CONTENT_EHCACHE_CACHE_NAME_DEFAULT = "sswap-content-cache";
	
	/**
	 * The key for the URI of the module extraction service
	 */
	public static final String MODULE_EXTRACTION_URI_KEY = "info.sswap.ontologies.modularity.uri";
	
	/**
	 * The default URI for the module extraction service
	 */
	public static final String MODULE_EXTRACTION_URI_DEFAULT = "http://community.clarkparsia.com:16384/sswap_me";

	/**
	 * The key for the URI of the module extraction service
	 */
	public static final String MODULE_EXTRACTION_ENABLED_KEY = "info.sswap.ontologies.modularity.enabled";
	
	/**
	 * The default URI for the module extraction service
	 */
	public static final String MODULE_EXTRACTION_ENABLED_DEFAULT = "false";
	
	/**
	 * A singleton instance of the config.
	 */
	private static Config instance = new Config();
	
	/**
	 * Actual property values.
	 */
	private Properties properties = new Properties();
	
	/**
	 * Getter for the singleton instance of the config.
	 * @return the singleton instance of the config
	 */
	public static Config get() {
		return instance;
	}

	/**
	 * Initializes the default values of all known properties.
	 */
	private Config() {
		properties.setProperty(MAX_CLOSURE_DEGREE_KEY, MAX_CLOSURE_DEGREE_DEFAULT);
		properties.setProperty(MAX_HIERARCHY_CLOSURE_DEGREE_KEY, MAX_HIERARCHY_CLOSURE_DEGREE_KEY);
		properties.setProperty(CLOSURE_CONNECT_TIMEOUT_KEY, CLOSURE_CONNECT_TIMEOUT_DEFAULT); 
		properties.setProperty(CLOSURE_READ_TIMEOUT_KEY, CLOSURE_READ_TIMEOUT_DEFAULT); 
		properties.setProperty(CLOSURE_BYTES_LIMIT_KEY, CLOSURE_BYTES_LIMIT_DEFAULT); 
		properties.setProperty(CLOSURE_TIME_LIMIT_KEY, CLOSURE_TIME_LIMIT_DEFAULT); 
		properties.setProperty(CLOSURE_THREADS_KEY, CLOSURE_THREADS_DEFAULT); 
		
		properties.setProperty(MODEL_CACHE_ENTRY_TTL_KEY, MODEL_CACHE_ENTRY_TTL_DEFAULT);
		properties.setProperty(MODEL_CACHE_NEGATIVE_ENTRY_TTL_KEY, MODEL_CACHE_NEGATIVE_ENTRY_TTL_DEFAULT);
		properties.setProperty(HTTP_API_URI_KEY, HTTP_API_URI_DEFAULT);
		properties.setProperty(DISCOVERY_SERVER_QUERY_URI_KEY, DISCOVERY_SERVER_QUERY_URI_DEFAULT);
		properties.setProperty(DISCOVERY_SERVER_QUERY_URI_ALT_KEY, DISCOVERY_SERVER_QUERY_URI_DEFAULT);
		properties.setProperty(RIG_INVOCATION_TIMEOUT_KEY, RIG_INVOCATION_TIMEOUT_DEFAULT);

		properties.setProperty(CACHE_DIR_KEY, CACHE_DIR_DEFAULT + File.separator + UUID.randomUUID().toString());
		properties.setProperty(CACHE_INDEX_FILE_KEY, CACHE_INDEX_FILE_DEFAULT);
		
		properties.setProperty(MODULE_EXTRACTION_URI_KEY, MODULE_EXTRACTION_URI_DEFAULT);
		properties.setProperty(MODULE_EXTRACTION_ENABLED_KEY, MODULE_EXTRACTION_ENABLED_DEFAULT);

		properties.setProperty(DISK_CACHE_ENABLED_KEY, DISK_CACHE_ENABLED_DEFAULT);
		properties.setProperty(CACHE_ENABLED_KEY, CACHE_ENABLED_DEFAULT);
		
		properties.setProperty(DISABLE_UNA_WHEN_CLOSING_WORLD_KEY, DISABLE_UNA_WHEN_CLOSING_WORLD);
		
		properties.setProperty(CONTENT_EHCACHE_CACHE_NAME_KEY, CONTENT_EHCACHE_CACHE_NAME_DEFAULT);
		properties.setProperty(RRG_EHCACHE_CACHE_NAME_KEY, RRG_EHCACHE_CACHE_NAME_DEFAULT);
	}
	
	/**
	 * Gets the value of the specified property.
	 * 
	 * @param key the key identifying the property
	 * @return the value, if known, otherwise null
	 */
	public synchronized String getProperty(String key) {
		return properties.getProperty(key);
	}
	
	/**
	 * Gets the value of the specified property or the specified default value, if the default value is not defined.
	 * 
	 * @param key the key identifying the property
	 * @param defaultValue the default value to be returned, if there is no property with the specified key
	 * @return value if known, or the default value (this method may return null, if the default value is null)
	 */
	public synchronized String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}
	
	/**
	 * Sets the value of the specified property.
	 * 
	 * @param key the key identifying the property
	 * 
	 * @param value the new value for the property
	 */
	public synchronized void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}
	
	/**
	 * Loads the values from the specified input stream (which is expected to contain standard
	 * Java properties file).
	 * 
	 * @param is the input stream
	 * @throws IOException if an I/O error should occur
	 */
	public synchronized void load(InputStream is) throws IOException {
		properties.load(is);
	}		
	
	/**
	 * Loads the values from the specified reader (which is expected to contain standard
	 * Java properties file).
	 * 
	 * @param reader the reader
	 * @throws IOException if an I/O error should occur
	 */
	public synchronized void load(Reader reader) throws IOException {
		properties.load(reader);
	}
}
