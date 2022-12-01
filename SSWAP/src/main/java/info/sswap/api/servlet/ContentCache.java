/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.servlet;

import info.sswap.api.model.Config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Caches arbitrary user/service content (e.g., resources referenced in an RRG)
 * 
 * The underlying cache implementation is currently provided by EHCache
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class ContentCache {
	/**
	 * Name of resource that contains EHCache configuration file
	 */
	private static final String EHCACHE_CONF_FILE = "ehcache-sswap.xml";
		
	/**
	 * Singleton instance of this class
	 */
	private static final ContentCache instance = new ContentCache();
	
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
	public static ContentCache get() {
		return instance;
	}
	
	/**
	 * Private constructor (to be accessed only when creating the singleton instance)
	 */
	private ContentCache() {
		try {
			InputStream is = null;
			String configFile = Config.get().getProperty(Config.CONTENT_EHCACHE_FILE_KEY);
			
			if (configFile != null) {
				is = new FileInputStream(configFile);
			}
			else {
				// read the configuration file from the resource stream (bundled in the jar file)
				// and initialize the EHCache cache manager
				is = ContentCache.class.getResourceAsStream(EHCACHE_CONF_FILE);
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
		return cacheManager.getCache(Config.get().getProperty(Config.CONTENT_EHCACHE_CACHE_NAME_KEY, 
                                                              Config.CONTENT_EHCACHE_CACHE_NAME_DEFAULT));		
	}
	
	/**
	 * Stores content (data), allowing its future retrieval by the client.
	 * This call overwrites any previous contents for that token in the cache.
	 * 
	 * @param token the token identifying the RRG that has been computed
	 * @param content the "content" (data) to be stored
	 * @param contentType "type" of content, such as a MIME type
	 */
	public synchronized void store(String token, byte[] content, String contentType) {
		Entry entry = new Entry();
		entry.setContent(content);
		entry.setContentType(contentType);
		
		getCache().put(new Element(token, entry));
		getCache().flush();
	}
	
	/**
	 * Gets an entry for a (possibly future) content (data). The entry may contain the content,
	 * an error message, or none of the them (in such a case, the entry is a mere indicator
	 * that the token is a valid token, and the content is still being generated; in such a case,
	 * the useful information is in suggestedPollingInterval)
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
        private static final long serialVersionUID = -2085485172676066622L;

		private byte[] content;
		
		private String contentType;
		
		public byte[] getContent() {
			return content;
		}
		
		public void setContent(byte[] content) {
			this.content = content;
		}
		
		public String getContentType() {
			return contentType;
		}
		
		public void setContentType(String contentType) {
			this.contentType = contentType;
		}		
	}
}
