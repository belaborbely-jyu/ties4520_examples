/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import info.sswap.impl.empire.model.GraphImpl;
import info.sswap.impl.empire.model.ObjectImpl;
import info.sswap.impl.empire.model.PDGImpl;
import info.sswap.impl.empire.model.ProviderImpl;
import info.sswap.impl.empire.model.RDGImpl;
import info.sswap.impl.empire.model.RIGImpl;
import info.sswap.impl.empire.model.RQGImpl;
import info.sswap.impl.empire.model.RRGImpl;
import info.sswap.impl.empire.model.ResourceImpl;
import info.sswap.impl.empire.model.SubjectImpl;

import org.junit.Test;

import com.clarkparsia.empire.codegen.InstanceGenerator;

/**
 * Tests whether all abstract classes that are meant to be completed by Empire (i.e., they
 * contain abstract methods with Empire-specific annotations) can actually be completed by Empire
 * and instantiated.
 * 
 * These tests are to detect programming errors that occur if a SSWAP interface is changed (e.g., a new method
 * is added or an existing signature of a method is changed), and the implementation of this interface is not updated
 * accordingly. This kind of error is not detected by a compiler because the implementations of these interfaces are often abstract
 * because they contain methods to be completed by Empire.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 *
 */
public class EmpireGenerationTest {
	/**
	 * Tests the implementation of PDG.
	 */
	@Test
	public void testPDGImpl() {
		testInstanceGeneration(PDGImpl.class);
	}
	
	/**
	 * Tests the implementation of RDG
	 */
	@Test
	public void testRDGImpl() {
		testInstanceGeneration(RDGImpl.class);
	}
	
	/**
	 * Tests the implementation of RIG
	 */
	@Test
	public void testRIGImpl() {
		testInstanceGeneration(RIGImpl.class);
	}
	
	/**
	 * Tests the implementation of RRG
	 */
	@Test
	public void testRRGImpl() {
		testInstanceGeneration(RRGImpl.class);
	}
	
	/**
	 * Tests the implementation of RQG
	 */
	@Test
	public void testRQGImpl() {
		testInstanceGeneration(RQGImpl.class);
	}

	/**
	 * Tests the implementation of SSWAPSubject
	 */
	@Test
	public void testSubjectImpl() {
		testInstanceGeneration(SubjectImpl.class);
	}
	
	/**
	 * Tests the implementation of SSWAPResource
	 */
	@Test
	public void testResourceImpl() {
		testInstanceGeneration(ResourceImpl.class);
	}
	
	/**
	 * Tests the implementation of SSWAPProvider
	 */
	@Test
	public void testProviderImpl() {
		testInstanceGeneration(ProviderImpl.class);
	}
	
	/**
	 * Tests the implementation of SSWAPObject
	 */
	@Test
	public void testObjectImpl() {
		testInstanceGeneration(ObjectImpl.class);
	}
	
	/**
	 * Tests the implementation of SSWAPGraph
	 */
	@Test
	public void testGraphImpl() {
		testInstanceGeneration(GraphImpl.class);
	}
		
	/**
	 * Checks whether the class contains no abstract methods. If there is
	 * an abstract method, the test is failed.
	 * 
	 * @param <T> the class parameter
	 * @param clazz the class to be tested
	 */
	private <T> void testForAbstractMethods(Class<T> clazz) {
		for (Method method : clazz.getMethods()) {
			assertFalse(Modifier.isAbstract(method.getModifiers()));
		}
	}
	
	/**
	 * Checks whether the given class with Empire annotations can be completed by Empire, whether
	 * the Empire-generated class contains no abstract methods, and whether it can be instantiated.
	 * 
	 * @param <T> the class parameter
	 * @param clazz the class to be tested
	 * @return a generated instance of the class
	 */
	private <T> T testInstanceGeneration(Class<T> clazz) {
		try {
			System.out.println(clazz.toString());
			for (Method method : clazz.getMethods()) {
				if (Modifier.isAbstract(method.getModifiers())) {
					System.out.println(method.getName());
				}
			}
			
			Class<T> generatedClass = InstanceGenerator.generateInstanceClass(clazz);
		
			testForAbstractMethods(generatedClass);
		
			return generatedClass.newInstance();
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail();
			
			// the line below won't be executed (since fail() always generates an exception), but
			// the compiler will complain if that return statement is missing
			return null;
		}
	}
}
