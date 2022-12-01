/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.modularity;

import info.sswap.api.model.ModelResolver;
import info.sswap.api.model.SSWAPProtocol;
import info.sswap.impl.empire.Vocabulary;
import info.sswap.impl.empire.model.ModelUtils;
import info.sswap.impl.empire.model.ProtocolImpl;
import info.sswap.impl.empire.model.ReasoningServiceImpl;
import info.sswap.impl.empire.model.SourceModel;
import info.sswap.ontologies.modularity.client.HttpMEClient;
import info.sswap.ontologies.modularity.client.ModuleExtractionException;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * @author Pavel Klinov
 *
 */
public class ModularityModelResolver implements ModelResolver {

	private static final Logger LOGGER = LogManager.getLogger(ModularityModelResolver.class);
	
	private boolean mDerefUnknownURIs = false;
	
	public ModularityModelResolver() {}
	
	public ModularityModelResolver(boolean derefUnknownURIs) {
		mDerefUnknownURIs = derefUnknownURIs;
	}
	
	@Override
	public Model resolveSourceModel(SourceModel model) {
		// Do nothing if we don't know what kind of model it is
		return model.getModel();
	}

	@Override
	public Model resolveProtocolModel(SSWAPProtocol protocol) {
		ProtocolImpl graph = (ProtocolImpl) protocol;
		/*
		 * The difference between RDG and other graphs is
		 * that for an RDG we compute a Top module for the subject type and Bot modules for
		 * resource and the object types while for other graph types it's the other way around
		 * 
		 * TODO Reduce the copy/paste
		 */
		LOGGER.debug("Resolving " + graph.getGraphType());
		
		if ("RDG".equals(graph.getGraphType())) {
			return resolveRDG(graph);
		}
		else {
			return resolveGraph(graph);
		}
	}

	private Model resolveGraph(ProtocolImpl graph) {
		HttpMEClient client = new HttpMEClient();
		Collection<URI> sigForTopME = getResourceSignature(graph.getSourceModel().getModel());
		Collection<URI> sigForBotME = getSubjectSignature(graph.getSourceModel().getModel());
		Model resolvedModel = ModelFactory.createDefaultModel();
		
		//resolvedModel.add(graph.getSourceModel().getModel());
		sigForBotME.addAll(getObjectSignature(graph.getSourceModel().getModel()));
		
		if (sigForBotME.isEmpty() && sigForBotME.isEmpty()) {
			//TODO Compute the import closure?		
			return graph.getSourceModel().getModel();
		}
		
		try {
			Model botModule = client.extract(sigForBotME, ModuleType.BOT, true, mDerefUnknownURIs);
			Model topModule = client.extract(sigForTopME, ModuleType.BOT, true, mDerefUnknownURIs);//TODO Do we really need TOP modules?
			Model main = topModule.union(botModule);

			resolvedModel.add(main);
			
			return resolvedModel;
			
		} catch (Throwable e) {
			// TODO handle it in some reasonable way
			return graph.getSourceModel().getModel();
		}
	}

	private Model resolveRDG(ProtocolImpl graph) {
		HttpMEClient client = new HttpMEClient();
		Collection<URI> sigForBotME = getResourceSignature(graph.getSourceModel().getModel());
		Collection<URI> sigForTopME = getSubjectSignature(graph.getSourceModel().getModel());
		Model resolvedModel = ModelFactory.createDefaultModel();
		
		//resolvedModel.add(graph.getSourceModel().getModel());
		sigForBotME.addAll(getObjectSignature(graph.getSourceModel().getModel()));
		
		if (sigForBotME.isEmpty() && sigForBotME.isEmpty()) {
			//TODO Compute the import closure?		
			return graph.getSourceModel().getModel();
		}
		
		try {
			Model botModule = client.extract(sigForBotME, ModuleType.BOT, true, mDerefUnknownURIs);
			Model topModule = client.extract(sigForTopME, ModuleType.BOT, true, mDerefUnknownURIs);//TODO Do we really need TOP modules?
			Model main = topModule.union(botModule);

			resolvedModel.add(main);
			
			return resolvedModel;
			
		} catch (ModuleExtractionException e) { 
			// TODO handle it in some reasonable way
			return graph.getSourceModel().getModel();
		}
	}

