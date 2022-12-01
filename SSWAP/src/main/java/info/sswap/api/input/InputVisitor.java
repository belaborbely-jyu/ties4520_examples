/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;

/**
 * Visitor interface for {@link Input} and {@link InputValue} objects.
 * 
 * @author Evren Sirin
 */
public interface InputVisitor {
	public void visit(AtomicInput input);
	
	public void visit(IntersectionInput input);
	
	public void visit(UnionInput input);
	
	public void visit(EnumeratedInput input);
	
	public void visit(PropertyInput input);

	public void visit(URIValue value);
	
	public void visit(BNodeValue value);
	
	public void visit(LiteralValue value);
}
