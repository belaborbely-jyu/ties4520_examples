/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.util.Map.Entry;

/**
 * A convenience type for a pair of two elements
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * 
 * @param <K>
 *            the type of the first element in the pair
 * @param <V>
 *            the type of the second element in the pair
 */
public interface Pair<K, V> extends Entry<K, V> {
}
