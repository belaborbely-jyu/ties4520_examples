/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.api;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface to web resources that parse a web data source.
 * 
 * @author Damian Gessler
 * 
 */
public interface Parser {
	
	/**
	 * Method acts as a filter: it is passed an input stream and should
	 * return a input stream of parsed data suitable for immediate reading.
	 * 
	 * @param inputStream raw (unparsed) stream to be read
	 * @return parsed stream ready for reading
	 * @throws IOException on any network error
	 * @throws ParserException on any Parser-specific error
	 */
	public InputStream parse(InputStream inputStream) throws IOException, ParserException;

}
