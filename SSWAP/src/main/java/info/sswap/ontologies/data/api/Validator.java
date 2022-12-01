/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.api;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface to web resources that validate data.
 * 
 * @author Damian Gessler
 * 
 */
public interface Validator {

	/**
	 * Method acts as a filter: it is passed an input stream and should return a
	 * input stream of validated data suitable for immediate reading.
	 * <p>
	 * Method may be called on input data after it is parsed, or output data
	 * before it is serialized.
	 * 
	 * @param inputStream
	 *            raw (pre-validated) stream to be read
	 * @return validated stream ready for reading
	 * @throws IOException
	 *             on any network error
	 * @throws ValidatorException
	 *             on any Validator-specific error
	 */
	public InputStream validate(InputStream inputStream) throws IOException, ValidatorException;
	
}
