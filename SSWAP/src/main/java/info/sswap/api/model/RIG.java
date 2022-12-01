/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import info.sswap.api.http.HTTPProvider;
import info.sswap.api.spi.ExtensionAPI;

import java.io.InputStream;
import java.util.Collection;

/**
 * A Resource Invocation Graph (<code>RIG</code>) is for invocation of a SSWAP
 * semantic web service. An <code>RIG</code> is sent by a client to a SSWAP
 * Resource to invoke the service as described by its Resource Description Graph
 * (<code>RDG</code>). Clients (agents invoking a service) make an
 * <code>RIG</code> by augmenting the <code>RDG</code> of a service with the
 * client's actual invocation values--<i>i.e.</i>, the "data"--by adding or
 * editing the <code>SSWAPSubject</code>s. Providers (agents delivering a
 * service) accept <code>RIG</code>s and change them (according to the contract
 * of their <code>RDG</code> into a Resource Response Graph (<code>RRG</code>).
 * 
 * <p>
 * For more on the protocol, see <a href="http://sswap.info/protocol">SSWAP
 * Protocol</a>.
 * 
 * @see PDG
 * @see RDG
 * @see RRG
 * @see RQG
 * @see SSWAPResource
 * @see SSWAPSubject
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 * 
 */
public interface RIG extends SSWAPProtocol {
	/**
	 * Translates a SSWAPNode in this RIG to the vocabulary used by the RDG (the translation only makes sense
	 * for SSWAPResources and SSWAPSubjects; for all others the nodes are returned unmodified). 
	 * The translated node contains only properties that are described in 
	 * the RDG (this includes using the name of a superproperty, if the RIG used a subproperty with respect to the RDG), 
	 * and types defined in the RDG.
	 * 
	 * @param node the node that must belong to this RIG
	 * @return a translated node, or null if the node does not belong to this RIG
	 */
	public <T extends SSWAPNode> T translate(T node);
		
	/**
	 * Gets the RRG that can be used to create a response to the RIG. This
	 * method need not be called by clients (creators of the RIG), but is called
	 * by providers after they have mutated an incoming RIG so as to create a
	 * response. This method enforces that the RIG--as a putative RRG--is
	 * validated against the contract of the RDG. Additionally, if the RIG is
	 * OWL DL, the RRG must also be OWL DL.
	 * 
	 * @return the RRG for the RIG.
	 * @throws DataAccessException
	 *             if an error should occur while accessing the data in the RIG
	 *             to create the RRG
	 * @throws ValidationException
	 *             if the RRG is not valid with respect to the RIG invoking the
	 *             service
	 */
	public RRG getRRG() throws DataAccessException, ValidationException;
	
	/**
	 * Creates an RRG based on the serialized RRG data (e.g., sent by a provider of the service in response to
	 * this RIG having been sent to the provider).
	 * 
	 * @param is the input stream from which the serialized RRG data can be read
	 * @return the created RRG
	 * @throws DataAccessException if an error should occur while reading the data (either an I/O error or problem parsing the data)
	 * @throws ValidationException if the RRG is not valid with respect to the RIG for invoking service
	 */
	public RRG getRRG(InputStream is) throws DataAccessException, ValidationException;
	
	/**
	 * Invokes the service with this RIG, blocks until the service responds, and reads back the RRG. 
	 * 
	 * @return the returned RRG, if available, and response information from the invocation call
	 */
	public HTTPProvider.RRGResponse invoke();
	
	/**
	 * Invokes the service with this RIG, blocks until the service responds, and reads back the RRG
	 * 
	 * @param timeout connect and read timeout in milliseconds; if communication with the service should stall for more than the specified time
	 * the call will be interrupted and the returned RRGResponse will contain the error code.
	 * 
	 * @return the returned RRG, if available, and response information from the invocation call
	 */
	public HTTPProvider.RRGResponse invoke(long timeout);


	/**
	 * Returns a read-only copy of the <code>SSWAPResource</code> with
	 * properties and types of this <code>RIG</code> translated into the
	 * vocabulary of the <code>RDG</code>.
	 * 
	 * The translated resource contains only properties that are described in
	 * the <code>RDG</code> (this includes using the name of a super-property,
	 * if the <code>RIG</code> used a subproperty with respect to the
	 * <code>RDG</code>), and types defined in the <code>RDG</code>.
	 * <p>
	 * Given the translated copy returned by the method, use {@link ExtensionAPI#getUntranslatedNode(SSWAPNode)}
	 * to retrieve this source individual.
	 * 
	 * @return a translated resource
	 * @see ExtensionAPI#getUntranslatedNode(SSWAPNode)
	 */
	public SSWAPResource getTranslatedResource();
	

	/**
	 * Returns a read-only copy of the <code>SSWAPSubject</code> from all
	 * matching <code>SSWAPGraphs</code> with properties and types of this
	 * <code>RIG</code> translated into the vocabulary of the <code>RDG</code>.
	 * 
	 * The translated subjects contain only properties that are described in the
	 * <code>RDG</code> (this includes using the name of a super-property, if
	 * the <code>RIG</code> used a subproperty with respect to the
	 * <code>RDG</code> ), and types defined in the <code>RDG</code>.
	 * <p>
	 * The special case of {@link SSWAPSubject#addObject(SSWAPObject)} and its
	 * <code>set()</code> variants are not read-only and changes the underlying
	 * <code>SSWAPSubject</code>.
	 * <p>
	 * Given the translated copy returned by the method, use {@link ExtensionAPI#getUntranslatedNode(SSWAPNode)}
	 * to retrieve this source individual.
	 * 
	 * @return a collection of translated subjects
	 * @see ExtensionAPI#getUntranslatedNode(SSWAPNode)
	 */
	public Collection<SSWAPSubject> getTranslatedSubjects();
}
