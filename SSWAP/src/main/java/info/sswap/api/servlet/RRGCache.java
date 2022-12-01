/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.servlet;

import info.sswap.api.model.Config;
import info.sswap.api.model.RRG;
import info.sswap.api.model.SSWAP;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Manages cached RRGs and information about them (including information about RRGs that have not yet been computed,
 * but they already have a token (identifier) so that the clients can query/poll for them).
 * 
 * The underlying cache implementation is currently provided by EHCache
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class RRGCache {
	/**
	 * Name of resource that contains EHCache configuration file
	 */
	private static final String EHCACHE_CONF_FILE = "ehcache-sswap.xml";
	
	/**
	 * Default suggested polling interval (in milliseconds)
	 */
	public static final int DEFAULT_SUGGESTED_POLLING_INTERVAL = 5 * 1000; // 5 seconds
	
	/**
	 * Singleton instance of this class
	 */
	private static final RRGCache instance = new RRGCache();
	
	/**
	 * EHCache CacheManager
	 */
	private CacheManager cacheManager = null;
	
	private boolean active;
	
	/**
	 * Accessor for the singleton instance
	 * 
	 * @return the singleton instance
	 */
	public static RRGCache get() {
		return instance;
	}
	
	/**
	 * Private constructor (to be accessed only when creating the singleton instance)
	 */
	private RRGCache() {
		try {
			InputStream is = null;
			
			String configFile = Config.get().getProperty(Config.RRG_EHCACHE_FILE_KEY);
			
			if (configFile != null) {
				is = new FileInputStream(configFile);
			}
			else {
				// read the configuration file from the resource stream (bundled in the jar file)
				// and initialize the EHCache cache manager
				is = RRGCache.class.getResourceAsStream(EHCACHE_CONF_FILE);				
			}
			
			cacheManager = new CacheManager(is);
			active = true;
			is.close();
		} 
		catch (IOException e) {
			throw new IllegalStateException("Unable to read configuration file for ehcache", e);
		}
	}
	
	/**
	 * Gets the cache used for storing RRGs from EHCache cache manager
	 * @return the cache for storing RRGs
	 */
	private Cache getCache() {
		return cacheManager.getCache(Config.get().getProperty(Config.RRG_EHCACHE_CACHE_NAME_KEY, 
						                                      Config.RRG_EHCACHE_CACHE_NAME_DEFAULT));	
	}
	
	/**
	 * Sets suggested polling interval for a (potentially future) RRG identified by a token.
	 * If there is no entry in the cache for this token, it is created, otherwise the suggested polling
	 * interval is added to the existing entry.
	 * 
	 * @param token the token identifying the (potentially future) RRG
	 * @param suggestedPollingInterval suggested polling interval in milliseconds
	 */
	public synchronized void setSuggestedPollingInterval(String token, int suggestedPollingInterval) {
		Cache cache = getCache(); 
		
		Element element = cache.get(token);
		Entry entry = null;
		
		// if there is already an entry, reuse it, otherwise, create a new one
		if (element != null) {
			entry = (Entry) element.getValue();
		}
		else {
			entry = new Entry();
		}
		
		entry.setSuggestedPollingInterval(suggestedPollingInterval);
		
		cache.put(new Element(token, entry));
	}
	
	/**
	 * Sets an error message, indicating that computation of the particular RRG has failed. This call
	 * overwrites any previous contents for that token in the cache. (If there is a previous entry in the cache, 
	 * it should only contain polling interval -- it is safe to overwrite such an entry, since there should be no
	 * polling anymore; an error is a terminal state for an RRG computation).
	 * 
	 * @param token the token identifying the RRG that has been computed
	 * @param error the error message
	 */
	public synchronized void setError(String token, String error) {
	Entry entry = get(token);
		
		if (entry == null) {
			entry = new Entry();
		}
	
		entry.setErrorMessage(error);
		
		getCache().put(new Element(token, entry));		
	}
	
	public synchronized void setStatus(String token, String status) {
		Entry entry = get(token);
		
		if (entry == null) {
			entry = new Entry();
		}
		
		entry.setStatus(status);
		
		getCache().put(new Element(token, entry));
	}
	
	/**
	 * Stores a computed RRG, indicating that the computation of an RRG was successful, and allowing its future
	 * retrieval by the client. This call overwrites any previous contents for that token in the cache. (If there is a previous
	 * entry in the cache, it should only contain polling interval -- it is safe to overwrite such an entry, since there should be
	 * no polling anymore; storing the computed RRG is a terminal state for an RRG computation).
	 * 
	 * @param token the token identifying the RRG that has been computed
	 * @param rrg the computed RRG
	 */
	public synchronized void store(String token, RRG rrg) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		rrg.serialize(bos);
		try {
	        bos.close();
        }
        catch (IOException e) {
        	// ignored
        }
		
		String serialization = new String(bos.toByteArray());
		
		
		Entry entry = get(token);
		
		if (entry == null) {
			entry = new Entry();
		}
		
		entry.setRrgSerialization(serialization);
		
		getCache().put(new Element(token, entry));
		getCache().flush();
	}
	
	/**
	 * Gets an entry for a (possibly future) RRG. The entry may contain RRG (if the computation has finished successfully),
	 * an error message (if the computation failed), or none of the them (in such a case, the entry is a mere indicator
	 * that the token is a valid token, and the computation is still being performed; in such a case, the useful information
	 * is in suggestedPollingInterval)
	 * 
	 * @param token the token for which the entry should be obtained
	 * @return the entry or null (if there is no entry for this token)
	 */
	public synchronized Entry get(String token) {
		Element element = getCache().get(token);
		
		if (element == null) {
			return null;
		}
		
		return (Entry) element.getValue();		
	}
	
	public boolean isActive() {
		return active;
	}
	
	public synchronized void shutdown() {
		cacheManager.shutdown();
		active = false;
	}
	
	/**
	 * An entry of the cache. It implements Serializable so that EHCache can persist it to disk.
	 */
	static class Entry implements Serializable {
		/**
         * Generated serial version identifier for a serializable class
         */
        private static final long serialVersionUID = -3870783536093394229L;

		/**
		 * The serialization of RRG as RDF/XML (we cannot have RRG object there because it is not Serializable)
		 * It may be null, if RRG has not yet been computed
		 */
		private String rrgSerialization;
		
		/**
		 * The error message (if there is one; it may be null)
		 */
		private String errorMessage;
		
		/**
		 * The status messge (if there is one; it may be null)
		 */
		private String status;
		
		/**
		 * Suggested polling interval in milliseconds (this field only matters if the other fields are null; i.e., 
		 * if polling makes sense; if the other fields are not null, the computation reached a terminal state).
		 */
		private int suggestedPollingInterval;
		
		/**
         * @return the rrgSerialization
         */
        public String getRrgSerialization() {
        	return rrgSerialization;
        }
        
        public RRG getRRG() {
        	if (rrgSerialization == null) {
        		return null;
        	}
        	
        	ByteArrayInputStream bis = new ByteArrayInputStream(rrgSerialization.getBytes());
    		
    		RRG result = SSWAP.getResourceGraph(bis, RRG.class);
    		
    		try {
    	        bis.close();
            }
            catch (IOException e) {
            	// ignored -- we are closing an in-memory stream
            }
    		
    		return result;
        }
        
		/**
         * @param rrgSerialization the rrgSerialization to set
         */
        public void setRrgSerialization(String rrgSerialization) {
        	this.rrgSerialization = rrgSerialization;
        }
        
		/**
         * @return the errorMessage
         */
        public String getErrorMessage() {
        	return errorMessage;
        }
        
		/**
         * @param errorMessage the errorMessage to set
         */
        public void setErrorMessage(String errorMessage) {
        	this.errorMessage = errorMessage;
        }
        
        public void setStatus(String status) {
        	this.status = status;
        }
        
        public String getStatus() {
        	return status;
        }
        
		/**
         * @return the suggestedPollingInterval
         */
        public int getSuggestedPollingInterval() {
        	return suggestedPollingInterval;
        }
        
		/**
         * @param suggestedPollingInterval the suggestedPollingInterval to set
         */
        public void setSuggestedPollingInterval(int suggestedPollingInterval) {
        	this.suggestedPollingInterval = suggestedPollingInterval;
        }		
	}
}
