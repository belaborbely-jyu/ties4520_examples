/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.impl;

import info.sswap.ontologies.data.api.Parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.codec.binary.Base64;

import com.google.common.io.ByteStreams;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Parses an input stream through a base64 decoder.
 * 
 * @see Base64Serializer
 *  
 * @author Damian Gessler
 * 
 */
public class Base64Parser implements Parser {

	/**
	 * A marker URN for designating XSD base64 decoding. This can be used as the
	 * object of the data:hasParser property to designate internally supported
	 * base64 decoding.
	 */
	public final static URI uri = URI.create("urn:sswap:parser:xsd:base64Binary");
	
	/**
	 * The URI of the XSD base64Binary resource
	 */
	public final static URI XSDbase64Binary = URI.create(XSD.base64Binary.toString());

	/**
	 * @inheritDoc
	 */
	@Override
	public InputStream parse(InputStream inputStream) throws IOException {
		return new ByteArrayInputStream(Base64.decodeBase64(ByteStreams.toByteArray(inputStream)));
	}

}
