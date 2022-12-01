/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.io;

import info.sswap.api.model.Cache;
import info.sswap.api.model.Config;
import info.sswap.impl.empire.Namespaces;
import info.sswap.impl.empire.model.JenaModelFactory;
import info.sswap.impl.empire.model.ModelUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * A cache for Jena models. This class is thread safe and can be used by concurrently downloading
 * threads.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class ModelCache implements Cache {	
	private static final Logger LOGGER = LogManager.getLogger(ModelCache.class);

	private static final String FILE_PREFIX = "file:";
	
	/**
	 * Cache directory
	 */
	private String cacheDirName = Config.get().getProperty(Config.CACHE_DIR_KEY);
	
	/**
	 * Cache index file
	 */
	private String cacheIndexFile = Config.get().getProperty(Config.CACHE_INDEX_FILE_KEY);
	
	/**
	 * Cache contents mapping URIs to local file paths. 
	 */
	private Properties locationMap = null;
	
	/**
	 * Map of model URIs to Jena Models
	 */
	private Map<String,SoftReference<MemoryCacheEntry>> cache = new HashMap<String,SoftReference<MemoryCacheEntry>>();
	
	/**
	 * Map of model URIs to the time when they were stored in this cache
	 * (to properly detect expired caches).
	 */
	private Map<String,Long> storagePositiveTimes = new HashMap<String,Long>();
	
	private Map<String,Long> storageNegativeTimes = new HashMap<String,Long>();
	
	/**
	 * The queue of URIs in the order they were added (new caches are always added at the end, and
	 * the expired caches are removed from the front).
	 */
	private List<String> positiveUriQueue = new LinkedList<String>();
	
	private List<String> negativeUriQueue = new LinkedList<String>();
	
	/**
	 * Default time-to-live (TTL).
	 * @see #entryTTL
	 */
	private static final long defaultEntryTTL = 4l * 3600l * 1000l; // 4 hours
	
	/**
	 * The time-to-live (TTL) for entries in ms. After this time passes since
	 * the time an entry was stored in the cache, it is considered invalid, and
	 * no longer reported as being in the cache.
	 */
	private long entryTTL = defaultEntryTTL;
	
	private long negativeEntryTTL = 0; // none
	
	/**
	 * Flag whether persistent cache is enabled or disabled.
	 */
	private boolean diskCacheEnabled;
	
	/**
	 * Flag whether any (memory or disk) caching is enabled
	 */
	private boolean cacheEnabled;
	
	/**
	 * Creates an empty cache
	 */
	public ModelCache() {
		this(getConfigEntryTTL(), getConfigNegativeEntryTTL());
	}

	/**
	 * Creates an empty cache with a time-to-live TTL. A negative value resets
	 * to the default.
	 * 
	 * @param entryTTL
	 *            time-to-live for a URI cache, in milliseconds, before it is
	 *            considered stale and removed from the cache
	 * @see #getTimeToLive
	 */
	public ModelCache(long entryTTL, long negativeEntryTTL) {
		this(entryTTL, negativeEntryTTL, true /* diskCacheEnabled */);
	}
	
	public ModelCache(long entryTTL, long negativeEntryTTL, boolean diskCacheEnabled) {
		this.diskCacheEnabled = diskCacheEnabled 
		                        && Boolean.valueOf(Config.get().getProperty(Config.DISK_CACHE_ENABLED_KEY, 
						                                                    Config.DISK_CACHE_ENABLED_DEFAULT));
		
		cacheEnabled = Boolean.valueOf(Config.get().getProperty(Config.CACHE_ENABLED_KEY, Config.CACHE_ENABLED_DEFAULT));		
		
		initDiskCache();
		
		setTimeToLive(entryTTL);
		setNegativeTimeToLive(negativeEntryTTL);
	}

	private synchronized void initDiskCache() {		    
		if (!cacheEnabled || !diskCacheEnabled) {
			return;
		}
		
        try {
        	cacheDirName = Config.get().getProperty(Config.CACHE_DIR_KEY);
        	cacheIndexFile = Config.get().getProperty(Config.CACHE_INDEX_FILE_KEY);
        	locationMap = new Properties();
        	
        	LOGGER.debug("Initialize cache; Directory: " + cacheDirName + " Index: " + cacheIndexFile);
        	
	        File cacheDir = new File(cacheDirName);
	        if (!cacheDir.exists()) {
	        	cacheDir.mkdirs();
	        }
	        
	        File indexFile = new File(cacheDir, cacheIndexFile);
	        if (indexFile.exists()) {
	        	locationMap.load(new FileInputStream(indexFile));
	        	LOGGER.debug("Initialized cache with " + locationMap.size() + " entries");
	        }
        }
        catch (Exception e) {
            LOGGER.error("Error initializing cache", e);
        }
	}
	
	private synchronized Properties getLocationMap() {
		if (locationMap == null) {
			initDiskCache();
		}
		
		return locationMap;
	}
	
	/**
	 * Gets the time-to-live (TTL) for entries. After this time passes since an
	 * entry was stored in the cache, it is considered invalid, and will be
	 * replaced, if possible, by a fresh network call.
	 * 
	 * @return TTL in ms
	 */
	public long getTimeToLive() {
		return entryTTL;
	}

	/**
	 * Sets the time-to-live (in milliseconds) for entries in this cache. A
	 * negative value resets to the default.
	 * 
	 * @param entryTTL
	 *            the new TTL in ms
	 */
	public void setTimeToLive(long entryTTL) {
		this.entryTTL = (entryTTL < 0) ? defaultEntryTTL 
						               : entryTTL;
		
		removeExpiredData();
	}
	
	public long getNegativeTimeToLive() {
		return negativeEntryTTL;
	}
	
	public void setNegativeTimeToLive(long negativeEntryTTL) {
		this.negativeEntryTTL = (negativeEntryTTL < 0) ? 0l 
						                               : negativeEntryTTL; 
	}
	
	private static File toFile(String fileURL) {		
		if (fileURL.startsWith(FILE_PREFIX)) {
			fileURL = fileURL.substring(FILE_PREFIX.length());
		}								
		
		return new File(fileURL);		
	}
	
	private void clearMemory() {
		if (cacheEnabled) {
			cache.clear();
			storagePositiveTimes.clear();
			storageNegativeTimes.clear();
			positiveUriQueue.clear();
			negativeUriQueue.clear();
		}
	}
	
	private void clearDisk() {
		if (cacheEnabled && diskCacheEnabled) {
			try {
				Properties properties = getLocationMap();

				for (String term : properties.stringPropertyNames()) {
					String url = properties.getProperty(term).toString();

					File file = toFile(url);

					file.delete();
				}

				FileWriter fw = new FileWriter(new File(cacheDirName, cacheIndexFile));
				getLocationMap().clear();
				getLocationMap().store(fw, null);
				fw.close();
			}
			catch (Exception e) {
				LOGGER.error("Problem while clearing the ModelCache", e);
			}
		}	
	}
	
	public synchronized void clear() {
		LOGGER.debug("Cleaning cache");
		clearMemory();	
		clearDisk();		
	}
	
	/**
	 * Gets the directory where the files are stored in the cache.
	 * 
	 * @return the file object for the directory
	 */
	public File getDirectory() {
		return new File(cacheDirName);
	}
	
	/**
	 * Checks whether the cache already contains model for this URI. 
	 * 
	 * @param uri the URI of the model
	 * @return true if the model exists, false otherwise; but see deprecation warnings
	 * @deprecated The use of this method may cause problems with TTL. (It is possible
	 * that this method would report a model as existing in this cache, but then 
	 * the model's TTL would be exceeded before getModel() is called, which would cause
	 * getModel() to return null)
	 */
	public synchronized boolean containsModel(String uri) {
		if (!cacheEnabled) {
			return false;
		}
		
		if ( (uri = ModelUtils.normalizeURI(uri)) == null ) {	// assign; failure equals does not contain
			return false;
		}
		
		return cache.containsKey(uri);
	}
	
	/**
	 * Gets a copy of a model that is already in the cache.
	 * (This method returns a copy so that the caller may start modifying the model.)
	 * 
	 * @param uri the URI of the model
	 * @return the copy of the cached model or null, if the model is not currently cached
	 */		
	public synchronized Model getModel(String uri) {
		if (!cacheEnabled) {
			return null;
		}
		
		if ( (uri = ModelUtils.normalizeURI(uri)) == null ) {	// assign; fail silently
			return null;
		}
		
		LOGGER.trace("Getting " + uri + " from memory");
		
		Model result = getModelFromMemory(uri);
		
		if (result == null) {
			LOGGER.trace("Getting " + uri + " from memory failed; getting from disk");
			result = getModelFromDisk(uri);
		}
		
		if (result == null) {
			LOGGER.trace("Cache miss for " + uri);
		}
		
		return result;
	}
	
	private Model getModelFromMemory(String uri) {
		if ( (uri = ModelUtils.normalizeURI(uri)) == null ) {	// assign; fail silently
			return null;
		}
		
		removeExpiredData();

		Model cachedModel = null; 
		
		SoftReference<MemoryCacheEntry> cachedReference = cache.get(uri);
		boolean negative = false;
		
		if (cachedReference != null) {
			MemoryCacheEntry memCacheEntry = cachedReference.get();
			
			if (memCacheEntry != null) {
				if (memCacheEntry.isNegativeEntry()) {
					negative = true;
				}
				else {
					cachedModel = memCacheEntry.getModel();
				}
			}
		}
		
		Model result = null;
		
		if ((cachedModel != null) || negative) {
			result = JenaModelFactory.get().createEmptyModel();
			
			if (cachedModel != null) {
				result.add(cachedModel);
			}
		}				
		
		return result;		
	}
	
	private boolean isDiskEntryExpired(String fileURL) {
		File file = toFile(fileURL);
		
		return (System.currentTimeMillis() > (file.lastModified() + entryTTL)); 
	}
	
	private Model getModelFromDisk(String uri) {
		if (!diskCacheEnabled) {
			LOGGER.trace("Disk cache is disabled");
			return null;
		}
		
		Model result = null;
		
		try {
			String fileURL = getLocationMap().getProperty(uri, null);
			
			if (fileURL == null) {
				LOGGER.trace("Entry not found in disk cache: "  + uri);
				return null;
			}
			
			if (isDiskEntryExpired(fileURL)) {
				LOGGER.debug("Entry is expired in disk cache: "  + uri + " / " + fileURL);
				return null;
			}

			URL url = new URL(fileURL);

			URLConnection urlConnection = url.openConnection();
			urlConnection.connect();
			InputStream in = urlConnection.getInputStream();

			// parse the model
			result = JenaModelFactory.get().getModel(in);
			in.close();
		}
		catch (Exception e) {
			LOGGER.debug("Error while reading data from cache");
			// nothing -- result will just be null on failure, which is what we want
		}

		return result;
	}
	
	/**
	 * Stores a copy of a model in the cache. (The method copies the model so that the caller
	 * may still modify its model.)
	 * 
	 * @param uri the URI of the model
	 * @param model the model whose copy will be cached.
	 */
	public void setModel(String uri, Model model) {
		setModel(uri, model, false /* negative */);
	}
	
	private synchronized void setModel(String uri, Model model, boolean negative) {
		if (!cacheEnabled) {
			return;
		}
		
		if ((uri = ModelUtils.normalizeURI(uri)) == null) {	// assign; fail silently
			return;
		}

		storeInMemory(uri, model, negative);

		if (!negative) {
			// we do not store on disk failures
			storeOnDisk(uri, model);
		}
	}
	
	public void setAsInaccessible(String uri) {
		if (!cacheEnabled) {
			return;
		}
		
		// do not ever record failures about retrieval of sswap terms
		if (uri.startsWith(Namespaces.SSWAP_NS)) {
			return;
		}
		
		setModel(uri, null, true /* negative */);
	}
	
	private void storeInMemory(String uri, Model model, boolean negative) {
		removeExpiredData();
		
		if (cache.get(uri) != null) {
			positiveUriQueue.remove(uri);
			negativeUriQueue.remove(uri);
		}
		
		if (entryTTL > 0) {	// only add if we have a positive time-to-live			
			Model cachedModel = null;
			
			if (model != null) {
				cachedModel = JenaModelFactory.get().createEmptyModel();
				cachedModel.add(model);
			}
			
			MemoryCacheEntry entry = new MemoryCacheEntry(cachedModel, negative);
			
			cache.put(uri, new SoftReference<MemoryCacheEntry>(entry));
			
			if (negative) {
				storageNegativeTimes.put(uri, System.currentTimeMillis());
				negativeUriQueue.add(uri);
			} 
			else {
				storagePositiveTimes.put(uri, System.currentTimeMillis());
				positiveUriQueue.add(uri);
			}									
		}	
	}
	
	private void storeOnDisk(String uri, Model model) {
		if (!diskCacheEnabled) {
			return;
		}
		
		try {
			File file = null;
			
			String existingFileURL = getLocationMap().getProperty(uri, null);
			
			if (existingFileURL != null) {
				// update of an existing term
				file = toFile(existingFileURL);
			}
			else {
				// new term -- generate a new temp file
				file = File.createTempFile("file", "", new File(cacheDirName));
			}
			
			String fileURI = file.toURI().toString();

			LOGGER.debug("Adding to file cache URL " + uri + " -> " + fileURI);

			model.write(new FileOutputStream(file));

			FileWriter fw = new FileWriter(new File(cacheDirName, cacheIndexFile));
			getLocationMap().setProperty(uri, fileURI);
			getLocationMap().store(fw, null);
			fw.close();
		}
		catch (Exception e) {
			// nothing -- on error the file won't be stored in the disk cache
		}		
	}
	
	/**
	 * Checks whether an entry is expired. If the entry does not exists, it is always
	 * treated as expired
	 * 
	 * @param uri the URI of the entry to check for expiration
	 * @return true if the entry is expired (or does not exist at all), false otherwise
	 */
	private boolean isExpired(String uri, boolean negative) {		
		if ( (uri = ModelUtils.normalizeURI(uri)) != null ) {	// assign; failure equals expired
		
			Long storageTime = negative ? storageNegativeTimes.get(uri) 
							            : storagePositiveTimes.get(uri);
			
			long ttl = negative ? negativeEntryTTL
							    : entryTTL;
		
			if ((storageTime != null) && ((storageTime.longValue() + ttl) >= System.currentTimeMillis())) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Removes all entries that are expired
	 */
	private synchronized void removeExpiredData() {
		removeExpiredData(true /* negative */);
		removeExpiredData(false /* negative */);
	}
	
	private synchronized void removeExpiredData(boolean negative) {		
		Map<String,Long> storageTimes = negative ? storageNegativeTimes 
						                         : storagePositiveTimes;
		
		List<String> uriQueue = negative ? negativeUriQueue 
						                 : positiveUriQueue;
				
		for (Iterator<String> it = uriQueue.iterator(); it.hasNext(); ) {
			String uri = it.next();
			
			if (isExpired(uri, negative)) {
				cache.remove(uri);
				storageTimes.remove(uri);
				it.remove();
			}
			else {
				// the queue is sorted by storage time -- so if we reach an non-expired model, we can quit at this point
				break;
			}
		}	
	}

	private static long getConfigEntryTTL() {
		try {
			return Long.parseLong(Config.get().getProperty(Config.MODEL_CACHE_ENTRY_TTL_KEY, Config.MODEL_CACHE_ENTRY_TTL_DEFAULT));
		}
		catch (NumberFormatException e) {
			return Long.parseLong(Config.MODEL_CACHE_ENTRY_TTL_DEFAULT);
		}
	}
	
	private static long getConfigNegativeEntryTTL() {
		try {
			return Long.parseLong(Config.get().getProperty(Config.MODEL_CACHE_NEGATIVE_ENTRY_TTL_KEY, Config.MODEL_CACHE_NEGATIVE_ENTRY_TTL_DEFAULT));
		}
		catch (NumberFormatException e) {
			return Long.parseLong(Config.MODEL_CACHE_NEGATIVE_ENTRY_TTL_DEFAULT);
		}
	}
	
	static class MemoryCacheEntry {
		private Model model;
		private boolean negativeEntry;
		
		public MemoryCacheEntry(Model model, boolean negativeEntry) {
			this.model = model;
			this.negativeEntry = negativeEntry;
		}
		
		public boolean isNegativeEntry() {
			return negativeEntry;
		}
		
		public Model getModel() {
			return model;
		}
	}
}
