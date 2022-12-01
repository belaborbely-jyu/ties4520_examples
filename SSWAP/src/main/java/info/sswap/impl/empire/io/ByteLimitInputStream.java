/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * A wrapper input stream that will not allow to transfer more than the specified amount of bytes 
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 *
 */
public class ByteLimitInputStream extends InputStream {
	/**
	 * The wrapped input stream.
	 */
	private InputStream in;
	
	/**
	 * The byte limit
	 */
	private long maxBytes;
	
	/**
	 * The bytes already read
	 */
	private long bytesRead;
	
	/**
	 * Creates a new stream wrapping the underlying stream, with the specified byte limit
	 * 
	 * @param in the wrapped input stream
	 * @param maxBytes the byte limit
	 */
	public ByteLimitInputStream(InputStream in, long maxBytes) {
		this.in = in;
		this.maxBytes = maxBytes;
		this.bytesRead = 0;
	}
	
	/**
	 * Gets the number of bytes already read from the underlying stream.
	 * 
	 * @return the number of bytes already read
	 */
	public long getBytesRead() {
		return bytesRead;
	}
	
	/**
	 * Gets the number of bytes remaining before the limit is reached. 
	 * 
	 * @return the number of bytes remaining
	 */
	public long getBytesRemaining() {
		return maxBytes - bytesRead;
	}
	
	/**
	 * Gets the byte limit for this stream
	 * 
	 * @return the byte limit
	 */
	public long getMaxBytes() {
		return maxBytes;
	}
	
	/**
	 * Reads a single byte from the underlying stream.
	 * 
	 * @return the byte read, or -1 if the end of the stream has been reached
	 * @throws ByteLimitExceededException if reading the byte would cause
	 * to go over the byte limit
	 * @throws IOException if the underlying stream reports I/O problem
	 */
	@Override
	public int read() throws IOException {
		if (bytesRead >= maxBytes) {
			throw new ByteLimitExceededException("Byte limit exceeded");
		}
		
		int readByte = in.read();
		
		if (readByte != -1) {
			bytesRead++;
		}
		
		return readByte;
	}
	
	/**
	 * Reads multiple bytes from the underlying stream
	 * 
	 * @param b the byte array where the results will be placed
	 * @param off the offset where the first byte read should be placed in the array
	 * @param len the requested amount of bytes to read
	 * @return the amount of bytes actually read (it may be shorter for multiple reasons; these reasons include
	 * the premature end of stream (or other reason reported by the underlying stream), or reaching the byte limit)
	 * @throws ByteLimitExceededException if the limit has already been reached when invoking this method (if
	 * the limit is reached in the middle of the read, the read will be successfully completed, but will be truncated).
	 * @throws IOException if the underlying stream reports an I/O problem
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException { 
		if (bytesRead >= maxBytes) {
			throw new ByteLimitExceededException("Byte limit exceeded");
		}
		
		long bytesRemaining = getBytesRemaining();
		
		if (len >= bytesRemaining) {
			len = (int) bytesRemaining;
		}
		
		int bytesRead = in.read(b, off, len);
		this.bytesRead += bytesRead;
		
		return bytesRead;
	}
	
	/**
	 * Closes the stream.
	 * @throws IOException if the underlying stream reports a problem while closing a stream.
	 */
	@Override
	public void close() throws IOException {
		in.close();
	}
}
