/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.impl.empire.model;

import info.sswap.api.model.Expressivity;
import info.sswap.api.model.ValidationException;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.profiles.CycleInDatatypeDefinition;
import org.semanticweb.owlapi.profiles.DatatypeIRIAlsoUsedAsClassIRI;
import org.semanticweb.owlapi.profiles.LastPropertyInChainNotInImposedRange;
import org.semanticweb.owlapi.profiles.LexicalNotInLexicalSpace;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWL2DLProfileViolation;
import org.semanticweb.owlapi.profiles.OWL2DLProfileViolationVisitor;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWL2ELProfileViolation;
import org.semanticweb.owlapi.profiles.OWL2ELProfileViolationVisitor;
import org.semanticweb.owlapi.profiles.OWL2ProfileViolation;
import org.semanticweb.owlapi.profiles.OWL2ProfileViolationVisitor;
import org.semanticweb.owlapi.profiles.OWL2QLProfile;
import org.semanticweb.owlapi.profiles.OWL2QLProfileViolation;
import org.semanticweb.owlapi.profiles.OWL2QLProfileViolationVisitor;
import org.semanticweb.owlapi.profiles.OWL2RLProfile;
import org.semanticweb.owlapi.profiles.OWL2RLProfileViolation;
import org.semanticweb.owlapi.profiles.OWL2RLProfileViolationVisitor;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;
import org.semanticweb.owlapi.profiles.OntologyIRINotAbsolute;
import org.semanticweb.owlapi.profiles.OntologyVersionIRINotAbsolute;
import org.semanticweb.owlapi.profiles.UseOfAnonymousIndividual;
import org.semanticweb.owlapi.profiles.UseOfBuiltInDatatypeInDatatypeDefinition;
import org.semanticweb.owlapi.profiles.UseOfDataOneOfWithMultipleLiterals;
import org.semanticweb.owlapi.profiles.UseOfDefinedDatatypeInDatatypeRestriction;
import org.semanticweb.owlapi.profiles.UseOfIllegalAxiom;
import org.semanticweb.owlapi.profiles.UseOfIllegalClassExpression;
import org.semanticweb.owlapi.profiles.UseOfIllegalDataRange;
import org.semanticweb.owlapi.profiles.UseOfIllegalFacetRestriction;
import org.semanticweb.owlapi.profiles.UseOfNonAbsoluteIRI;
import org.semanticweb.owlapi.profiles.UseOfNonAtomicClassExpression;
import org.semanticweb.owlapi.profiles.UseOfNonEquivalentClassExpression;
import org.semanticweb.owlapi.profiles.UseOfNonSimplePropertyInAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.profiles.UseOfNonSimplePropertyInCardinalityRestriction;
import org.semanticweb.owlapi.profiles.UseOfNonSimplePropertyInDisjointPropertiesAxiom;
import org.semanticweb.owlapi.profiles.UseOfNonSimplePropertyInFunctionalPropertyAxiom;
import org.semanticweb.owlapi.profiles.UseOfNonSimplePropertyInInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.profiles.UseOfNonSimplePropertyInIrreflexivePropertyAxiom;
import org.semanticweb.owlapi.profiles.UseOfNonSimplePropertyInObjectHasSelf;
import org.semanticweb.owlapi.profiles.UseOfNonSubClassExpression;
import org.semanticweb.owlapi.profiles.UseOfNonSuperClassExpression;
import org.semanticweb.owlapi.profiles.UseOfObjectOneOfWithMultipleIndividuals;
import org.semanticweb.owlapi.profiles.UseOfObjectPropertyInverse;
import org.semanticweb.owlapi.profiles.UseOfPropertyInChainCausesCycle;
import org.semanticweb.owlapi.profiles.UseOfReservedVocabularyForAnnotationPropertyIRI;
import org.semanticweb.owlapi.profiles.UseOfReservedVocabularyForClassIRI;
import org.semanticweb.owlapi.profiles.UseOfReservedVocabularyForDataPropertyIRI;
import org.semanticweb.owlapi.profiles.UseOfReservedVocabularyForIndividualIRI;
import org.semanticweb.owlapi.profiles.UseOfReservedVocabularyForObjectPropertyIRI;
import org.semanticweb.owlapi.profiles.UseOfReservedVocabularyForOntologyIRI;
import org.semanticweb.owlapi.profiles.UseOfReservedVocabularyForVersionIRI;
import org.semanticweb.owlapi.profiles.UseOfTopDataPropertyAsSubPropertyInSubPropertyAxiom;
import org.semanticweb.owlapi.profiles.UseOfUndeclaredAnnotationProperty;
import org.semanticweb.owlapi.profiles.UseOfUndeclaredClass;
import org.semanticweb.owlapi.profiles.UseOfUndeclaredDataProperty;
import org.semanticweb.owlapi.profiles.UseOfUndeclaredDatatype;
import org.semanticweb.owlapi.profiles.UseOfUndeclaredObjectProperty;
import org.semanticweb.owlapi.profiles.UseOfUnknownDatatype;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Checks whether an OWLOntology has a specified expressivity
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 */
public class ExpressivityChecker {
	@SuppressWarnings("unchecked")
	private static final Predicate<OWLProfileViolation> IGNORE_UNDECLARED_ANNOTATION_PROPERTY = (Predicate) Predicates
	                .instanceOf(UseOfUndeclaredAnnotationProperty.class);

