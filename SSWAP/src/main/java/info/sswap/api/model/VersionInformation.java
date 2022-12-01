/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Provides information about the version of Java API.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class VersionInformation {
	/**
	 * Interface to Logging API
	 */
	private static final Logger LOGGER = LogManager.getLogger(VersionInformation.class);
	
	/**
	 * Default value for the release date if no other information is available
	 */
	private static String UNKNOWN = "(unknown)";
	
	/**
	 * Name of the property that contains the version of the API
	 */
	private static String VERSION_PROPERTY = "info.sswap.api.version";
	
	/**
	 * Name of the property that contains the release date.
	 */
	private static String RELEASE_DATE_PROPERTY = "info.sswap.api.releaseDate";

	/**
	 * Contains properties read from the file.
	 */
	private Properties versionProperties = null;

	/**
	 * Creates new version information object. The object reads version data from a resource file that is bundled
	 * along with the API.
	 */
	private VersionInformation() {
		versionProperties = new Properties();

		// read the data from the resource that should be in the same jar file as this class
		InputStream vstream = VersionInformation.class.getResourceAsStream("/info/sswap/api/model/version.properties");
		
		if (vstream != null) {
			try {
				versionProperties.load(vstream);
			}
			catch (IOException e) {
				LOGGER.error("Could not load version properties", e);
			}
			finally {
				try {
					vstream.close();
				}
				catch (IOException e) {
					LOGGER.error("Could not close version properties", e);
				}
			}
		}
	}

	/**
	 * Static getter for the version information.
	 * @return this class
	 */
	public static final VersionInformation get() {
		return new VersionInformation();
	}

	/**
	 * Returns the version of Java API
	 * @return the version of Java API or "(unreleased)" if the version information is missing
	 */
	public String getVersionString() {
		return versionProperties.getProperty(VERSION_PROPERTY, "(unreleased)");
	}

	/**
	 * Returns the date of the release 
	 * 
	 * @return the date of the release or "(unknown)" if the date is not known
	 */
	public String getReleaseDate() {
		return versionProperties.getProperty(RELEASE_DATE_PROPERTY, UNKNOWN);
	}

	/**
	 * Returns information about the version and release date as a single string.
	 * 
	 * @return the string containing information about the version and release date of this software
	 */
	public String toString() {
		return "Version: " + getVersionString() + " Released: " + getReleaseDate();
	}
}
