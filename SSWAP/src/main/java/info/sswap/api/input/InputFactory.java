/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;

import info.sswap.api.input.impl.AtomicInputImpl;
import info.sswap.api.input.impl.BNodeValueImpl;
import info.sswap.api.input.impl.EnumeratedInputImpl;
import info.sswap.api.input.impl.IntersectionInputImpl;
import info.sswap.api.input.impl.LiteralValueImpl;
import info.sswap.api.input.impl.PropertyInputImpl;
import info.sswap.api.input.impl.URIValueImpl;
import info.sswap.api.input.impl.UnionInputImpl;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

/**
 * Factory class used to create {@link Input} objects directly.
 * 
 * @author Evren Sirin
 */
public class InputFactory {
	public static Input createUnrestricedInput() {
		return new AtomicInputImpl();
	}

	public static AtomicInput createAtomicInput(URI type) {
		return new AtomicInputImpl(type);
	}

	public static IntersectionInput createIntersectionInput(Input... inputs) {
		return createIntersectionInput(Arrays.asList(inputs));
	}

	public static IntersectionInput createIntersectionInput(Collection<Input> inputs) {
		return new IntersectionInputImpl(inputs);
	}

	public static UnionInput createUnionInput(Input... inputs) {
		return createUnionInput(Arrays.asList(inputs));
	}

	public static UnionInput createUnionInput(Collection<Input> inputs) {
		return new UnionInputImpl(inputs);
	}

	public static EnumeratedInput createEnumeratedInput(InputValue... values) {
		return createEnumeratedInput(Arrays.asList(values));
	}

	public static EnumeratedInput createEnumeratedInput(Collection<InputValue> values) {
		return new EnumeratedInputImpl(values);
	}

	public static PropertyInput createPropertyInput(URI property) {
		return new PropertyInputImpl(property);
	}

	public static PropertyInput createPropertyInput(URI property, Input range) {
		PropertyInput input = createPropertyInput(property);
		input.setRange(range);
		
		if (range != null) {
			range.setPropertyInput(input);
		}
		return input;
	}

	public static PropertyInput createPropertyInput(URI property, Input range, int minCardinality, int maxCardinality) {
		PropertyInput input = createPropertyInput(property);
		input.setRange(range);
		if (range != null) {
			range.setPropertyInput(input);
		}
		input.setMinCardinality(minCardinality);
		input.setMaxCardinality(maxCardinality);
		return input;
	}

	/**
	 * Creates a URI value.
	 */
	public static URIValue createURI(String uri) {
		return createURI(URI.create(uri));
	}

	/**
	 * Creates a URI value.
	 */
	public static URIValue createURI(URI uri) {
		return new URIValueImpl(uri);
	}

	/**
	 * Creates a bnode value.
	 */
	public static BNodeValue createBNode(String bnodeID) {
		return new BNodeValueImpl(bnodeID);
	}

	/**
	 * Creates a plain literal with no language tag or datatype URI.
	 */
	public static LiteralValue createLiteral(String label) {
		return new LiteralValueImpl(label);
	}

	/**
	 * Creates a literal with the given label and language tag.
	 */
	public static LiteralValue createLiteral(String label, String language) {
		return new LiteralValueImpl(label, language);
	}

	/**
	 * Creates a typed literal with the given datatype URI.
	 */
	public static LiteralValue createLiteral(String label, URI datatype) {
		return new LiteralValueImpl(label, datatype);
	}
}
