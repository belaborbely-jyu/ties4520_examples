/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.impl;

import java.io.Serializable;

import info.sswap.api.input.BNodeValue;
import info.sswap.api.input.InputVisitor;

/**
 * @author Evren Sirin
 */
public class BNodeValueImpl implements BNodeValue, Serializable {
	private final String bnodeID;
	
	public BNodeValueImpl(String id) {
	    this.bnodeID = id;
    }

	/**
     * {@inheritDoc}
     */
    @Override
    public void accept(InputVisitor visitor) {
	    visitor.visit(this);
    }
    
	@Override
    public int hashCode() {
	    return 31 * bnodeID.hashCode();
    }

	@Override
    public boolean equals(Object obj) {
		if (!(obj instanceof BNodeValue))
			return false;
		
		BNodeValue that = (BNodeValue) obj;
		return this.bnodeID.equals(that.getID());
    }

	@Override
    public String getID() {
	    return bnodeID;
    }

	@Override
	public String toString() {
		return "_:" + bnodeID;
	}
}
