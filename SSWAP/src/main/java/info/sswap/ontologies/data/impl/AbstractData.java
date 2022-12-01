/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.impl;

import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPType;
import info.sswap.ontologies.data.api.Data;
import info.sswap.ontologies.data.api.DataException;
import info.sswap.ontologies.data.api.Parser;
import info.sswap.ontologies.data.api.ParserException;
import info.sswap.ontologies.data.api.Serializer;
import info.sswap.ontologies.data.api.SerializerException;
import info.sswap.ontologies.data.api.Validator;
import info.sswap.ontologies.data.api.ValidatorException;
import info.sswap.ontologies.sswapmeet.SSWAPMeet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.io.ByteStreams;

/**
 * emphasize point that in cases:
 * 1. one literal data prop
 * 2. no literal data prop but ind URL
 * that all is automatic: just call readData and writeData
 * @author ddg
 *
 */
public abstract class AbstractData implements Data, Parser, Serializer, Validator {
	
	/*
	 * Allow protected access to these variables because they are final
	 */
	protected final SSWAPType DataFormatType;
	
	protected final SSWAPDocument sswapDocument;

	protected final SSWAPIndividual sswapIndividual;
	
	// private final static URI irodsAccessor = URI.create("urn:sswap:accessor:irods");
	

	/**
	 * Constructor to access a literalData property or the individual itself as the data object.
	 * Calls <code>setDataElement<code> as part of the initialization.
	 * 
	 * @param sswapIndividual individual to be the subject of data types and properties
	 */
	public AbstractData(SSWAPIndividual sswapIndividual) throws DataException {
	
		this.sswapIndividual = sswapIndividual;
		sswapDocument = sswapIndividual.getDocument();
		DataFormatType = sswapDocument.getType(SSWAPMeet.Data.DataFormat);

	}
	
	/**
	 * Default implementation just passes inputStream untouched. Custom
	 * implementations may extend this class and override this method. Note that
	 * parsing is filter: accepting an input stream to read and returning an
	 * input stream for the next downstream step to read.
	 */
	@Override
	public InputStream parse(InputStream inputStream) throws IOException, ParserException {
		return inputStream;
	}
	
	
	/**
	 * Default implementation just passes inputStream untouched. Custom
	 * implementations may extend this class and override this method. Note that
	 * serializing is filter: accepting an input stream to read and returning an
	 * input stream for the next downstream step to read.
	 */
	@Override
	public InputStream serialize(InputStream inputStream) throws IOException, SerializerException {
		return inputStream;
	}

	
	/**
	 * Default implementation just passes inputStream untouched. Custom
	 * implementations may extend this class and override this method. Note that
	 * validating is filter: accepting an input stream to read and returning an
	 * input stream for the next downstream step to read.
	 */
	@Override
	public InputStream validate(InputStream inputStream) throws IOException, ValidatorException {
		return inputStream;
	}
	
	
	/**
	 * Returns true if any instance of the predicate has the given value; false otherwise.
	 * 
	 * @param ontologyPredicateURI 
	 * @param value base64Parser or base64Serializer
	 * @return parserOrSerializer if set; null otherwise
	 */
	protected boolean hasValue(URI ontologyPredicateURI, URI value) {
		
		SSWAPPredicate sswapPredicate = sswapDocument.getPredicate(ontologyPredicateURI);
		
		for ( SSWAPProperty property : sswapIndividual.getProperties(sswapPredicate) ) {
			
			try {
				URI uri = property.getValue().asIndividual().getURI();
				if ( uri.equals(value) ) {
					return true;
				}
			} catch ( Exception e ) {
				; // should not happen for supported properties, but may happen if misused.
			}
		}
		
		return false;
		
	}
	
