/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.net.URI;

/**
 * Represents a literal value in SSWAP. Literals are immutable and are set as an
 * assignment with a {@link SSWAPProperty}.
 * 
 * @see SSWAPProperty
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public interface SSWAPLiteral extends SSWAPElement {
	
	/**
	 * Gets the URI of the datatype. 
	 * 
	 * For the standard XSD datatypes, it is advised
	 * to use constants defined in XSD class (com.hp.hpl.jena.vocabulary);
	 * for example, XSD.xstring.toString() for "xsd:string" and XSD.anyURI.toString()
	 * for "xsd:anyURI".
	 * 
	 * @return the URI of the datatype (if specified) or null.
	 */
	public URI getDatatypeURI();
	
	/**
	 * Gets the language of the literal (if specified)
	 * @return the language code or null if no language is specified
	 */
	public String getLanguage();
}
