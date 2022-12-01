/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.impl;

import info.sswap.ontologies.data.api.Serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.codec.binary.Base64;

import com.hp.hpl.jena.vocabulary.XSD;
import com.google.common.io.ByteStreams;

/**
 * Serializes an input stream through a base64 encoder.
 * 
 * @see Base64Parser
 * 
 * @author Damian Gessler
 * 
 */
public class Base64Serializer implements Serializer {

	/**
	 * A marker URN for designating XSD base64 encoding. This can be used as the
	 * object of the data:hasSerializer property to designate internally supported
	 * base64 encoding.
	 */
	public final static URI uri = URI.create("urn:sswap:serializer:xsd:base64Binary");
	
	/**
	 * The URI of the XSD base64Binary resource
	 */
	public final static URI XSDbase64Binary = URI.create(XSD.base64Binary.toString());
	
	/**
	 * @inheritDoc
	 */
	@Override
	public InputStream serialize(InputStream inputStream) throws IOException {
		return new ByteArrayInputStream(Base64.encodeBase64(ByteStreams.toByteArray(inputStream)));
	}

}