	@Override
	public Model resolveTerm(URI termURI) {
		try {
			return new HttpMEClient().resolveTerm(termURI, null);
		} catch (ModuleExtractionException e) {
			return ModelFactory.createDefaultModel();
		}
	}	
	
	/**
	 * 
	 * @param entityType sswap:Subject, sswap:Predicate, sswap:Resource, or sswap:Subject
	 * @return The set of all RDF nodes standing for (potentially complex) types used to described individuals of the given entity type
	 */
	public Collection<URI> getSignatureForSSWAPEntity(Resource entityType, Model sourceModel) {
		Collection<Resource> entities = Sets.newHashSet();
		StmtIterator iter = sourceModel.listStatements(null, RDF.type, entityType);
		Collection<URI> types = Sets.newHashSet();
		
		while (iter.hasNext()) {
			entities.add(iter.next().getSubject());
		}
		
		for (Resource entity : entities) {
			// TODO This seems slow. Can we request triples for a set of subjects in Jena?
			iter = sourceModel.listStatements(entity, null, (Resource) null);
			
			while (iter.hasNext()) {
				Statement stmt = iter.next();
				
				if (!belongs2RDForOWLVocabulary(stmt.getPredicate().getURI()) /*&& !isSSWAPTerm(stmt.getPredicate().getURI())*/) {
					types.add(URI.create(stmt.getPredicate().getURI()));
				}
				//TODO It'd be good to not add individuals here
				if (stmt.getObject().isURIResource() /*&& !isSSWAPTerm(stmt.getObject().asResource().getURI())*/) {
					types.add(URI.create(stmt.getObject().asResource().getURI()));
				}
			}
		}
		
		return types;
	}	

	public Collection<URI> getResourceSignature(Model sourceModel) {
		Collection<URI> terms = getSignatureForSSWAPEntity(Vocabulary.SSWAP_RESOURCE, sourceModel);
		Collection<URI> result = Sets.newHashSetWithExpectedSize(terms.size());
		
		for (URI term : terms) {
			result.addAll(extractTypeSignature(term, sourceModel));
		}
		
		return result;
	}
	
	public Collection<URI> getSubjectSignature(Model sourceModel) {
		Collection<URI> terms = getSignatureForSSWAPEntity(Vocabulary.SSWAP_SUBJECT, sourceModel);
		Collection<URI> result = Sets.newHashSetWithExpectedSize(terms.size());
		
		for (URI term : terms) {
			result.addAll(extractTypeSignature(term, sourceModel));
		}
		
		return result;
	}
	
	public Collection<URI> getObjectSignature(Model sourceModel) {
		Collection<URI> terms = getSignatureForSSWAPEntity(Vocabulary.SSWAP_OBJECT, sourceModel);
		Collection<URI> result = Sets.newHashSetWithExpectedSize(terms.size());
		
		for (URI term : terms) {
			result.addAll(extractTypeSignature(term, sourceModel));
		}
		
		return result;
	}

	public Collection<URI> extractTypeSignature(URI type, Model sourceModel) {
		if (!ModelUtils.isBNodeURI(type.toString())) {
			return Collections.singletonList(type);
		} else {
			//Anonymous type, so "we need to go deeper" (c)
			Resource typeRes = sourceModel.getResource(type.toString());

			if (typeRes == null) {
				return Collections.emptyList();
			} else {
				Collection<URI> termPairs = Sets.newHashSet();
				// TODO
				// Account for rdfs:isDefinedBy
				for (Statement stmt : ReasoningServiceImpl.extractTypeDefinition(sourceModel, typeRes)) {
					if (stmt.getSubject().isURIResource()) {
						addTermToSignature(termPairs, stmt.getSubject().getURI());
					}
					if (stmt.getPredicate().isURIResource()) {
						addTermToSignature(termPairs, stmt.getPredicate().getURI());
					}
					if (stmt.getObject().isURIResource()) {
						addTermToSignature(termPairs, stmt.getObject().asResource().getURI());
					}
				}

				return termPairs;
			}
		}
	}
	
	private void addTermToSignature(Collection<URI> terms, String uri) {
		if (!belongs2RDForOWLVocabulary(uri)) {
			terms.add(URI.create(uri));
		}
	}

	private boolean belongs2RDForOWLVocabulary(String term) {
		return OWLRDFVocabulary.BUILT_IN_VOCABULARY_IRIS.contains(IRI.create(term));
	}		
}
