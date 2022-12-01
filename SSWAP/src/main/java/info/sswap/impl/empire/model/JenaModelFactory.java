/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.RDFRepresentation;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFErrorHandler;
import com.hp.hpl.jena.rdf.model.RDFReader;

/**
 * A factory that reads and creates Jena models in this implementation. It ensures
 * that all models are read and initialized in the same way.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class JenaModelFactory {
	
	private JenaModelFactory() {
		
		// config a custom TSV (tab separated value) writer (for all/any models across the system)
		ModelFactory.createDefaultModel().setWriterClassName(RDFRepresentation.TSV.name(),RDFTSVWriter.class.getName());
		
	}
	
	/**
	 * A singleton instance
	 */
	private static JenaModelFactory instance = new JenaModelFactory();
	
	/**
	 * The getter for the singleton instance
	 * @return sole instance of the JenaModelFactory
	 */
	public static JenaModelFactory get() {
		return instance;
	}
	
	/**
	 * Gets a reader for the model. For maximum performance, the reader
	 * is set to ignore redefinition of identifiers.
	 * 
	 * @param m the model for which the reader should be created
	 * @return the reader
	 */
	private RDFReader getReader(Model m, String format) {
		RDFReader reader = m.getReader(format);

		if ("RDF/XML".equals(format)) {
			// change a few of Jena's default settings (or settings of the components used internally by Jena; 
			// e.g., Xerces parser)

			// prevent the parser from retrieving any external DTD (normally this won't happen for parsing
			// any valid RDF/XML document, but can happen if the reader is erroneously fed e.g., an HTML document,
			// which frequently contains a DOCTYPE statement pointing to a DTD hosted at W3C's servers;
			// in such a case Xerces will try to connect to these servers (they are very slow), which will stall
			// the whole parsing process without an easy/practical way to terminate it/put time limit onto it)
			reader.setProperty("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

			// stop warnings about redefinitions of an ID -- this increases the parsing speed considerably
			reader.setProperty("WARN_REDEFINITION_OF_ID", "EM_IGNORE");
		}
		
		return reader;
	}
		
	/**
	 * Creates an empty model
	 * 
	 * @return the empty model
	 */
	public Model createEmptyModel() {
		return ModelFactory.createDefaultModel();
	}
	
	/**
	 * Gets a model read from an input stream
	 * 
	 * @param is the input stream from which the model should be read
	 * @return the read model
	 */
	/**
	 * Gets a model read from an input stream
	 * 
	 * @param is the input stream from which the model should be read
	 * @return the read model
	 */
	public Model getModel(InputStream is) throws DataAccessException {
		return getModel(is, "RDF/XML");
	}
	
	public Model getModel(InputStream is, String format) throws DataAccessException {
		Model result = createEmptyModel();
		RDFReader reader = getReader(result, format);
		ErrorHandler errorHandler = new ErrorHandler();
		
		reader.setErrorHandler(errorHandler);
		
		try {
			reader.read(result, is, "");
		}
		catch (NullPointerException e) {
			// Jena readers turn out to throw sometimes NPEs when confronted with a severely malformed input
			// (e.g., when input stream contains HTML and not RDF/XML)
			throw new DataAccessException("Problem while reading data from the underlying stream (is the data RDF/XML?)");
		}
		
		if (errorHandler.wereErrors()) {
			Exception underlyingCause = null;
			
			if (!errorHandler.fatalErrors.isEmpty()) {
				underlyingCause = errorHandler.fatalErrors.iterator().next();
			} else if (!errorHandler.errors.isEmpty()) {
				underlyingCause = errorHandler.errors.iterator().next();
			}
			
			throw new DataAccessException("Problem while reading RDF data from the underlying stream", underlyingCause);
		}
		try {
			ModelUtils singleton = new ModelUtils();
			try {
				ModelUtils.removeBNodes(result);
			} catch (Throwable ex2){
				ex2.printStackTrace();
			}
		} catch (Throwable ex){
			ex.printStackTrace();
		}


		return result;
	}
	
	/**
	 * An internal error handler for errors encountered while parsing the model 
	 * 
	 * @author Blazej Bulka <blazej@clarkparsia.com>
	 *
	 */
	private static class ErrorHandler implements RDFErrorHandler {
		private List<Exception> errors = new LinkedList<Exception>();
		private List<Exception> fatalErrors = new LinkedList<Exception>();
		private List<Exception> warnings = new LinkedList<Exception>();
		
		/**
		 * Method used by Jena to report that an error occurred
		 * 
		 * @param ex the exception
		 */
        public void error(Exception ex) {
	        errors.add(ex);	        
        }

        /**
         * Method used by jena to report that a fatal error occurred
         * 
         * @param ex the exception
         */
        public void fatalError(Exception ex) {
        	fatalErrors.add(ex);
	    }

        /**
         * Method used by Jena to report that a warning occurred
         * 
         * @param ex the exception
         */
        public void warning(Exception ex) {
			warnings.add(ex);	        
        }
        
        /**
         * Checks whether there were any errors (either regular or fatal).
         * 
         * @return true if there were any errors
         */
        public boolean wereErrors() {
        	return !errors.isEmpty() || !fatalErrors.isEmpty(); 
        }
	}
}
