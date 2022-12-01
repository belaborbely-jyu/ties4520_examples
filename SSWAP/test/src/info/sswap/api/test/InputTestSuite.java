/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * The suite of tests for Inputs/
 * @author Evren Sirin
 */
@RunWith(Suite.class)
@SuiteClasses( {
	InputSSWAPTests.class,
	InputJSONTests.class,
	InputJenaTests.class,
	InputValidationTests.class
})
public class InputTestSuite {
}
