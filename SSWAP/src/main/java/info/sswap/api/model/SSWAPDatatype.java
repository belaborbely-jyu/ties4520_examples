/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.model;

import java.net.URI;
import java.util.Collection;

/**
 * Represents a datatype definition. It contains methods for limited
 * manipulation of <code>rdfs:Datatype</code> definitions.
 * <p>
 * Also contains a sub-interface of XSD URI constants.
 * 
 * @author Blazej Bulka <blazej@clarkparsia.com>
 * 
 */
public interface SSWAPDatatype extends SSWAPElement {
	
	/**
	 * Adds an owl:oneOf axiom to this datatype
	 * @param oneOf the collection of literals that will be the only valid values for this datatype
	 */
	public void addOneOf(Collection<SSWAPLiteral> oneOf);
	
	/**
	 * XSD URIs based on the Jena vocabulary. May be used for
	 * methods such as {@link SSWAPIndividual#addProperty(SSWAPPredicate,String,URI)}.
	 * 
	 * @see <a href="http://www.w3.org/TR/xmlschema11-2">http://www.w3.org/TR/xmlschema11-2</a>
	 * 
	 */
	public interface XSD {
		
		// See http://incubator.apache.org/jena/documentation/javadoc/jena/index.html
		
		/**
		 * xsd:anyURI
		 */
		static final URI anyURI = URI.create(com.hp.hpl.jena.vocabulary.XSD.anyURI.getURI());
		
		/**
		 * xsd:base64Binary
		 */
		static final URI base64Binary = URI.create(com.hp.hpl.jena.vocabulary.XSD.base64Binary.getURI());
		
		/**
		 * xsd:date
		 */
		static final URI date = URI.create(com.hp.hpl.jena.vocabulary.XSD.date.getURI());
		
		/**
		 * xsd:dateTime
		 */
		static final URI dateTime = URI.create(com.hp.hpl.jena.vocabulary.XSD.dateTime.getURI());
		
		/**
		 * xsd:decimal
		 */
		static final URI decimal = URI.create(com.hp.hpl.jena.vocabulary.XSD.decimal.getURI());
		
		/**
		 * xsd:duration
		 */
		static final URI duration = URI.create(com.hp.hpl.jena.vocabulary.XSD.duration.getURI());
		
		/*
		 * xsd:ENTITIES
		 */
		// fails w/ ENTITIES == null
		// static final URI ENTITIES = URI.create(com.hp.hpl.jena.vocabulary.XSD.ENTITIES.getURI());
		
		/**
		 * xsd:ENTITY
		 */
		static final URI ENTITY = URI.create(com.hp.hpl.jena.vocabulary.XSD.ENTITY.getURI());
		
		/**
		 * xsd:gDay
		 */
		static final URI gDay = URI.create(com.hp.hpl.jena.vocabulary.XSD.gDay.getURI());
		
		/**
		 * xsd:gMonth
		 */
		static final URI gMonth = URI.create(com.hp.hpl.jena.vocabulary.XSD.gMonth.getURI());
		
		/**
		 * xsd:gMonthDay
		 */
		static final URI gMonthDay = URI.create(com.hp.hpl.jena.vocabulary.XSD.gMonthDay.getURI());
		
		/**
		 * xsd:gYear
		 */
		static final URI gYear = URI.create(com.hp.hpl.jena.vocabulary.XSD.gYear.getURI());
		
		/**
		 * xsd:gYearMonth
		 */
		static final URI gYearMonth = URI.create(com.hp.hpl.jena.vocabulary.XSD.gYearMonth.getURI());
		
		/**
		 * xsd:hexBinary
		 */
		static final URI hexBinary = URI.create(com.hp.hpl.jena.vocabulary.XSD.hexBinary.getURI());
		
		/**
		 * xsd:ID
		 */
		static final URI ID = URI.create(com.hp.hpl.jena.vocabulary.XSD.ID.getURI());
		
		/**
		 * xsd:IDREF
		 */
		static final URI IDREF = URI.create(com.hp.hpl.jena.vocabulary.XSD.IDREF.getURI());
		
		/*
		 * xsd:IDREFS
		 */
		// fails w/ IDREFS == null
		// static final URI IDREFS = URI.create(com.hp.hpl.jena.vocabulary.XSD.IDREFS.getURI());
		
		/**
		 * xsd:integer
		 */
		static final URI integer = URI.create(com.hp.hpl.jena.vocabulary.XSD.integer.getURI());
		
		/**
		 * xsd:language
		 */
		static final URI language = URI.create(com.hp.hpl.jena.vocabulary.XSD.language.getURI());
		
