/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.ontologies.exec.impl;

import info.aduna.io.FileUtil;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPLiteral;
import info.sswap.api.model.SSWAPNode;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.servlet.AbstractSSWAPServlet;
import info.sswap.ontologies.data.api.Data;
import info.sswap.ontologies.data.api.DataFactory;
import info.sswap.ontologies.exec.api.ArgumentTooLongException;
import info.sswap.ontologies.exec.api.Exec;
import info.sswap.ontologies.exec.api.ExecServlet;
import info.sswap.ontologies.exec.api.HotboxPoller;
import info.sswap.ontologies.exec.api.TooManyArgumentsException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.hp.hpl.jena.vocabulary.XSD;
import com.martiansoftware.jsap.CommandLineTokenizer;

/**
 * Implementation for ExecServlet.
 * 
 * @see ExecServlet
 */
public class ExecImpl extends AbstractSSWAPServlet implements Exec, HotboxPoller {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Limits on user-supplied arguments: size, length, and number
	 */
	private final static int BLOCK_SIZ = 8 * 1024; // 8KB
	private final static long MAX_BLOCKS = (2L * 1024 * 1024 * 1024) / BLOCK_SIZ; // maximum data to read
	private final static int EOF = -1;
	private final static int MAX_ARG_LENGTH = 8192;
	private final static int MAX_NUM_ARGS = 127;
	
	/**
	 * "RDGPath" as defined in web.xml. This is the path to the "public" directory where the RDF/XML RDGs reside.
	 */
	private String rdgPath = null;
	
	/**
	 * Optional "hotboxPath" as defined in web.xml. This is the directory that is polled
	 * (scanned every interval) for JSON files awaiting conversion to RDGs, to be
	 * placed in the public folder.
	 */
	private String hotboxPath = null;
	
	/**
	 * The hotbox poller; may be null if not invoked
	 */
	HotboxPoller hotboxPoller = null;
	
	/**
	 * Temporary working directory (TWD) for the executed process. Each process
	 * invocation gets a new TWD; TWD is recursively deleted upon normal or
	 * exception exit.
	 */
	private File tmpDir;
	

	/**
	 * Internal initialization for HttpServlet. This method is marked
	 * <code>public</code> solely for package access purposes and should not be
	 * called directly nor overridden.
	 */
	// called from super class construction process
	@Override
	public synchronized void init(ServletConfig servletConfig) throws ServletException {

		super.init(servletConfig);

		// same code as in super class
		if ( (rdgPath = servletConfig.getInitParameter("RDGPath")) != null ) {
			rdgPath = getServletContext().getRealPath(rdgPath);
		}
		
		if ( (hotboxPath = servletConfig.getInitParameter("hotboxPath")) != null ) {
			hotboxPath = getServletContext().getRealPath(hotboxPath);
		}

		if ( hotboxPath != null && rdgPath != null ) {
			try {
				hotboxPoller = HotboxPollerImpl.getInstance(hotboxPath,rdgPath);
			} catch ( Exception e ) {
				throw new ServletException(e);
			}
		}
				
		// may return with rdgPath == null and/or hotboxPath == null

	}
	
