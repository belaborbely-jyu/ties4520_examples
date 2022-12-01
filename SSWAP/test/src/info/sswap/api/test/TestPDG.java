/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.test;

import java.net.URI;
import java.net.URISyntaxException;

import info.sswap.api.model.PDG;
import info.sswap.api.model.RDG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPProvider;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.model.SSWAPType;

public class TestPDG {
	public static void main(String[] args) throws URISyntaxException {
		
		PDG pdg = SSWAP.getPDG( URI.create("http://sswap.info/examples/resourceProvider") );
		System.out.println("PDG URI: " + pdg.getURI());
		System.out.println();
		
		System.out.println( "Dereferenced " + pdg.isDereferenced() );
		System.out.println();
		
		pdg.dereference();
		
		System.out.println( "Dereferenced " + pdg.isDereferenced() );
		System.out.println();
				
		SSWAPProvider provider = pdg.getProvider();
		
		System.out.println("Provider URI: " + provider.getURI());
		System.out.println("Name: " + provider.getName());
		System.out.println("Description: " + provider.getOneLineDescription());
		System.out.println("AboutURI : " + provider.getAboutURI());
		System.out.println("Metadata : " + provider.getMetadata());
		
		// now test getting provider just by itself
		
		provider = SSWAP.createProvider( URI.create("http://sswap.info/examples/resourceProvider") );
		
		System.out.println( "Dereferenced " + provider.isDereferenced() );
		System.out.println();
		
		provider.dereference();
		
		System.out.println( "Dereferenced " + provider.isDereferenced() );
		System.out.println();
		
		System.out.println("Provider URI: " + provider.getURI());
		System.out.println("Name: " + provider.getName());
		System.out.println("Description: " + provider.getOneLineDescription());
		System.out.println("AboutURI : " + provider.getAboutURI());
		System.out.println("Metadata : " + provider.getMetadata());
		System.out.println();
		
		for (SSWAPResource r :  provider.getProvidesResources()) {
			r.dereference();
			
			System.out.println("Name: " + r.getName());
		}
		
		System.out.println();
		
		RDG rdg = SSWAP.getRDG( URI.create("http://sswap.info/proxies/NAR/databases/resources/Tumor_Gene_Family_Databases_TGDBs/resourceProxy") );
		
		System.out.println("URI: " + rdg.getURI());
		
		System.out.println( "Dereferenced " + rdg.isDereferenced() );
		System.out.println();
		
		rdg.dereference();
		
		System.out.println( "Dereferenced " + rdg.isDereferenced() );
		System.out.println();
		
		SSWAPResource resource = rdg.getResource();
		
		System.out.println("Name: " + resource.getName());
		System.out.println("Description: " + resource.getOneLineDescription());
		System.out.println("AboutURI : " + resource.getAboutURI());
		System.out.println("Metadata : " + resource.getMetadata());
		System.out.println("Input URI: " + resource.getInputURI());
		System.out.println("Output URI: " + resource.getOutputURI());
		
		provider = resource.getProvider();
		
		System.out.println("Provided by: " + provider.getURI());
		
		provider.dereference();
		
		System.out.println(provider.getName());
		
		SSWAPGraph graph = resource.getGraph();
		
		if (graph != null) {
			System.out.println("Graph is not null");
			System.out.println("Graph's type is " + graph.getDeclaredType());
			
			SSWAPSubject s = graph.getSubject();
			
			if (s != null) {
				System.out.println("Subject is not null");
				System.out.println("Subject's type is " + s.getDeclaredType().getURI());
				SSWAPType complementOfSubjectType = s.getDeclaredType().complementOf();
				SSWAPType union = s.getDeclaredType().unionOf(complementOfSubjectType);
				
				System.out.println("subject type sub-class of itself: " + s.getDeclaredType().isSubTypeOf(s.getDeclaredType()));
				System.out.println("complement of subject type sub-class of itself: " + complementOfSubjectType.isSubTypeOf(complementOfSubjectType));
				System.out.println("subject type sub-class of its complement: " + s.getDeclaredType().isSubTypeOf(complementOfSubjectType));
								
				System.out.println("subject type sub-class of union: " + s.getDeclaredType().isSubTypeOf(union));
				System.out.println("complement of subject type sub-class of union: " + complementOfSubjectType.isSubTypeOf(union));
				
				rdg.serialize(System.out);
			}
		}
	}
}
