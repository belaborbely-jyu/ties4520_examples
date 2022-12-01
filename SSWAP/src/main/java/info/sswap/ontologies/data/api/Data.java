/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.api;

import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;

/**
 * The <code>data</code> package standardizes a simple interface to arbitrary
 * data for semantic Web services.
 * <p>
 * The package works in concert with the data ontology at <a
 * href="http://sswapmeet.sswap.info/data"
 * >http://sswapmeet.sswap.info/data</a>. The ontology allows "the data itself"
 * to be either a literal or to reside at a URI, whereby the data is retrieved
 * by dereferencing the URI and reading the input stream. When data resides at a
 * URI, it is commonly either the URI of a <code>SSWAPSubject</code>, or the
 * object (property value) of the <code>data:hasData</code> predicate.
 * <p>
 * In the first case where the data is literal, the data is instantiated as the
 * value to a datatype property. The data ontology has a super-property called
 * <code>data:literalData</code> that should be used as the root property for
 * all data literal properties. Users may <code>rdfs:subPropertyOf</code>
 * <code>data:literalData</code> to make specific properties as they need. This
 * API (Application Programming Interface) relies on SSWAP's transaction-time
 * reasoning to map every such subproperty instance to an instance of
 * <code>data:literalData</code>. The API then operates on instances of
 * <code>data:literalData</code> to retrieve the data.
 * <p>
 * The second case is where the data is to be dereferenced from the individual
 * itself. To signify this, the resource should belong to the type
 * <code>data:DataFormat</code>, and in most cases, to a domain-specific
 * subClass (e.g., seq:FASTA) of this marker class. In the absence of a
 * <code>data:literalData</code> superproperty, the data is considered to be at
 * the URI.
 * <p>
 * In simple (and recommended) cases, the <code>SSWAPSubject</code> itself is
 * typed to some data format. Using this class on the subject returns the data.
 * 
 * @see info.sswap.ontologies.data.api.Directory
 * @see info.sswap.api.model.RDG
 * @see info.sswap.api.model.RIG
 * @see info.sswap.api.model.SSWAPIndividual
 * @see info.sswap.api.model.SSWAPSubject
 * 
 * @author Damian Gessler
 */
public interface Data {
	
	/**
	 * Read the current data element (either a literal or by dereferencing the individual itself).
	 * Caller should close the input stream when done reading.
	 * 
	 * @return InputStream a stream to read the data
	 * @throws IOException on any stream error
	 * @throws DataException on data error such as parsing, validating, etc.
	 */
	public InputStream readData() throws IOException, DataException;
	
	/**
	 * 
	 * @param inputStream the stream to read for writing the data to the current data element
	 * @throws IOException on any stream error
	 * @throws DataException on data error such as validating, serializing, etc.
	 */
	public void writeData(InputStream inputStream) throws IOException, DataException;
	
	/**
	 * Get explicit and inferred subTypes of data:DataFormat
	 * 
	 * @return a set of all data types for which this individual belongs.
	 */
	public Set<SSWAPType> getFormats();
	
	/**
	 * Return a set of URIs of declared accessors for the data. An accessor is a
	 * agent for access to the data; it may be a simple read and stream of the
	 * underlying data, or it may wrap user authentication and authorization.
	 * Specifics of how an accessor is invoked and how it accesses the data are
	 * local to the specific accessor and not specified here.
	 * <p>
	 * Accessors are determined by the values of the
	 * <code>data:hasAccessor</code> property.
	 * 
	 * @return a set of unique URIs of declared accessors
	 */
	public Set<URI> getAccessors();

	/**
	 * Return a set of URIs for declared parsers for the data. A parser should
	 * read the data (given some format) and return it as a more manageable
	 * object. Specifics of how a parser is invoked to read the data, how it
	 * parses the data, and what it returns are local to the specific parser and
	 * not specified here.
	 * <p>
	 * Parsers are determined by the values of the
	 * <code>data:hasParser</code> property.
	 * 
	 * @return a set of unique URIs of declared parsers
	 */
	public Set<URI> getParsers();
	
	/**
	 * Return a set of URIs for declared serializers for the data. A serializer
	 * should write the data (given some format). Specifics of how a serializer
	 * is invoked to write the data and how it formats the data are local to the
	 * specific serializer and not specified here.
	 * <p>
	 * Serializer are determined by the values of the
	 * <code>data:hasSerializer</code>
	 * property.
	 * 
	 * @return a set of URIs of declared serializers
	 */
	public Set<URI> getSerializers();
	
	/**
	 * Return a set of URIs for declared validators for the data. A validator
	 * should test the data for conformity to a set of standards. Validators may
	 * be used both in reading and writing the data. Specifics of how a
	 * validator is invoked, how it validates the data, and what it returns are
	 * local to the specific validator and not specified here.
	 * <p>
	 * Validators are determined by the values of the
	 * <code>data:hasValidator</code> property.
	 * 
	 * @return a set of unique URIs of declared validators
	 */
	public Set<URI> getValidators();
	
	/**
	 * @return the source SSWAPIndividual for this Data object
	 */
	public SSWAPIndividual getIndividual();
		
}
