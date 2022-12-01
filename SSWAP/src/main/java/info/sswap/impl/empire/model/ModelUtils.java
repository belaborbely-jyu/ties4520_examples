/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.Config;
import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.ValidationException;
import info.sswap.impl.empire.Namespaces;
import info.sswap.impl.empire.Vocabulary;
import info.sswap.impl.empire.io.Closure;
import info.sswap.impl.empire.io.ClosureBuilder;
import info.sswap.impl.empire.io.ClosureBuilderFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.coode.owlapi.rdfxml.parser.AnonymousNodeChecker;
import org.coode.owlapi.rdfxml.parser.OWLRDFConsumer;
import org.json.JSONException;
import org.json.JSONObject;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.clarkparsia.utils.web.Header;
import com.clarkparsia.utils.web.Response;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Contains utilities to manipulate Jena models as they are read or written by this API.
 * 
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * 
 */
public class ModelUtils {


	public static ModelUtils singleton = new ModelUtils();

	/**
	 * The file name of the XSL StyleSheet that adds comments to an RDF/XML serialization of a SSWAP protocol graph
	 */
	private static final String SSWAP_PROTOCOL_COMMENTS_STYLESHEET = "sswap-protocol-comments.xsl";
	
	/**
	 * The file name that contains known URI schemes registered by IANA (so that we can distinguish a use
	 * of an undefined namespace from a valid URI with an unusual scheme).  
	 */
	private static final String KNOWN_URI_SCHEMES_FILE = "known-uri-schemes.txt";
	
	private static Set<String> KNOWN_URI_SCHEMES;	

	/**
	 * Specifies the identation depth of the generated XML (while commenting RDF/XML)
	 */
	private static final int COMMENTED_OUTPUT_INDENT = 2;

	/**
	 * The namespace used for prefixing Jena BNodes. Empire has problem handling Java BNodes for multiple reasons.
	 * First, it may be difficult to distinguish a bnode id from a URI by looking just at identifier (this happens in
	 * situations where there is no access to a Jena Resource; e.g., while evaluating SPARQL queries) -- there is an
	 * overlap in valid bnode syntax and URI syntax. Second, when a bnode id is mistaken for a URI, Empire internals may
	 * complain that it is not an absolute URI.
	 * 
	 * To solve this problems, all loaded Jena models have (almost all) their bnodes filtered and replaced with regular resources
	 * whose URIs belong to this specific, reserved namespace (i.e., no bnodes). When serializing the model out, all
	 * such resources can be easily identified (only these resources belong to this namespace), and can be properly
	 * converted back to a bnode.
	 * 
	 * The only bnodes that are not filtered on the load are certain nodes in OWL2 expressions, and where determined
	 * to cause problems with reasoning if converted to named resources. An example of such a bnode is currently
	 * a bnode typed as DataRange. (But there may be others, since OWL2 actually requires the use of bnodes in certain places,
	 * although reasoners can typically handle named resources in such places OK because of compatibility with OWL1,
	 * which did not have such a restriction.)
	 */
	public static final String BNODE_NS = "tag:sswap.info:bnode:";
	
	/**
	 * A constant specifying how many redirects an invocation of a RIG/RQG should
	 * be allowed before giving up (e.g., because the redirects may be an infinite loop).
	 */
	private static int MAX_REDIRECTS = 20;	

	/**
	 * Types of SSWAPNodes (resources that require special handling in SSWAP Protocol)
	 */
	private static final Resource[] SSWAP_NODE_TYPES = new Resource[] {
		Vocabulary.SSWAP_RESOURCE,
		Vocabulary.SSWAP_GRAPH,
		Vocabulary.SSWAP_SUBJECT,
		Vocabulary.SSWAP_OBJECT,
		Vocabulary.SSWAP_PROVIDER
	};

	/**
	 * Constant for name of types that will be passed as "prettyTypes" to Jena. This will be a suggestion
	 * to Jena RDF/XML-ABBREV writer to create RDF/XML withe resources of these types at the top level.
	 */
	private static final Resource[] PRETTY_TYPES = SSWAP_NODE_TYPES;	
	
	static {
		try {
	        KNOWN_URI_SCHEMES = readKnownURISchemes();	       
        }
        catch (IOException e) {
        	throw new RuntimeException("Unable to read information about known schemes", e);
        }
	}
	
	/**
	 * Serializes a Jena model into the output stream in the specified RDF representation. Since internally, this API
	 * removed all the BNodes (by renaming them with generated identifiers that belong to a special namespace), this
	 * method renames such previously renamed nodes back into bnodes.
	 * 
	 * Additionally, this method can produce a commented output (e.g., for educational purposes) of the RDG (only
	 * RDF/XML representation); i.e., it will contain XML comments describing parts of the protocol graph.
	 * 
	 * @param model
	 *            the Jena model to be serialized
	 * @param os
	 *            the output stream to which the model should be serialized
	 * @param rdfRepresentation
	 *            the RDF representation in which the model should be serialized
	 * @param commentedOutput
	 *            true, if the RDF/XML output should be commented, false otherwise (this flag is ignored for outputs
	 *            other than RDF/XML, and should only be used for protocol graphs).
	 */
	public static void serializeModel(Model model, OutputStream os, RDFRepresentation rdfRepresentation,
	                boolean commentedOutput) {
		Model outModel = createOutputModel(model);

		try {
			if (RDFRepresentation.RDF_XML.equals(rdfRepresentation)) {
				RDFWriter rdfWriter = outModel.getWriter("RDF/XML-ABBREV");
				rdfWriter.setProperty("prettyTypes", PRETTY_TYPES);
				rdfWriter.setProperty("showXMLDeclaration", "true");
				
				if (commentedOutput) {
					// creating of RDF/XML output and post-processing it to add the comments

					// first write the uncommented RDF/XML to an intermediate stream
					ByteArrayOutputStream intermediateOutputStream = new ByteArrayOutputStream();
					rdfWriter.write(outModel, intermediateOutputStream, null);
					ByteArrayInputStream intermediateInputStream = new ByteArrayInputStream(intermediateOutputStream
					                .toByteArray());

					try {
						// comment the persisted model and write to the actual output stream
						commentRdfXmlProtocolGraph(intermediateInputStream, os);
					}
					catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException("Unable to add comments to the model");
					}
				}
				else {
					rdfWriter.write(outModel, os, null);
				}
			}
			else if (RDFRepresentation.TURTLE.equals(rdfRepresentation)) {
				outModel.write(os, "TURTLE");
			}
			else if (RDFRepresentation.N3.equals(rdfRepresentation)) {
				outModel.write(os, "N3");
			}
			else if (RDFRepresentation.NTRIPLES.equals(rdfRepresentation)) {
				outModel.write(os, "N-TRIPLE");
			}
			else if (RDFRepresentation.TSV.equals(rdfRepresentation)) {
				outModel.write(os, "TSV");
			}
			else {
				throw new IllegalArgumentException("Unsupported RDF representation " + rdfRepresentation);
			}
		}
		finally {
			outModel.close();
		}
		
