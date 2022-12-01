/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.SSWAPModel;
import info.sswap.impl.empire.Vocabulary;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;

/**
 * An abstract class containing methods common to all Empire-generated SSWAPIndividuals.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
@Entity
public abstract class EmpireGeneratedNodeImpl extends NodeImpl {
	public EmpireGeneratedNodeImpl() {
		addIgnoredProperty(Vocabulary.NAME.getURI());
		addIgnoredProperty(Vocabulary.ONE_LINE_DESCRIPTION.getURI());
		addIgnoredProperty(Vocabulary.ABOUT_URI.getURI());
		addIgnoredProperty(Vocabulary.INPUT_URI.getURI());
		addIgnoredProperty(Vocabulary.OUTPUT_URI.getURI());
		addIgnoredProperty(Vocabulary.METADATA.getURI());
		addIgnoredProperty(Vocabulary.OPERATES_ON.getURI());
		addIgnoredProperty(Vocabulary.PROVIDED_BY.getURI());
		addIgnoredProperty(Vocabulary.ICON.getURI());
		addIgnoredProperty(Vocabulary.MAPS_TO.getURI());
		addIgnoredProperty(Vocabulary.HAS_MAPPING.getURI());
		addIgnoredProperty(Vocabulary.PROVIDES_RESOURCE.getURI());	
	}
	
	/**
	 * Informs the caller that this node is empire-generated.
	 * 
	 * @return always true
	 */
	@Override
	public boolean isEmpireGenerated() {
		return true;
	}

	// Convenience methods for dealing with Empire-generated methods returning collections.

	/**
	 * Convenience method for Empire-generated objects. It casts a list containing Empire-generated objects (e.g.,
	 * SubjectImpl) to a set typed with their API interfaces (e.g., SSWAPSubject).
	 * 
	 * For example, if one has a list declared as:
	 * 
	 * List<SubjectImpl> implTypedList
	 * 
	 * it can be cast to List<SSWAPSubject> by the following call:
	 * 
	 * List<SSWAPSubject> apiTypedList = listFromImpl(implTypedList, sourceModel, SSWAPSubject.class, SubjectImpl.class)
	 * 
	 * This method handles properly the situation when the list is null (Empire may return either an empty list or a
	 * null list, while SSWAP API always returns an empty list).
	 * 
	 * @param <T>
	 *            generic parameter for the API interface
	 * @param <S>
	 *            generic parameter for the Empire-generated class of the interface T
	 * @param implList
	 *            the list of Empire-generated objects (may be null)
	 * @param modelClass
	 *            the class for the interface in the SSWAP API
	 * @param implClass
	 *            the class for the Empire-based implementation
	 * @return a set typed with API interface (never null)
	 */
	@SuppressWarnings("unchecked")
	protected static <T extends SSWAPModel, S extends ModelImpl> Set<T> listFromImpl(List<S> implList,
	                Class<T> modelClass, Class<S> implClass) {
		Set<T> result = new HashSet<T>();

		if (implList != null) {
			for (S el : implList) {
				result.add((T) el);
			}
		}

		return result;
	}

	/**
	 * Convenience method for Empire-generated objects. It casts a list containing Empire-generated objects typed with
	 * API interfaces (e.g., SSWAPSubjects) to a set typed with their Empire-based implementations (e.g., SubjectImpl).
	 * 
	 * For example, if one has a list declared as:
	 * 
	 * List<SSWAPSubject> apiTypedList
	 * 
	 * it can be cast to Set<SubjectImpl> by the following call:
	 * 
	 * Set<SubjectImpl> implTypedSet = listToImpl(apiTypedList, SSWAPSubject.class, SubjectImpl.class)
	 * 
	 * @param <T>
	 *            generic parameter for the API interface
	 * @param <S>
	 *            generic parameter for the Empire-generated class of the interface T
	 * @param modelList
	 *            the list of Empire-generated objects typed using the API interfaces
	 * @param modelClass
	 *            the class for the interface in the SSWAP API
	 * @param implClass
	 *            the class for the Empire-based implementation
	 * @throws IllegalArgumentException
	 *             if some (or all) of the objects in the list were not created by this API implementation
	 * @return a set typed with API interface
	 */
	@SuppressWarnings("unchecked")
	protected static <T extends SSWAPModel, S extends ModelImpl> Set<S> listToImpl(List<T> modelList,
	                Class<T> modelClass, Class<S> implClass) throws IllegalArgumentException {
		Set<S> result = new HashSet<S>();

		if (modelList != null) {
			for (T el : modelList) {
				if (implClass.isInstance(el)) {
					result.add((S) el);
				}
				else {
					throw new IllegalArgumentException(
					                "One of the elements has not been created by this implementation of the API");
				}
			}
		}

		return result;
	}

	/**
	 * Convenience method for Empire-generated objects. It casts a collection containing objects typed as API interfaces
	 * (e.g., SSWAPSubjects) to a List of of objects typed with their Empire-generated implementations (e.g.,
	 * SubjectImpl).
	 * 
	 * For example, if one has a list declared as:
	 * 
	 * List<SSWAPSubject> apiTypedList
	 * 
	 * it can be cast to List<SubjectImpl> by the following call:
	 * 
	 * List<SubjectImpl> implTypedList = listToImpl(apiTypedList, SSWAPSubject.class, SubjectImpl.class)
	 * 
	 * @param <T>
	 *            generic parameter for the API interface
	 * @param <S>
	 *            generic parameter for the Empire-generated class of the interface T
	 * @param modelList
	 *            the list of Empire-generated objects typed using the API interfaces
	 * @param modelClass
	 *            the class for the interface in the SSWAP API
	 * @param implClass
	 *            the class for the Empire-based implementation
	 * @throws IllegalArgumentException
	 *             if some (or all) of the objects in the list were not created by this API implementation
	 * @return a set typed with API interface
	 */
	@SuppressWarnings("unchecked")
	protected static <T extends SSWAPModel, S extends ModelImpl> List<S> toListImpl(Collection<T> modelList,
	                Class<T> modelClass, Class<S> implClass) throws IllegalArgumentException {
		List<S> result = new LinkedList<S>();

		if (modelList != null) {
			for (T el : modelList) {
				if (implClass.isInstance(el)) {
					result.add((S) el);
				}
				else {
					throw new IllegalArgumentException(
					                "One of the elements has not been created by this implementation of the API");
				}
			}
		}

		return result;
	}

	/**
	 * Convenience method for setting the same source model to all elements in a list
	 * 
	 * @param models
	 *            a list of ModelImpls
	 * @param sourceModel
	 *            a model to be set.
	 * @return the same collection with the source model set for all elements
	 */
	protected static <T extends ModelImpl> List<T> setSourceModel(List<T> models, SourceModel sourceModel) {
		if (models != null) {
			for (ModelImpl model : models) {
				model.setSourceModel(sourceModel);
			}
		}

		return models;
	}
	
	protected <T extends ModelImpl> List<T> ensureProperView(List<? extends ModelImpl> models, Class<T> clazz) {
		if (models == null) {
			return null;			
		}
		
		List<T> result = new LinkedList<T>();
		
		for (ModelImpl model : models) {
			result.add(ensureProperView(model, clazz));
		}
		
		return result;
	}
	
	protected <T extends ModelImpl> T ensureProperView(ModelImpl model, Class<T> clazz) {
		SourceModel sourceModel = this.assertSourceModel();
		
		if (clazz.isAssignableFrom(model.getClass())) {
			return (T) model;
		}
		
		return ImplFactory.get().castDependentModel(sourceModel, model.getURI(), clazz);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void refresh() {
		super.refresh();

		// Refreshing EntityManager is only needed if the underlying data source should change
		// While it is possible for the Jena model to change (modification of properties via SSWAP Java API),
		// this changes are non-overlapping with elements managed by Empire. 
		// The only case this method (and the code commented out below) is needed, if it is possible
		// for the model to be replaced with another model. In the past, we had a use-case,
		// when it was possible to get a non-dereferenced entity, and then dereference the model
		// TODO: check whether this no longer is the case, and ultimately remove this method
		
		// Moreover, the code below caused a bug because a refresh causes Empire to reset *all* the references to
		// dependent objects (e.g., refreshing a sswap:Resource will cause subsequent calls to getGraphs() to return
		// different Java objects for graphs than returned previously).
		//if ((getSourceModel() != null) && (getSourceModel().getEntityManager() != null)) {
			//getSourceModel().getEntityManager().refresh(this);
		//}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void persist() {
		if ((getSourceModel() != null) && (getSourceModel().getEntityManager() != null)) {
			getSourceModel().getEntityManager().merge(this);
		}

		// IMPORTANT! super.persist() has to be AFTER the merge() invocation of the EntityManager
		// Super class contains SSWAPIndividual, which manages its own types and properties (in addition to Empire-managed
		// properties). Since entity manager is not aware of these, it will remove them. On the other hand, SSWAPIndividual
		// is aware of Empire-managed properties (via the list of "ignored" properties; i.e., ignored by SSWAPIndividual
		// management)
		super.persist();
	}
}
