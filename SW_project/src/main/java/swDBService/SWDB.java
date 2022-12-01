package swDBService;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.RDFDataMgr;

public class SWDB {

	private String queryResult;
	
	
	
	public void searchForResult(String pathDB, String p1, String p2, String p3) {		

	   System.out.println("Do query...");
		
       Model model = RDFDataMgr.loadModel(pathDB) ;
       OntModelSpec ontModelSpec = OntModelSpec.OWL_DL_MEM;
       OntModel ontModel = ModelFactory.createOntologyModel(ontModelSpec, model);

       String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
       		"PREFIX e: <http://example.org/ex>\n" + 
       		"PREFIX b: <http://example.org/ItemOntology>\n" + 
       		"SELECT ?itemId \n" + 
       		"WHERE {  ?item rdf:type b:Item.\n" + 
       		"         ?item b:hasParam01 \""+p1+"\".\n" + 
       		"         ?item b:hasParam02 \""+p2+"\".\n" + 
       		"         ?item b:hasParam03 \""+p3+"\".\n" + 
       		"         ?item b:itemID ?itemId.\n" + 
       		"}";
       
       String queryString_ = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
          		"PREFIX e: <http://example.org/ex>\n" + 
          		"PREFIX b: <http://example.org/ItemOntology>\n" + 
          		"SELECT ?itemId \n" + 
          		"WHERE {  ?item rdf:type b:Item.\n" + 
          		"         ?item b:hasParam01 \""+p1+"\".\n" + 
          		"         ?item b:itemID ?itemId.\n" + 
          		"}";
       
       Dataset dataset = DatasetFactory.create(ontModel);
       Query q = QueryFactory.create(queryString);

       QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
       ResultSet resultSet = qexec.execSelect();
       
       System.out.println("Results: ---");       
       while(resultSet.hasNext()) {
    	   QuerySolution row = (QuerySolution)resultSet.next();
    	   RDFNode nextItemId = row.get("itemId");
    	   System.out.print("ItemID is: "+nextItemId.toString()+".\n"); 
    
       }
       System.out.println("------------");
    	   
    	   
    	   
    	   






	} 	
	
	
	
	
	
	public String getResult(){
		queryResult = "Done!";
		return queryResult;
	}
}
