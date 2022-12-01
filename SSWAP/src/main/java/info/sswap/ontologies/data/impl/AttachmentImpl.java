/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.impl;

import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.ontologies.data.api.DataException;
import info.sswap.ontologies.data.api.ParserException;
import info.sswap.ontologies.data.api.SerializerException;
import info.sswap.ontologies.sswapmeet.SSWAPMeet.Data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Implementation of support for "attachments"; i.e., base64 encoding of literal data
 * 
 * @author Damian Gessler
 *
 */
public class AttachmentImpl extends LiteralData {
	
	/**
	 * Enables reading and writing of literal data as base64 "attachments".
	 * 
	 * @param sswapIndividual the individual holding access to the data
	 * @throws DataAccessException on any data access error
	 */
	public AttachmentImpl(SSWAPIndividual sswapIndividual) throws DataAccessException {
		super(sswapIndividual);
	}

	@Override
	public InputStream readData() throws IOException, DataException {

		// Establish if we need to filter the data through a parser
		setFilter(Base64Parser.XSDbase64Binary, Data.hasParser, Base64Parser.uri);
		
		// read the data
		InputStream inputStream = super.readData();
		
		// reading never changes (writes to) the object, so even if the object has a
		// base64 parser, and is successfully parsed base64, yet does not have datatype
		// XSD Base64Parser.XSDbase64Binary, it is not set to that datatype
		
		return inputStream;
		
	}

	@Override
	public void writeData(InputStream inputStream) throws IOException, DataException {

		// Establish if we need to filter the data through a serializer
		setFilter(Base64Serializer.XSDbase64Binary, Data.hasSerializer, Base64Serializer.uri);
		
		try {
			
			super.writeData(inputStream);
			
			// set the base64 datatype, as/if appropriate
			if ( hasValue(Data.hasSerializer,Base64Serializer.uri) ) {
				
				SSWAPPredicate literalDataPredicate = sswapDocument.getPredicate(Data.literalData);
				SSWAPProperty literalDataProperty = sswapIndividual.getProperty(literalDataPredicate);
				String value = literalDataProperty.getValue().asString();
				sswapIndividual.setProperty(literalDataPredicate, value, Base64Serializer.XSDbase64Binary);
				
			}
			
		} catch ( DataException e ) {
			throw e;
		}
		
	}

	/**
	 * Uses the default base64 parser if and only if there is a
	 * <code>data:hasParser</code> property with the default value
	 * (<code>Base64Parser.uri</code>).
	 * <p>
	 * For other parsers, override this method to return a parsed input stream
	 * suitable for reading.
	 * 
	 * @see Base64Parser#uri
	 */
	@Override
	public InputStream parse(InputStream inputStream) throws IOException, ParserException {

		if ( hasValue(Data.hasParser,Base64Parser.uri) ) {
			Base64Parser base64Parser = new Base64Parser();
			inputStream = base64Parser.parse(inputStream);
		}
		
		return inputStream;
	}
	
	/**
	 * Uses the default base64 serializer if and only if there is a
	 * <code>data:hasSerializer</code> property with the default value
	 * (<code>Base64Serializer.uri</code>).
	 * <p>
	 * For other serializers, override this method to return a serialized input stream
	 * suitable for reading by the internal writer.
	 * 
	 * @see Base64Serializer#uri
	 */
	@Override
	public InputStream serialize(InputStream inputStream) throws IOException, SerializerException {
		
		if ( hasValue(Data.hasSerializer,Base64Serializer.uri) ) {
			Base64Serializer base64Serializer = new Base64Serializer();
			inputStream = base64Serializer.serialize(inputStream);
		}
		
		return inputStream;
	}

	/**
	 * Set the parser or serializer depending on the datatype of the data or the
	 * ontology predicate value.
	 * 
	 * @param xsdbase64Binary
	 *            the URI of the XSD base64Binary type (e.g.,
	 *            Base64Parser.XSDbase64Binary or
	 *            Base64Serializer.XSDbase64Binary)
	 * @param hasFilterPredicate
	 *            the URI of the data ontology predicate for associating with a
	 *            filter (e.g., Ontology.hasParser or Ontology.hasSerializer)
	 * @param base64uri
	 *            the marker URN for base64 encoding or decoding (e.g.,
	 *            Base64Parser.uri or Base64Serializer.uri)
	 * 
	 * @see Base64Parser
	 * @see Base64Serializer
	 * 
	 */
	private void setFilter(URI xsdbase64Binary, URI hasFilterPredicate, URI base64uri) {
		
		URI datatypeURI;
		
		try {  // many ways to throw
			SSWAPPredicate literalDataPredicate = sswapDocument.getPredicate(Data.literalData);
			SSWAPProperty literalDataProperty = sswapIndividual.getProperty(literalDataPredicate);
			SSWAPElement literalDataElement = literalDataProperty.getValue();
			datatypeURI = literalDataElement.asLiteral().getDatatypeURI();
			if ( datatypeURI == null ) {
				throw new Exception();
			}
		} catch ( Exception e ) {	// throwing is an "exception", but in many cases not an error
			return;					// OK; just means that a filter is not set
		}
		
		if ( datatypeURI.equals(xsdbase64Binary) && ! hasValue(hasFilterPredicate,base64uri) ) {
			setValue(hasFilterPredicate,base64uri,true);
		}
	
	}
	
}
