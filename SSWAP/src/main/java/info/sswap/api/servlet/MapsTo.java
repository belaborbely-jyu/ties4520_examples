/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.servlet;

import info.sswap.api.model.RIG;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPSubject;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * A helper class that allows the mapping of subjects to objects without the
 * complexities of servlet programming. This class is instantiated anew for each
 * request, so unlike servlet programming, instance variables in the class are
 * not shared across requests (there is no danger of one request reading or
 * writing to a non-static variable of another request). To use, extend this
 * class to perform the mapping; <i>e.g.</i>,:
 * 
 * <pre>
 * public class MyService extends MapsTo {
 * 
 *   <code>@Override</code>
 *   protected void mapsTo(SSWAPSubject translatedSubject) {
 * 
 *    // inspect the subject and edit/add its objects
 *   }
 * }
 * </pre>
 * 
 * Then connect <code>MyService</code> to a servlet launcher via a class
 * extending {@link SimpleSSWAPServlet}.
 * <p>
 * This class handles processing the <code>RIG</code>; each translated subject
 * will be passed to <code>mapsTo</code>, which will be called once for each
 * valid subject in the <code>RIG</code>.
 * 
 * @see SimpleSSWAPServlet
 * @author Damian Gessler
 * 
 */
public abstract class MapsTo {

	protected RIG rig;

	// protected scope for direct access by SimpleSSWAPServlet
	protected Set<SSWAPObject> sswapObjects;
	
	private SimpleSSWAPServlet servlet;
	
	// no argument constructor for runtime instantiation via SimpleSSWAPServlet
	public MapsTo() {
		
		rig = null;
		
		// assignment is overridden in SimpleSSWAPServlet#handleRequest
		sswapObjects = new HashSet<SSWAPObject>();
	}
	
	/**
	 * The <code>RIG</code> for this mapping; this should be set at the first
	 * opportunity (<i>e.g.</i> in
	 * <code>initializeRequest<code>. This is a convenience setter; there is no getter: subclasses
	 * can access the instance variable <code>rig</code> directly.
	 * 
	 * @param rig
	 *            <code>RIG</code> for the mapping. All subsequent
	 *            <code>SSWAPSubjects</code> and <code>SSWAPObjects<code>
	 *            should be from this <code>rig<code>.
	 */
	public void setRIG(RIG rig) {
		this.rig = rig;
	}
	
	/**
	 * The servlet for which this service class is performing.
	 * 
	 * @return the super class, which may be down-cast to the specific servlet type
	 */
	public SimpleSSWAPServlet getServlet() {
		return servlet;
	}
	
	protected void setServlet(SimpleSSWAPServlet servlet) {
		this.servlet = servlet;
	}
	
	/**
	 * This method is called once, at the beginning of the request.
	 * By default, it is empty; override to do operations (<i>e.g.</i>,
	 * open a database connection).
	 * 
	 * @param rig the Resource Invocation Graph (RIG) of the request
	 * @throws Exception any exception thrown from the overridden method
	 */
	protected void initializeRequest(RIG rig) throws Exception {
		// do not include critical must-run code here because a developer
		// may not call super.initializeRequest(rig) when overriding.
	}
	
	/**
	 * This method is called once, at the end of the request. By default
	 * (<i>e.g.</i> either not overriding or by calling super(exception)),
	 * it rethrows the exception (if not null); override to do operations
	 * (<i>e.g.</i>, close a database connection).
	 * <p>
	 * On success, exception will be null; on entering this method because of a
	 * thrown exception, the exception will be passed and may be consumed or
	 * rethrown. Rethrown exceptions are wrapped as RuntimeExceptions and passed
	 * up to the caller.
	 * 
	 * @param exception
	 *            exception thrown by {@link #initializeRequest(RIG)} or
	 *            {@link #mapsTo(SSWAPSubject)}; null on success
	 * @throws Exception
	 *             any exception thrown from the overridden method, or the
	 *             argument exception if not overridden and not null
	 */
	protected void finalizeRequest(Exception exception) throws Exception {
		
		// do not include critical must-run code here because calling
		// super.finalizeRequest(exception) will rethrow the exception.
		
		// default is to rethrow
		if ( exception != null ) {
			throw exception;
		}
	}
	
	/**
	 * Define this method to perform the mapping from the translated subject to
	 * its objects.
	 * <p>
	 * In general, if the operation on the object is the same for any object,
	 * then inspect the subject's objects and edit them as appropriate. If it is
	 * a 1:many mapping, where each object gets a different state, then if there
	 * are no matching objects, build new objects for the subject via
	 * <code>newObject()</code> to complete the mapping.
	 * 
	 * @param translatedSubject
	 *            SSWAPSubject after ontology reasoning so that the properties
	 *            and types of the subject are in the vocabularies of the RDG
	 * @see #assignObject(SSWAPSubject)
	 * @see #assignObject(SSWAPSubject,URI)
	 * @see RIG#getTranslatedSubjects
	 * @throws Exception
	 *             thrown exception will be passed to {@link #finalizeRequest}
	 */
	protected abstract void mapsTo(SSWAPSubject translatedSubject) throws Exception;

