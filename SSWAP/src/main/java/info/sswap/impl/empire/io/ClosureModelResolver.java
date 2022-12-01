/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.io;

import java.net.URI;

import info.sswap.api.model.ModelResolver;
import info.sswap.api.model.SSWAPProtocol;
import info.sswap.impl.empire.model.SourceModel;
import info.sswap.impl.empire.model.SourceModelImpl;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Invokes the closure builder for model resolution
 * 
 * @author Pavel Klinov
 *
 */
public class ClosureModelResolver implements ModelResolver {

	private ClosureBuilder getClosureBuilder(SourceModelImpl model) {
		ClosureBuilderFactory builderFactory = ClosureBuilderFactory.newInstance();
		
		if (model.getMaxClosureThreads() != -1) {
			builderFactory = builderFactory.setMaxThreads((int)model.getMaxClosureThreads());
		}
		
		if (model.getMaxClosureTime() != -1) {
			builderFactory = builderFactory.setMaxTime(model.getMaxClosureTime());
		}
		
		if (model.getMaxClosureBytes() != -1) {
			builderFactory = builderFactory.setMaxBytes(model.getMaxClosureBytes());
		}
		
		return builderFactory.newBuilder();
	}
	
	@Override
	public Model resolveSourceModel(SourceModel model) {
		//TODO The cast isn't great but otherwise we won't get all needed info, i.e. max closure threads
		//should those really be configured per model and not globally?
		Closure closure = getClosureBuilder((SourceModelImpl)model).build(model.getModel(), model.getURI().toString());
		
		if (closure.getClosureModel() != null) {
			return closure.getClosureModel();
		}
		else {
			return model.getModel();
		}		
	}

	@Override
	public Model resolveProtocolModel(SSWAPProtocol protocol) {
		// Just do the same, the closure builder can't do any better
		return resolveSourceModel((SourceModel)protocol);
	}

	@Override
	public Model resolveTerm(URI termURI) {
		Closure closure = ClosureBuilderFactory.newInstance().newBuilder().build(null, termURI.toString());
		
		return closure.getClosureModel();
	}
}
