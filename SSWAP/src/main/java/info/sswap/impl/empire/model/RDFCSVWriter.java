/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.impl.empire.Namespaces;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFErrorHandler;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Write the model to the output stream in Comma Separated Value (CSV)
 * according to the fundamental RDF truism:
 * 
 * 	subject, predicate, object -> row, col, value
 * 
 * The output is exactly one row per unique subject, and one column per
 * property instance.
 * 
 * URIs are URLencoded to escape commas;
 * String datatype values are quoted
 * 
 */

/*
 * This class is deprecated in favor of RDFTSVWriter
 */
@Deprecated
public class RDFCSVWriter implements RDFWriter {
	
	/**
	 * Default field delimiter
	 */
	protected final String DELIMITER_STR = ",";	// Comma Separated Values
	private String DELIMITER_ENC;
	
	/**
	 * Default charset for URLEncoding
	 */
	protected final String CHARSET = "UTF-8";
	
	private RDFErrorHandler rdfErrorHandler;
	
	public RDFCSVWriter() {
		
		rdfErrorHandler = new RDFDefaultErrorHandler();
		
		try {
			DELIMITER_ENC = URLEncoder.encode(DELIMITER_STR,CHARSET);
		} catch ( UnsupportedEncodingException e ) {
			DELIMITER_ENC = "%2C";	// ","
		}
	}
	
	@Override
	public RDFErrorHandler setErrorHandler(RDFErrorHandler rdfErrorHandler) {
		
		RDFErrorHandler oldErrorHandler = this.rdfErrorHandler;
		this.rdfErrorHandler = rdfErrorHandler;
		
		return oldErrorHandler;
	}

	/**
	 * No properties are currently supported.
	 */
	@Override
	public Object setProperty(String arg0, Object arg1) {
		return null;
	}

	@Override
	public void write(Model model, Writer writer, String base) {
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		write(model,byteArrayOutputStream,base);
		
		// can rely only on writer.write(char[] cbuf, int off, int len) being defined,
		// so we'll just hand-off to CharArrayWriter
		CharArrayWriter charArrayWriter = new CharArrayWriter();
		
		try {
			String contents = byteArrayOutputStream.toString();
			charArrayWriter.write(contents);
			charArrayWriter.writeTo(writer);
		} catch ( Exception e ) {
			rdfErrorHandler.error(e);
		}
		
	}

