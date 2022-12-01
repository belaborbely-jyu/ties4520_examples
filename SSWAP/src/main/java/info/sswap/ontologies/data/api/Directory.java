/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */

package info.sswap.ontologies.data.api;

import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPSubject;

import java.util.Collection;

/**
 * A {@code data:DataBundle} from the {@code data} ontology is an individual
 * with one or more {@code data:hasData} statements. A {@code Directory} is akin
 * to a {@code DataBundle}, but allows for the possibility of zero
 * {@code data:hasData} statements; a {@code Directory} is essentially a virtual
 * directory of data (URIs), analogous to a file system directory of files.
 * <p>
 * The object of a {@code data:hasData} property (or its subproperties) is a
 * {@code data:DataFormat} resource: this API wraps those objects as
 * {@link Data} objects. Thus the model allows n-ary indirection on data reading
 * and writing: a {@code Directory} built from a {@code SSWAPIndividual} may
 * reference its multiple {@code Data} sources and sinks (for example, as
 * multiple input files to a service or multiple output files, all bundled from
 * a single {@code data:DataBundle} {@link SSWAPSubject} or {@link SSWAPObject}
 * respectively).
 * <p>
 * If the source individual is itself of type {@code data:DataFormat}, then it
 * too is included in the {@code Directory}.
 * 
 * @author Damian Gessler
 **/
public interface Directory {
	
	/**
	 * The individual subject for the {@code data:hasData} statements.
	 * 
	 * @return the source SSWAPIndividual for this Directory
	 * @see DataFactory#Directory(SSWAPIndividual)
	 */
	public SSWAPIndividual getIndividual();

	/**
	 * For every {@code data:hasData} statement on the subject individual (set
	 * at the {@link DataFactory#Directory(SSWAPIndividual)}), return a
	 * {@code Collection} of {@link Data} objects constructed from property
	 * instance values. Include in this {@code Collection} the source individual
	 * itself if it belongs to the type {@code data:DataFormat} (the
	 * {@code rdfs:range} of {@code data:hasData}), even if it does not have the
	 * property explicitly reflexive on itself.
	 * <p>
	 * One may then use the {@link Data#readData()},
	 * {@link Data#writeData(java.io.InputStream)}, etc. methods on the
	 * {@code Collection} of indirectly referenced data.
	 * <p>
	 * A single {@code Data} object shall be returned per unique
	 * {@code data:hasData} individual. Consider the following case: an
	 * individual has two distinct subproperties of {@code data:hasData}, each
	 * with the same individual value. In this case, the relation of the
	 * super-property {@code data:hasData} to the individual value may appear
	 * one or more times--there being no logical difference between it being
	 * present once, twice, thrice, etc. In the following pseudo-code we see
	 * only one instance of the {@code data:hasData} property:
	 * 
	 * <pre>
	 * :readFromThisFile rdfs:subPropertyOf data:hasData .
	 * :writeToThisFile rdfs:subPropertyOf data:hasData .
	 * :someInd :readFromThisFile :aFile .
	 * :someInd :writeToThisFile :aFile .
	 * </pre>
	 * 
	 * A reasoner will infer the single statement:
	 * 
	 * <pre>
	 * :someInd data:hasData :aFile .
	 * </pre>
	 * 
	 * Similarly, even if {@code :someInd} had only one subproperty, (it) or the
	 * super-property relation may appear one or more times; there being no new
	 * inferences that could be derived given the number of times a statement
	 * re-occurs. (Re the Open World Assumption, cardinality restrictions are
	 * relevant only if the object of multiple instances of an object property
	 * are not necessarily equivalent. Lexical [URI] equivalence implies
	 * necessary logical equivalence).
	 * <p>
	 * Thus, this method shall return a single {@code Data} object per unique
	 * {@code data:hasData} statement, regardless of the actual number of
	 * instances observed. It may not be assumed that reasoning is used by the
	 * method (the method need make no assumption about reasoning on the
	 * SSWAPDocument).
	 * <p>
	 * 
	 * @return Collection of {@code Data} objects from all unique
	 *         {@code data:hasData} property statements and the presence of the
	 *         type {@code data:DataFormat} on the subject individual itself, if
	 *         applicable. The absence of any {@code data:hasData} properties on
	 *         a non- {@code data:DataFormat} individual results in the return
	 *         of an empty (but not null) collection.
	 * 
	 * @see DataFactory#Directory(info.sswap.api.model.SSWAPIndividual)
	 */	
	public Collection<Data> getData();

	/**
	 * Build the {@code Collection} to be returned by {@link #getData}. Relevant
	 * if the subject individual has changed. Called automatically (and does not
	 * need to be called explicitly prior) to calling {@code getData} on its
	 * first invocation.
	 * 
	 * @throws DataException
	 *             Upon the presence of a {@code data:hasData} property, but a
	 *             failure to extract its value and create a {@code Data}
	 *             object.
	 */
	public void setData() throws DataException;
}
