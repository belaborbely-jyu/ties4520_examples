/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import java.net.URI;

/**
 * An concrete implementation of SSWAPIndividual that is not a SSWAPNode and is not Empire-managed.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class NonEmpireIndividual extends IndividualImpl {

	/**
	 * The URI key containing the URI of the individual
	 */
	private URIKey uriKey;

	public NonEmpireIndividual(URI uri) {
		this.uriKey = new URIKey(uri);
	}

	/**
	 * Gets the identifier of the individual
	 * 
	 * @return the identifier of the individual
	 */
	@SuppressWarnings("unchecked")
	public RdfKey getRdfId() {
		return uriKey;
	}

	/**
	 * A setter for the RDF Identifier of this individual. Since the objects of this type
	 * have their identifiers set in the constructor, and it is not allowed to set
	 * the identifier for the second time, this method always throws IllegalStateException
	 * 
	 * @param newURI the new URI
	 * @throws IllegalStateException always
	 */
	@SuppressWarnings("unchecked")
	public void setRdfId(RdfKey newURI) {
		throw new IllegalStateException("The RdfId is already set");
	}

}
