/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

/**
 * An enumeration containing possible mappings between subjects and objects in a SSWAPGraph
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 *
 */
public enum MappingPattern {
	PAIR,
	ONE_TO_MANY,
	MANY_TO_ONE,
	MANY_TO_MANY,
	NONE
}
