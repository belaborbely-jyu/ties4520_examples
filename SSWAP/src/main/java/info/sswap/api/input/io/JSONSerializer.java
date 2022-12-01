/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.io;

import info.sswap.api.input.AtomicInput;
import info.sswap.api.input.BNodeValue;
import info.sswap.api.input.EnumeratedInput;
import info.sswap.api.input.Input;
import info.sswap.api.input.InputValue;
import info.sswap.api.input.InputVisitor;
import info.sswap.api.input.IntersectionInput;
import info.sswap.api.input.LiteralValue;
import info.sswap.api.input.PropertyInput;
import info.sswap.api.input.URIValue;
import info.sswap.api.input.UnionInput;
import info.sswap.api.input.Vocabulary;

import java.net.URI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Creates a JSON representation of the given {@link Input} object. Example output looks as follows:
 * 
 * <pre>
{
  "type": "http://www.w3.org/2002/07/owl#intersectionOf",
  "inputs": [
    {    
      "type": "http://www.w3.org/2002/07/owl#unionOf",
      "inputs": [
        {        
          "type": "http://www.w3.org/2002/07/owl#Restriction",
          "property": "http://sswapmeet.sswap.info/OBO/id",
          "min": 1,
          "max": 1,
          "range": {"type": "http://www.w3.org/2001/XMLSchema#string"}
        },
        {
          "type": "http://www.w3.org/2002/07/owl#Restriction",          
          "property": "http://sswapmeet.sswap.info/OBO/name",
          "min": 1,
          "max": 1,
          "range": {"type": "http://www.w3.org/2001/XMLSchema#string"}
        }
      ]
    },
    {    
      "type": "http://www.w3.org/2002/07/owl#Restriction",
      "property": "http://plantontology.sswap.info/poAnnotations/ontologies/poAnnotation/ontology",
      "min": 0,
      "max": 1,
      "range": {      
        "type": "http://www.w3.org/2002/07/owl#oneOf",
        "values": [
          {
            "value": "plant_structure",
            "datatype": "http://www.w3.org/2001/XMLSchema#string",
            "type": "literal"
          },
          {
            "value": "plant_growth_and_development_stage",
            "datatype": "http://www.w3.org/2001/XMLSchema#string",
            "type": "literal"
          }
        ]
      }
    }
  ]
}
</pre>
 * @author Evren Sirin
 */
public class JSONSerializer implements InputVisitor, InputSerializer<JSONObject>  {
	private JSONObject out;

	public JSONSerializer() {
	}

	public JSONObject serialize(Input input) {
		out = null;		
		input.accept(this);		
		return out;
	}

	private JSONObject serialize(InputValue inputValue) {
		out = null;
		if (inputValue != null) {
			inputValue.accept(this);
		}
		return out;
	}
	
	private JSONObject newJSONObject(Input input) throws JSONException {		
		JSONObject target = new JSONObject();
		addIfNotNull(target, JSONConstants.VALUE, serialize(input.getValue()));
		addIfNotNull(target, JSONConstants.LABEL, input.getLabel());
		addIfNotNull(target, JSONConstants.DESCRIPTION, input.getDescription());
		return target;
	}
	
	private void addIfNotNull(JSONObject target, String key, Object value) throws JSONException {
		if (value != null) {
			target.put(key, value);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(AtomicInput input) {
		try {
			JSONObject result = newJSONObject(input);
			result.put(JSONConstants.TYPE, input.getType().toString());
			
			out = result;
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(IntersectionInput intersection) {
		try {
			JSONObject result = newJSONObject(intersection);
			result.put(JSONConstants.TYPE, Vocabulary.OWL_INTERSECTION.toString());

			JSONArray array = new JSONArray();
			for (Input input : intersection.getInputs()) {
				array.put(serialize(input));
			}
			result.put(JSONConstants.INPUTS, array);

			out = result;
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(UnionInput union) {
		try {
			JSONObject result = newJSONObject(union);
			result.put(JSONConstants.TYPE, Vocabulary.OWL_UNION.toString());
			result.put(JSONConstants.VALUE_INDEX, union.getValueIndex());

			JSONArray array = new JSONArray();
			for (Input input : union.getInputs()) {
				array.put(serialize(input));
			}
			result.put(JSONConstants.INPUTS, array);
			
			JSONArray typeArray = new JSONArray();
			for (int i = 0; i < union.getInputs().size(); i++) {
				URI typeURI = union.getValueType(i);						 
				typeArray.put(typeURI == null ? null : typeURI.toString());
			}
			result.put(JSONConstants.VALUE_TYPES, typeArray);
			
			out = result;
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(EnumeratedInput input) {
		try {
			JSONObject result = newJSONObject(input);
			result.put(JSONConstants.TYPE, Vocabulary.OWL_ENUMERATION.toString());

			JSONArray array = new JSONArray();
			for (InputValue value : input.getValues()) {
				array.put(serialize(value));
			}
			result.put(JSONConstants.VALUES, array);

			out = result;
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(PropertyInput input) {
		try {
	        JSONObject result = newJSONObject(input);
	        result.put(JSONConstants.TYPE, Vocabulary.OWL_RESTRICTION.toString());

	        if (input.getProperty() != null) {
	        	result.put(JSONConstants.PROPERTY, input.getProperty().toString());
	        }
	        result.put(JSONConstants.MIN, input.getMinCardinality());
	        result.put(JSONConstants.MAX, input.getMaxCardinality());
	        
	        result.put(JSONConstants.RANGE, serialize(input.getRange()));

	        out = result;
        }
        catch (JSONException e) {
	        throw new RuntimeException(e);
        }
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(URIValue value) {
		try {
			out = new JSONObject();
			out.put(JSONConstants.TYPE, JSONConstants.URI);
			out.put(JSONConstants.VALUE, value.getURI().toString());
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(BNodeValue value) {
		try {
			out = new JSONObject();
			out.put(JSONConstants.TYPE, JSONConstants.BNODE);
			out.put(JSONConstants.VALUE, value.getID());
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(LiteralValue value) {
		try {
			out = new JSONObject();
			out.put(JSONConstants.TYPE, JSONConstants.LITERAL);
			out.put(JSONConstants.VALUE, value.getLabel());
			if (value.getDatatype() != null) {
				out.put(JSONConstants.DATATYPE, value.getDatatype().toString());	
			}
			else if (value.getLanguage() != null) {
				out.put(JSONConstants.LANG, value.getLanguage());	
			}
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

}
