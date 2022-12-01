/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.io.File;

/**
 * Cache for ontology terms. This cache provides temporary storage for terms
 * after they are retrieved. It acts to minimize network calls (both improving
 * efficiency as well as allowing uninterrupted operation over short-term
 * network outages). Contents of the cache have limited life time
 * (time-to-live), which means that they are removed from the cache after a
 * period of time, and will be retrieved from their original source (when they
 * are needed; this is to allow the system to update to potential changes to the
 * published terminologies).
 * <p>
 * The cache can be primed by simply reading a term (<i>e.g.</i>, with
 * {@link SSWAP#createSSWAPDocument(java.net.URI)}, for example, during servlet
 * initialization. Upon reading, full closure of terms referenced therein will
 * be also be cached. But terms will be purged after the time-to-live (TTL), so
 * to keep terms persistent in the cache, said terms will need to be re-read at
 * an interval less than the cache TTL.
 * <p>
 * The methods in this interface allow basic of control the cache (<i>e.g.</i>,
 * clear its contents, set time-to-live, etc.). Low level control
 * of certain parameters may be set with {@link Config}.
 * 
 * @see Config
 * @see SSWAP
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public interface Cache {
	/**
	 * Removes all entries from the cache.
	 */
	public void clear();
	
	/**
	 * Gets the current time-to-live for entries in this cache; that is, the time after which an entry in this
	 * cache is considered stale, and will be retrieved again from its source.
	 *  
	 * (All contents in this cache share the same time-to-live value, but the decision when each term
	 * expires depends on the time when that particular term was originally retrieved.)
	 *   
	 * @return the time-to-live for a cache entry in milliseconds
	 */
	public long getTimeToLive();	
	
	/**
	 * Sets the time-to-live for entries in this cache; that is, the time after which an entry in this
	 * cache is considered stale, and will be retrieved again from its source.
	 *  
	 * (All contents in this cache share the same time-to-live value, but the decision when each term
	 * expires depends on the time when that particular term was originally retrieved.)
	 *  
	 * 
	 * @param timeToLive time-to-live for a cache entry in milliseconds (0 disables the cache;
	 * a negative value resets the time-to-live to the default value, as defined in {@link Config}).
	 */
	public void setTimeToLive(long timeToLive);
	
	/**
	 * Gets the directory where the cache contents are stored. 
	 * 
	 * @return Java File object representing the directory where the cache contents are stored.
	 */
	public File getDirectory();
}
