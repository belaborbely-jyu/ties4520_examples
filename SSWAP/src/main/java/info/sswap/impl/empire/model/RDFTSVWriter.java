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
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

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
 * Write the model to the output stream in Tab Separated Value (TSV) according
 * to the fundamental RDF truism:
 * 
 *	subject, predicate, object -> row, col, value
 * 
 * The output is (possibly multiple) rows per subject, and one column per
 * predicate. Subjects have multiple rows when they have multiple property
 * instances.
 * 
 * URIs are URLencoded to escape tabs; String datatype values are quoted.
 * 
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 * 
 */
public class RDFTSVWriter implements RDFWriter {
	
	/**
	 * Read/write buffer size
	 */
	protected final int BUF_SIZ = 4096;

	/**
	 * Default field delimiter
	 */
	protected final String DELIMITER_STR = "	";	// Tab Separated Values
	protected String DELIMITER_ENC = "%09";			// may be over-ridden w/ charset value in constructor
	
	/**
	 * Newline
	 */
	protected String NEWLINE_STR = System.getProperty("line.separator");
	protected final byte[] NEWLINE_BYTES = NEWLINE_STR != null ? NEWLINE_STR.getBytes() : (NEWLINE_STR = "\n").getBytes();

	/**
	 * Default charset for URLEncoding
	 */
	protected final String CHARSET = "UTF-8";
	
	/**
	 * Blank (anonymous) node prefix designator
	 */
	public final static String BNODE_PREFIX = "_:";

	/**
	 * Column header for row1, col1
	 */
	protected final String COLUMN_1_HEADER = "Resource";
	
	protected RDFErrorHandler rdfErrorHandler;
	
