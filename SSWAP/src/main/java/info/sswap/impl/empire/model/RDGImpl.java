/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.Expressivity;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.ValidationException;

import java.io.InputStream;

public abstract class RDGImpl extends ProtocolImpl implements RDG {
	/**
	 * @inheritDoc
	 */
	public RIG getRIG() throws DataAccessException {
		RIGImpl result =  ImplFactory.get().createEmptySSWAPDataObject(getURI(), RIGImpl.class);
		
		// read from this RDG
		result.dereference(this);
		
		if (!result.checkProfile(Expressivity.DL)) {
			result.setOwlDlRequired(false);
		}
		
		result.setRDG(this);
		result.setClientSideTranslation(true);
		
		return result;
	}
	
	/**
	 * @inheritDoc
	 */
	public RIG getRIG(InputStream is) throws DataAccessException, ValidationException {
		RIGImpl result =  ImplFactory.get().createEmptySSWAPDataObject(getURI(), RIGImpl.class);
		
		result.dereference(is);
	
		result.validateAgainstRDG(this);

		if (!result.checkProfile(Expressivity.DL)) {
			result.setOwlDlRequired(false);
		}
		
		result.setRDG(this);
		result.setClientSideTranslation(false);
		
		return result;
	}
	
	public String getGraphType() {
		return "RDG";
	}
}