	@Override
	public void write(Model model, OutputStream outputStream, String base) {
		
		/**
		 * Mapping RDF (subject, predicate, object) => (row, col, value)
		 * will result in a (sparse) (n+1) x (m+1) matrix, where n is the
		 * number of (unique) subjects (rows), and m is the sum over
		 * all predicates of the maximum number of values for each
		 * predicate (over all subjects). "+1" row for the header row;
		 * "+1" for column for the first subject column
		 * 
		 * Example:
		 * 
		 * RDF triples:
		 * 	urn:subject1 urn:predicate1 value1
		 * 	urn:subject1 urn:predicate1 value2
		 * 	urn:subject1 urn:predicate2 value1
		 * 
		 * 	urn:subject2 urn:predicate1 value3
		 * 	urn:subject2 urn:predicate3 value1
		 * 
		 * Summary:
		 * 	number of subjects = 2
		 * 	sum of max property instances = 2 + 1 + 1 = 4
		 * 
		 * CSV (2+1 x 4+1) = (3,5):
		 * 	,urn:predicate1,urn:predicate1,urn:predicate2,urn:predicate3
		 * 	urn:subject1,value1,value2,value1,
		 * 	urn:subject2,value3,,,value1
		 * 
		 */
		
		final byte[] DELIMITER_BYTES = DELIMITER_STR.getBytes();
		String NEWLINE_STR = System.getProperty("line.separator");
		final byte[] NEWLINE_BYTES = NEWLINE_STR != null ? NEWLINE_STR.getBytes() : (NEWLINE_STR = "\n").getBytes();

		BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
		
		try {
			
			DataStructure ds = makeDataStructure(model);

			// print header row
			for ( String predicate : ds.maxPropertyInstances.keySet() ) {			
				for ( int numCols = 0; numCols < ds.maxPropertyInstances.get(predicate).intValue(); numCols++ ) {				
					bufferedOutputStream.write((DELIMITER_STR + predicate).getBytes());
				}
			}
			
			bufferedOutputStream.write(NEWLINE_BYTES);
			// bufferedOutputStream.flush();	// leave it to OS to decide when to flush

			// for every (unique) RDF subject, print a row ...
			for ( String subjectStr : ds.sortedRows.keySet() ) {

				SortedPredicates sortedPredicates = ds.sortedRows.get(subjectStr);

				// "column 1"; the subject
				bufferedOutputStream.write(subjectStr.getBytes());
				
				// for each predicate in sorted order ...
				for ( String predicateStr : ds.maxPropertyInstances.keySet() ) {
					
					int numBlankCells = 0;
					Values values = sortedPredicates.get(predicateStr);
					
					if ( values != null) {
						
						for ( String value : values ) {
							bufferedOutputStream.write((DELIMITER_STR + value).getBytes());
						}
						
						numBlankCells = values.size();
						
					}

					// fill remainder w/ blank place holders
					for ( int i = numBlankCells; i < ds.maxPropertyInstances.get(predicateStr); i++ ) {
						bufferedOutputStream.write(DELIMITER_BYTES);
					}
					
				}
				
				bufferedOutputStream.write(NEWLINE_BYTES);
				// bufferedOutputStream.flush();	// leave it to OS to decide when to flush

			}
			

		} catch ( Exception e ) {
			rdfErrorHandler.error(e);
		} finally {
			
			try {
				bufferedOutputStream.close();
			} catch ( IOException ioe ) {
				;
			}
		}
		
	}
	
	
	/**
	 * The data structure for a RDF model is:
	 * 
	 * a map of "rows", where the key to each row is a (unique) RDF subject;
	 * each subject points to a map of (unique) predicates;
	 * each predicate points to a list of (possibly non-unique) values
	 * 
	 * @param model the source RDF model to transform
	 * @return a data structure of rows and properties 
	 */
	protected DataStructure makeDataStructure(Model model) {
		
		// Data Structure to hold subject,predicate,object mappings
		DataStructure ds = new DataStructure();
		
		// map to hold name mappings (e.g., re-writing bnode IDs, qnames, etc.)
		NameMapper nameMapper = new NameMapper(model);

		// for every (unique) RDF subject ...
		for ( ResIterator rItr = model.listSubjects(); rItr.hasNext(); ) {
			
			// use string representations for (lexically unique) keys
			Resource resource = rItr.nextResource();
			String subjectStr = nameMapper.asString(resource);	// handles blank nodes
			
			// get the predicates for this subject
			SortedPredicates sortedPredicates = ds.sortedRows.get(subjectStr);
			if ( sortedPredicates == null ) {
				sortedPredicates = new SortedPredicates();
			}

			// for every property instance of this subject
			for ( StmtIterator sItr = resource.listProperties(); sItr.hasNext(); ) {
				
				// get the predicate and object for the subject
				Statement stmt = sItr.nextStatement();
				Property property = stmt.getPredicate();
				RDFNode objectNode = stmt.getObject();
				
				// use string representations for (lexically unique) keys
				String predicateStr = nameMapper.asString(property);

				// get or make an array of values for the predicate
				Values values = sortedPredicates.get(predicateStr);
				if ( values == null ) {
					values = new Values();
				}
				
				// add the lexical value to the list
				String objectStr;
				if ( objectNode.isLiteral() ) {
					
					Literal literal = objectNode.asLiteral();
					objectStr = literal.getLexicalForm();	// string representation of the value w/o the datatype info

					// add double quotes around "plain" literals, strings, and anyURIs
					String datatypeURI = literal.getDatatypeURI();
					if ( datatypeURI == null || datatypeURI.equals(XSD.xstring.getURI()) || datatypeURI.equals(XSD.anyURI.getURI()) ) {
						
						// double quote escaping according to: http://tools.ietf.org/html/rfc4180#section-2
						// (double quotes are escaped by preceding with a double quote, on fields surrounded by double quotes)
						objectStr = objectStr.replace("\"","\"\"");
						objectStr = "\"" + objectStr + "\"";	// surround w/ double quotes
					}
				} else {	// Resource (URI or bnode)
					objectStr = nameMapper.asString(objectNode);
				}
				
				// add the value and update the data structures
				values.add(objectStr);
				sortedPredicates.put(predicateStr, values);
				ds.sortedRows.put(subjectStr, sortedPredicates);
				
				// update the list of maximum values for this predicate
				int numValues = values.size();
				Integer currentMax = ds.maxPropertyInstances.get(predicateStr);
				if ( currentMax == null || currentMax.intValue() < numValues ) {
					ds.maxPropertyInstances.put(predicateStr, new Integer(numValues));
				}
				
			}
		}
		
		return ds;
		
	}
	
