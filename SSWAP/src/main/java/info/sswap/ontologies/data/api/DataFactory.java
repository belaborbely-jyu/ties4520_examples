/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.api;

import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.ontologies.data.impl.AttachmentImpl;
import info.sswap.ontologies.data.impl.DirectoryImpl;
import info.sswap.ontologies.data.impl.HTTPBasicAuthImpl;
import info.sswap.ontologies.data.impl.LiteralData;
import info.sswap.ontologies.data.impl.ResourceData;
import info.sswap.ontologies.sswapmeet.SSWAPMeet;

/**
 * Main entry point for support for the data ontology. Use this factory to
 * create a data object to allow reading/writing of literal or resource data,
 * with optional "attachment" (literal data base64 encoding and decoding) or
 * HTTP Basic authentication.
 * 
 * @author Damian Gessler
 * 
 */
public class DataFactory {
	
	/**
	 * Directives on how to open data elements for reading and writing.
	 */
	public enum Open {
		/**
		 * Attempt to open for literal data; if there is no
		 * <code>data:literalData</code> property on the individual, then
		 * attempt to open the individual resource itself for reading/writing.
		 * (an anonymous individuals or individuals with non-URL URI may be
		 * opened, but they will need an accessor for a successful read/write).
		 * Resources used for reading only are never changed; resources used for
		 * reading that do not belong to the upper ontology type
		 * <code>data:DataFormat</code> always succeed immediately on a trivial
		 * read of empty data. Resources used for writing may be changed to type
		 * <code>data:DataFormat</code> with the addition of the literal data
		 * property <code>data:literalData</code> if and only if required for a
		 * successful write. This is suitable for reading or writing to a data
		 * source or sink such as SSWAPSubject or SSWAPObject.
		 */
		AUTO,
		
		/**
		 * Attempt to open for literal data only; throw on failure (no
		 * <code>data:literalData</code> property on the individual)
		 */
		LITERAL,
		
		/**
		 * Attempt to open for resource data only; throw on failure (an
		 * anonymous node or non-URL URI)
		 */
		RESOURCE
	};
	
	/**
	 * Open with automatic support for literal or resource data, including
	 * automatic support for base64 encoded attachments.
	 * 
	 * @param sswapIndividual
	 *            the individual from which to read the data
	 * @return Data a data object to allow reading and writing
	 * @throws DataException
	 *             on any error establishing data support
	 * 
	 * @see LiteralData
	 */
	public static Data Data(SSWAPIndividual sswapIndividual) throws DataException {
		return constructor(sswapIndividual, Open.AUTO, true /* attachment */, false /* httpAuth */, null, null);
	}
	
	/**
	 * Open with automatic support for base64 encoded attachments.
	 * 
	 * @param sswapIndividual
	 *            the individual from which to read the data
	 * @param state directive on how to handle the data source/sink
	 * @return Data a data object to allow reading and writing
	 * @throws DataException on any error establishing data support
	 * 
	 * @see LiteralData
	 */
	public static Data Data(SSWAPIndividual sswapIndividual, Open state) throws DataException {
		return constructor(sswapIndividual, state, true /* attachment */, false /* httpAuth */, null, null);
	}

	/**
	 * Open with automatic support for base64 encoded attachments or HTTP Basic
	 * Authentication.
	 * 
	 * @param sswapIndividual
	 *            the individual from which to read the data
	 * @param state directive on how to handle the data source/sink
	 * @param username
	 *            User name as defined for HTTP Basic Authentication
	 * @param passwd
	 *            Password as defined for HTTP Basic Authentication
	 * @return Data a data object to allow reading and writing
	 * @throws DataException on any error establishing data support
	 * 
	 * @see LiteralData
	 * @see ResourceData
	 */
	public static Data Data(SSWAPIndividual sswapIndividual, Open state, String username, String passwd) throws DataException {
		return constructor(sswapIndividual, state, true /* attachment */, true /* httpAuth */, username, passwd);
	}

	/**
	 * Raw data (no base64 encoding or decoding).
	 * 
	 * @param sswapIndividual
	 *            the individual from which to read the data
	 * @param state directive on how to handle the data source/sink
	 * @return Data a data object to allow reading and writing
	 * @throws DataException on any error establishing data support
	 * 
	 * @see LiteralData
	 * @see ResourceData
	 */
	public static Data RawData(SSWAPIndividual sswapIndividual, Open state) throws DataException {
		return constructor(sswapIndividual, state, false /* attachment */, false /* httpAuth */, null, null);
	}

	/**
	 * Raw data (no base64 encoding or decoding) with HTTP Basic Authentication.
	 * 
	 * @param sswapIndividual
	 *            the individual from which to read the data
	 * @param state directive on how to handle the data source/sink
	 * @param username
	 *            User name as defined for HTTP Basic Authentication
	 * @param passwd
	 *            Password as defined for HTTP Basic Authentication
	 * @return Data a data object to allow reading and writing
	 * @throws DataException on any error establishing data support
	 * 
	 * @see ResourceData
	 */
	public static Data RawData(SSWAPIndividual sswapIndividual, Open state, String username, String passwd) throws DataException {
		return constructor(sswapIndividual, state, false /* attachment */, true /* httpAuth */, username, passwd);
	}


	private static Data constructor(SSWAPIndividual sswapIndividual, Open state, boolean attachment, boolean httpAuth, String username, String passwd) {
	
		boolean hasLiteralData = hasLiteralData(sswapIndividual);
	
		if ( state == Open.LITERAL ) {
			if ( hasLiteralData ) {
				return attachment ? new AttachmentImpl(sswapIndividual) : new LiteralData(sswapIndividual);
			}
			throw new DataException("Cannot open Literal data: individual does not have a data:literalData property");

		} else if ( state == Open.RESOURCE ) {
			return httpAuth ? new HTTPBasicAuthImpl(sswapIndividual, username, passwd) : new ResourceData(sswapIndividual);
					
		} else if ( state == Open.AUTO ) {
			
			if ( hasLiteralData ) {
				return attachment ? new AttachmentImpl(sswapIndividual) : new LiteralData(sswapIndividual);
			} else {
				return httpAuth ? new HTTPBasicAuthImpl(sswapIndividual, username, passwd) : new ResourceData(sswapIndividual);
			}
			
		} else {
			throw new DataException("Internal error: invalid Open field directive");
		}
	
	}
	
	/**
	 * Create a new {@code Directory} from the subject individual.
	 * 
	 * @param sswapIndividual
	 *            Individual for which to inspect for {@code data:hasData}
	 *            properties.
	 * @return a {@code Directory} of {@link Data}.
	 * @throws DataException upon {@link DataFactory#Data(SSWAPIndividual)} failure
	 */
	public static Directory Directory(SSWAPIndividual sswapIndividual) throws DataException {
		return new DirectoryImpl(sswapIndividual);
	}
	
	private static boolean hasLiteralData(SSWAPIndividual sswapIndividual) {
		
		SSWAPPredicate literalDataPredicate = sswapIndividual.getDocument().getPredicate(SSWAPMeet.Data.literalData);
		return sswapIndividual.getProperty(literalDataPredicate) != null ? true : false;
	}
	
	private static boolean isURL(SSWAPIndividual sswapIndividual) {
		
		try {
			sswapIndividual.getURI().toURL();
			return true;
		} catch ( Exception e ) {
			return false;
		}
		
	}

}
