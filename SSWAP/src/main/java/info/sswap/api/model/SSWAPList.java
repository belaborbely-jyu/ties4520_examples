/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.util.List;

/**
 * A list of SSWAP elements.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * @see SSWAPElement
 */
public interface SSWAPList extends SSWAPElement, List<SSWAPElement> {
}
