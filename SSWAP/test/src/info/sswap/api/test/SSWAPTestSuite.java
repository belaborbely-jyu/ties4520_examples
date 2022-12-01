/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import info.sswap.api.model.SSWAP;
import info.sswap.impl.empire.io.ClosureBuilderFactory;
import javassist.ClassClassPath;
import javassist.ClassPool;

import org.junit.BeforeClass;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.InitializationError;

import junit.framework.JUnit4TestAdapter;

/**
 * The suite of tests for SSWAP Java API
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
@RunWith(Suite.class)
@SuiteClasses( {
	ProviderTests.class,
	TypeTests.class,
	EmpireGenerationTest.class,
	RDGTests.class,
	RIGTests.class,
	IndividualTests.class,
	ClosureTests.class,
	PredicateTests.class,
	RRGTests.class,
	ExpressivityDetectionTests.class,
	RQGTests.class,
	ReasoningTests.class,
	InputTestSuite.class,
	HTTPAPITests.class,
	ExtensionAPITests.class,
	TSVTests.class,
	CreateObject.class,
	SharedURITests.class,
	CrossDocumentReasoningTests.class
})
public class SSWAPTestSuite {
	@BeforeClass
	public static void beforeClass() {
		SSWAP.getCache().clear();
	}
	
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(SSWAPTestSuite.class);
	}
	
	public static void main(String[] args) throws InitializationError {
		// required if this is code called in an environment with a custom class loader (e.g., Emma)
		ClassPool.getDefault().insertClassPath(new ClassClassPath(SSWAPTestSuite.class));
		
		JUnitCore.main(SSWAPTestSuite.class.getName());
	}
}
