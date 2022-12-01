/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.net.URI;
import java.util.Collection;

/**
 * A Provider Description Graph (<code>PDG</code>) describes a SSWAP semantic
 * web service Provider. <code>SSWAPProvider</code>s host one or more SSWAP
 * services, each described by its own Resource Description Graph (
 * <code>RDG</code>). Usually, an individual, institution, or web site has only
 * a single <code>PDG</code>, accessible to any one on the web by dereferencing
 * the URI of the <code>SSWAPProvider</code>.
 * <p>
 * For more on the protocol, see <a href="http://sswap.info/protocol">SSWAP
 * Protocol</a>.
 * 
 * @see RDG
 * @see RIG
 * @see RRG
 * @see RQG
 * @see SSWAPProvider
 * @see SSWAPResource
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 */
public interface PDG extends SSWAPDocument {
	/**
	 * Gets the provider described in this PDG.
	 * 
	 * @return SSWAPProvider object (dereferenced, since the main purpose of the PDG is to describe that provider).
	 */
	public SSWAPProvider getProvider();
	
	/**
	 * Creates a new SSWAPProvider in this PDG
	 * 
	 * @param providerURI the URI of the provider (which should be the same as the URI of the PDG)
	 * @return the newly created provider
	 */
	public SSWAPProvider createProvider(URI providerURI);
	
	/**
	 * Gets all RDGs pointed to by a sswap:providesResource statement in this PDG
	 * 
	 * @return a collection of RDGs
	 * @throws DataAccessException if an error should occur while accesing the RDG data
	 */
	public Collection<RDG> getRDGs() throws DataAccessException;
}