	@SuppressWarnings("unchecked")
	private static final Predicate<OWLProfileViolation> IGNORE_UNDECLARED_ENTITY = (Predicate) Predicates
	                .or(Predicates.instanceOf(UseOfUndeclaredAnnotationProperty.class),
	                    Predicates.instanceOf(UseOfUndeclaredClass.class),
	                    Predicates.instanceOf(UseOfUndeclaredDataProperty.class),
	                    Predicates.instanceOf(UseOfUndeclaredObjectProperty.class));

	private static final Function<OWLProfileViolation, String> PROFILE_DESCRIPTION_GENERATOR = new Function<OWLProfileViolation, String>() {
		@Override
		public String apply(OWLProfileViolation profileViolation) {
			return ProfileViolationDescriptionGenerator.generateMessage(profileViolation);
		}
	};

	
	/**
	 * A mapping between SSWAP expressivities to OWLAPI OWLProfiles
	 */
	private Map<Expressivity,OWLProfile> profileMap;	
	
	/**
	 * Creates a new expressivity checker
	 */
	public ExpressivityChecker() {		
		profileMap = new EnumMap<Expressivity,OWLProfile>(Expressivity.class);
		 
		profileMap.put(Expressivity.EL, new OWL2ELProfile());
		profileMap.put(Expressivity.DL, new OWL2DLProfile());
		profileMap.put(Expressivity.RL, new OWL2RLProfile());
		profileMap.put(Expressivity.QL, new OWL2QLProfile());
	}
	
	private boolean isSupported(Expressivity expressivity) {
		return profileMap.containsKey(expressivity);
	}
	
	/**
	 * Checks whether the given OWLOntology belongs to the specified profile/expressivity
	 * 
	 * @param expressivity the expressivity to be checked
	 * @param ontology the ontology to be checked
	 * @return true if the ontology is in the profile
	 */
	public boolean checkProfile(Expressivity expressivity, OWLOntology ontology) {
		return isSupported(expressivity)
		       && validateProfile(expressivity, ontology, IGNORE_UNDECLARED_ANNOTATION_PROPERTY).isEmpty();
	}
	
	public Collection<String> getViolationExplanations(Expressivity expressivity, OWLOntology ontology) {
		Collection<OWLProfileViolation> profileViolations = validateProfile(expressivity, ontology,
		                                                                    IGNORE_UNDECLARED_ANNOTATION_PROPERTY);

		return Collections2.transform(profileViolations, PROFILE_DESCRIPTION_GENERATOR);
	}
		
