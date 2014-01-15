package com.thinkgaming.data;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class ThinkGamingStore implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String storeId;
	private String displayName;

	public String getStoreId() {
		return storeId;
	}

	public void setStoreId(String storeId) {
		this.storeId = storeId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public static ThinkGamingStore fromJSONObject(JSONObject jObject) {
		try {
			ThinkGamingStore result = new ThinkGamingStore();
			result.setStoreId(getField(jObject, "store_id"));
			result.setDisplayName(getField(jObject, "display_name"));
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String getField(JSONObject jObject, String fieldName) {
		if (jObject.has(fieldName)) {
			try {
				return jObject.getString(fieldName);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