	/**
	 * This method is marked <code>protected</code> solely for package access
	 * purposes and should not be called directly nor overridden.
	 */
	@Override
	protected final void handleRequest(RIG rig) {

		tmpDir = null;
		OutputStream stdin = null; // will write to stdin of the command, so OutputStream
		InputStream stdout = null; // will read from stdout of the command, so InputStream
		InputStream stderr = null; // will read from stderr of the command, so InputStream
		
		SSWAPPredicate literalDataPredicate = rig.getPredicate(info.sswap.ontologies.sswapmeet.SSWAPMeet.Data.literalData);
		SSWAPPredicate exitValuePredicate = rig.getPredicate(info.sswap.ontologies.sswapmeet.SSWAPMeet.Exec.exitValue);
		SSWAPPredicate stderrPredicate = rig.getPredicate(info.sswap.ontologies.sswapmeet.SSWAPMeet.Exec.stderr);
		SSWAPPredicate literalValuePredicate = rig.getPredicate(info.sswap.ontologies.sswapmeet.SSWAPMeet.Data.literalData);
		
		try {

			RDG rdg = SSWAP.getRDG(rig.getURI());

			/*
			 * For security reasons, we get the command from the RDG, not the
			 * RIG. We never read the exec:command from the RIG because this
			 * could allow the user to inject a malicious command: the RIG's
			 * exec:command is silently ignored.
			 * 
			 * Once the command line is built, we pass it through a filter
			 * method that can be overridden: the service provider may actually
			 * state anything in the RDG (e.g, hide implementation path info)
			 * and replace it with anything at runtime. (This is not a security
			 * risk for the user, because any provider of a website/webservice
			 * can do anything anyway).
			 * 
			 * The default implementation returns the RDG's exec:command value,
			 * so the default is a "honest" transaction in executing what is
			 * published.
			 */
			String command = getStrValue(rdg.getResource(),info.sswap.ontologies.sswapmeet.SSWAPMeet.Exec.command);

			// we do not reset the value in the RIG (which will be returned to the user)
			if ( command.isEmpty() ) { // required property
				return;
			}

			// get the SSWAP Resource and translate it
			SSWAPResource sswapResource = rig.getResource();
			SSWAPResource translatedResource = rig.translate(sswapResource);
			
			// get the exec:args property and its value
			String args = getStrValue(translatedResource, info.sswap.ontologies.sswapmeet.SSWAPMeet.Exec.args);
			
			// build the commandLine appropriate for execution
			String[] commandLine = getCommandLine(command + " " + args);
			
			// for every SSWAP Subject of every SSWAP Graph ...
			for ( SSWAPGraph sswapGraph : sswapResource.getGraphs() ) {
				for ( SSWAPSubject sswapSubject : sswapGraph.getSubjects() ) {
					
					// translate the subject
					SSWAPSubject translatedSubject = rig.translate(sswapSubject);
					
					// start (execute) the command
					Process process = startProcess(commandLine);
					
					// stdin is the stream which to write the incoming data
					stdin = process.getOutputStream();

					Data inputData = DataFactory.Data(translatedSubject);
					InputStream inputStream = inputData.readData();

					// send the data to the executing process
					fillPipe(inputStream,stdin);
					
					// close up
					stdin.close();
					stdin = null;

					// wait 'til done
					process.waitFor();	// should put a timer on this
					
					// grab the output
					stdout = process.getInputStream();
					stderr = process.getErrorStream();

					// set the return value
					int exitValue = process.exitValue();
					
					// Read the response
					ByteArrayInputStream stdoutResponse = new ByteArrayInputStream(ByteStreams.toByteArray(stdout));
					String stderrStr = null;
					
					try {
						stderrStr = new String(ByteStreams.toByteArray(stderr)).trim();
						if ( stderrStr.isEmpty() ) {
							stderr = null;
						}
					} catch ( Exception e ) {
						;
					}
					
					// write the response to every SSWAP Object to which this SSWAP Subject maps
					for ( SSWAPObject sswapObject : sswapSubject.getObjects() ) {
						// if there is no data:literalValue on the object and the object is anonymous, add data:literalValue
						if (sswapObject.isAnonymous() && (sswapObject.getProperty(literalValuePredicate) == null)) {
							sswapObject.setProperty(literalValuePredicate, "");
						}						
						
						// establish the existence of data:literalData property for serializing stdout as a property value
						sswapObject.setProperty(literalDataPredicate, "");
						
						// stdout
						Data outputData = DataFactory.Data(sswapObject);
						outputData.writeData(stdoutResponse);
						stdoutResponse.reset();
						
						// stderr
						if ( stderrStr != null ) {
							SSWAPLiteral stderrLiteral = rig.createTypedLiteral(stderrStr, URI.create(XSD.xstring.toString()));
							sswapObject.setProperty(stderrPredicate,stderrLiteral);
						}

						// exit value
						SSWAPLiteral exitValueLiteral = rig.createTypedLiteral(String.valueOf(exitValue), URI.create(XSD.integer.toString()));
						sswapObject.setProperty(exitValuePredicate,exitValueLiteral);
					}
				}
				
			}
			
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		} finally {
			close(stdin,stdout,stderr);
		}
	}
	
	/**
	 * @inheritDoc
	 */
	public String[] getCommandLine(String commandLineStr) throws TooManyArgumentsException, ArgumentTooLongException {
		
		String[] args = CommandLineTokenizer.tokenize(commandLineStr);
		validateArgs(args);
		
		return args;
	}
	
	/**
	 * @inheritDoc
	 */
	public void setEnvironment(Map<String, String> env) {
		env.clear();
	}

	/**
	 * Get a String value from a property.
	 * 
	 * @param sswapNode individual with the property
	 * @param ontologyPredicate predicate of the property
	 * @return first string value retrieved; empty string "" on any error
	 */
	private String getStrValue(SSWAPNode sswapNode, URI ontologyPredicate) {
		
		String valueStr = "";
		
		if ( sswapNode != null ) {
		
			SSWAPPredicate sswapPredicate = sswapNode.getDocument().getPredicate(ontologyPredicate);
			
			if ( sswapPredicate != null ) {
				
				SSWAPProperty sswapProperty = sswapNode.getProperty(sswapPredicate);
		
				if ( sswapProperty != null && sswapProperty.getValue() != null ) {
					valueStr = sswapProperty.getValue().asString();
				}
			}
		}
		
		return valueStr;
	}
	
