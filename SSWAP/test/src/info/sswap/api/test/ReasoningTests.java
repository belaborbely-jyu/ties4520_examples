/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPDocument;
import info.sswap.api.model.SSWAPType;
import info.sswap.impl.empire.model.ReasoningServiceImpl;
import info.sswap.impl.empire.model.SourceModel;
import info.sswap.impl.empire.model.SourceModelImpl;

import java.net.URI;

import org.junit.Test;
import org.mindswap.pellet.KnowledgeBase;

import com.clarkparsia.pellet.utils.TermFactory;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Tests for SSWAPReasoningService
 * @author Evren Sirin <evren@clarkparsia.com>
 *
 */
public class ReasoningTests {
	private static final String NS = "tag:sswap.info,2011-01-31:sswap:java:api:ReasoningTest#";
	
	private static class MockSourceModel extends SourceModelImpl {		
		private Model closureModel;
		
		private MockSourceModel(Model model, Model closureModel) {
			setModel(model);
			
			this.closureModel = closureModel;
		}
		
		@Override
		public void setRdfId(RdfKey key) {			
		}
		
		@Override
		public RdfKey getRdfId() {
			return null;
		}
		
		@Override
		public Model getClosureModel() {
			return closureModel;
		}
	}
	
	@Test
	public void recursivelyDependentModels() {
		OntModel model  = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		OntModel closure  = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		
		Property p1 = ResourceFactory.createProperty(NS+"p1");
		Resource C1 = ResourceFactory.createResource(NS+"C1");
		Property p2 = ResourceFactory.createProperty(NS+"p2");
		Resource C2 = ResourceFactory.createResource(NS+"C2");
		
		// model declares p1, uses p2 without declaration
		model.add(p1, RDF.type, OWL.DatatypeProperty);		
		model.add(C1, RDFS.subClassOf, model.createMinCardinalityRestriction(null, p2, 1));

		// model declares p2, uses p1 without declaration
		closure.add(p2, RDF.type, OWL.DatatypeProperty);
		closure.add(C2, RDFS.subClassOf, closure.createMinCardinalityRestriction(null, p1, 1));
		
		SourceModel sourceModel = new MockSourceModel(model, closure);

		ReasoningServiceImpl reasoner = new ReasoningServiceImpl(sourceModel);
		KnowledgeBase kb = reasoner.getPelletKB();
		
		// make sure both p1 and p2 are correctly identified as datatype properties
		assertTrue(kb.isDatatypeProperty(TermFactory.term(p1.toString())));
		assertTrue(kb.isDatatypeProperty(TermFactory.term(p2.toString())));
		
		
		// add a new model to the reasoner
		SSWAPDocument doc = SSWAP.createSSWAPDocument();
		SSWAPType A = doc.getType(URI.create(NS+ "A"));
		SSWAPType B = doc.getType(URI.create(NS+ "A"));
		A.addSubClassOf(B);
		
		reasoner.addModel(doc);
		
		// check if the inferences from new model is found 
		assertTrue(reasoner.getSuperClasses(A.getURI().toString()).contains(A.getURI().toString()));
		// make sure both p1 and p2 are still datatype properties after model addition
		assertTrue(kb.isDatatypeProperty(TermFactory.term(p1.toString())));
		assertTrue(kb.isDatatypeProperty(TermFactory.term(p2.toString())));
		
		// remove the model from the reasoner
		reasoner.removeModel(doc);
		
		// the inferences from the model is gone 
		assertFalse(reasoner.getSuperClasses(C1.getURI().toString()).contains(C2.getURI().toString()));
		// p1 and p2 are still datatype properties
		assertTrue(kb.isDatatypeProperty(TermFactory.term(p1.toString())));
		assertTrue(kb.isDatatypeProperty(TermFactory.term(p2.toString())));

		//add a statement directly to the model
		sourceModel.getType(URI.create(NS+"C1")).addSubClassOf(sourceModel.getType(URI.create(NS+"C2")));
		
		// check the inference is found 
		assertTrue(reasoner.getSuperClasses(C1.getURI().toString()).contains(C2.getURI().toString()));
		// p1 and p2 are still datatype properties
		assertTrue(kb.isDatatypeProperty(TermFactory.term(p1.toString())));
		assertTrue(kb.isDatatypeProperty(TermFactory.term(p2.toString())));
	}
}