	/**
	 * Checks whether the given OWLOntology belongs to the specified profile/expressivity,
	 * while ignoring any violations about undefined entities.
	 * 
	 * @param expressivity the expressivity to be checked
	 * @param ontology the ontology to be checked
	 * @throws ValidationException if the ontology does not pass the check
	 */
	public void validateProfileIgnoringUndefinedEntities(Expressivity expressivity, OWLOntology ontology)
	                throws ValidationException {
		Iterable<OWLProfileViolation> profileViolations = validateProfile(expressivity, ontology,
		                                                                  IGNORE_UNDECLARED_ENTITY);

		OWLProfileViolation profileViolation = Iterables.getFirst(profileViolations, null);
		if (profileViolation != null) {
			throw new ValidationException(ProfileViolationDescriptionGenerator.generateMessage(profileViolation));
		}
	}
	
	
	/**
	 * Validates the ontology for the given profile expressivity and returns any violations that does not satisfy the
	 * given predicate. Returns emoty results for unsupported expressivity;
	 */
	private Collection<OWLProfileViolation> validateProfile(Expressivity expressivity, OWLOntology ontology,
	                Predicate<OWLProfileViolation> ignoredViolations) {
		OWLProfile profile = profileMap.get(expressivity);
		
		if (profile == null) {
			return ImmutableList.of();
		}
		
		OWLProfileReport report = profile.checkOntology(ontology);
		
		if (report.isInProfile()) {
			return ImmutableList.of();
		}
		
		return Collections2.filter(report.getViolations(), Predicates.not(ignoredViolations));
	}
	
	private static class ProfileViolationDescriptionGenerator implements OWL2ProfileViolationVisitor, OWL2DLProfileViolationVisitor, OWL2ELProfileViolationVisitor, OWL2QLProfileViolationVisitor, OWL2RLProfileViolationVisitor {
		private String message;
		
		public String getMessage() {
			return message;
		}
		
		public static String generateMessage(OWLProfileViolation violation) {
			ProfileViolationDescriptionGenerator visitor = new ProfileViolationDescriptionGenerator();
			
			if (violation instanceof OWL2ProfileViolation) {
				((OWL2ProfileViolation) violation).accept(visitor);
			}
			else if (violation instanceof OWL2DLProfileViolation) {
				((OWL2DLProfileViolation) violation).accept(visitor);
			}
			else if (violation instanceof OWL2ELProfileViolation) {
				((OWL2ELProfileViolation) violation).accept(visitor);
			}
			else if (violation instanceof OWL2QLProfileViolation) {
				((OWL2QLProfileViolation) violation).accept(visitor);
			}
			else if (violation instanceof OWL2RLProfileViolation) {
				((OWL2RLProfileViolation) violation).accept(visitor);
			}
			
			if (visitor.getMessage() == null) {
				return violation.toString();
			}
			
			return visitor.getMessage();
		}
		
        public void visit(UseOfNonAbsoluteIRI violation) {
        	message = "Use of non-absolute IRI: " + violation.getAxiom();	        
        }

        public void visit(UseOfIllegalFacetRestriction violation) {
	        message = violation.toString();	        
        }

        public void visit(LexicalNotInLexicalSpace violation) {
			message = violation.toString();	        
        }

        public void visit(OntologyIRINotAbsolute violation) {
        	message = "The IRI of the ontology is not absolute";	        
        }

        public void visit(OntologyVersionIRINotAbsolute violation) {
        	message = violation.toString();	        
        }

        public void visit(UseOfDefinedDatatypeInDatatypeRestriction violation) {
	        message = violation.toString();	        
        }

        public void visit(UseOfUndeclaredDatatype violation) {
	        message = violation.toString();	        
        }

        public void visit(UseOfUnknownDatatype violation) {
	        message = violation.toString();	        
        }

        public void visit(CycleInDatatypeDefinition violation) {
	        message = violation.toString();	        
        }

        public void visit(UseOfBuiltInDatatypeInDatatypeDefinition violation) {
        	message = violation.toString();
        }

