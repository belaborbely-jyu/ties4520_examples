/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import info.sswap.impl.empire.model.SourceModel;

import java.net.URI;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Resolves a source model/SSWAP protocol graph by extracting definitions for all externally defined types
 * 
 * @author Pavel Klinov
 *
 */
public interface ModelResolver {
	/**
	 * 
	 * @param model Model to be resolved
	 * @return The resolved model which includes the source model
	 */
	public Model resolveSourceModel(SourceModel model);
	
	/**
	 * Resolves a graph by performing a more specific resolution than for a generic source model 
	 * @param protocol Protocol graph to be resolved
	 * @return The resolved graph's model
	 */
	public Model resolveProtocolModel(SSWAPProtocol protocol);
	
	/**
	 * Resolves a specific term
	 * @param termURI Term URI to be resolved
	 * @return The term's definition
	 */
	public Model resolveTerm(URI termURI);
}
