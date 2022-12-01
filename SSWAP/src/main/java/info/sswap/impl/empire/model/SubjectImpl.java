/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import static info.sswap.impl.empire.Namespaces.SSWAP_NS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;

import com.clarkparsia.empire.annotation.Namespaces;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;
import com.clarkparsia.utils.AbstractDataCommand;
import com.clarkparsia.utils.collections.CollectionUtil;

import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.ValidationException;
import info.sswap.impl.empire.Vocabulary;

/**
 * Implementation of SSWAPSubject. This abstract class contains a few abstract methods whose implementation will be
 * automatically provided by Empire at run-time.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
@Namespaces( { "sswap", SSWAP_NS })
@Entity
@RdfsClass("sswap:Subject")
public abstract class SubjectImpl extends EmpireGeneratedNodeImpl implements SSWAPSubject {
	
	private Set<GraphImpl> graphs = new HashSet<GraphImpl>();
	
	/**
	 * The default constructor
	 */
	public SubjectImpl() {
		addIgnoredType(Vocabulary.SSWAP_SUBJECT.getURI());
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPGraph getGraph() {
		if (!graphs.isEmpty()) {
			return graphs.iterator().next();
		}
		
		return null;
	}
	
	/**
	 * @inheritDoc
	 */
	public Collection<SSWAPGraph> getGraphs() {
		return listFromImpl(new ArrayList<GraphImpl>(graphs), SSWAPGraph.class, GraphImpl.class);
	}
	
	void addGraph(SSWAPGraph graph) {
		if (graph instanceof GraphImpl) {
			graphs.add((GraphImpl) graph);	
		}
		else {
			throw new IllegalArgumentException("The graph has not been created by this API implementation");
		}
	}
	
	void removeGraph(SSWAPGraph graph) {
		if (graph != null) {
			graphs.remove(graph);	
		}
		else {
			throw new IllegalArgumentException("The graph has not been created by this API implementation");
		}
	}
	
	/**
	 * @inheritDoc
	 */
	public SSWAPObject getObject() {
		ObjectImpl result = null;

		List<ObjectImpl> objectList = getMapsToList();

		if ((objectList != null) && (!objectList.isEmpty())) {
			result = objectList.get(0);

			// properly set the source model and the link back to this subject
			if (result != null) {
				result.addSubject(this);
				result.setSourceModel(getSourceModel());
			}
		}

		return result;
	}

	/**
	 * @inheritDoc
	 */
	public Collection<SSWAPObject> getObjects() {
		// type conversion from an implementation-typed collection to API-typed collection. Also it sets the source
		// model for the SSWAPObject objects
		Collection<SSWAPObject> result = listFromImpl(setSourceModel(getMapsToList(), getSourceModel()), SSWAPObject.class, ObjectImpl.class);
		
		CollectionUtil.each(result, new AbstractDataCommand<SSWAPObject>() {
            public void execute() {
            	((ObjectImpl) getData()).addSubject(SubjectImpl.this);
            }
		});
		
		return result;
	}

	/**
	 * @inheritDoc
	 */
	public void setObject(SSWAPObject object) {
		if (object instanceof ObjectImpl) {
			detachExistingObjects();
			((ObjectImpl) object).addSubject(this);
			
			List<ObjectImpl> objectList = new LinkedList<ObjectImpl>();
			objectList.add((ObjectImpl) object);

			setMapsToEmpireList(objectList);
		}
		else {
			throw new IllegalArgumentException(
			                "The SSWAPObject has been created by a different implementation of the API");
		}
	}
	
	public void addObject(SSWAPObject object) {
		List<SSWAPObject> objects = new LinkedList<SSWAPObject>(getObjects());
		
		objects.add(object);
		
		setObjects(objects);
	}
	
	/**
	 * @inheritDoc
	 */
	public void setObjects(Collection<SSWAPObject> objects) {
		detachExistingObjects();
		
		CollectionUtil.each(objects, new AbstractDataCommand<SSWAPObject>() {
            public void execute() {
            	((ObjectImpl) getData()).addSubject(SubjectImpl.this);
            }
		});
		
		setMapsToEmpireList(toListImpl(objects, SSWAPObject.class, ObjectImpl.class));
		persist();
	}

	private void detachExistingObjects() {
		List<ObjectImpl> existingObjects = getMapsToList();		
		
		if (existingObjects != null) {
			CollectionUtil.each(existingObjects, new AbstractDataCommand<ObjectImpl>() {
				public void execute() {
					getData().removeSubject(SubjectImpl.this);
				}
			});
		}
	}
		
	/**
	 * Gets a list of SSWAPObject implementations that are all connected to this SSWAPSubject by sswap:mapsTo predicate.
	 * 
	 * @return a list of SSWAPObject implementations. The list may be either empty or null, if there are no such
	 *         objects.
	 */
	@RdfProperty("sswap:mapsTo")
	public abstract List<ObjectImpl> getMapsToEmpireList();

	public List<ObjectImpl> getMapsToList() {
		return ensureProperView(getMapsToEmpireList(), ObjectImpl.class);
	}
	
	/**
	 * Sets a list of SSWAPObject implementations that will all be connected to this SSWAPSubject by sswap:mapsTo
	 * predicate.
	 * 
	 * @param mapsToList
	 *            a list of SSWAPObject implementations. The list may be either empty or null, if there are no such
	 *            objects.
	 */
	public abstract void setMapsToEmpireList(List<ObjectImpl> mapsToList);

	@Override
	public void validate() throws ValidationException {
		super.validate();
		
		Collection<SSWAPObject> objects = getObjects();
		
		if (objects.isEmpty()) {
			throw new ValidationException("There are no objects to which this subject is mapped");
		}
		
		for (SSWAPObject object : objects) {
			object.validate();
		}
	}	
}
