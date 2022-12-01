/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;

import java.net.URI;

/**
 * @author Evren Sirin
 */
public interface PropertyInput extends Input {
	public static final int DEFAULT_MIN = 0;
	public static final int DEFAULT_MAX = Integer.MAX_VALUE;
	
	/**
	 * Returns <code>owl:Restriction</code>
	 */
	public URI getType();
	
	public URI getProperty();
	
	public Input getRange();
	public void setRange(Input range);
	
	public int getMinCardinality();
	public boolean hasMinCardinality();
	public void setMinCardinality(int minCardinality);
	
	public int getMaxCardinality();
	public boolean hasMaxCardinality();
	public void setMaxCardinality(int maxCardinality);
}
