/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import static info.sswap.impl.empire.Namespaces.SSWAP_NS;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPSubject;

import java.util.Collection;

import javax.persistence.Entity;

import com.clarkparsia.empire.annotation.Namespaces;
import com.clarkparsia.empire.annotation.RdfsClass;

/**
 * Represents a translated subject. This class overrides setObject() and setObjects() method
 * so that they can be proxied to the original (non-translated) subject.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
@Namespaces( { "sswap", SSWAP_NS })
@Entity
@RdfsClass("sswap:Subject")
public abstract class TranslatedSubjectImpl extends SubjectImpl {
	/**
	 * The original (non-translated) subject
	 */
	private SSWAPSubject originalSubject;
	
	public SSWAPSubject getOriginalSubject() {
		return originalSubject;
	}
	
	/**
	 * Sets the original (non-translated) subject.
	 * 
	 * @param originalSubject the original (non-translated) subject
	 */
	void setOriginalSubject(SSWAPSubject originalSubject) {
		this.originalSubject = originalSubject;
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public void setObject(SSWAPObject sswapObject) {				
		if (originalSubject == null) {
			// if there is no non-translated subject, then just
			// behave as a regular SubjectImpl
			super.setObject(sswapObject);
		}
		else {
			// if there is a non-translated subject, forward the request to that
			// subject
			originalSubject.setObject(sswapObject);
		}
	}
	
	/**
	 * @inheritDoc
	 */
	@Override
	public void setObjects(Collection<SSWAPObject> objects) {
		if (originalSubject == null) {
			// if there is no non-translated subject, then just
			// behave as a regular SubjectImpl
			super.setObjects(objects);
		}
		else {
			// if there is a non-translated subject, forward the request to that
			// subject
			originalSubject.setObjects(objects);
		}		
	}
	
	@Override
	public SSWAPObject getObject() {
		if (originalSubject == null) {
			// if there is no non-translated subject, then just
			// behave as a regular SubjectImpl
			return super.getObject();
		}
		else {
			// if there is a non-translated subject, forward the request to that
			// subject
			return originalSubject.getObject();
		}
	}
	
	@Override
	public Collection<SSWAPObject> getObjects() {
		if (originalSubject == null) {
			// if there is no non-translated subject, then just
			// behave as a regular SubjectImpl
			return super.getObjects();
		}
		else {
			// if there is a non-translated subject, forward the request to that
			// subject
			return originalSubject.getObjects();
		}
	}
}
