/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.sswapmeet;

import info.sswap.impl.empire.Namespaces;

import java.net.URI;

/**
 * 
 * Core SSWAPMeet Ontologies
 * 
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 *
 */
public interface SSWAPMeet {
	
	public static final String NS = Namespaces.SSWAPMEET_NS;

	public interface Mime {
		
		public static final String NS = Namespaces.SSWAP_MIME_NS;

	    public static final URI Textual = URI.create(NS + "Textual");
	    
	    public static interface text {
	    	
			public static final String NS = Mime.NS + "text/";

		    public static final URI Html = URI.create(NS + "Html");

		    public static final URI X_fasta = URI.create(NS + "X-fasta");
		    public static final URI X_multiFasta = URI.create(NS + "X-multiFasta");
		    public static final URI X_newick = URI.create(NS + "X-newick");
		    public static final URI X_nexus = URI.create(NS + "X-nexus");
		    public static final URI X_phylip = URI.create(NS + "X-phylip");

	    }

	}
    
	public interface Data {
		
		public static final String NS = Namespaces.SSWAP_DATA_NS;
		
		public static final URI Data = URI.create(NS + "Data");
		
		public static final URI DataFormat = URI.create(NS + "DataFormat");
		public static final URI DataBundle = URI.create(NS + "DataBundle");

		public static final URI Accessor = URI.create(NS + "Accessor");
		public static final URI Parser = URI.create(NS + "Parser");
		public static final URI Serializer = URI.create(NS + "Serializer");
		public static final URI Validator = URI.create(NS + "Validator");
		
		public static final URI hasData = URI.create(NS + "hasData");

		public static final URI hasAccessor = URI.create(NS + "hasAccessor");
		public static final URI hasParser = URI.create(NS + "hasParser");
		public static final URI hasSerializer = URI.create(NS + "hasSerializer");
		public static final URI hasValidator = URI.create(NS + "hasValidator");
		
		public static final URI literalData = URI.create(NS + "literalData");
		
	}
	
	public interface Exec {
		
		public static final String NS = Namespaces.SSWAP_EXEC_NS;
		
		public static final URI Exec = URI.create(NS + "Exec");
		
		public static final URI ExecCmd = URI.create(NS + "ExecCmd");
		
		public static final URI datatypeProperty = URI.create(NS + "datatypeProperty");
		public static final URI args = URI.create(NS + "args");
		public static final URI command = URI.create(NS + "command");
		public static final URI exitValue = URI.create(NS + "exitValue");
		public static final URI stderr = URI.create(NS + "stderr");
		public static final URI synopsis = URI.create(NS + "synopsis");

	}
}