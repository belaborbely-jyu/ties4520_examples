/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * A Resource Query Graph (<code>RQG</code>) is a specialized query sent to a
 * Discovery Server in request of all SSWAP semantic web services
 * (<code>RDG</code>s) that satisfy the query. The query is interpreted as:
 * "Get me all <code>RDG</code>s such that:"
 * <ul>
 * <li>the <code>RQG</code> <code>SSWAPResource</code> is a sub-type of the
 * <code>RDG</code> <code>SSWAPResource</code>, and
 * <li>the <code>RQG</code> <code>SSWAPSubject</code> is a super-type of the
 * <code>RDG</code> <code>SSWAPResource</code>, and
 * <li>the <code>RQG</code> <code>SSWAPObject</code> is a sub-type of the
 * <code>RDG</code> <code>SSWAPResource</code>.
 * </ul>
 * Every type (OWL Class) is a sub-type and super-type (subclass and superclass,
 * respectively) of itself, so the above includes type equivalency.
 * <p>
 * Unlike other SSWAP Protocol graphs which have a <code>SSWAPResorce</code> set
 * as a dereferencable URI of the resource on the web, an RQG may have a
 * <code>SSWAPResource</code> of:
 * <ul>
 * <li>nothing (a blank node), meaning "Find me all resources", or
 * <li>regex expression, meaning,
 * "Find me all resources with URIs that match the regex"
 * </ul>
 * regex URIs must be of the form <i>urn:sswap:regex:</i>{@code <regex>} where
 * {@code <regex>} is a SPARQL regex expression (see <a
 * href="http://www.w3.org/TR/xpath-functions/#regex-syntax">XQuery 1.0 and
 * XPath 2.0 Functions and Operators</a>).
 * <p>
 * For more on the protocol, see <a href="http://sswap.info/protocol">SSWAP
 * Protocol</a>.
 * 
 * @see PDG
 * @see RDG
 * @see RIG
 * @see RRG
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 * 
 */
public interface RQG extends SSWAPProtocol {
	/**
	 * Gets a SPARQL query that can be executed over a model containing RDGs to determine
	 * which RDGs match this RQG. The model containing RDGs should contain inferred facts for
	 * these models (i.e., it should be either a model that contains previously computed inferences,
	 * or be backed by a reasoner to compute them dynamically; e.g., an OntModel). Additionally,
	 * the execution environment for the query should take into account the inferred facts for this RQG.
	 * 
	 * @return the SPARQL query.
	 */
	public String getQuery();
	
	/**
	 * Creates and executes a SPARQL query that determines which RDGs match this RQG. The query
	 * is executed over (potentially) multiple models that contain closures and inferred statements about
	 * the RDGs. The implementation of this method will also take into account the closure and inferred statements
	 * about this RQG when executiong the query.
	 * 
	 * @param models models containing RDGs, their closures and inferred statements
	 * @return a collection of URIs of matching RDGs
	 */
	public Collection<String> executeQuery(Model... models);
	
	/**
	 * Sends this RQG as a query to the specified discovery server instance.
	 * 
	 * @param discoveryServerURI the URI of the Discovery Server query end-point (may be null; in such a case, the default DS will be used)
	 * @return the collection of RDGs matching the query
	 * @throws IOException if an I/O error should occur
	 * @throws DataAccessException if there is a problem retrieving/reading RDGs for the services returned by the discovery server
	 */
	public Collection<RDG> invoke(URI discoveryServerURI) throws IOException, DataAccessException;
	
	/**
	 * Sends this RQG as a query to the default Discovery Server
	 * 
	 * @return the collection of RDGs matching the query
	 * @throws IOException if an I/O error should occur
	 * @throws DataAccessException if there is a problem retrieving/reading RDGs for the services returned by the discovery server
	 */
	public Collection<RDG> invoke() throws IOException, DataAccessException;
	
	/**
	 * Checks whether this RQG matches the given RDG
	 * 
	 * @param rdg the RDG
	 * @return true if this RQG matches the given RDG, false otherwise
	 */
	public boolean satisfiesResource(RDG rdg);
}