	/**
	 * Map names via re-writing rules:
	 * 
	 * 	* blank nodes get mapped to simpler names; e.g., _:b1, _:b2, etc.
	 * 	* terms belonging to reserved namespaces get qnames; e.g., owl:Class
	 *
	 */
	protected class NameMapper {
		
		PrefixMapping prefixMapping;

		// Map to rename blank nodes
		HashMap<RDFNode,String> bnodeMap;
		
		int bnodeCounter = 0;
		final String bnodePrefix = "_:b";

		NameMapper(PrefixMapping prefixMapping) {
			
			this.prefixMapping = prefixMapping;
			bnodeMap = new HashMap<RDFNode,String>();
		}
		
		/**
		 * Return a string representation of the RDFNode according
		 * to internal re-write rules.
		 * 
		 * @param rdfNode node to extract string representation
		 * @return string representation
		 */
		String asString(RDFNode rdfNode) {
			
			String name;
			
			if ( rdfNode.isAnon() ) {
				
				// map blank nodes from their internal representation to a simple string naming system
				name = bnodeMap.get(rdfNode);
				if ( name == null ) {
					name = bnodePrefix + ++bnodeCounter;
					bnodeMap.put(rdfNode,name);
				}
				
			} else if ( rdfNode.isURIResource() ) {
				name = asReservedQName(rdfNode.asResource());
			} else {	// Literal
				name = rdfNode.toString();
			}
			
			return name;
			
		}
		
		/**
		 * If the resource belongs to a reserved namespace, return it's QName;
		 * otherwise return its toString() representation.
		 * 
		 * @param resource
		 * @return string representation
		 */
		private String asReservedQName(Resource resource) {
			
			String resourceStr = resource.toString();	// safe for blank nodes
			String name = null;
			String[] reservedNamespaces = { Namespaces.RDF_NS, Namespaces.RDFS_NS, Namespaces.XSD_NS, Namespaces.OWL_NS, Namespaces.SSWAP_NS };
			
			for ( String namespace : reservedNamespaces ) {
				if ( resourceStr.startsWith(namespace) ) {
					name = prefixMapping.qnameFor(resourceStr);	// may be null
					break;
				}
			}
			
			if ( name == null ) {
				name = resourceStr;
			}
			
			// Need to escape delimiter (e.g., ",") in non-quoted URIs.
			// Blind URLEncoding will "break" many URIs, for example by
			// encoding a '/' that should remain a delimiter (not be encoded)
			// or encoding '%' of an already encoded URI.
			// Thus we simply "encode" only the DELIMITER_STR, which can always
			// be safely undone w/ URL decoding.
			if ( name.contains(DELIMITER_STR) ) {
				name = name.replace(DELIMITER_STR,DELIMITER_ENC);
			}
			
			return name;

		}
	}
	
	
	/**
	 * Key: subject URI or blank node id as a string
	 * Value: data structure of predicates for that subject
	 */
	protected class SortedRows extends TreeMap<String,SortedPredicates> { }
	
	/**
	 * Key: predicate URI as a string
	 * Value: array of values for each property instance of the predicate (for a subject)
	 * 
	 * TreeMap sort order must agree with MaxPropertyInstances
	 */
	protected class SortedPredicates extends TreeMap<String,Values> { }
	
	/**
	 * An array of values (for a predicate)
	 */
	protected class Values extends ArrayList<String> { }
	
	/**
	 * The maximum number of instances of predicate 
	 * (a subject with a property instance and a value)
	 * across all subjects.
	 * 
	 * TreeMap sort order must agree with SortedPredicates
	 * 
	 * Key: predicate URI as a string
	 * Value: maximum observed number of instances
	 */
	protected class MaxPropertyInstances extends TreeMap<String,Integer> { }
	
	
	protected class DataStructure {
		
		SortedRows sortedRows;
		MaxPropertyInstances maxPropertyInstances;
		
		DataStructure() {
			sortedRows = new SortedRows();
			maxPropertyInstances = new MaxPropertyInstances();
		}
		
	}
	
}
