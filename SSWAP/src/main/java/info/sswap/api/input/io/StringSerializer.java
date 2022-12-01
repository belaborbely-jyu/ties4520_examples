/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.io;

import info.sswap.api.input.AtomicInput;
import info.sswap.api.input.BNodeValue;
import info.sswap.api.input.EnumeratedInput;
import info.sswap.api.input.Input;
import info.sswap.api.input.InputVisitor;
import info.sswap.api.input.IntersectionInput;
import info.sswap.api.input.LiteralValue;
import info.sswap.api.input.NaryInput;
import info.sswap.api.input.PropertyInput;
import info.sswap.api.input.URIValue;
import info.sswap.api.input.UnionInput;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

import org.mindswap.pellet.utils.URIUtils;

/**
 * Serializes the given {@link Input} object into a pretty string that spans multiple lines with indentation.
 * 
 * @author Evren Sirin
 */
public class StringSerializer implements InputVisitor, InputSerializer<String>  {
	private static final String INDENT = "   ";

	private PrintWriter out;
	private String indent;

	public StringSerializer() {
	}

	public String serialize(Input input) {
		StringWriter sw = new StringWriter();
		serialize(sw, input);
		return sw.toString();
	}

	public void serialize(Writer writer, Input input) {
		indent = INDENT;
		out = new PrintWriter(writer);
		input.accept(this);
	}

	private void incIndent() {
		indent += INDENT;
	}

	private void decIndent() {
		indent = indent.substring(0, indent.length() - INDENT.length());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(AtomicInput input) {
		if (input.isUnrestricted()) {
			out.print("unrestricted");
		}
		else {	
			out.print(URIUtils.getLocalName(input.getType()));
		}
		
		if (input.getValue() != null) {
			out.print(" (value=" + input.getValue() + ")");
		}
		
		out.println();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(IntersectionInput intersection) {
		// out.println("Intersection");
		// incIndent();
		Iterator<Input> inputs = intersection.getInputs().iterator();
		while (inputs.hasNext()) {
			out.print(indent);
			Input input = inputs.next();
			input.accept(this);
		}
		// decIndent();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(UnionInput union) {
		out.println("Union:");
		incIndent();
		int option = 1;
		Iterator<Input> inputs = union.getInputs().iterator();
		while (inputs.hasNext()) {
			Input input = inputs.next();
			out.print(indent);			
			out.println("(" + (option++) + " " + input.getLabel() + ") ");
			incIndent();
			input.accept(this);
			decIndent();
		}
		decIndent();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(EnumeratedInput input) {
		out.print("Enumeration: " + input.getValues());
		if (input.getValue() != null) {
			out.print(" (value=" + input.getValue() + ")");
		}
		out.println();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(PropertyInput input) {
		String prop = URIUtils.getLocalName(input.getProperty());
		String min = input.getMinCardinality() == 0 ? "_" : String.valueOf(input.getMinCardinality());
		String max = input.getMaxCardinality() == Integer.MAX_VALUE ? "_" : String.valueOf(input.getMaxCardinality());
		out.print(prop + " (" + min + "," + max + "): ");
		
		Input range = input.getRange();
		if (range instanceof NaryInput || range instanceof PropertyInput) {
			out.println();
		}
		
		incIndent();
		range.accept(this);
		decIndent();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(URIValue value) {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(BNodeValue value) {
		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(LiteralValue value) {
		// do nothing
	}

}
