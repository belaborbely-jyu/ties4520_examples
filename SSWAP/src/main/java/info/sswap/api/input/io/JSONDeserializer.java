/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input.io;

import info.sswap.api.input.Input;
import info.sswap.api.input.InputFactory;
import info.sswap.api.input.InputValue;
import info.sswap.api.input.PropertyInput;
import info.sswap.api.input.UnionInput;
import info.sswap.api.input.Vocabulary;

import java.net.URI;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Lists;

/**
 * Creates an {@link Input} object from a JSON object serialized by the {@link JSONSerializer}.
 *  
 * @author Evren Sirin
 */
public class JSONDeserializer implements InputDeserializer<JSONObject> {
	public JSONDeserializer() {
	}
	
	private void readLabel(JSONObject obj,  Input input) throws JSONException {		
		if (obj.has(JSONConstants.LABEL)) {
			String label = obj.getString(JSONConstants.LABEL);			
			input.setLabel(label);
		}
	}
	
	private void readDescription(JSONObject obj,  Input input) throws JSONException {		
		if (obj.has(JSONConstants.DESCRIPTION)) {
			String label = obj.getString(JSONConstants.DESCRIPTION);			
			input.setDescription(label);
		}
	}
	
	private void readValue(JSONObject obj, Input input) throws JSONException {		
		if (obj.has(JSONConstants.VALUE)) {
			JSONObject value = obj.getJSONObject(JSONConstants.VALUE);
			if (value != null) {
				InputValue inputValue = deserializeValue(value);
				input.setValue(inputValue);
			}
		}
	}

	public Input deserialize(JSONObject obj) {
		try {
			String type = obj.getString(JSONConstants.TYPE);
			Input result = null;
			if (type.equals(Vocabulary.OWL_INTERSECTION.toString())) {
				result = InputFactory.createIntersectionInput(createInputs(obj));
			}
			else if (type.equals(Vocabulary.OWL_UNION.toString())) {
				UnionInput union = InputFactory.createUnionInput(createInputs(obj));
				union.setValueIndex(obj.getInt(JSONConstants.VALUE_INDEX));
				setValueTypes(obj, union);
				result = union;
			}
			else if (type.equals(Vocabulary.OWL_ENUMERATION.toString())) {
				result = InputFactory.createEnumeratedInput(createValues(obj));
			}
			else if (type.equals(Vocabulary.OWL_RESTRICTION.toString())) {
				result = createPropertyInput(obj);
			}
			else {
				result = InputFactory.createAtomicInput(URI.create(type));
			}
			
			readLabel(obj, result);
			readDescription(obj, result);
			readValue(obj, result);
			
			return result;
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public InputValue deserializeValue(JSONObject obj) {
		try {
			String type = obj.getString(JSONConstants.TYPE);
			String value = obj.getString(JSONConstants.VALUE);

			if (type.equals(JSONConstants.URI)) {
				return InputFactory.createURI(URI.create(value));
			}
			if (type.equals(JSONConstants.BNODE)) {
				return InputFactory.createBNode(value);
			}
			if (type.equals(JSONConstants.LITERAL)) {
				if (obj.has(JSONConstants.DATATYPE)) {
					return InputFactory.createLiteral(value, URI.create(obj.getString(JSONConstants.DATATYPE)));
				}
				else if (obj.has(JSONConstants.LANG)) {
					return InputFactory.createLiteral(value, obj.getString(JSONConstants.LANG));
				}
				else {
					return InputFactory.createLiteral(value);
				}
			}

			throw new RuntimeException("Unrecognized value type: " + type + " " + obj);
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	protected List<Input> createInputs(JSONObject obj) {
		try {
			List<Input> inputs = Lists.newArrayList();

			JSONArray array = obj.getJSONArray(JSONConstants.INPUTS);
			for (int i = 0; i < array.length(); i++) {
				JSONObject o = array.getJSONObject(i);
				Input input = deserialize(o);
				inputs.add(input);
			}

			return inputs;
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	protected List<InputValue> createValues(JSONObject obj) {
		try {
			List<InputValue> values = Lists.newArrayList();

			JSONArray array = obj.getJSONArray(JSONConstants.VALUES);
			for (int i = 0; i < array.length(); i++) {
				JSONObject o = array.getJSONObject(i);
				InputValue value = deserializeValue(o);
				values.add(value);
			}

			return values;
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	protected void setValueTypes(JSONObject obj, UnionInput input) {
		try {
			JSONArray array = obj.getJSONArray(JSONConstants.VALUE_TYPES);
			for (int i = 0; i < array.length(); i++) {
				Object valueType = array.opt(i);
				if (valueType != null) {
					input.setValueType(i, URI.create(valueType.toString()));
				}
			}
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	public PropertyInput createPropertyInput(JSONObject obj) {
		try {
			String prop = obj.getString(JSONConstants.PROPERTY);
			URI propURI = URI.create(prop);

			PropertyInput input = InputFactory.createPropertyInput(propURI);

			input.setMinCardinality(obj.getInt(JSONConstants.MIN));
			input.setMaxCardinality(obj.getInt(JSONConstants.MAX));

			JSONObject range = obj.getJSONObject(JSONConstants.RANGE);
			Input rangeInput = deserialize(range);
			rangeInput.setPropertyInput(input);
			input.setRange(rangeInput);			
			
			return input;
		}
		catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

}
