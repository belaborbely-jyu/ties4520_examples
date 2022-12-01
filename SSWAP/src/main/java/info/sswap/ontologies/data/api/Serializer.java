/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.api;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface to web resources that serialize data.
 * 
 * @author Damian Gessler
 * 
 */
public interface Serializer {

	/**
	 * Method acts as a filter: it is passed an input stream and should
	 * return a input stream of serialized data suitable for immediate reading.
	 * 
	 * @param inputStream raw (un-serialized) stream to be read
	 * @return serialized stream ready for reading (e.g., for consequent writing to an output stream)
	 * @throws IOException on any network error
	 * @throws SerializerException on any Serializer-specific error
	 */
	public InputStream serialize(InputStream inputStream) throws IOException, SerializerException;

}
