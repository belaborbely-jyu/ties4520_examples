/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;

import java.net.URI;

import org.mindswap.pellet.utils.Namespaces;

/**
 * Constants used for <code>Input</code> objects.
 * 
 * @author Evren Sirin
 */
public class Vocabulary {

	public static final URI OWL_INTERSECTION = URI.create(Namespaces.OWL + "intersectionOf");
	public static final URI OWL_UNION = URI.create(Namespaces.OWL + "unionOf");
	public static final URI OWL_ENUMERATION = URI.create(Namespaces.OWL + "oneOf");
	public static final URI OWL_RESTRICTION = URI.create(Namespaces.OWL + "Restriction");
	public static final URI OWL_THING = URI.create(Namespaces.OWL + "Thing");
	public static final URI RDFS_LITERAL = URI.create(Namespaces.RDFS + "Literal");

}
