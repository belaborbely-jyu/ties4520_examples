/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.Config;
import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.RDFRepresentation;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RQG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.SSWAPType;
import info.sswap.api.model.ValidationException;
import info.sswap.impl.empire.Namespaces;
import info.sswap.impl.empire.model.ProtocolImpl.MappingValidator.MappingType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import com.clarkparsia.utils.BasicUtils;
import com.clarkparsia.utils.web.Method;
import com.clarkparsia.utils.web.Request;
import com.clarkparsia.utils.web.Response;
import com.google.common.io.ByteStreams;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Implementation of RQG interface
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public abstract class RQGImpl extends ProtocolImpl implements RQG {
	
	/**
	 * The width of the indentation in the generated SPARQL query.
	 */
	private static final int QUERY_INDENT = 2;
	
	private boolean resourceValidationEnabled = true;
	
	/**
	 * @inheritDoc
	 */
	public String getQuery() {
		StringBuffer result = new StringBuffer();
		
		SSWAPResource resource = getResource();
		
		// set up prefixes for the query
		result.append(Namespaces.SPARQL_NS_PREFIXES);
				
		result.append("SELECT DISTINCT ?rdg\n");
		result.append("WHERE {\n");
		
		// generate restrictions on sswap:Resource (?rdg)
		result.append(generateSubClassRestriction(1, "?rdg", resource));
		
		// define query variables and properties that connect sswap:Resource (?rdg) with sswap:Graph (?graph)
		// and sswap:Subject (?subject)
		result.append(indent(1)).append("?rdg sswap:operatesOn ?graph .\n");
		result.append(indent(1)).append("?graph sswap:hasMapping ?subject .\n");
		
		// generate restrictions on sswap:Subjects
		SSWAPSubject subject = resource.getGraph().getSubject();
		result.append(generateSubjectRestriction(1, "?subject", subject));
		
		// define query variable for sswap:Object (?object) and the connecting property
		// (sswap:mapsTo)
		result.append(indent(1)).append("?subject sswap:mapsTo ?object .\n");
		
		SSWAPObject object = subject.getObject();
		
		// generate restrictions on sswap:Object
		result.append(generateSubClassRestriction(1, "?object", object));
		
		// in case the RQG's resource is an anonymous node, make sure that the RQG's resource is not returned
		// (since the RQG is included in the triple store over which the reasoning is performed, and obviously
		// RQG matches itself ...)
		if (resource.getURI() != null && ModelUtils.isBNodeURI(resource.getURI().toString())) {
			result.append(indent(1)).append("FILTER (?rdg != <" + resource.getURI().toString() + ">)\n");
		}
		
		result.append(generateRegexRestriction("?rdg", 1, "sswap:name", "?name", resource.getName()));
		result.append(generateRegexRestriction("?rdg", 1, "sswap:oneLineDescription", "?oneLineDescription", resource.getOneLineDescription()));
		//result.append(generateRestriction("?rdg", 1, "sswap:aboutURI", "?aboutURI", resource.getAboutURI()));
		//result.append(generateRestriction("?rdg", 1, "sswap:inputURI", "?inputURI", resource.getInputURI()));
		//result.append(generateRestriction("?rdg", 1, "sswap:outputURI", "?outputURI", resource.getOutputURI()));
		//result.append(generateRestriction("?rdg", 1, "sswap:metadata", "?metadata", resource.getMetadata()));
		
		if (resource.getProvider() != null) {
			result.append(generateRestriction("?rdg", 1, "sswap:providedBy", "?providedBy", resource.getProvider().getURI()));
		}
		
		result.append("}");
		
		return result.toString();
	}
	
	private String querySafe(String value) {
		return value.replaceAll("\n", "").replaceAll("\"", "");
	}
	
	private String generateRestriction(String varName, int indent, String propertyName, String propertyValueVar, Object propertyValueRestriction) {		
		if (propertyValueRestriction != null) {
			StringBuffer result = new StringBuffer();
			
			result.append(indent(indent)).append(varName).append(" ").append(propertyName).append(" ").append(propertyValueVar).append(" .\n");
			result.append(indent(indent)).append("FILTER (").append(propertyValueVar).append(" = ").append(" \"").append(querySafe(propertyValueRestriction.toString())).append("\")\n");
			
			return result.toString();
		} 
		else {
			return "";
		}
	}
	
	private String generateRegexRestriction(String varName, int indent, String propertyName, String propertyValueVar, Object propertyValueRestriction) {		
		if (propertyValueRestriction != null) {
			StringBuffer result = new StringBuffer();
			
			result.append(indent(indent)).append(varName).append(" ").append(propertyName).append(" ").append(propertyValueVar).append(" .\n");
			result.append(indent(indent)).append("FILTER (regex(").append(propertyValueVar).append(", \"").append(querySafe(propertyValueRestriction.toString())).append("\"))\n");
			
			return result.toString();
		} 
		else {
			return "";
		}
	}
	
	/**
	 * @inheritDoc
	 */
	public Collection<String> executeQuery(Model... models) {
		// set up the execution environment
		
		// initialize the reasoning and turn off import processing (the computed closure should have
		// included all the imported elements already).
		OntModel ontModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
		ontModel.getDocumentManager().setProcessImports(false);
		
		// include the RQG's model and its closure in the execution environment for the query
		ontModel.addSubModel(getModel());
		ontModel.addSubModel(getClosureModel());
		
		// add the models passed by the caller to the execution environment
		for (Model model : models) {
			ontModel.addSubModel(model);	
		}
	
		// Query execution and gathering results
		
		// Will contain the results
		List<String> result = new LinkedList<String>();
		
		// prepare the query and QueryExecution object
		Query query = QueryFactory.create(getQuery());		
		QueryExecution queryExecution = QueryExecutionFactory.create(query, ontModel);
		
		// execute the query
		ResultSet rs = queryExecution.execSelect();
		
		// iterate over the results and extract the URIs of matching RDGs from the ?rdg variable
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			result.add(qs.getResource("rdg").getURI());
		}
		
		return result;
	}
	
	/**
	 * Utility method that generates the desired indentation level (consisting of spaces). 
	 * 
	 * @param level the indentation level
	 * @return the spaces to create the desired indentation level
	 */
	private static String indent(int level) {
		return BasicUtils.repeat(' ', QUERY_INDENT * level);
	}
	
	/**
	 * Creates the part of the query that matches the RQG's subject with RDGs' subjects. The intention is that for any
	 * RDG matched, the subject's type of that RDG is a superclass of the RQG's subject's type.
	 * 
	 * Since the query is executed against a model with inference results, for every instance you can obtain all of their
	 * types (including inferred types) by querying the model for the instance's rdf:type property. When this information is
	 * present, it is possible to rephrase the above matching rule (RDGs subject type must be a superclass of the RQGs 
	 * subject type) as: the set of types for the RDG's subject instance must be a subset of the set of types for the RQG's subject.
	 * 
	 * The query generated by this method takes advantage of that reformulated matching rule; that is, it tries to determine 
	 * for a subject whether it contains any rdf:type statements that do not appear in the set of types for the RQG's subject. 
	 * Next, it filters all the subjects that had any such rdf:type statements (i.e., the set of types for the RDG's subject
	 * is not a subset of the types of RQG's subject).
	 * 
	 * @param indentLevel the base indentation level for the generated restriction (so that it lines up with other conditions
	 * on the same logical level)
	 * @param var the variable used to bind the subject in the encompassing query
	 * @param subject the SSWAPSubject for the RQG
	 * @return the part of the query that contains the restriction on the subject
	 */
	private String generateSubjectRestriction(int indentLevel, String var, SSWAPSubject subject) {
		StringBuffer result = new StringBuffer();
		
		// the code below uses the negation-by-failure technique (as described in http://www.w3.org/TR/sparql-features )
		
		// the combination of OPTIONAL statement and the FILTER (!BOUND(?var)) effectively selects all the instances
		// that do not match the statements listed in the OPTIONAL pattern (i.e., if these statements hold, the variable in
		// the filter condition becomes bound, and the current binding becomes filtered).
		
		// the query fragment below uses a two-level negation. The most nested OPTIONAL pattern will match all ?subject rdf:type
		// statements that do not exist in the RQG's subject (or more exactly, it will filter out all the ?subject rdf:type 
		// statements that exist in the RQG's subject, effectively leaving only the ones that do not exist). Since 
		// negation-by-failure depends on existence of a variable whose binding (or lack of thereof) determines the truth
		// of the condition, the most nested OPTIONAL pattern contains ?anonVar for that purpose.
		
		// The outer OPTIONAL pattern filters out all the subjects that were matched by the inner OPTIONAL pattern (i.e.,
		// they had types that do not belong to RQG's subject)
		
		result.append(indent(indentLevel)).append("OPTIONAL {\n");
		result.append(indent(indentLevel + 1)).append(var).append(" rdf:type ?subjectType .\n");
		result.append(indent(indentLevel + 1)).append("OPTIONAL {\n");
		result.append(indent(indentLevel + 2)).append("<").append(subject.getURI().toString()).append("> rdf:type ?subjectType .\n");
		result.append(indent(indentLevel + 2)).append("<").append(subject.getURI().toString()).append("> rdf:type ?anonVar .\n");
		result.append(indent(indentLevel + 1)).append("}\n");
		result.append(indent(indentLevel + 1)).append("FILTER (!BOUND(?anonVar))\n");
		result.append(indent(indentLevel)).append("}\n");
		result.append(indent(indentLevel)).append("FILTER (!BOUND(?subjectType))\n");
		
		return result.toString();
	}
	
	/**
	 * Generates a restriction that the type of matched individuals is a subclass of the types of the specified individual   
	 * 
	 * @param indentLevel the base indentation level of the generated restriction (so that it lines up with other parts
	 * of the query at the same logical level of nesting)
	 * @param var the variable used to match the individual
	 * @param individual the individual whose types should be included in the restriction
	 * @return the part of the query that contains the restriction
	 */
	private String generateSubClassRestriction(int indentLevel, String var, SSWAPIndividual individual) {
		StringBuffer result = new StringBuffer();
		
		// since the query is executed against a model with inferences, it just adds a "?var rdf:type" statement
		// for each type of the individual (if any individual belongs to a type that is a subclass, it will
		// also have the inferred rdf:type for all the superclasses)
		
		for (SSWAPType individualType : individual.getDeclaredTypes()) {			
			result.append(indent(indentLevel));
			result.append(var).append(" rdf:type <").append(individualType.getURI()).append("> .\n");
		}
		
		return result.toString();
	}
	
	public Collection<RDG> invoke() throws IOException, DataAccessException {
		return invoke(null /* discoveryServerURIString */);
	}
		
	public Collection<RDG> invoke(URI discoveryServerQueryEndpoint) throws IOException, DataAccessException {
		List<RDG> results = new LinkedList<RDG>();
		
		// TODO do not submit the resource's URI/quote it
		
		URI discoveryServerQueryURI = null;
		
		if (discoveryServerQueryEndpoint != null) {
			discoveryServerQueryURI = discoveryServerQueryEndpoint;
		}
		else {
			try {
	            discoveryServerQueryURI = new URI(getDefaultDiscoveryServerQueryURL());
            }
            catch (URISyntaxException e) {
            	throw new IOException("The default URI of the Discovery Server is not a valid URI", e);
            }
		}
		
		// serialize RIG
		ByteArrayOutputStream bos = new ByteArrayOutputStream();		
		serialize(bos, RDFRepresentation.RDF_XML, false /* commentedOutput */);		
			
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		
		Request request = new Request(Method.POST, discoveryServerQueryURI.toURL());
		request.setBody(bis);
		request.addHeader("Content-Type", "application/rdf+xml");
		
		// invoke and read results into an RRG				
		Response response = null;
		
		try {
			response = request.execute();
		
			String responseJson = new String(ByteStreams.toByteArray(response.getContent()));
			
			JSONArray responseArray = new JSONArray(responseJson);
			
			for (int i = 0; i < responseArray.length(); i++) {
				try {
					results.add(SSWAP.getRDG(new URI(responseArray.getString(i))));
				}
				catch (DataAccessException e) {
					// ignored -- just proceed to the next result
				}
			}
		}
		catch (JSONException e) {
			throw new DataAccessException("Received a malformed JSON response from the Discovery Server", e);
		}
        catch (URISyntaxException e) {
        	throw new DataAccessException("Received a malformed response from the Discovery Server (URI of one of the results is not valid)", e);
        } finally {
			if ( response != null ) {
				response.close();
			}
		}
		
		return results;
	}
	
	private String getDefaultDiscoveryServerQueryURL() {
		return System.getProperty(Config.DISCOVERY_SERVER_QUERY_URI_ALT_KEY, Config.DISCOVERY_SERVER_QUERY_URI_DEFAULT);
	}
	
	public String getGraphType() {
		return "RQG";
	}
	
	@Override
	protected boolean needsClosedWorldForValidation() {
		return true;
	}
	
	public boolean satisfiesResource(RDG rdg) {
		try {			
			validateAgainstRDG(rdg);
		}
		catch (ValidationException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean validatesResourceURIMatch() {
		return false;
	}
	
	@Override
	public boolean needsDefaultParametersSet() {
		// RQGs do not need default values of parameters
		return false;
	}
	
	@Override
	protected MappingValidator<SSWAPResource> getResourceMappingValidator() {
		return (isResourceValidationEnabled())? new DefaultMappingValidator<SSWAPResource>(MappingType.SUPER) 
						                      : new DefaultMappingValidator<SSWAPResource>(MappingType.ANY);
	}
	
	public void setResourceValidationEnabled(boolean resourceValidationEnabled) {
		this.resourceValidationEnabled = resourceValidationEnabled;
	}
	
	public boolean isResourceValidationEnabled() {
		return resourceValidationEnabled;
	}
	
	@Override
	protected MappingValidator<SSWAPSubject> getSubjectMappingValidator() {
		return new DefaultMappingValidator<SSWAPSubject>(MappingType.SUB_IF_NOT_TOP);
	}

	@Override
	protected MappingValidator<SSWAPObject> getObjectMappingValidator() {
		return new DefaultMappingValidator<SSWAPObject>(MappingType.SUPER);
	}	
}
