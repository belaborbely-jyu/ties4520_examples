/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.impl;

import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPLiteral;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.ontologies.data.api.DataException;
import info.sswap.ontologies.sswapmeet.SSWAPMeet.Data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Support for reading and writing literal data (the value of the data:literalData predicate).
 * 
 * @author Damian Gessler
 *
 */
public class LiteralData extends AbstractData {

	/**
	 * Construct a literal data object for reading/writing.
	 * 
	 * @param sswapIndividual the data source or sink
	 * @throws DataException on any error establishing literal data support
	 */
	public LiteralData(SSWAPIndividual sswapIndividual) throws DataException {
		super(sswapIndividual);
	}

	/**
	 * Read the data, reading nothing (but no error) if there is no data to read.
	 * <p>
	 * The data source must be of type <code>data:DataFormat</code> for any
	 * meaningful read; otherwise a read "succeeds" with trivial empty content.
	 */
	@Override
	public InputStream readData() throws IOException, DataException {
		
		boolean nullRead = false;
		SSWAPProperty literalDataProperty = null;
		
		if ( ! sswapIndividual.isOfType(DataFormatType) ) {
			nullRead = true;
		} else {
		
			SSWAPPredicate literalDataPredicate = sswapDocument.getPredicate(Data.literalData);
			literalDataProperty = sswapIndividual.getProperty(literalDataPredicate);

			// attempting to read a null element is not an error, just returns nothing to read
			if ( literalDataProperty == null ) {
				nullRead = true;
			}
		}
		
		if ( nullRead ) {
			return new ByteArrayInputStream(new byte[0]);
		}

		String value = literalDataProperty.getValue().asString();
		
		return validate(parse(stringToInputStream(value)));
	}

	/**
	 * Write to the property data:literalData, creating one if needed
	 * 
	 * @param inputStream stream from which to read the data to write
	 */
	@Override
	public void writeData(InputStream inputStream) throws IOException, DataException {
				
		inputStream = serialize(validate(inputStream));
		
		SSWAPPredicate literalDataPredicate = sswapDocument.getPredicate(Data.literalData);
		SSWAPLiteral sswapLiteral = sswapDocument.createLiteral(inputStreamToString(inputStream));
		sswapIndividual.setProperty(literalDataPredicate, sswapLiteral);
		
		if ( ! sswapIndividual.isOfType(DataFormatType) ) {
			sswapIndividual.addType(DataFormatType);
		}
		
	}
}
