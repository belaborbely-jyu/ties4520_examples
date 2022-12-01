/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.io;

import java.io.IOException;

/**
 * An exception generated when a byte limit is exceeded during a stream operation.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class ByteLimitExceededException extends IOException {

	/**
     * Serial version identifier for ByteLimitExceededException
     */
    private static final long serialVersionUID = -5541799997721676884L;
    
    /**
     * The default constructor
     */
    public ByteLimitExceededException() {    	
    }
    
    /**
     * The constructor that accepts a message
     * 
     * @param message the message describing the problem
     */
    public ByteLimitExceededException(String message) {
    	super(message);
    }

}