		try {
			os.flush();
		}
		catch (IOException e) {
			// TODO: what do we do, if we can't flush?
		}
	}

	/**
	 * Adds XML comments to RDF/XML representation of a sswap protocol graph. The comments are added by performing an XSLT
	 * transformation of the original RDF/XML data (the XSLT stylesheet specifies which comments are added and where).
	 * Current implementation uses the stylesheet that is returned by getSSWAPProtocolCommentsXsltStream().
	 * 
	 * @param is
	 *            the input stream from which the data for commenting is read.
	 * @param os
	 *            the output stream to which the commented data is written
	 * @throws TransformerException
	 *             if an exception occurs during the XSLT transformation
	 * @throws IOException
	 *             if an I/O error should occur
	 */
	private static void commentRdfXmlProtocolGraph(InputStream is, OutputStream os) throws TransformerException,
	                IOException {
		// Prepare the javax.xml.transformation Sources for the input, xslt source and the Result (for the ouput).
		// The output from the transformation will be first to a DOM model and not a stream/writer (xslt transformers
		// are not very good at formatting/indenting the output), so it will have to be done later correctly
		StreamSource source = new StreamSource(is);
		StreamSource xsltSource = new StreamSource(getSSWAPProtocolCommentsXsltStream());
		DOMResult result = new DOMResult();

		// prepare the transformer
		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer trans = transFactory.newTransformer(xsltSource);

		// do actual transformation
		trans.transform(source, result);

		// output the result of the transformation (DOM model) to the output stream
		// while properly formatting it (proper indentation)
		OutputFormat format = new OutputFormat();
		format.setIndenting(true);
		format.setIndent(COMMENTED_OUTPUT_INDENT);
		XMLSerializer serializer = new XMLSerializer(os, format);
		serializer.serialize((Document) result.getNode());

		// close the input sources
		source.getInputStream().close();
		xsltSource.getInputStream().close();
	}

	/**
	 * Opens a stream that leads to an XSL style sheet that adds comments to a sswap protocol graph
	 * 
	 * @return the stream that contains the XSL style sheet
	 * @throws FileNotFoundException
	 *             if the underlying XSL data cannot be found
	 */
	private static InputStream getSSWAPProtocolCommentsXsltStream() throws FileNotFoundException {
		InputStream result = ModelUtils.class.getResourceAsStream(SSWAP_PROTOCOL_COMMENTS_STYLESHEET);

		if (result == null) {
			throw new FileNotFoundException("Unable to find the stylesheet resource: "
			                + SSWAP_PROTOCOL_COMMENTS_STYLESHEET);
		}

		return result;
	}
	
	private static InputStream getKnownURISchemesStream() throws FileNotFoundException {
		InputStream result = ModelUtils.class.getResourceAsStream(KNOWN_URI_SCHEMES_FILE);

		if (result == null) {
			throw new FileNotFoundException("Unable to find the information about the known URI schemes: "
			                + KNOWN_URI_SCHEMES_FILE);
		}

		return result;
	}

	private static Set<String> readKnownURISchemes() throws IOException {
		Set<String> result = new HashSet<String>();
		

		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(getKnownURISchemesStream()));
		String line = null;

		while ((line = lnr.readLine()) != null) {
			if (line.startsWith("#")) {
				// skip comments (start with #)
				continue;
			}

			String scheme = line.trim();

			if (!scheme.isEmpty()) {
				result.add(scheme);
			}	
		}

		lnr.close();

		return result;
	}
	
	/**
	 * Checks whether the scheme is a known URI scheme (i.e., registered by IANA)  
	 * 
	 * @param scheme the scheme to be checked
	 * @return true if the scheme is a registered scheme, false otherwise
	 */
	public static boolean isSchemeKnown(String scheme) {
		return KNOWN_URI_SCHEMES.contains(scheme);
	}
	
	/**
	 * Checks whether the URI has a known scheme (i.e., registered by IANA).
	 * 
	 * @param uriString string containing a URI
	 * @return true, if the string is a valid URI and it has a scheme that is known, false otherwise
	 * (i.e., scheme is not known or the string is not a valid URI, and therefore has no scheme).
	 */
	public static boolean hasKnownScheme(String uriString) {
		try {
			URI uri = new URI(uriString);
			
			return isSchemeKnown(uri.getScheme());
		}
		catch (URISyntaxException e) {
			// if this is not a valid URI at all, then it does not have a valid scheme 
			return false;
		}		
	}
	
	/**
	 * Gets the terms in the model whose URIs do not have a valid scheme (i.e., registered by IANA). Most
	 * likely such terms are just QNames whose namespace prefixes did not get expanded properly (e.g., 
	 * a particular prefix was not defined).  
	 * 
	 * @param model Jena model that should be checked for terms without a valid scheme
	 * @return a collection of terms (strings containing URIs of terms) with all terms that
	 * do not have a valid scheme
	 */
	public static Collection<String> getTermsWithoutValidScheme(Model model) {
		Set<String> result = new HashSet<String>();
		
		// check all statements
		for (StmtIterator it = model.listStatements(); it.hasNext(); ) {
			Statement statement = it.next();
	
			// check valid scheme only if the subject is not a bnode (not encoded by our bnode encoding strategy)
			if (statement.getSubject().isURIResource() && !hasKnownScheme(statement.getSubject().getURI())) {
				result.add(statement.getSubject().getURI());
			}

			// check all predicates (they cannot be bnodes)
			if (!hasKnownScheme(statement.getPredicate().getURI())) {
				result.add(statement.getPredicate().getURI());
			}
			
			// check valid scheme only if the object is a resource and is not a bnode (not encoded by our bnode encoding strategy)
			if (statement.getObject().isURIResource()) {			
				if (!hasKnownScheme(statement.getObject().asResource().getURI())) {
					result.add(statement.getObject().asResource().getURI());
				}
			}			
		}
		
		return result;
	}
	
	/**
	 * Removes BNodes from a model by converting them into a resource with a URI (which belongs to a reserved
	 * namespace).
	 * 
	 * @param model
	 *            the model from which all bnodes should be removed
	 */
	public static void removeBNodes(Model model) {
		removeBNodes(model, model.listSubjects());
		removeBNodes(model, model.listObjects());
	}
	
	/**
	 * Converts all SSWAP nodes (nodes that require special handling in SSWAP protocol; e.g., sswap:Resource) to BNodes
	 * (or more exactly to nodes named using the special BNode naming scheme).
	 * 
	 * @param model the model whose SSWAP nodes should be converted to BNodes
	 */
	public static void convertSSWAPNodesToBNodes(Model model) {
		for (Resource nodeType : SSWAP_NODE_TYPES) {
			convertToBNodes(getResourcesByType(model, nodeType));
		}
	}
	
	/**
	 * Converts a given set of resources to BNodes (i.e., resources named using the BNode naming scheme)
	 * @param resources the resources to be converted
	 */
	private static void convertToBNodes(Collection<Resource> resources) {
		for (Resource resource : resources) {
			if (!resource.isAnon() && !isBNodeURI(resource.getURI())) {
				ResourceUtils.renameResource(resource, generateBNodeId());
			}
		}
	}
	
	/**
	 * Gets all resources that have the specified type (via rdf:type property)
	 * 
	 * @param model the model where the resources are 
	 * @param typeResource the resource describing the type
	 * @return a collection of all resources that are of the specified type
	 */
	private static Collection<Resource> getResourcesByType(Model model, Resource typeResource) {
		List<Resource> result = new LinkedList<Resource>();
		
		StmtIterator it = model.listStatements(null, RDF.type, typeResource);
		
		while (it.hasNext()) {
			Statement s = it.next();
			
			result.add(s.getSubject());
		}
		
		return result;
	}
	
	/**
	 * Removes BNodes from a Node iterator by converting them into a resource with a URI (which belongs to a reserved
	 * namespace).
	 * 
	 * @param model
	 *            the model from which all bnodes should be removed
	 * @param it the iterator
	 */
	private static void removeBNodes(Model model, Iterator<? extends RDFNode> it) {
		while (it.hasNext()) {
			RDFNode node = (RDFNode) it.next();
			
			if (node.isResource() && (node.isAnon()) && !shouldRemainAnon(model, node.asResource())) {							
				node = convertBNode(model, (Resource) node);				
			}
			
			if (node.isResource()  
				&& node.isURIResource() 
				&& isBNodeURI(node.asResource().getURI())
				&& hasDeclaredType(model, node.asResource(), OWL.Restriction)) {
				// additionally, if resource is typed owl:Restriction, it must NOT be typed as owl:Class
				// (Pellet has an issue with non-bnode restrictions that are classes at the same time)
				
				model.remove(node.asResource(), RDF.type, OWL.Class);
			}
		}		
	}
	
	/**
	 * Checks whether a resource should remain anonymous. Some bnodes that belong to OWL2 expressions
	 * should remain anonymous, or they will cause problems with reasoning. Currently,
	 * the only type of bnodes that remain anonymous are those typed (rdf:type) as owl:DataRange
	 * 
	 * @param model the model to which the resource belongs
	 * @param resource the resource to be checked
	 * @return true if the resource should remain anonymous, false if it can safely be converted to a named resource
	 */
	private static boolean shouldRemainAnon(Model model, Resource resource) {
		return hasDeclaredType(model, resource, OWL2.DataRange) || hasDeclaredType(model, resource, RDFS.Datatype); 
	}
	
	/**
	 * Checks whether a resource has a certain declared (rdf:type) type.
	 * 
	 * @param model model to which the resource belongs
	 * @param resource the resource to be checked
	 * @param type the type to which the resource should belong
	 * @return true if a triple resource rdf:type type exists in the model, false otherwise
	 */
	private static boolean hasDeclaredType(Model model, Resource resource, Resource type) {
		StmtIterator it = model.listStatements(resource, RDF.type, type);
		
		boolean result = it.hasNext();
		it.close();
		
		return result;
	}

	/**
	 * Converts a bnode resource into a non-bnode resource (by generating a URI that belongs to a reserved namespace).
	 * 
	 * @param model
	 *            the model to which this resource belongs
	 * @param resource
	 *            the bnode resource
	 *            
	 * @return the new resource after renaming
	 * @throws IllegalArgumentException
	 *             if the resource is not a bnode resource
	 */
	private static Resource convertBNode(Model model, Resource resource) {
		if (!resource.isAnon()) {
			throw new IllegalArgumentException("This resource is not a bnode: " + resource.getURI());
		}

		String id = convertBNodeId(resource.getId().getLabelString());

		return ResourceUtils.renameResource(resource, id);
	}

	/**
	 * Converts an identifier of an anonymous node (AnonId) into a valid URI in the special, reserved namespace that
	 * will later be used to convert that node back into a bnode (during serialization)
	 * 
	 * @param anonId anonymous node (blank node) id
	 * @return a valid URI to be used as the id
	 */
	private static synchronized String convertBNodeId(String anonId) {
		String id = anonId;

		// replace all ':' (colons) with '-' (minuses)
		id = id.replace(':', '-');

		// add the reserved namespace
		id = BNODE_NS + id;

		return id;
	}
	
	public static synchronized String generateBNodeId() {
		return convertBNodeId(new AnonId().getLabelString());
	}

	public static boolean isBNodeURI(String bnodeURI) {
		return (bnodeURI.startsWith(BNODE_NS));
	}
	
	/**
	 * Creates a model that is suitable for writing out using Jena Writers. It copies all the statements in the given
	 * model, sets up namespace prefixes, and handles bnodes that were converted into URIs when the data was read in.
	 * 
	 * @param model
	 *            the source model for the new model
	 * @return the model that has been processed for writing.
	 */
	private static Model createOutputModel(Model model) {
		Model newModel = ModelFactory.createDefaultModel();
		newModel.add(model);

		newModel.setNsPrefixes(model);
		newModel.setNsPrefix("sswap", Namespaces.SSWAP_NS);

		forEachResource(newModel, new Function<Resource,Void>() {
            public Void apply(Resource resource) {
    			if (!resource.isAnon() && resource.getURI().startsWith(BNODE_NS)) {
    				ResourceUtils.renameResource(resource, null);
    			}
            	
	            return null;
            }			
		});
		
		return newModel;
	}

	/**
	 * Applies a function to each resource in the model that is mentioned as a subject or an object in any statement
	 * in the model.
	 * 
	 * @param model the model that contains the resources
	 * @param function the function that should be applied
	 */
	private static void forEachResource(Model model, Function<Resource,Void> function) {
		for (ResIterator resIterator = model.listSubjects(); resIterator.hasNext();) {
			Resource resource = resIterator.next();
			
			function.apply(resource);
		}

		for (NodeIterator resIterator = model.listObjects(); resIterator.hasNext();) {
			RDFNode node = resIterator.next();

			if (node.isResource()) {
				Resource resource = (Resource) node;

				function.apply(resource);
			}
		}
	}
	
	/**
	 * A utility method for extracting the first encountered value in the object position for the given resource and
	 * predicate. This method is generally used for properties for which it is expected that at most one value exists.
	 * 
	 * @param model
	 *            the model from which the value should be extracted
	 * @param subject
	 *            the subject for which the value should be extracted
	 * @param predicate
	 *            the predicate for which the value should be extracted
	 * 
	 * @return the rdf node for the first encountered value or null, if no value was encountered
	 */
	public static RDFNode getFirstObjectValue(Model model, Resource subject, Property predicate) {
		StmtIterator it = model.listStatements(subject, predicate, (RDFNode) null);
		RDFNode result = null;

		try {
			if (it.hasNext()) {
				Statement statement = it.next();
				result = statement.getObject();
			}
		}
		finally {
			it.close();
		}

		return result;
	}
	
	/**
	 * Gets the first encountered statement for the with the given resource in the subject position and the 
	 * predicate
	 * 
	 * @param model model that should contain the statement
	 * @param subject the subject of the requested statement
	 * @param predicate the predicate for the statement
	 * @return the statement (first encountered one if there are multiple ones), or null if none could be encountered
	 */
	public static Statement getFirstStatement(Model model, Resource subject, Property predicate) {
		StmtIterator it = model.listStatements(subject, predicate, (RDFNode) null);

		try {
			if (it.hasNext()) {
				Statement statement = it.next();
				return statement;
			}
		}
		finally {
			it.close();
		}

		return null;
	}
	
	/**
	 * Gets the URIs of all lists the given resource is a member
	 * 
	 * @param model model where the resource is located
	 * @param listMember a (potential) member of some lists (not an internal node within a list!)
	 * @return collection of the first resources in every list, listMember is member of
	 */
	public static Collection<Resource> getListURIs(Model model, Resource listMember) {
		List<Resource> result = new LinkedList<Resource>();
		
		// try to find an rdf:first statement pointing to the listMember
		StmtIterator it = model.listStatements(null, RDF.first, listMember); 
		
		try {
			// for every such a statement, listMember is a member of a list -- retrieve the head of this list.
			while (it.hasNext()) {
				Statement s = it.next();
				
				result.add(getListHead(model, s.getSubject()));
			}
		}
		finally {
			it.close();
		}
				
		return result;
	}
	
	/**
	 * Gets a head of a list for a given internal node (not a list member!)
	 * 
	 * @param model model where the node is located
	 * @param listNode the list node for the list
	 * @return the list head (may be the same as listNode, if listNode is a head) or null, if listNode
	 * is not in fact a list node
	 */
	private static Resource getListHead(Model model, Resource listNode) {
		// Contains all visited nodes -- to be able to handle seriously malformed
		// lists, which otherwise could cause us to enter an infinite loop
		// (such lists are unlikely to occur, but they could be intentionally/maliciously prepared)
		Set<Resource> visitedNodes = new HashSet<Resource>();
		
		// contains the currently visited list node
		Resource currentListNode = listNode;
		
		// contains the previously visited list node
		Resource previousListNode = null;
		
		do {
			visitedNodes.add(currentListNode);
			
			// try to find a list node that points to the current node via rdf:rest
			StmtIterator it = model.listStatements(null, RDF.rest, currentListNode);
			
			// will hold the resource that points to the currentList node via rdf:rest
			Resource precedingNode = null;
			
			// if iterator has any value, that's the precedingNode
			if (it.hasNext()) {
				precedingNode = it.next().getSubject();
				
				if (visitedNodes.contains(precedingNode)) {
					throw new IllegalArgumentException("Detected a cycle in rdf:List!");
				}
			}
			
			it.close();
			
			previousListNode = currentListNode;
			
			// proceed to the preceding node
			currentListNode = precedingNode;
		}
		while (currentListNode != null);
		
		// since the currentListNode is null at the end of the do/while loop, the previousNode contains the last
		// node that did not have the preceding node; that is, the head of the list
		return previousListNode;
	}

	/**
	 * Creates an RDFList object based on the rdf:List data in the underlying Jena model.
	 * 
	 * @param model
	 *            the model containing the data describing the list
	 * @param listResource
	 *            the resource containing the head of the list
	 * @return the RDFList with the data
	 * @throws IllegalArgumentException
	 *             if the underlying data does not form a well-formed list
	 */
	public static RDFList createRDFList(Model model, Resource listResource) throws IllegalArgumentException {
		List<RDFNode> contents = new LinkedList<RDFNode>();

		Resource currentResource = listResource;

		while (!currentResource.equals(RDF.nil)) {
			RDFNode firstNode = getFirstObjectValue(model, currentResource, RDF.first);

			if (firstNode == null) {
				throw new IllegalArgumentException(
				                "This RDFList is not well formed. There is no rdf:first for a resource!");
			}

			
			contents.add(firstNode);

			RDFNode restNode = getFirstObjectValue(model, currentResource, RDF.rest);

			if (restNode == null) {
				throw new IllegalArgumentException("This RDFList is not well formed. There is no rdf:rest property!");
			}

			if (restNode.isResource()) {
				currentResource = (Resource) restNode;
			}
			else {
				throw new IllegalArgumentException("This RDFList is not well formed. The rdf:rest is not a resource!");
			}
		}

		return model.createList(contents.iterator());
	}
	
	/**
	 * Gets all Jena Statements that encode an RDFList
	 * 
	 * @param model the model with the Jena Statements
	 * @param listResource the first resource of the list
	 * @return a collection of statements that make up that list
	 */
	public static Collection<Statement> getAllStatementsForList(Model model, Resource listResource) {
		List<Statement> contents = new LinkedList<Statement>();
		
		Resource currentResource = listResource;

		while (!currentResource.equals(RDF.nil)) {
			Statement firstStatement = getFirstStatement(model, currentResource, RDF.first);
			contents.add(firstStatement);
			
			if (firstStatement == null) {
				throw new IllegalArgumentException(
				                "This RDFList is not well formed. There is no rdf:first for a resource!");
			}

			Statement restStatement = getFirstStatement(model, currentResource, RDF.rest);
			contents.add(restStatement);

			if (restStatement == null) {
				throw new IllegalArgumentException("This RDFList is not well formed. There is no rdf:rest property!");
			}

			if (restStatement.getObject().isResource()) {
				currentResource = (Resource) restStatement.getObject().asResource();
			}
			else {
				throw new IllegalArgumentException("This RDFList is not well formed. The rdf:rest is not a resource!");
			}
		}
				
		return contents;
	}

	/**
	 * Gets all resources involved in an rdf:List. This includes all the intermediate nodes (connected via rdf:rest
	 * predicates), with the exception of the terminating rdf:nil.
	 * 
	 * The rationale for introducing this method is the need to properly remove all the statements relating to a list
	 * when the list itself should be removed.
	 * 
	 * @param model
	 *            the model containing the list data
	 * @param listResource
	 *            the resource that is the head of the list
	 * @return a collection of all resources involved in RDF serialization of this list (including the intermediate
	 *         nodes connected by rdf:rest, but not including rdf:nil)
	 */
	public static Collection<Resource> getAllResourcesForList(Model model, Resource listResource) {
		List<Resource> contents = new LinkedList<Resource>();

		Resource currentResource = listResource;

		while (!currentResource.equals(RDF.nil)) {
			contents.add(currentResource);

			RDFNode firstNode = getFirstObjectValue(model, currentResource, RDF.first);

			if (firstNode == null) {
				throw new IllegalArgumentException(
				                "This RDFList is not well formed. There is no rdf:first for a resource!");
			}

			if (firstNode.isResource()) {
				contents.add((Resource) firstNode);
			}

			RDFNode restNode = getFirstObjectValue(model, currentResource, RDF.rest);

			if (restNode == null) {
				throw new IllegalArgumentException("This RDFList is not well formed. There is no rdf:rest property!");
			}

			if (restNode.isResource()) {
				currentResource = (Resource) restNode;
			}
			else {
				throw new IllegalArgumentException("This RDFList is not well formed. The rdf:rest is not a resource!");
			}
		}

		return contents;
	}
	
	/**
	 * Extracts from the original Jena Model facts that relate only to the specified individual.
	 * Additionally, this method also copies all owl:imports statements so that all the classes in the produced
	 * model are properly declared. 
	 * 
	 * @param originalModel the model containing all the facts
	 * @param uri the URI of the individual
	 * @return the sub model that contains only the facts that relate to this individual
	 */
	public static Model partitionModel(Model originalModel, String uri, boolean followAllResources) {
		Model resultModel = JenaModelFactory.get().createEmptyModel();
		
		resultModel.setNsPrefixes(originalModel.getNsPrefixMap());

		// generate import statements
		addImportStatements(originalModel, resultModel);
		
		// copy relevant statements from the original model
		addStatementsToPartitionedModel(originalModel.getResource(uri), originalModel, resultModel, new HashSet<Resource>(), followAllResources);
		
		return resultModel;
	}
	
	/**
	 * Copies statements that only relate to a specific resource. This method copies all the statements where this resource is a subject.
	 * Moreover, if any of that statements refers to a bnode, all the statements for that bnode will also be included.
	 * 
	 * @param resource the resource for which statements should be copied.
	 * @param originalModel the original model that contains all the statements
	 * @param partitionedModel the partitioned model being created
	 * @param alreadyAdded a collection of the resources that were already processed (to speed up the process, in case some resources are referenced
	 * multiple times).
	 */
	private static void addStatementsToPartitionedModel(Resource resource, Model originalModel, Model partitionedModel, Collection<Resource> alreadyAdded, boolean followAllResources) {
		if (!alreadyAdded.contains(resource)) {
			alreadyAdded.add(resource);

			// process every statement where the resource is the subject
			for (StmtIterator it = originalModel.listStatements(resource, null, (RDFNode) null); it.hasNext(); ) {
				Statement statement = it.nextStatement();

				RDFNode node = statement.getObject();

				// add such statement
				partitionedModel.add(statement);

				if (node.canAs(Resource.class)) {
					Resource object = (Resource) node.as(Resource.class);

					// for every anonymous resource (bnode) call this method recursively, with the bnode as the subject to be copied
					if (followAllResources || object.isAnon() || isBNodeURI(object.getURI())) {
						addStatementsToPartitionedModel(object, originalModel, partitionedModel, alreadyAdded, followAllResources);
					}
					/*
					else if (statement.getPredicate().getURI().equals(OWL.onProperty.getURI())) {
						addTypeStatements(object.asResource(), originalModel, partitionedModel);
					}
					*/
					
					addTypeStatements(object.asResource(), originalModel, partitionedModel);
				}
			}
		}
	}
	
	private static void addTypeStatements(Resource resource, Model originalModel, Model partitionedModel) {
		for (StmtIterator it = originalModel.listStatements(resource, RDF.type, (RDFNode) null); it.hasNext(); ) {
			Statement resourceTypeStatement = it.nextStatement();
			partitionedModel.add(resourceTypeStatement);
		}
	}
	
	/**
	 * Copies all import statements from the original model.
	 * 
	 * @param originalModel the model from which the import statements should be copied
	 * @param partitionedModel the model to which the import statements should be copied
	 */
	private static void addImportStatements(Model originalModel, Model partitionedModel) {
		// copy all ?x rdf:type owl:Ontology statements
		for (StmtIterator it = originalModel.listStatements(null, RDF.type, OWL.Ontology); it.hasNext(); ) {
			Statement ontologyTypeStmt = it.nextStatement();
			partitionedModel.add(ontologyTypeStmt);
		}
		
		// copy all ?x owl:imports ?y statements
		for (StmtIterator it = originalModel.listStatements(null, OWL.imports, (RDFNode) null); it.hasNext(); ) {
			Statement importStmt = it.nextStatement();
			partitionedModel.add(importStmt);
		}
	}

	public static OWLOntology createOWLOntology(OWLOntologyManager manager, String url, Model model) throws OWLOntologyCreationException {
		OWLOntology result = manager.createOntology(IRI.create(url));
		
		convertToOWLOntology(result, model);
		
		return result;
	}

	
	public static OWLOntology createOWLOntology(OWLOntologyManager manager, String url, Model baseModel, Model closure) throws OWLOntologyCreationException {
		OWLOntology result = manager.createOntology(IRI.create(url));
		
		convertToOWLOntology(result, baseModel, closure);
			
		return result;
	}
	
	private static void convertToOWLOntology(OWLOntology ontology, Model ... models ) throws OWLOntologyCreationException {
		try {
			OWLRDFConsumer consumer = new OWLRDFConsumer(ontology, new AnonymousNodeChecker() {
				public boolean isAnonymousNode(IRI iri) {					
					return isAnonymousNode(iri.toString());
				}

				public boolean isAnonymousNode(String iri) {
					return isBNodeURI(iri) || iri.startsWith("anon:");
				}

                public boolean isAnonymousSharedNode(String iri) {
	                return false;
                }			
			}, new OWLOntologyLoaderConfiguration());
			consumer.setOntologyFormat(new RDFXMLOntologyFormat());
			consumer.startModel("");
			
			for (Model model : models) {
				Model baseModel = model instanceof InfModel ? ((InfModel) model).getRawModel()
								                            : model;

				StmtIterator statements = baseModel.listStatements();

				while (statements.hasNext()) {
					Statement stmt = statements.nextStatement();
					String subj = convert(stmt.getSubject());
					String pred = convert(stmt.getPredicate());
					String obj = convert(stmt.getObject());

					if (pred.equals(OWL.imports.getURI())) {
						continue;
					}
					
					//if (pred.equals(RDF.type.getURI()) && obj.equals(OWL.Ontology)) {
					//	continue;
					//}

					if (stmt.getObject() instanceof Literal) {
						Literal literal = stmt.getLiteral();
						String datatypeIRI = literal.getDatatypeURI();
						String lang = literal.getLanguage();

						if ((lang != null) && (lang.length() == 0)) {
							lang = null;
						}

						consumer.statementWithLiteralValue(subj, pred, obj, lang, datatypeIRI);
					} else {
						consumer.statementWithResourceValue(subj, pred, obj);
					}
				}
			}

			consumer.endModel();			
		} catch (SAXException e) {
			e.printStackTrace();
			throw new OWLOntologyCreationException("Received a SAX exception while reading a Jena model!");
		}
	}
	
	private static String convert(RDFNode node) {
		if (node instanceof Literal) {
			return ((Literal) node).getLexicalForm();			
		} else {
			Resource resource = (Resource) node;
			
			if (resource.isAnon()) {
				return convertBNodeId(resource.getId().getLabelString());
			} else if (node.equals(OWL.DataRange)) {
				// workaround OWLAPI bug, which incorrectly maps owl:DataRange to owl:Datatype (should be rdfs:Datatype)
				// therefore, we perform the mapping here, before OWLAPI can get to this concept
				return RDFS.Datatype.getURI();
			} else {
				return resource.getURI();
			}
		}
	}
	
	/**
	 * Gets URIs of all resources in the model that belong to the specified namespace 
	 * 
	 * @param model the model whose resources are to be returned
	 * @param ns the namespace URI
	 * @return a collection of URIs of resources that belong to the specified namespace
	 */
	public static Collection<String> getResourcesInNS(final Model model, final String ns) {
		final List<String> result = new LinkedList<String>();
		
		forEachResource(model, new Function<Resource,Void>() {
            public Void apply(Resource resource) {
            	if (resource.isURIResource() && resource.getURI().startsWith(ns)) {
            		result.add(resource.getURI());
            	}
            	
	            return null;
            }			
		});
		
		return result;
	}
	
	/**
	 * Validates whether all resources in SSWAP namespace used in the model are defined in SSWAP ontology.
	 * (The use of SSWAP namespace is restricted, similarly to OWL namespace; only terms defined in SSWAP ontology can
	 * be used in other models.)  
	 * 
	 * @param model the model to be validated
	 * @throws ValidationException if the model contains a resource from SSWAP namespace that is not defined in SSWAP ontology.
	 */
	public static void validateSSWAPVocabulary(Model model) throws ValidationException {
		// retrieve the whole SSWAP ontology with imports. We are using here the ClosureBuilder to take advantage
		// of the ModelCache (since SSWAP ontology is used by virtually every model, it is very likely to be cached,
		// and no network connections will occur
		ClosureBuilder closureBuilder = ClosureBuilderFactory.newInstance().newBuilder();
		Closure closure = closureBuilder.build(null, Vocabulary.SSWAP_ONTOLOGY_URI);
		
		Model closureModel = closure.getClosureModel();
		
		// check all resources
		for (String sswapResourceURI : getResourcesInNS(model, Namespaces.SSWAP_NS)) {
			if (!sswapResourceURI.startsWith(Namespaces.SSWAP_ASYNC_NS) 
				&& !sswapResourceURI.equals(Vocabulary.ICON.toString())			
				&& !closureModel.containsResource(model.getResource(sswapResourceURI))) {
				throw new ValidationException("Use of unrecognized SSWAP vocabulary: " + sswapResourceURI);
			}
		}
	}
	
	/**
	 * Convenience method to
	 * <code>invoke(URI invocationURI, InputStream graphContentsStream)</code>
	 * to execute a HTTP GET.
	 * 
	 * @param invocationURI
	 *            URI to invoke (upon successful conversion of URI to a URL)
	 * @return the response of the invocation attempt
	 * @throws IOException
	 *             on any failure to execute call, including HTTP response
	 *             codes of 400- or 500- series
	 * @see #invoke(URI,InputStream,boolean)
	 */
	public static Response invoke(URI invocationURI) throws IOException {
		return invoke(invocationURI, (InputStream) null);
	}
	
	/**
	 * Convenience method to
	 * <code>invoke(URI invocationURI, InputStream graphContentsStream)</code>
	 * to execute a HTTP POST with array data as the body of the POST.
	 * 
	 * @param invocationURI
	 *            URI to invoke (upon successful conversion of URI to a URL)
	 * @param graphContentsArray
	 *            contents of a HTTP POST; set to null to invoke HTTP GET
	 * @return the response of the invocation attempt
	 * @throws IOException
	 *             on any failure to execute call, including HTTP response
	 *             codes of 400- or 500- series
	 * @see #invoke(URI,InputStream,boolean)
	 */
	public static Response invoke(URI invocationURI, byte[] graphContentsArray) throws IOException {
		
		ByteArrayInputStream byteArrayInputStream = null;
		
		if ( graphContentsArray != null ) {
			byteArrayInputStream = new ByteArrayInputStream(graphContentsArray);
		}
		
		return invoke(invocationURI, byteArrayInputStream);
	}

	
	/**
	 * Invokes HTTP GET or POST to URL. Access to the invocation response
	 * content can be gained by calling <code>getContent</code> on the
	 * returned response.
	 * 
	 * @param invocationURI
	 *            URI to invoke (upon successful conversion of URI to a URL)
	 * @param graphContentsStream
	 *            contents of a HTTP POST; set to null to invoke HTTP GET
	 * @return the response of the invocation attempt
	 * @throws IOException
	 *             on any failure to execute call, including HTTP response
	 *             codes of 400- or 500- series
	 * @see #invoke(URI,InputStream,boolean)
	 */
	public static Response invoke(URI invocationURI, InputStream graphContentsStream) throws IOException {
		return invoke(invocationURI, graphContentsStream, false);	
	}
	
	/**
	 * Invokes HTTP GET or POST to URL. Access to the invocation response
	 * content can be gained by calling <code>getContent</code> on the
	 * returned response.
	 * 
	 * @param invocationURI
	 *            URI to invoke (upon successful conversion of URI to a URL)
	 * @param graphContentsStream
	 *            contents of a HTTP POST; set to null to invoke HTTP GET
	 * @param returnOnHTTPError if true, HTTP error response codes (e.g., 400-
	 * 			or 500- series will not result in a thrown exception; returned
	 * 			response object may be examined
	 * @return the response of the invocation attempt
	 * @throws IOException
	 *             on any failure to execute call
	 */
	public static Response invoke(URI invocationURI, InputStream graphContentsStream, boolean returnOnHTTPError) throws IOException {
		return invoke(invocationURI, graphContentsStream, returnOnHTTPError, getInvocationTimeout());
	}
	
	/**
	 * Invokes HTTP GET or POST to URL. Access to the invocation response
	 * content can be gained by calling <code>getContent</code> on the
	 * returned response.
	 * 
	 * @param invocationURI
	 *            URI to invoke (upon successful conversion of URI to a URL)
	 * @param graphContentsStream
	 *            contents of a HTTP POST; set to null to invoke HTTP GET
	 * @param returnOnHTTPError if true, HTTP error response codes (e.g., 400-
	 * 			or 500- series will not result in a thrown exception; returned
	 * 			response object may be examined
	 * @param timeout connect and read timeout in milliseconds
	 * @return the response of the invocation attempt
	 * @throws IOException
	 *             on any failure to execute call
	 */
	public static Response invoke(URI invocationURI, InputStream graphContentsStream, boolean returnOnHTTPError, long timeout) throws IOException {
		HttpURLConnection httpConn = httpConn(invocationURI.toURL());
		if (timeout != -1) {
			httpConn.setConnectTimeout((int) timeout);
			httpConn.setReadTimeout((int) timeout);
		}
		
		if ( graphContentsStream != null ) {
			// create an object that will execute the POST request on the service's URL
			httpConn.setRequestMethod("POST");
			httpConn.setDoOutput(true);
		}
	
		return executeRequest(httpConn, graphContentsStream, returnOnHTTPError);
	}
	
	/**
	 * Casts the given connection to an HTTP connection
	 * 
	 * @throws IllegalArgumentException if the given connection is not an HTTP connection
	 */
	private static HttpURLConnection httpConn(URLConnection conn) throws IllegalArgumentException {
		Preconditions.checkArgument(conn instanceof HttpURLConnection, "Only HTTP or HTTPS are supported: " + conn.getURL());
		
		return (HttpURLConnection) conn;
	}
	
	/**
	 * Creates a new HTTP connection for the given URL.
	 */
	private static HttpURLConnection httpConn(URL url) throws IOException {
		return httpConn(url.openConnection());
	}
	
	/**
	 * Creates a new HTTP connection for the given URL and copies the settings from the given connection and request properties.
	 */
	private static HttpURLConnection httpConn(URL url, HttpURLConnection conn, Map<String,List<String>> props) throws IOException {
		HttpURLConnection httpConn = httpConn(url.openConnection());
		
		httpConn.setRequestMethod(conn.getRequestMethod());
		httpConn.setConnectTimeout(conn.getConnectTimeout());
		httpConn.setReadTimeout(conn.getReadTimeout());
		httpConn.setAllowUserInteraction(conn.getAllowUserInteraction());
		httpConn.setDoInput(conn.getDoInput());
		httpConn.setDoOutput(conn.getDoOutput());
		httpConn.setIfModifiedSince(conn.getIfModifiedSince());
		httpConn.setUseCaches(conn.getUseCaches());
		
		for (Entry<String,List<String>> e : props.entrySet()) {
			String prop = e.getKey();
			for (String val : e.getValue()) {
				httpConn.addRequestProperty(prop, val);
			}
		}
		
		return httpConn;
	}

	/**
	 * Executes the HTTP request associated with the given connection without any input.
	 * 
	 * @param requestConn
	 * @return a response object
	 * @throws IOException if an exception occurs or the HTTP response code indicates an error
	 */
	public static Response executeRequest(URLConnection requestConn) throws IOException {
		return executeRequest(requestConn, null);
	}
	
	/**
	 * Executes the HTTP request associated with the given connection with the given input which may be null.
	 * 
	 * @param requestConn URL connection
	 * @param requestBody input to the request
	 * @return a response object
	 * @throws IOException if an exception occurs or the HTTP response code indicates an error
	 */
	public static Response executeRequest(URLConnection requestConn, InputStream requestBody) throws IOException {
		return executeRequest(requestConn, requestBody, false);
	}
	
	private static int connect(HttpURLConnection httpConn, byte[] inputBytes) throws IOException {
		// send the request
		httpConn.connect();
		
		// if there is any input we need to set it now because we cannot do it after we check 
		// the response code which would get the input stream
		if (inputBytes != null) {
			OutputStream aOut = httpConn.getOutputStream();
			aOut.write(inputBytes);
			aOut.close();			
		}
		
		return httpConn.getResponseCode();
	}
	
	/**
	 * Executes the HTTP request associated with the given connection with the given input which may be null.
	 * 
	 * @param requestConn URL connection
	 * @param requestBody input to the request
	 * @param returnOnHTTPError if false an exception is thrown if the response code indicates an error
	 * @return a response object
	 * @throws IOException if an exception occurs or the HTTP response code indicates an error and returnOnHTTPError is false
	 */
	private static Response executeRequest(URLConnection requestConn, InputStream requestBody, boolean returnOnHTTPError) throws IOException {
		HttpURLConnection httpConn = httpConn(requestConn);
		
		byte[] inputBytes = null;
		if (requestBody != null) {
			inputBytes = ByteStreams.toByteArray(requestBody);
			requestBody.close();			
		}
		
		Map<String,List<String>> requestProps = httpConn.getRequestProperties();
		//httpConn.setInstanceFollowRedirects(false);
		
		try {
			// connect and get the response code
			int responseCode = connect(httpConn, inputBytes);
			
			// counter to keep track of the redirects
			int redirects = 0;
			
			// only follow a 307 redirect on a GET
			boolean onGET = false;
			if ( responseCode == 307 && httpConn.getRequestMethod().equals("GET")  ) {
				onGET = true;
			}
			
			// check for a possible redirects and follow them
			while ( responseCode == 301 || responseCode == 302 || responseCode == 303 || (responseCode == 307 && onGET) ) {
				
				if ( ++redirects > MAX_REDIRECTS ) {
					throw new IOException("Exceeded maximum number of redirects (" + MAX_REDIRECTS + ") from initial call to: " + requestConn.getURL());
				}

				// create a request for the redirect (all redirects are GET requests)
				URL redirectedRequest = new URL(httpConn.getHeaderField("Location"));

				// send the GET request to the redirected location
				try {
					// open a connection to the new location
					httpConn = httpConn(redirectedRequest, httpConn, requestProps);
					// get the new response code
					responseCode = connect(httpConn, inputBytes);
				} catch ( IOException ioe ) {
					throw new IOException("Redirection failure on: " + redirectedRequest);
				}
			}
			
		} catch ( IOException ioe ) {
			
			// close the response (to release connection resources) and rethrow the error
			if ( httpConn != null ) {
				try {
					httpConn.disconnect();
				} catch ( Throwable t ) {
					;	// consume
				}
			}
			
			throw ioe;
		}
		
		Collection<Header> aResponseHeaders = new HashSet<Header>();

		Map<String, List<String>> aHeaderMap = requestConn.getHeaderFields();

		for (Map.Entry<String, List<String>> aEntry : aHeaderMap.entrySet()) {
			aResponseHeaders.add(new Header(aEntry.getKey(), aEntry.getValue()));
		}


		Response response = new Response(httpConn, aResponseHeaders);
		
		// check for errors
		if ( ! returnOnHTTPError && response.hasErrorCode() ) {

			String exceptionMsg;
			Header header = response.getHeader(Vocabulary.SSWAP_HTTP_EXCEPTION_HEADER);

			if ( header != null ) {
				exceptionMsg = header.getRawHeaderValue();
				
				try {
					JSONObject jsonObject = new JSONObject(exceptionMsg);
					
					if (jsonObject.optString(Vocabulary.SSWAP_HTTP_EXCEPTION_HEADER, null) != null) {
						exceptionMsg = jsonObject.getString(Vocabulary.SSWAP_HTTP_EXCEPTION_HEADER);						
					}
				}
				catch (JSONException e) {
					// nothing
				}
			} else if ( response.getResponseCode() < 0 ) {
				exceptionMsg = "The execution of the service returned an invalid HTTP response or provided no response by the time the connection timed out";
			} else {
				exceptionMsg = "The execution of the service returned an HTTP error code: " + response.getResponseCode();
			}

			throw new IOException(exceptionMsg);

		}
		
		return response;

	}
	
	private static int getInvocationTimeout() {
		int invocationTimeout;
		
		try {
			invocationTimeout = Integer.parseInt(Config.get().getProperty(Config.RIG_INVOCATION_TIMEOUT_KEY, Config.RIG_INVOCATION_TIMEOUT_DEFAULT));
		} catch (NumberFormatException e) {
			invocationTimeout = Integer.parseInt(Config.RIG_INVOCATION_TIMEOUT_DEFAULT);
		}
		
		return invocationTimeout;
	}

	/**
	 * Trims the #fragment identifier from a URI string to allow subsequent
	 * matching on the URI string minus any fragment identifier.
	 * <p>
	 * Note: experience shows that query strings (part of the URI following a
	 * '?') are often encountered, so future modifications to this method should
	 * not ignore the query string, if any.
	 * 
	 * @param uriStr
	 *            original full URI string (w/ fragment identifier, if any)
	 * @return URI string minus any fragment identifier; returns null if an
	 *         appropriate URI transformation cannot be constructed
	 */
	public static String normalizeURI(String uriStr) {
		
		String normalizedURIStr;
		
		try {
			URI uri = new URI(uriStr);
	
			if ( uri.getRawFragment() == null ) {
				normalizedURIStr = uriStr;
			} else {
			
				String scheme = uri.getScheme();
				String schemeSpecificPart = uri.getSchemeSpecificPart();
			
				normalizedURIStr = new URI(scheme,schemeSpecificPart,null /* fragment */).toString();
			}
			
		} catch ( Exception e ) {
			normalizedURIStr = null;
		}
		
		return normalizedURIStr;
	}
}
