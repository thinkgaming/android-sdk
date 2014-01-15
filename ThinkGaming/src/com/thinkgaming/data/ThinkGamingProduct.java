package com.thinkgaming.data;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import com.thinkgaming.sdk.ThinkGamingStoreSDK;

public class ThinkGamingProduct implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String productId;
	private String storeId;
	private String priceId;
	private String displayName;
	private String price;
	private String offerText;
	private String iTunesId;
	private String iTunesReference;
	private String gPlayId;
	private String gPlayReference;
	private String messageId;
	private String storeLocale;
	private String storeTitle;

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getStoreId() {
		return storeId;
	}

	public void setStoreId(String storeId) {
		this.storeId = storeId;
	}

	public String getPriceId() {
		return priceId;
	}

	public void setPriceId(String priceId) {
		this.priceId = priceId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getOfferText() {
		return offerText;
	}

	public void setOfferText(String offerText) {
		this.offerText = offerText;
	}

	public String getiTunesId() {
		return iTunesId;
	}

	public void setiTunesId(String iTunesId) {
		this.iTunesId = iTunesId;
	}

	public String getiTunesReference() {
		return iTunesReference;
	}

	public void setiTunesReference(String iTunesReference) {
		this.iTunesReference = iTunesReference;
	}

	public String getgPlayId() {
		return gPlayId;
	}

	public void setgPlayId(String gPlayId) {
		this.gPlayId = gPlayId;
	}

	public String getgPlayReference() {
		return gPlayReference;
	}

	public void setgPlayReference(String gPlayReference) {
		this.gPlayReference = gPlayReference;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getStoreLocale() {
		return storeLocale;
	}

	public void setStoreLocale(String storeLocale) {
		this.storeLocale = storeLocale;
	}

	public String getStoreTitle() {
		return storeTitle;
	}

	public void setStoreTitle(String storeTitle) {
		this.storeTitle = storeTitle;
	}
	
	public boolean isOwned() {
		String[] skus = ThinkGamingStoreSDK.getInstance().getPreviouslyPurchasedSkus();
		for (String sku : skus) {
			if (getgPlayId().equals(sku)) {
				return true;
			}
		}
		return false;
	}
	
	public static ThinkGamingProduct fromJSONObject(JSONObject jObject, String storeId) {
		try {
			ThinkGamingProduct result = new ThinkGamingProduct();
			result.setStoreId(storeId);
			result.setDisplayName(getField(jObject, "display_name"));
			result.setProductId(getField(jObject, "product_id"));
			result.setPrice(getField(jObject, "price"));
			result.setOfferText(getField(jObject, "offer_text"));
			result.setPriceId(getField(jObject, "price_id"));
			result.setiTunesId(getField(jObject, "itunes_id"));
			result.setiTunesReference(getField(jObject, "itunes_reference"));
			result.setgPlayId(getField(jObject, "gplay_id"));
			result.setgPlayReference(getField(jObject, "gplay_reference"));
			result.setMessageId(getField(jObject, "message_id"));
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
