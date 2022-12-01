/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.exec.api;

import info.sswap.ontologies.data.api.Data;
import info.sswap.ontologies.exec.impl.ExecImpl;

/**
 * The Exec servlet allows the wrapping of legacy, non-semantic programs as
 * semantic web services. For default behavior, no coding is necessary: simply
 * map this servlet to a URL pattern in web.xml and place Resource Description
 * Graphs (<code>RDG</code>s) in the <code>RDGPath</code> directory.
 * <p>
 * For sanitizing user input--an important security measure--extend this servlet
 * and override <code>getCommandLine</code> and optionally
 * <code>getEnvironment</code>.
 * <p>
 * Legacy programs are wrapped using the <code>exec</code> ontology at <a
 * href="http://sswapmeet.sswap.info/exec">sswapmeet.sswap.info/exec</a>.
 * An <code>RDG</code> can be created using the <code>exec</code> and other
 * ontologies and the HTTP API service <a
 * href="http://sswap.info/api/makeRDG">/makeRDG</a>. Below is a JSON
 * description of the UNIX program <code>echo</code> ready for conversion into a
 * SSWAP OWL RDF/XML RDG.
 * 
 * <pre>
 * {
 *   "prefix" : {
 *      "myExec" : "http://localhost:8080/",
 *   	"mime"   : "http://sswapmeet.sswap.info/mime/",
 *       ""      : "http://sswapmeet.sswap.info/exec/"
 *   },
 * 
 *   "myExec:exec/echo" : {
 * 
 *   	"rdf:type" : ":ExecCmd",
 * 
 *       "sswap:name" : "echo",
 *       "sswap:oneLineDescription" : "write arguments to standard output",
 *       "sswap:providedBy" : "myExec:resourceProvider",
 * 
 *       "sswap:aboutURI" : "http://www.unix.com/man-page/POSIX/1/echo",
 *      
 *       ":command" : "/bin/echo",
 *       ":synopsis" : "echo [-n] [string ...]"
 * 
 *   },
 * 
 *   "mapping" : { "" : "mime:text/Plain" }
 * 
 * }
 * </pre>
 * 
 * The conversion from JSON to RDF/XML OWL can be done automatically by dropping
 * the JSON description into the <code>hotboxPath</code> directory as specified
 * in web.xml file. A directory scanner monitors the directory and converts JSON
 * files into <code>RDG</code>s and places them in the <code>RDGPath</code>
 * directory as semantic web services.
 * 
 * To activate the Exec package simply map this servlet in the web.xml file;
 * <i>e.g.</i>,
 * 
 * <pre>
 * {@code
 * <servlet>
 *   <servlet-name>ExecServlet</servlet-name>
 *   <servlet-class>info.sswap.ontologies.exec.api.ExecServlet</servlet-class>
 *   
 *   <init-param>
 *     <param-name>RDGPath</param-name>
 *     <param-value>/public</param-value>
 *   </init-param>
 *   
 *   <init-param>
 *     <param-name>hotboxPath</param-name>
 *     <param-value>/json</param-value>
 *   </init-param>
 *   
 * </servlet>
 * 
 * <servlet-mapping>
 *   <servlet-name>ExecServlet</servlet-name>
 *   <url-pattern>/exec/*</url-pattern>
 * </servlet-mapping>
 * }
 * </pre>
 * 
 * You can execute the above program with a HTTP GET as
 * <code>http://localhost:8080/exec/echo?hello world</code>. Note that command
 * line arguments are passed as the query string. This is akin to setting the <a
 * href="http://sswapmeet.sswap.info/exec/args">args</a> property value in
 * standard SSWAP GET query string parsing (<i>e.g.</i>,
 * <code>?~args=hello world</code>).
 * 
 * The <code>ExecServlet</code> will attempt to open each SSWAP Subject of the
 * invoking <code>RIG</code> as a <code>DataFormat</code> object (see the
 * <code>data</code> package and the <code>data</code> ontology at <a
 * href="http://sswapmeet.sswap.info/data">sswapmeet.sswap.info/data</a>). On reading the
 * data it will pipe it to the standard input of the command with the parameters
 * as set by the <code>args</code> property. The servlet will block on the
 * process (wait for the process to finish), and will map the standard output to
 * <code>DataFormat</code> types of each SSWAP Object.
 * 
 * @see Exec#getCommandLine(String)
 * @see Exec#setEnvironment(java.util.Map)
 * @see HotboxPoller
 * @see Data
 * 
 * @author Damian Gessler
 */
public class ExecServlet extends ExecImpl {

	/**
	 * Default
	 */
	private static final long serialVersionUID = 1L;

}
