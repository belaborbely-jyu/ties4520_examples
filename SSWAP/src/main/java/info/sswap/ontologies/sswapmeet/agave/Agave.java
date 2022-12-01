/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.sswapmeet.agave;

import info.sswap.impl.empire.Namespaces;

import java.net.URI;

/**
 * 
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 *
 */
public interface Agave {

	public static final String NS = Namespaces.SSWAP_AGAVE_NS;
	
	public interface Apps {
		
		public static final String NS = Agave.NS + "apps/";

	    public static final URI Application = URI.create(NS + "Application");

	    public static final URI id = URI.create(NS + "id");
	    public static final URI name = URI.create(NS + "name");
	    public static final URI version = URI.create(NS + "version");
	    
	    public interface Outputs {
	    	
			public static final String NS = Agave.Apps.NS + "outputs/";

		    public static final URI id = URI.create(NS + "id");

	    }
	}
	
	public interface Auth {
		
		public static final String NS = Agave.NS + "auth/";

	    public static final URI token = URI.create(NS + "token");
	    public static final URI username = URI.create(NS + "username");
	    
	}
	
	public interface Job {
		
		public static final String NS = Agave.NS + "job/";

	    public static final URI archive = URI.create(NS + "archive");
	    public static final URI archivePath = URI.create(NS + "archivePath");
	    public static final URI callbackURL = URI.create(NS + "callbackURL");
	    public static final URI name = URI.create(NS + "name");
	    public static final URI maxMemory = URI.create(NS + "maxMemory");
	    public static final URI processorCount = URI.create(NS + "processorCount");
	    public static final URI requestedTime = URI.create(NS + "requestedTime");

	}
	
	public interface IO {
		
		public static final String NS = Agave.NS + "io/";

	    public static final URI IPlantData = URI.create(NS + "IPlantData");

	}
    
}