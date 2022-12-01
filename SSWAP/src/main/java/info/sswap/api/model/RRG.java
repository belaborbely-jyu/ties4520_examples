/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

/**
 * A Resource Response Graph (<code>RRG</code>) is the return result of a SSWAP
 * semantic web service. An <code>RRG</code> is returned to the invoking client
 * by a SSWAP Resource in response to its invocation via a Resource Invocation
 * Graph (<code>RIG</code>). <code>RRG</code>s are usually made by augmenting the
 * incoming <code>RIG</code> with the mapped values (<code>SSWAPObject</code>s)
 * satisfying the mapping "contract" as stated by the service's <code>RDG</code>.
 * To create an <code>RRG</code>, see methods on <code>RIG</code>.
 * <p>
 * For more on the protocol, see <a href="http://sswap.info/protocol">SSWAP
 * Protocol</a>.
 * 
 * @see PDG
 * @see RDG
 * @see RIG
 * @see RQG
 * @see SSWAPResource
 * @see SSWAPSubject
 * @see SSWAPObject
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 * 
 */
public interface RRG extends SSWAPProtocol {
	
	/**
	 * Returns a new Resource Invocation Graph (<code>RIG</code>) based on the
	 * contents of this Resource Response Graph (<code>RRG</code>) suitable for
	 * the service represented by the Resource Description Graph
	 * (<code>RRG</code>). This allows the output of one service to become the
	 * input to another service. Conversion involves transferring the
	 * <code>SSWAPObject</code>s of the <code>RRG</code> into
	 * <code>SSWAPSubject</code>s of the <code>RIG</code> and updating the
	 * <code>SSWAPResource</code>.
	 * 
	 * @param rdg
	 *            service for which to create the <code>RIG</code>
	 * @return <code>RIG</code> suitable for invocation
	 * @throws DataAccessException
	 *             on inability to create a pre-transformed <code>RIG</code> for
	 *             the <code>RDG</code>
	 * @throws IllegalArgumentException
	 *             on inability to create a transformed <code>RIG</code> from
	 *             this <code>RRG</code>
	 */
	public RIG createRIG(RDG rdg) throws DataAccessException, IllegalArgumentException;
	
	public RQG createRQG() throws DataAccessException, IllegalArgumentException;
}
