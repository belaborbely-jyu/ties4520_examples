/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.io.InputStream;

/**
 * A Resource Description Graph (<code>RDG</code>) describes a SSWAP semantic
 * web service. <code>SSWAPProvider</code>s host one or more of these services,
 * each described by its own <code>RDG</code>, each accessible to any one on the
 * web by dereferencing the URI of the <code>SSWAPResource</code>.
 * <code>RDG</code>s are the core of the SSWAP architecture.
 * <p>
 * For more on the protocol, see <a href="http://sswap.info/protocol">SSWAP
 * Protocol</a>.
 * 
 * @see PDG
 * @see RIG
 * @see RRG
 * @see RQG
 * @see SSWAPProvider
 * @see SSWAPResource
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 */
public interface RDG extends SSWAPProtocol {
	/**
	 * Gets the RIG that can be used to invoke the service described in this RDG (after setting the values for
	 * the properties).
	 * 
	 * @return the RIG created based on this RDG
	 * @throws DataAccessException if an error should occur while accessing the data in the RDG to create the RIG
	 */
	public RIG getRIG() throws DataAccessException;
	
	/**
	 * Creates a RIG object based on the serialized RIG (e.g., sent by a client of a service). The RIG
	 * should be an invocation of the service described in this RDG, and this method validates that this
	 * is the case.  
	 * 
	 * @param is the input stream to read the RIG
	 * @return the created RIG
	 * @throws ValidationException if the submitted data is not a valid RIG for this service.
	 * @throws DataAccessException if an error should occur while reading the data (either an I/O error or problem parsing the data)
	 */
	public RIG getRIG(InputStream is) throws ValidationException, DataAccessException;
}
