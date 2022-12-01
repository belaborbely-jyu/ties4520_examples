/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.servlet;

import info.sswap.api.model.RIG;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPSubject;

import java.util.Collection;
import java.util.HashSet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * A servlet wrapper that works in concert with {@link MapsTo} for the mapping
 * of subjects to objects. Using a service class that is an extension of
 * {@link MapsTo} allows developers to implement their service without the
 * constraints and subtleties of servlet programming. This is best practice for
 * most implementations. To use, extend and add a servlet launcher; <i>e.g.</i>:
 * 
 * <pre>
 * public class MyServlet extends SimpleSSWAPServlet {
 *     ...
 * }
 * </pre>
 * 
 * The servlet launcher <code>MyServlet</code> can be connected to the service class via
 * either of two methods. Via web.xml; <i>e.g.</i>:
 * 
 * <pre>
 * {@code
 * <servlet>
 * 
 *  <servlet-name>MyServlet</servlet-name>
 *  <servlet-class>org.mySite.sswap.servlets.MyServlet</servlet-class>
 * 
 *  <init-param>
 *    <param-name>ServiceClass</param-name>
 *    <param-value>org.mySite.sswap.services.MyService</param-value>
 *  </init-param>
 *  
 * </servlet>
 * 
 * <servlet-mapping>
 *  <servlet-name>MyServlet</servlet-name>
 *  <url-pattern>/MyService/*</url-pattern>
 * </servlet-mapping>
 * }
 * </pre>
 * 
 * Or, if <code>ServiceClass</code> is not defined in web.xml, the class
 * may be specified by overriding the method <code>getServiceClass</code>; see example
 * in {@link #getServiceClass}.
 * <p> 
 * <code>MyService</code> must extend {@link MapsTo} to implement the
 * service.
 * 
 * @author Damian Gessler
 * @see MapsTo
 * 
 */
public abstract class SimpleSSWAPServlet extends AbstractSSWAPServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		
		super.init(servletConfig);
		
		// test now, so that any dynamic class instantiation
		// errors are thrown at startup
		try {
			newMapsTo(null);
		} catch ( Exception e) {
			throw new ServletException(e);
		}
	}

	/**
	 * Declare the class (<i>e.g.</i>, <code>MyService</code> that performs the
	 * service. The class must be a subclass of {@link MapsTo}. This can be done
	 * by overriding this method to return the class; <i>e.g.</i>:
	 * 
	 * <pre>
	 * {@code
	 * public class MyServlet extends SimpleSSWAPServlet {
     *
     *   //@Override
     *   public void init(ServletConfig servletConfig) throws ServletException {
     * 
     *     // always do this
     *     super.init(servletConfig);
     * 
     *     // do anything else here that needs to be done once, on servlet load;
     *     // for example, increase the service timeout above the default in Config.
     * 
     *     int timeout = 10 * 60 * 1000;    // 10 mins in milliseconds
     *     setTimeout(timeout);
     * 
     *   }
     *   
     *   //@SuppressWarnings("unchecked")
     *   //@Override
     *   public <T> Class<T> getServiceClass() {
     *     return (Class<T>) MyService.class;
     *   }
     * }
     * }
	 * </pre>
	 * 
	 * A value for the ServiceClass parameter in web.xml, if defined, will take precedence over this method.
	 * 
	 * @param <T> subclass of {@link MapsTo}
	 * @return class to load for the service
	 * @see MapsTo
	 * @see info.sswap.api.model.Config
	 */
	public <T> Class<T> getServiceClass() {
		return null;
	}
	
	/**
	 * This method is marked <code>protected</code> solely for package access
	 * purposes. It should not be called directly and cannot be overridden. The
	 * servlet handler will call this method automatically. To perform a
	 * specific subject -> object mapping, override {@link MapsTo#mapsTo }.
	 * @see MapsTo
	 */
	@Override
	protected final void handleRequest(RIG rig) {
		
		Exception exception = null;
		MapsTo mapsTo;
		
		try {
			mapsTo = newMapsTo(rig);
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		} 
		
		try {
			
			mapsTo.initializeRequest(rig);
			
			for ( SSWAPSubject translatedSubject : rig.getTranslatedSubjects() ) {
				
				// assign w/ extant sswapObjects
				// (a sswapObject may be removed by overridden mapsTo calling MapsTo#unassignObject
				Collection<SSWAPObject> initialCollection = translatedSubject.getObjects();
				mapsTo.sswapObjects = new HashSet<SSWAPObject>(initialCollection);
				
				mapsTo.mapsTo(translatedSubject);
				
				// support case where SSWAPObjects have been added
				// via the SSWAPProtocol and SSWAPSubject interfaces
				
				Collection<SSWAPObject> finalCollection = translatedSubject.getObjects();
				if ( finalCollection.removeAll(initialCollection) && ! finalCollection.isEmpty() ) {
					mapsTo.sswapObjects.addAll(finalCollection); // objects added using SSWAPProtocol and SSWAPSubject interfaces (not using assignSubject)
				}
				
				translatedSubject.setObjects(mapsTo.sswapObjects);
			}
			
		} catch ( Exception e ) {
			exception = e;
		} finally {
			try {
				mapsTo.finalizeRequest(exception);
			} catch ( RuntimeException re ) {
				throw re;
			} catch ( Exception ex ) {
				// constructing on ex.getMessage() separately from ex results in the Exception not being part of the message itself
				throw new RuntimeException(ex.getMessage(),ex);
			}
		}
		
	}
	
	/**
	 * Dynamically instantiates a MapsTo object either first by name (from an
	 * init value in web.xml), or if that is not defined, by value (from
	 * getServiceClass).
	 * 
	 * @param rig
	 *            RIG for this request
	 * @return an instance of the service class
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private MapsTo newMapsTo(RIG rig) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		
		String serviceClassName = getServletConfig().getInitParameter("ServiceClass");
		MapsTo mapsTo;
		
		// try to load from the <init-param>ServiceClass</init-param> in web.xml
		if ( serviceClassName != null ) {
			mapsTo = (MapsTo) Class.forName(serviceClassName).newInstance();
		} else { // try to load from the overriding of getServiceClass()
			mapsTo = (MapsTo) getServiceClass().newInstance();
		}
		
		mapsTo.rig = rig;
		mapsTo.setServlet(this);
		
		return mapsTo;
	}

}