		/**
		 * xsd:Name
		 */
		static final URI Name = URI.create(com.hp.hpl.jena.vocabulary.XSD.Name.getURI());
		
		/**
		 * xsd:NCName
		 */
		static final URI NCName = URI.create(com.hp.hpl.jena.vocabulary.XSD.NCName.getURI());
		
		/**
		 * xsd:negativeInteger
		 */
		static final URI negativeInteger = URI.create(com.hp.hpl.jena.vocabulary.XSD.negativeInteger.getURI());
		
		/**
		 * xsd:NMTOKEN
		 */
		static final URI NMTOKEN = URI.create(com.hp.hpl.jena.vocabulary.XSD.NMTOKEN.getURI());
		
		/*
		 * xsd:NMTOKENS
		 */
		// fails w/ NMTOKENS == null
		// static final URI NMTOKENS = URI.create(com.hp.hpl.jena.vocabulary.XSD.NMTOKENS.getURI());
		
		/**
		 * xsd:nonNegativeInteger
		 */
		static final URI nonNegativeInteger = URI.create(com.hp.hpl.jena.vocabulary.XSD.nonNegativeInteger.getURI());
		
		/**
		 * xsd:nonPositiveInteger
		 */
		static final URI nonPositiveInteger = URI.create(com.hp.hpl.jena.vocabulary.XSD.nonPositiveInteger.getURI());
		
		/**
		 * xsd:normalizedString
		 */
		static final URI normalizedString = URI.create(com.hp.hpl.jena.vocabulary.XSD.normalizedString.getURI());
		
		/**
		 * xsd:NOTATION
		 */
		static final URI NOTATION = URI.create(com.hp.hpl.jena.vocabulary.XSD.NOTATION.getURI());
		
		/**
		 * xsd:positiveInteger
		 */
		static final URI positiveInteger = URI.create(com.hp.hpl.jena.vocabulary.XSD.positiveInteger.getURI());
		
		/**
		 * xsd:QName
		 */
		static final URI QName = URI.create(com.hp.hpl.jena.vocabulary.XSD.QName.getURI());
		
		/**
		 * xsd:time
		 */
		static final URI time = URI.create(com.hp.hpl.jena.vocabulary.XSD.time.getURI());
		
		/**
		 * xsd:token
		 */
		static final URI token = URI.create(com.hp.hpl.jena.vocabulary.XSD.token.getURI());
		
		/**
		 * xsd:unsignedByte
		 */
		static final URI unsignedByte = URI.create(com.hp.hpl.jena.vocabulary.XSD.unsignedByte.getURI());
		
		/**
		 * xsd:unsignedInt
		 */
		static final URI unsignedInt = URI.create(com.hp.hpl.jena.vocabulary.XSD.unsignedInt.getURI());
		
		/**
		 * xsd:unsignedLong
		 */
		static final URI unsignedLong = URI.create(com.hp.hpl.jena.vocabulary.XSD.unsignedLong.getURI());
		
		/**
		 * xsd:unsignedShort
		 */
		static final URI unsignedShort = URI.create(com.hp.hpl.jena.vocabulary.XSD.unsignedShort.getURI());
		
		/**
		 * xsd:boolean
		 */
		static final URI xboolean = URI.create(com.hp.hpl.jena.vocabulary.XSD.xboolean.getURI());
		
		/**
		 * xsd:byte
		 */
		static final URI xbyte = URI.create(com.hp.hpl.jena.vocabulary.XSD.xbyte.getURI());
		
		/**
		 * xsd:double
		 */
		static final URI xdouble = URI.create(com.hp.hpl.jena.vocabulary.XSD.xdouble.getURI());
		
		/**
		 * xsd:float
		 */
		static final URI xfloat = URI.create(com.hp.hpl.jena.vocabulary.XSD.xfloat.getURI());
		
		/**
		 * xsd:int
		 */
		static final URI xint = URI.create(com.hp.hpl.jena.vocabulary.XSD.xint.getURI());
		
		/**
		 * xsd:long
		 */
		static final URI xlong = URI.create(com.hp.hpl.jena.vocabulary.XSD.xlong.getURI());
		
		/**
		 * xsd:short
		 */
		static final URI xshort = URI.create(com.hp.hpl.jena.vocabulary.XSD.xshort.getURI());
		
		/**
		 * xsd:string
		 */
		static final URI xstring = URI.create(com.hp.hpl.jena.vocabulary.XSD.xstring.getURI());

	}
}
