/**
 * This software is copyrighted and licensed; see the accompanying license file for copyright holders and terms.
 */
package info.sswap.api.input;

import info.sswap.api.input.io.JSONDeserializer;
import info.sswap.api.input.io.JSONSerializer;
import info.sswap.api.input.io.SSWAPDeserializer;
import info.sswap.api.input.io.SSWAPIndividualDeserializer;
import info.sswap.api.input.io.StringSerializer;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides convenience functions to create {@link Input} objects from different formats and serialize those objects
 * accordingly.
 * 
 * @author Evren Sirin
 */
public class Inputs {
	public static Input fromSSWAP(SSWAPType type) {
		return new SSWAPDeserializer().deserialize(type);
	}
	
	public static Input fromSSWAP(SSWAPIndividual ind) {
		return new SSWAPIndividualDeserializer().deserialize(ind);
	}

	public static Input fromJSON(JSONObject obj) {
		return new JSONDeserializer().deserialize(obj);
	}

	public static JSONObject toJSON(Input input) {
		return new JSONSerializer().serialize(input);
	}

	public static String toJSONString(Input input) {
		try {
			return new JSONSerializer().serialize(input).toString(2);
		} catch (JSONException e) {
			return e.getMessage();
		}
	}

	public static String toPrettyString(Input input) {
		return new StringSerializer().serialize(input);
	}
}
