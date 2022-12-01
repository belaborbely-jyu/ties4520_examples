/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.data.impl;

import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPType;
import info.sswap.ontologies.data.api.Data;
import info.sswap.ontologies.data.api.DataException;
import info.sswap.ontologies.data.api.DataFactory;
import info.sswap.ontologies.data.api.Directory;
import info.sswap.ontologies.sswapmeet.SSWAPMeet;

import java.util.Collection;
import java.util.HashSet;

/**
 * {@link Directory} implementation.
 * 
 * @see Directory
 * @author Damian Gessler
 *
 */
public class DirectoryImpl implements Directory {

	private SSWAPPredicate	data_hasData;
	private SSWAPType		data_DataFormat;

	private SSWAPIndividual sswapIndividual;
	private HashSet<Data>	dataSet;

	/**
	 * Create a {@link Directory} from the subject individual.
	 * 
	 * @param sswapIndividual
	 *            Individual for which to inspect for {@code data:hasData}
	 *            properties.
	 * @throws DataException upon failure to internally {@link #setData()}
	 */
	public DirectoryImpl(SSWAPIndividual sswapIndividual) throws DataException {
		
		this.sswapIndividual = sswapIndividual;

		data_hasData = sswapIndividual.getDocument().getPredicate(SSWAPMeet.Data.hasData);
		data_DataFormat = sswapIndividual.getDocument().getType(SSWAPMeet.Data.DataFormat);
		
		dataSet = new HashSet<Data>();
		setData();
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public SSWAPIndividual getIndividual() {
		return sswapIndividual;
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public Collection<Data> getData() {
		return dataSet;
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public void setData() throws DataException {
		
		// make a set of (unique) individuals (so there will be at most one Data object per individual value)
		HashSet<SSWAPIndividual> indSet = new HashSet<SSWAPIndividual>();
		Collection<SSWAPProperty> hasDataProperties = sswapIndividual.getProperties(data_hasData);

		dataSet.clear();
		
		// check/include the ind itself if it is of type data:DataFormat, even if there is no reflexive data:hasData relation
		if ( sswapIndividual.isOfType(data_DataFormat) ) {
			indSet.add(sswapIndividual);
		}
		
		if ( hasDataProperties != null ) {			
			for ( SSWAPProperty hasData : hasDataProperties ) {
				indSet.add(hasData.getValue().asIndividual());	// asIndividual() may be pathologically null
			}
		}
		
		// for each (unique) individual, create a Data object
		for ( SSWAPIndividual ind : indSet ) {
			if ( ind != null ) {
				dataSet.add(DataFactory.Data(ind));	// may throw
			}
		}
	
	} 

}