	public RDFTSVWriter() {
		
		rdfErrorHandler = new RDFDefaultErrorHandler();

		try {
			DELIMITER_ENC = URLEncoder.encode(DELIMITER_STR,CHARSET);
		} catch ( UnsupportedEncodingException e ) {
			;	// see initialization
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
	
	/**
	 * Mapping RDF (subject, predicate, object) => (row, col, value) will result
	 * in a (sparse) (n+1) x (m+1) matrix, where n is the number of (possibly
	 * replicated) subjects (rows), and m is the number of (unique) predicates
	 * (over all subjects). "+1" row for the header row; "+1" for column for the
	 * first subject column.
	 * <p>
	 * The first cell (row1, col1) is given the fixed header COLUMN_1_HEADER.
	 * Multiple property values for a subject generate a new row per property
	 * instance, such that the first row is the most densely populated and each
	 * successive row is populated until all property values are reported.
	 * <p>
	 * The model of fixing "columns" to non-repetitive predicates ("attributes",
	 * "fields"), and replicating additional row entries for a subject is akin to
	 * 1NF (first normal form) in database table modeling best practice.
	 * <p>
	 * Example:
	 * <pre>
	 * RDF triples:
	 * urn:subject1 urn:predicate1 strValue
	 * urn:subject1 urn:predicate2 numValue1
	 * urn:subject1 urn:predicate2 numValue2
	 * urn:subject2 urn:predicate1 strValue
	 * urn:subject2 urn:predicate3 anyURIValue
	 * urn:subject3 urn:predicate4 urn:subject2
	 * urn:subject3 urn:predicate4 _:b0
	 * 
	 * TSV (4+1 x 5+1) = (5,6):
	 * Resource \t urn:predicate1 \t urn:predicate2 \t urn:predicate3 \t predicate4
	 * urn:subject1 \t "strVvalue" \t numValue1	\t\t
	 * urn:subject1 \t\t numValue2 \t\t
	 * urn:subject2 \t "strVvalue" \t\t "anyURIValue" \t
	 * urn:subject3 \t\t\t\t urn:subject2
	 * urn:subject3 \t\t\t\t _:b0
	 * 
	 * </pre>
	 */
	@Override
	public void write(Model model, OutputStream outputStream, String base) {
		
		/*
		 * Note: for URLs with prefixes, one could print as:
		 * 
		 * <a href=""http://..."">prefix:term</a>
		 * 
		 * (escaping the double quotes with double quotes).
		 * This will not be recognized as a link by MS Excel.
		 * 
		 * MS Excel will parse and recognize:
		 * 
		 * =HYPERLINK("http://...","prefix:term")
		 * 
         * When MS Excel parses a TSV with the above HYPERLINK function it will display
		 * the cell with the second argument name; clicking on the cell will open
		 * a browser and GET the link.
		 * 
		 * The above is not implemented below.
		 */
		
		BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
		
		try {
			
			DataStructure ds = makeDataStructure(model);

			// print header row
			bufferedOutputStream.write(COLUMN_1_HEADER.getBytes());
			for ( String predicateStr : ds.predicates ) {			
				bufferedOutputStream.write((DELIMITER_STR + predicateStr).getBytes());
			}
			
			bufferedOutputStream.write(NEWLINE_BYTES);
			// bufferedOutputStream.flush();	// leave it to OS to decide when to flush
			
			// for every resource (subject) ...
			for ( String resourceStr : ds.resources ) {
				
				// for as many times as it has multiple property instances
				for ( int i = 0; i < ds.numRows.get(resourceStr); i++ ) {
				
					// print the resource (column 1)
					bufferedOutputStream.write(resourceStr.getBytes());

					// print its values
					for ( String predicateStr : ds.predicates ) {
					
						String key = makeKey(resourceStr,predicateStr,String.valueOf(i));
						String valueStr = ds.values.get(key);
					
						if ( valueStr == null ) {
							valueStr = "";
						}
					
						bufferedOutputStream.write((DELIMITER_STR + valueStr).getBytes());
					}
				
					bufferedOutputStream.write(NEWLINE_BYTES);
					// bufferedOutputStream.flush();	// leave it to OS to decide when to flush
				}

			}
						
		} catch ( IOException ioe ) {
			throw new RuntimeException(ioe);
		} finally {
		
			try {
				bufferedOutputStream.flush(); // flush, but leave open for caller
			} catch ( IOException ioe ) {
				; // consume
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
		NameMapper nameMapper = new NameMapper();
		//NameMapper nameMapper = new NameMapper(model);  // to use model's Qnames as well as reserved prefixes

		// for every (unique) RDF subject ...
		for ( ResIterator rItr = model.listSubjects(); rItr.hasNext(); ) {
			
			// use string representations for (lexically unique) keys
			Resource resource = rItr.nextResource();
			String resourceStr = nameMapper.asString(resource);	// handles blank nodes

			// record the resource
			ds.resources.add(resourceStr);
			
			// for every property instance of this subject
			for ( StmtIterator sItr = resource.listProperties(); sItr.hasNext(); ) {
				
				// get the predicate and object for the subject
				Statement stmt = sItr.nextStatement();
				Property property = stmt.getPredicate();
				RDFNode objectNode = stmt.getObject();
				
				// use string representations for (lexically unique) keys
				String predicateStr = nameMapper.asString(property);
				
				// record the predicate
				ds.predicates.add(predicateStr);
				
				// add the lexical value to the list
				String objectStr;
				if ( objectNode.isLiteral() ) {
					
					Literal literal = objectNode.asLiteral();
					objectStr = literal.getLexicalForm();	// string representation of the value w/o the datatype info

					// add double quotes around "plain" literals, strings, and anyURIs
					String datatypeURI = literal.getDatatypeURI();
					if ( datatypeURI == null || datatypeURI.equals(XSD.xstring.getURI()) || datatypeURI.equals(XSD.anyURI.getURI()) ) {
						
						// See http://www.iana.org/assignments/media-types/text/tab-separated-values
						
						// tabs are disallowed in TSV, so any and all tabs in datatype values are simply mapped to spaces, preserving the count
						objectStr = objectStr.replace(DELIMITER_STR," ");
						
						// double quote escaping according to: http://tools.ietf.org/html/rfc4180#section-2
						// (double quotes are escaped by preceding with a double quote, on fields surrounded by double quotes)
						objectStr = objectStr.replace("\"","\"\"");
						
						// surround the entire string w/ double quotes
						objectStr = "\"" + objectStr + "\"";
					}
				} else {	// Resource (URI or bnode)
					objectStr = nameMapper.asString(objectNode);
				}
				
				// get the number of times we've already seen this subject have this predicate (over all values)
				String key = makeKey(resourceStr,predicateStr,"");
				Integer count = ds.numRows.get(key);
				if ( count == null ) {
					count = 0;
				}
				
				// inc the property occurrence by 1
				ds.numRows.put(key,count+1);
				
				// update the total count, over all predicates (and all values)
				Integer max = ds.numRows.get(resourceStr);
				if ( max == null || (count+1) > max ) {
					ds.numRows.put(resourceStr,count+1);
				}
				
				// create a new key, using the count to uniquely identify the "cell"
				key = makeKey(resourceStr,predicateStr,count.toString());
				
				// store the value (the object string)
				ds.values.put(key, objectStr);
				
			}
		}
		
		return ds;
		
	}
	
	
	protected class DataStructure {
		
		SortedSet<String> resources;
		SortedSet<String> predicates;
		
		// key on a complex key of resource, predicate, and "row counter"
		// (repetitive property occurrence) to a (unique) "cell" (value)
		Map<String,String> values;
		
		// key on the resource : value = maximum number of rows (property instance count);
		// key on resource + predicate : value == number of instance for that resource/predicate pair;
		// key on resource + predicate + rowCount : value = object
		Map<String,Integer> numRows;
		
		DataStructure() {
			
			resources = new TreeSet<String>();
			predicates = new TreeSet<String>();
			values = new HashMap<String,String>();
			numRows = new HashMap<String,Integer>();
			
		}
		
	}
	
	/*
	 * Simple manner to turn multiple String keys into one key for a hash map
	 */
	private String makeKey(String key1, String key2, String key3) {
		
		String concatenator = Character.toString('\0');
		return key1 + concatenator + key2 + concatenator + key3;
	}
	
	protected class NameMapper {
		
		// inferred models may not have even RDF, OWL, etc QNames defined,
		// (as returned by Jena's prefixMapping.qnameFor()), so we build a
		// manual reserved ns mapping
		final String[][] reservedQnames = {
				{ "rdf" , Namespaces.RDF_NS },
				{ "rdfs", Namespaces.RDFS_NS },
				{ "xsd", Namespaces.XSD_NS },
				{ "owl", Namespaces.OWL_NS },
				{ "sswap", Namespaces.SSWAP_NS } };
		
		// basal prefix mapping for QNames
		PrefixMapping prefixMapping;

		// Map to rename blank nodes
		HashMap<RDFNode,String> bnodeMap;
		
		int bnodeCounter = 0;
		final String bnodePrefix = BNODE_PREFIX + "b";

		// constructor
		NameMapper() {
			
			prefixMapping = PrefixMapping.Factory.create();
			for ( String[] qname : reservedQnames ) {
				prefixMapping.setNsPrefix(qname[0],qname[1]);
			}
			
			bnodeMap = new HashMap<RDFNode,String>();
		}
		
		// constructor: add the model (prefixMapping) to the basic reserved ns mapping
		NameMapper(PrefixMapping prefixMapping) {
			
			this();
			this.prefixMapping.withDefaultMappings(prefixMapping);
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
			String name = prefixMapping.qnameFor(resourceStr);
			
			if ( name == null ) {
				name = resourceStr;
			}
			
			// Need to escape delimiter in non-quoted URIs.
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

}