        public void visit(DatatypeIRIAlsoUsedAsClassIRI violation) {
	        message = "IRI that is used for a datatype is also used for a class IRI: " + ((violation.getIRI() != null)? violation.getIRI().toString() : "");
        }

        public void visit(UseOfNonSimplePropertyInAsymmetricObjectPropertyAxiom violation) {
	        message = violation.toString();	        
        }

        public void visit(UseOfNonSimplePropertyInCardinalityRestriction violation) {
	        message = violation.toString();	        
        }

        public void visit(UseOfNonSimplePropertyInDisjointPropertiesAxiom violation) {
        	message = violation.toString();	        
        }

        public void visit(UseOfNonSimplePropertyInFunctionalPropertyAxiom violation) {
			message = violation.toString();
        }

        public void visit(UseOfNonSimplePropertyInInverseFunctionalObjectPropertyAxiom violation) {
			message = violation.toString();	        
        }

        public void visit(UseOfNonSimplePropertyInIrreflexivePropertyAxiom violation) {
			message = violation.toString();	        
        }

        public void visit(UseOfNonSimplePropertyInObjectHasSelf violation) {
			message = violation.toString();	        
        }

        public void visit(UseOfPropertyInChainCausesCycle violation) {
	        message = "Use of property in chain causes cycle";	    
	        // TODO extract information about the cycle/property
        }

        public void visit(UseOfReservedVocabularyForAnnotationPropertyIRI violation) {
        	message = violation.toString();
        }

        public void visit(UseOfReservedVocabularyForClassIRI violation) {
        	message = violation.toString(); 	        
        }

        public void visit(UseOfReservedVocabularyForDataPropertyIRI violation) {
			message = violation.toString();
        }

        public void visit(UseOfReservedVocabularyForIndividualIRI violation) {
			message = violation.toString();	        
        }

        public void visit(UseOfReservedVocabularyForObjectPropertyIRI violation) {
			message = violation.toString();	        
        }

        public void visit(UseOfReservedVocabularyForOntologyIRI violation) {
			message = violation.toString();	        
        }

        public void visit(UseOfReservedVocabularyForVersionIRI violation) {
			message = violation.toString();	        
        }

        public void visit(UseOfTopDataPropertyAsSubPropertyInSubPropertyAxiom violation) {
			message = violation.toString();	        	        
        }

        public void visit(UseOfUndeclaredAnnotationProperty violation) {
        	message = violation.toString();
        }

        public void visit(UseOfUndeclaredClass violation) {
			message = violation.toString();	        
        }

        public void visit(UseOfUndeclaredDataProperty violation) {
	       message = violation.toString();	        
        }

        public void visit(UseOfUndeclaredObjectProperty violation) {
        	message = violation.toString();
        }

        public void visit(LastPropertyInChainNotInImposedRange violation) {
        	message = violation.toString();
        }

        public void visit(UseOfAnonymousIndividual violation) {
        	message = violation.toString();
        }

        public void visit(UseOfDataOneOfWithMultipleLiterals violation) {
        	message = violation.toString();
        }

        public void visit(UseOfIllegalAxiom violation) {
        	message = violation.toString();
        }

        public void visit(UseOfIllegalClassExpression violation) {
        	message = violation.toString();
        }

        public void visit(UseOfIllegalDataRange violation) {
        	message = violation.toString();
        }

        public void visit(UseOfObjectPropertyInverse violation) {
			message = violation.toString();	        
        }

        public void visit(UseOfObjectOneOfWithMultipleIndividuals violation) {
			message = violation.toString();
        }

        public void visit(UseOfNonAtomicClassExpression violation) {
			message = violation.toString();
        }

        public void visit(UseOfNonSubClassExpression violation) {
			message = violation.toString();	        
        }

        public void visit(UseOfNonSuperClassExpression violation) {
			message = violation.toString();	        	        
        }

        public void visit(UseOfNonEquivalentClassExpression violation) {
			message = violation.toString();
        }
	}
}
