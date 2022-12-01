/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;

/**
 * Represent a value that can be provided as input. 
 * 
 * @see URIValue BNodeValue LiteralValue
 * 
 * @author Evren Sirin
 */
public interface InputValue {
	public void accept(InputVisitor visitor);
}
