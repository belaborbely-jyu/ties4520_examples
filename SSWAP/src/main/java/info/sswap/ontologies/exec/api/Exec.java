/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.exec.api;

import java.util.Map;

/**
 * Execution of legacy programs. The Exec package allows one to wrap a
 * non-semantic legacy program and run it as a semantic web service. The package
 * requires no source code for implementation: simply define the appropriate
 * <code>RDGPath</code> (and optional <code>hotboxPath</code>) in web.xml (see {@link ExecServlet}). Then
 * place RDF/XML <code>RDG</code>s in the <code>RDGPath</code> or place their
 * JSON equivalents in the <code>hotboxPath</code>. <code>RDG</code> data types
 * should use the data ontology (<a
 * href="http://sswapmeet.sswap.info/data">http://sswapmeet.sswap.info/data</a>)
 * either explicitly, or usually implicitly, for example as is achieved by using
 * the <code>mime</code> ontology at <a
 * href="http://sswapmeet.sswap.info/mime">http://sswapmeet.sswap.info/mime</a>.
 * Invocation of legacy services is then handled automatically by the Exec
 * package.
 * <p>
 * 
 * <h4>WARNING: Security Risk</h4>
 * Running programs that may not have been originally designed for web
 * invocation has inherent security risks. Many of these risks are not specific
 * to SSWAP, but are generally applicable; for example, such risks also exist
 * for traditional CGI (Common Gateway Interface) wrapping.
 * <p>
 * Risks include:
 * <ul>
 * <li>Programs such as the UNIX command 'ls' or the Windows' command 'dir'.
 * These programs, by design, will report server configurations, such as
 * directory structures outside of the web server container. These types of
 * programs should never be exposed as web services.
 * <li>Programs such as the UNIX 'awk'. Some programs allow users to invoke a
 * shell, which can enable broad-scale server access based on user input. These
 * types of programs should never be exposed as web services.
 * <li>Programs such as the UNIX 'sed'. Some programs may not explicitly support
 * shells, but may allow scripted file name reading, writing, deletion, etc.
 * These types of programs should never be exposed as web services.
 * <li>Programs such as the UNIX 'head'. Some programs accept file names as
 * arguments. This can expose well-known system files, such as /etc/passwd. Even
 * if file name arguments do not map to existent files, error messages from such
 * programs can allow probing of a system. These types of programs should never
 * be exposed as web services.
 * <li>Programs that are not "hardened", such as those that execute unsanitized
 * user input, or contain exploitable bugs. It can be difficult to know if
 * programs are adequately hardened; best practice is to not expose programs as
 * web services until they have passed thorough testing.
 * <li>Any program. Ultimately, exposing any program as a web service has
 * inherent risks. Protect against this by running the web server itself with
 * the minimum permissions and access control rights necessary for it to perform
 * its job, and exposing programs that are field-proven to provide
 * well-understood functionality.
 * </ul>
 * <p>
 * <h4>The Exec package security model</h4>
 * <ul>
 * <li>The servlet must have a servlet mapping in web.xml. If there is no
 * mapping, the servlet will not load. If you have no need for the Exec package
 * this is the safest setting.
 * <li>The servlet is mapped in web.xml, but the "RDGPath" parameter is
 * undefined. The servlet will throw an exception and not load.
 * <li>The servlet is mapped and configured, but there are no RDGs in the
 * RDGPath. Only RDGs in the RDGPath directory are executable.
 * <li>If the "hotboxPath" parameter is configured, then the hotbox directory
 * will be polled for JSON RDG files. If the JSON file is newer than its
 * equivalent RDF/XML RDG in the RDGPath directory, then it will be sent to the
 * HTTP API /makeRDG converter, and upon success, an RDG will be placed in the
 * RDGPath directory. This allows one to simply drop-n-go JSON RDG files and
 * they will be automatically generated into RDGs and enabled as services.
 * Hotbox polling will not occur if the hotboxPath is not defined at server
 * startup. At present, hotbox polling enforces a file name filter (only files
 * ending in ".json"); sub-directories are not scanned.
 * <li>The program to be executed is configured as the value of the
 * <code>exec:command</code> property in the service's RDG. The
 * <code>exec:command</code> property in a RIG (received from a user invoking
 * the service) is always ignored (never used); it is replaced by the RDG's
 * <code>exec:command</code>.
 * <li>The user passes the program's arguments as the value of the
 * <code>exec:args</code> property in the RIG. This string is parsed into a
 * parameter array. Malicious values and weak programs are a security risk here,
 * since user input will be passed to the program.
 * <li>Both the replaced RDG command and the parsed user <code>exec:args</code>
 * parameters are passed to the <code>getCommandLine</code> method. There is no
 * universal, operating system independent algorithm to parse command line
 * parameters in all cases. Thus, this method may be overridden to provide
 * sanitized argument restructuring. This includes redefining the command
 * itself; <i>e.g.</i>, an RDG may define the command as
 * <code>exec:command = myProgram</code>, which may be changed to
 * <code>/usr/local/bin/myProgramVersion2</code> or anything the provider so
 * chooses. This allows RDGs to publish generic command names without committing
 * nor revealing invocation internals.
 * <li>The execution environment is set in the <code>setEnvironment</code>
 * method. This method may be overridden to set the execution environment as
 * appropriate. By default, the environment is cleared of all settings.
 * <li>The program is executed by passing the command and its arguments directly
 * to a process execution method. A shell is not explicitly invoked, so there is
 * no explicit file name expansion or other shell pre-execution interpretation
 * of the arguments.
 * </ul>
 * 
 * @see ExecServlet
 * @author Damian Gessler <dgessler@iplantcollaborative.org>
 * 
 */
public interface Exec {
	
	/**
	 * Basic parsing and sanity checking of the command line. Parsing implies
	 * deconstructing a single string into a string array analogous to the array
	 * argument passed to Java main(). Override this method to validate, sanitize, and otherwise change the
	 * command and the user arguments. 
	 * 
	 * @param commandLineStr
	 *            a concatenation of the <code>RDG</code>'s
	 *            <code>exec:command</code> and the <code>RIG</code>'s
	 *            <code>exec:args</code>.
	 * @return Java main() equivalent of command line arguments. The command
	 *         itself must be the first element.
	 * @throws ArgumentTooLongException
	 *             if an argument exceeds a preset maximum
	 * @throws TooManyArgumentsException
	 *             if the number of arguments exceeds a preset maximum
	 */
	public String[] getCommandLine(String commandLineStr) throws TooManyArgumentsException, ArgumentTooLongException;
	
	/**
	 * Set the process' execution environment. Override this method to set the
	 * execution environment. Default implementation is to clear the
	 * environment.
	 * 
	 * @param env
	 *            the default environment <code>ProcessBuilder</code>
	 *            environment
	 * @see ProcessBuilder#environment()
	 */
	public void setEnvironment(Map<String, String> env);

}
