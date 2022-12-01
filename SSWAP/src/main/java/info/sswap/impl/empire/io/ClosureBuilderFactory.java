/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.io;

import java.util.List;

import com.google.common.collect.Lists;

import info.sswap.api.model.Config;

/**
 * Creates a ClosureBuilder and specifies the default parameters (if not 
 * provided by the caller)
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 *
 */
public class ClosureBuilderFactory {
	/**
	 * The default model cache shared by all builders, unless overridden for a specific builder.
	 */
	private static final ModelCache MODEL_CACHE = new ModelCache();
	
	/**
	 * The byte limit for the generated closure builder. The default is 512KB
	 */
	private long maxBytes;
	
	/**
	 * The time limit for the generated closure builder. The default is 30 seconds
	 */
	private long maxTime;
	
	/**
	 * The number of concurrent threads used to compute the closure. The default is 2.
	 */
	private int maxThreads;
	
	/**
	 * The model cache that will be used by the builder -- by default, MODEL_CACHE is used, unless
	 * overridden via setModelCache(ModelCache) method.
	 */
	private ModelCache modelCache = MODEL_CACHE;
	
	/**
	 * Namespaces for which closure will not be computed.
	 */
	private final List<String> ignoredNamespaces = Lists.newArrayList();
	
	/**
	 * A private constructor
	 */
	private ClosureBuilderFactory() {	
		try {
			maxBytes = Long.parseLong(Config.get().getProperty(Config.CLOSURE_BYTES_LIMIT_KEY, Config.CLOSURE_BYTES_LIMIT_DEFAULT));
		}
		catch (NumberFormatException e) {
			maxBytes = Long.parseLong(Config.CLOSURE_BYTES_LIMIT_DEFAULT);
		}
		
		try {
			maxTime = Long.parseLong(Config.get().getProperty(Config.CLOSURE_TIME_LIMIT_KEY, Config.CLOSURE_TIME_LIMIT_DEFAULT));
		}
		catch (NumberFormatException e) {
			maxTime = Long.parseLong(Config.CLOSURE_TIME_LIMIT_DEFAULT);
		}
		
		try {
			maxThreads = Integer.parseInt(Config.get().getProperty(Config.CLOSURE_THREADS_KEY, Config.CLOSURE_THREADS_DEFAULT));
		}
		catch (NumberFormatException e) {
			maxThreads = Integer.parseInt(Config.CLOSURE_THREADS_DEFAULT);
		}
	}
	
	/**
	 * Creates a new ClosureBuilderFactory
	 * @return a new ClosureBuilderFactory
	 */
	public static ClosureBuilderFactory newInstance() {
		return new ClosureBuilderFactory();
	}
	
	/**
	 * Creates a ClosureBuilder with the currently set parameters
	 * @return the new ClosureBuilder
	 */
	public ClosureBuilder newBuilder() {
		return new ClosureBuilder(maxBytes, maxTime, maxThreads, modelCache, ignoredNamespaces);
	}

	/**
	 * Gets the current byte limit
	 * 
     * @return the current byte limit
     */
    public long getMaxBytes() {
    	return maxBytes;
    }

	/**
	 * Sets the current byte limit.
	 * 
     * @param maxBytes the new byte limit
     * @return this factory
     */
    public ClosureBuilderFactory setMaxBytes(long maxBytes) {
    	this.maxBytes = maxBytes;
    	
    	return this;
    }

	/**
	 * Gets the current time limit.
	 * 
     * @return the time limit (in milliseconds)
     */
    public long getMaxTime() {
    	return maxTime;
    }

	/**
	 * Sets the new time limit (in milliseconds)
	 * 
     * @param maxTime the time limit (in milliseconds)
     * @return this factory
     */
    public ClosureBuilderFactory setMaxTime(long maxTime) {
    	this.maxTime = maxTime;
    	
    	return this;
    }

	/**
	 * Gets the number of concurrent threads that will build the closure
	 * 
     * @return the number of concurrent threads
     */
    public int getMaxThreads() {
    	return maxThreads;
    }

	/**
	 * Sets the number of concurrent threads that will build the closure
	 * 
     * @param maxThreads the number of concurrent threads
	 * @return this object
     */
    public ClosureBuilderFactory setMaxThreads(int maxThreads) {
    	this.maxThreads = maxThreads;
    	
    	return this;
    }

	/**
	 * Gets the model cache that will be used by the ClosureBuilders
	 * 
     * @return the modelCache the model cache
     */
    public ModelCache getModelCache() {
    	return modelCache;
    }

	/**
	 * Sets a new model cache that will be used by ClosureBuilders
	 * 
     * @param modelCache the modelCache to set
     */
    public void setModelCache(ModelCache modelCache) {
    	this.modelCache = modelCache;
    }	
    
    /**
	 * Adds a new namespace to the ignore list for which closure will not be
	 * computed. The standard namespaces for RDF, RDFS, OWL and XSD are always
	 * ignored and do not need to be added here explicitly.
	 * 
	 * @param ns
	 *            a new namespace
	 * @return this object
	 */
    public ClosureBuilderFactory addIgnoredNamespace(String ns) {
    	ignoredNamespaces.add(ns);
    	return this;
    }
    
    /**
     * Gets the default model cache that is shared by all closure-builders (unless it is specifically overridden).
     * 
     * @return a model cache
     */
    public static ModelCache getDefaultModelCache() {
    	return MODEL_CACHE;
    }
}
