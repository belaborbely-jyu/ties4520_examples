/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import info.sswap.api.model.SSWAPElement;
import info.sswap.api.model.SSWAPList;

/**
 * Implementation of SSWAPList. This class wraps a LinkedList to provide both the functionality of the list and the
 * SSWAPElement.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class ListImpl extends ElementImpl implements SSWAPList {

	/**
	 * The underlying implementation of the list.
	 */
	private LinkedList<SSWAPElement> list;

	/**
	 * Creates an empty list.
	 */
	public ListImpl() {
		this.list = new LinkedList<SSWAPElement>();
	}

	/**
	 * Creates a list initialized with data from a collection of SSWAPElements.
	 * 
	 * @param originalList
	 *            the original list whose elements will be used to populate this list.
	 */
	public ListImpl(Collection<SSWAPElement> originalList) {
		this.list = new LinkedList<SSWAPElement>(originalList);
	}

	/**
	 * @inheritDoc
	 */
	public boolean add(SSWAPElement o) {
		return list.add(o);
	}

	/**
	 * @inheritDoc
	 */
	public void add(int index, SSWAPElement element) {
		list.add(index, element);
	}

	/**
	 * @inheritDoc
	 */
	public boolean addAll(Collection<? extends SSWAPElement> c) {
		return list.addAll(c);
	}

	/**
	 * @inheritDoc
	 */
	public boolean addAll(int index, Collection<? extends SSWAPElement> c) {
		return list.addAll(index, c);
	}

	/**
	 * @inheritDoc
	 */
	public void clear() {
		list.clear();
	}

	/**
	 * @inheritDoc
	 */
	public boolean contains(Object o) {
		return list.contains(o);
	}

	/**
	 * @inheritDoc
	 */
	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPElement get(int index) {
		return list.get(index);
	}

	/**
	 * @inheritDoc
	 */
	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	/**
	 * @inheritDoc
	 */
	public boolean isEmpty() {
		return list.isEmpty();
	}

	/**
	 * @inheritDoc
	 */
	public Iterator<SSWAPElement> iterator() {
		return list.iterator();
	}

	/**
	 * @inheritDoc
	 */
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	/**
	 * @inheritDoc
	 */
	public ListIterator<SSWAPElement> listIterator() {
		return list.listIterator();
	}

	/**
	 * @inheritDoc
	 */
	public ListIterator<SSWAPElement> listIterator(int index) {
		return list.listIterator(index);
	}

	/**
	 * @inheritDoc
	 */
	public boolean remove(Object o) {
		return list.remove(o);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPElement remove(int index) {
		return list.remove(index);
	}

	/**
	 * @inheritDoc
	 */
	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	/**
	 * @inheritDoc
	 */
	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	/**
	 * @inheritDoc
	 */
	public SSWAPElement set(int index, SSWAPElement element) {
		return list.set(index, element);
	}

	/**
	 * @inheritDoc
	 */
	public int size() {
		return list.size();
	}

	/**
	 * @inheritDoc
	 */
	public List<SSWAPElement> subList(int fromIndex, int toIndex) {
		return new ListImpl(list.subList(fromIndex, toIndex));
	}

	/**
	 * @inheritDoc
	 */
	public Object[] toArray() {
		return list.toArray();
	}

	/**
	 * @inheritDoc
	 */
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	/**
	 * Returns the RDF identifier of this list, which is always null.
	 * 
	 * @return always null
	 */
	@SuppressWarnings("unchecked")
	public RdfKey getRdfId() {
		return null;
	}

	/**
	 * This method is required by the interface, but since lists cannot have their own identifiers, a call to this will
	 * result in UnsupportedOperationException
	 * 
	 * @throws UnsupportedOperationException
	 *             every time this method is called
	 * @param rdfId
	 *            the RDF identifier
	 */
	@SuppressWarnings("unchecked")
	public void setRdfId(RdfKey rdfId) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Informs the caller that this object is a list.
	 * 
	 * @return true
	 */
	@Override
	public boolean isList() {
		return true;
	}

	/**
	 * Type-safe cast of this SSWAPElement into SSWAPList
	 * 
	 * @return this object typed as SSWAPList
	 */
	@Override
	public SSWAPList asList() {
		return this;
	}
}