	/**
	 * A convenience method for {@link #assignObject(SSWAPSubject, URI)} for an
	 * anonymous <code>sswapObject</code> (URI == null).
	 * 
	 * @param sswapSubject subject of the mapping
	 * @return a new SSWAPObject ready for editing
	 * @see RIG#getTranslatedSubjects
	 */
	public SSWAPObject assignObject(SSWAPSubject sswapSubject) {
		return assignObject(sswapSubject,null);
	}

	/**
	 * An efficient manner to create a new URI <code>SSWAPObject</code> and
	 * schedule it for adding to the <code>translatedSubject</code>. This method
	 * is preferred over those in <code>SSWAPProtocol</code> and
	 * <code>SSWAPSubject</code> when adding many <code>SSWAPObjects</code> to a
	 * single <code>SSWAPSubject</code> (1:many mapping) repetitively in a loop.
	 * Assignment to the <code>SSWAPSubject</code> is delayed until
	 * {@link #mapsTo(SSWAPSubject)} is completed. Add/set properties and types to this
	 * object to complete it.
	 * 
	 * @param sswapSubject subject of the mapping
	 * @param uri
	 *            URI of the SSWAPObject; null for a blank node
	 * @return a new SSWAPObject ready for editing
	 * @see RIG#getTranslatedSubjects
	 */
	public SSWAPObject assignObject(SSWAPSubject sswapSubject, URI uri) {
	
		if ( rig == null ) {
			rig = (RIG) sswapSubject.getDocument();
		}
		
		SSWAPObject sswapObject = rig.createObject(uri);
		sswapObjects.add(sswapObject);
		
		return sswapObject;
	}
	
	/**
	 * Schedules a <code>sswapObject</code> to be unassigned (removed) from a
	 * mapping to the <code>sswapSubject</code>. The object does not have to be
	 * first assigned by {@link #assignObject(SSWAPSubject,URI)}, but may be any
	 * <code>SSWAPObject</code> of the <code>sswapSubject</code>.
	 * Actual removal is delayed until the completion of
	 * {@link #mapsTo(SSWAPSubject)}.
	 * 
	 * @param sswapSubject subject of the mapping
	 * @param sswapObject object to schedule for removal
	 */
	public void unassignObject(SSWAPSubject sswapSubject, SSWAPObject sswapObject) {
		sswapObjects.remove(sswapObject);
	}

	/**
	 * If the argument URI is different from the <code>sswapObject</code> URI,
	 * then schedule to {@link #assignObject(SSWAPSubject,URI)} and
	 * {@link #unassignObject(SSWAPSubject,SSWAPObject)}. A null argument URI
	 * always results in a scheduled replacement.
	 * <p>
	 * In the act of replacing the <code>sswapObject</code> with a new
	 * object (at the URI), the new object will include the types and
	 * properties of the source object.
	 * 
	 * @param sswapSubject
	 *            subject of the mapping
	 * @param sswapObject
	 *            object to schedule for possible replacement (unassignment)
	 * @param uri
	 *            URI of the new SSWAPObject; null for a blank node
	 * @return a new SSWAPObject ready for editing, or null if nothing was
	 *         scheduled
	 * @deprecated Use {@link #replaceObject(SSWAPSubject,SSWAPObject,URI)} instead
	 */
	public SSWAPObject replaceObject(SSWAPSubject sswapSubject, URI uri, SSWAPObject sswapObject) {
		return replaceObject(sswapSubject, sswapObject, uri);
	}

	/**
	 * If the argument URI is different from the <code>sswapObject</code> URI,
	 * then schedule to {@link #assignObject(SSWAPSubject,URI)} and
	 * {@link #unassignObject(SSWAPSubject,SSWAPObject)}. A null argument URI
	 * always results in a scheduled replacement.
	 * <p>
	 * In the act of replacing the <code>sswapObject</code> with a new
	 * object (at the URI), the new object will include the types and
	 * properties of the source object.
	 * 
	 * @param sswapSubject
	 *            subject of the mapping
	 * @param sswapObject
	 *            object to schedule for possible replacement (unassignment)
	 * @param uri
	 *            URI of the new SSWAPObject; null for a blank node
	 * @return a new SSWAPObject ready for editing, or null if nothing was
	 *         scheduled
	 */
	public SSWAPObject replaceObject(SSWAPSubject sswapSubject, SSWAPObject sswapObject, URI uri) {

		SSWAPObject newObject = null;
		
		if ( uri == null || ! uri.equals(sswapObject.getURI()) ) {

			if ( rig == null ) {
				rig = (RIG) sswapSubject.getDocument();
			}

			// copy all the existing sswapObject's properties and types
			newObject = rig.newIndividual(sswapObject,uri);

			// schedule this newObject at uri to be added to this subject
			// (and update it's assignment to the underlying data object)
			newObject = assignObject(sswapSubject,uri);

			// mark sswapObject for removal
			unassignObject(sswapSubject,sswapObject);
		}
		
		return newObject;	// may be null
	}
}