	/**
	 * Start the process. This implies sending the command line to be exectued
	 * to the operating system.
	 * 
	 * @param commandLine
	 *            the command and its arguments to be executed
	 * @return the Process object for process control
	 * @throws IOException
	 *             on any error to execute the process
	 */
	private Process startProcess(String[] commandLine) throws IOException {
		
		ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
		
		// service provider can override the environment
		setEnvironment(processBuilder.environment());
		
		// create a unique tmp dir
		try {
			tmpDir = Files.createTempDir();
			if ( ! tmpDir.canRead() || ! tmpDir.canWrite() ) {
				throw new Exception();
			}
		} catch ( Exception e ) {
			throw new IOException("Exec: Cannot create a temporary working directory");
		}
		
		processBuilder.directory(tmpDir);
		
		return processBuilder.start();
	}
	
	/**
	 * Simple argument sanity checking.
	 * 
	 * @param args array of arguments, not including the command itself
	 * @throws TooManyArgumentsException if the number of arguments exceeds a preset maximum
	 * @throws ArgumentTooLongException if the length of any argument exceeds a preset maximum
	 */
	private void validateArgs(String[] args) throws TooManyArgumentsException, ArgumentTooLongException {
		
		if ( args.length > MAX_NUM_ARGS ) {
			throw new TooManyArgumentsException("Number of arguments cannot exceed " + MAX_NUM_ARGS);
		}
		
		for ( int i = 0; i < args.length; i++ ) {
			if ( args[i].length() > MAX_ARG_LENGTH ) {
				// we don't report 'i', because arguments and values
				// may be combined or separate (e.g., '-oX' or '-o X'),
				// thereby confounding an 'argument' count
				throw new ArgumentTooLongException((i == 0 ? "The command" : "A command argument") + " exceeds the maximum length of " + MAX_ARG_LENGTH);
			}
		}
		
	}
	
	/**
	 * Byte-count limited transferring of bytes from the inputStream to the
	 * outputStream.
	 * 
	 * @param inputStream
	 *            data to read
	 * @param stdin
	 *            stream to write
	 * @throws IOException
	 *             on any error
	 */
	private void fillPipe(InputStream inputStream, OutputStream stdin) throws IOException {
		
		// for security, we limit the number of bytes read
		long maxBytes = BLOCK_SIZ * MAX_BLOCKS;
		int numBytesRead = 0; // any value except EOF
		byte[] buf = new byte[BLOCK_SIZ];
		
		for ( int bytesRead = 0; numBytesRead != EOF && bytesRead < maxBytes; ) {
			
			if ( (numBytesRead = inputStream.read(buf,0,BLOCK_SIZ)) > 0 ) {					
				stdin.write(buf,0,numBytesRead);
				bytesRead += numBytesRead;
			}
		}
		
		if ( numBytesRead != EOF ) {
			throw new IOException("Input exceeded maximum number of bytes (" + maxBytes / 1024 + "KB)");
		}
		
	}

	/**
	 * Close streams to release resources and flush output.
	 * 
	 * @param stdin output stream accepting data as input to send to process to execute
	 * @param stdout input stream for reading data from executed process
	 * @param stderr input stream for reading error messages from executed process
	 */
	private void close(OutputStream stdin, InputStream stdout, InputStream stderr) {
		
		if ( tmpDir != null ) {
			try {
				FileUtil.deltree(tmpDir);		
			} finally {
				tmpDir = null;
			}
		}
		
		if ( stdin != null ) {
			try {
				stdin.close();
			} catch ( IOException e ) {
				; // consume
			} finally {
				stdin = null;
			}
		}
		
		if ( stdout != null ) {
			try {
				stdout.close();
			} catch ( IOException e ) {
				; // consume
			} finally {
				stdout = null;
			}
		}
		
		if ( stderr != null ) {
			try {
				stderr.close();
			} catch ( IOException e ) {
				; // consume
			} finally {
				stderr = null;
			};
		}
	}


	/**
	 * @inheritDoc
	 */
	@Override
	public void start() {
		
		if ( hotboxPoller != null ) {
			hotboxPoller.start();
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void setInterval(int sleepSeconds) {
		
		if ( hotboxPoller != null ) {
			hotboxPoller.setInterval(sleepSeconds);
		}
		
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void stop() {
		
		if ( hotboxPoller != null ) {
			hotboxPoller.stop();
		}
	
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean isRunning() {
		
		if ( hotboxPoller != null ) {
			return hotboxPoller.isRunning();
		}
		
		return false;
	}
	
}
