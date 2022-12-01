/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import java.net.URI;

import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPModel;
import info.sswap.api.model.SSWAPNode;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPProvider;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;

/**
 * Implementation of SSWAPNode (a SSWAP entity that requires special handling in SSWAP protocol)
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public abstract class NodeImpl extends IndividualImpl implements SSWAPNode {
	/**
	 * Dereferences this node, by first calling dereference() on the source model (if there is one; in case there is no
	 * source model, this method immediately returns). In case, this node is empire-generated, it also forces Empire to
	 * refresh/populate the object with the data read from the source model.
	 * 
	 * @throws DataAccessException when trying to access the underlying data source while dereferencing
	 */
	@Override
	public void dereference() throws DataAccessException {
		if (getSourceModel() != null && !getSourceModel().isDereferenced()) {
			getSourceModel().dereference();

			if (isEmpireGenerated()) {
				getSourceModel().getEntityManager().refresh(this);
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean isDereferenced() {
		if (getSourceModel() != null) {
			return getSourceModel().isDereferenced();
		}

		return false;
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPGraph asSSWAPGraph() {
		return as(SSWAPGraph.class);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPObject asSSWAPObject() {
		return as(SSWAPObject.class);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPProvider asSSWAPProvider() {
		return as(SSWAPProvider.class);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPResource asSSWAPResource() {
		return as(SSWAPResource.class);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPSubject asSSWAPSubject() {
		return as(SSWAPSubject.class);
	}

	/**
	 * @inheritDoc
	 */
	public URI getID() {
		URI uri = getURI();

		return uri;
	}

	/**
	 * Checks whether the final implementation of this object is provided by Empire. The current implementation returns
	 * always false, and this method should always be overridden by Empire-backed classes.
	 * 
	 * @return always false.
	 */
	public boolean isEmpireGenerated() {
		return false; // to be overriden by subclasses, if necessary
	}

	/**
	 * @inheritDoc
	 */
	public boolean isSSWAPGraph() {
		return isOfType(SSWAPGraph.class);
	}

	/**
	 * @inheritDoc
	 */
	public boolean isSSWAPObject() {
		return isOfType(SSWAPObject.class);
	}

	/**
	 * @inheritDoc
	 */
	public boolean isSSWAPProvider() {
		return isOfType(SSWAPProvider.class);
	}

	/**
	 * @inheritDoc
	 */
	public boolean isSSWAPResource() {
		return isOfType(SSWAPResource.class);
	}

	/**
	 * @inheritDoc
	 */
	public boolean isSSWAPSubject() {
		return isOfType(SSWAPSubject.class);
	}
	
	private <T extends SSWAPModel> boolean isOfType(Class<T> clazz) {
		SourceModel sourceModel = getSourceModel();
		
		if ((sourceModel != null) && (getURI() != null) && (sourceModel instanceof SourceModelImpl)) {
			return (ImplFactory.get().canAs(sourceModel, getURI(), getImplClass(clazz)));
		}
		
		return false;
	}
	
	private <T extends SSWAPModel> T as(Class<T> clazz) {
		if (!isOfType(clazz)) {
			return null;
		}
		
		SourceModel sourceModel = getSourceModel();
		
		if ((sourceModel != null) && (getURI() != null) && (sourceModel instanceof SourceModelImpl)) {
			return (T) ImplFactory.get().castDependentModel(sourceModel, getURI(), getImplClass(clazz));
		}
		
		return null;
	}
	
	private static Class<? extends ModelImpl> getImplClass(Class<? extends SSWAPModel> apiClass) {
		if (SSWAPResource.class.equals(apiClass)) {
			return ResourceImpl.class;
		}
		else if (SSWAPSubject.class.equals(apiClass)) {
			return SubjectImpl.class;
		}
		else if (SSWAPObject.class.equals(apiClass)) {
			return ObjectImpl.class;
		}
		else if (SSWAPGraph.class.equals(apiClass)) {
			return GraphImpl.class;
		}
		else if (SSWAPProvider.class.equals(apiClass)) {
			return ProviderImpl.class;
		}
		else {
			return NodeImpl.class;
		}
	}
}
