/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

/**
 * Enumeration of different representations of RDF data.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public enum RDFRepresentation {
	RDF_XML("application/rdf+xml"),
	N3("text/n3"),
	NTRIPLES("text/plain"),
	TURTLE("text/turtle"),
	TSV("text/tab-separated-values");
	
	private final String mimeType;
	
	private RDFRepresentation(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getMIMEType() {
		return mimeType;
	}
}