	/**
	 * Sets th
	 * 
	 * @param ontologyPredicateURI 
	 * @param value base64Parser or base64Serializer
	 */
	protected void setValue(URI ontologyPredicateURI, URI value, boolean on) {
		
		SSWAPPredicate sswapPredicate = sswapDocument.getPredicate(ontologyPredicateURI);
		Set<SSWAPProperty> instancesToRemove = new HashSet<SSWAPProperty>();
		
		// find all instances of the property with the value
		for ( SSWAPProperty sswapProperty : sswapIndividual.getProperties(sswapPredicate) ) {
			try {
				
				URI uri = sswapProperty.getValue().asIndividual().getURI();
				if ( uri.equals(value) ) {
					instancesToRemove.add(sswapProperty);
				}
				
			} catch ( Exception e ) {
				; // consume
			}
			
		}
		
		// if ! on, remove all;
		// if on and only one already exists, leave as is
		// if on and more than one exists, remove all but one
		// if on and none exist, add one
		
		Iterator<SSWAPProperty> itr = instancesToRemove.iterator();
		if ( on && itr.hasNext() )
			itr.next();
		
		for ( ; itr.hasNext(); ) {
			sswapIndividual.removeProperty(itr.next());
		}
		
		if ( on && instancesToRemove.size() == 0 ) {
			
			SSWAPIndividual newIndividual = sswapDocument.createIndividual(value);
			sswapIndividual.addProperty(sswapPredicate,newIndividual);
		}
		
	}

	/**
	 * Convenience method to convert an input stream into a string.
	 * 
	 * @param inputStream stream to read
	 * @return String string representation of the stream, or null on error
	 */
	public String inputStreamToString(InputStream inputStream) {
		try {
			return new String(ByteStreams.toByteArray(inputStream));
		} catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * Convenience method to convert a string into an input stream.
	 * Implementation is an in-memory input stream which does not
	 * need to be closed when done.
	 * 
	 * @param string string to read
	 * @return input stream ready for reading
	 */
	public InputStream stringToInputStream(String string) {
		return new ByteArrayInputStream(string != null ? string.getBytes() : new byte[0]);
	}
	
	/**
	 * @inheritDoc
	 */
	public Set<SSWAPType> getFormats() {
		
		Collection<SSWAPType> typesCollection = sswapIndividual.getTypes();
		Set<SSWAPType> dataTypesSet = new HashSet<SSWAPType>();
		
		for ( SSWAPType sswapType : typesCollection ) {
			if ( sswapType.isSubTypeOf(DataFormatType) ) {
				dataTypesSet.add(sswapType);
			}
		}
		
		return dataTypesSet;
	}

	/**
	 * @inheritDoc
	 * @return set of URIs that are the objects of the data:hasAccessor property
	 */
	public Set<URI> getAccessors() {
		return getObjectURIs(SSWAPMeet.Data.hasAccessor);
	}
	
	/**
	 * @inheritDoc
	 * @return set of URIs that are the objects of the data:hasParser property
	 */
	public Set<URI> getParsers() {
		return getObjectURIs(SSWAPMeet.Data.hasParser);
	}
	
	/**
	 * @inheritDoc
	 * @return set of URIs that are the objects of the data:hasSerializer property
	 */
	public Set<URI> getSerializers() {
		return getObjectURIs(SSWAPMeet.Data.hasSerializer);
	}
	
	/**
	 * @inheritDoc
	 * @return set of URIs that are the objects of the data:hasValidator property
	 */
	public Set<URI> getValidators() {
		return getObjectURIs(SSWAPMeet.Data.hasValidator);
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public SSWAPIndividual getIndividual() {
		return sswapIndividual;
	}
	
	/**
	 * Returns the URIs that correspond to the objects of an object property.
	 * 
	 * @param objectPredicateURI
	 *            URI of the property for which to return its object
	 * @return the set of URIs (RDF resources, not xsd:anyURI) that are the
	 *         objects of the argument property
	 */
	private Set<URI> getObjectURIs(URI objectPredicateURI) {
		
		SSWAPPredicate sswapPredicate = sswapDocument.getPredicate(objectPredicateURI);
		Collection<SSWAPProperty> hasProperties = sswapIndividual.getProperties(sswapPredicate);
		Set<URI> set = new HashSet<URI>();
		
		for ( SSWAPProperty hasProperty : hasProperties ) {
			
			SSWAPElement sswapElement = hasProperty.getValue();
			try {
				set.add(sswapElement.asIndividual().getURI());
			} catch ( Exception e ) {
				; // skip and move on
			}
		}
		
		return set;
	}
	
}
