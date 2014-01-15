package com.thinkgaming;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.thinkgaming.data.ThinkGamingProduct;
import com.thinkgaming.data.ThinkGamingStore;
import com.thinkgaming.interfaces.ICallbackListener;
import com.thinkgaming.interfaces.IOnGetProductsListener;
import com.thinkgaming.interfaces.IOnGetStoresListener;
import com.thinkgaming.sdk.ThinkGamingStoreSDK;

import android.os.AsyncTask;

public class RequestTask extends AsyncTask<String, String, String>{
	
	private static final String HEADER_THINK_GAMING_API_KEY = "X-ThinkGaming-API-Key";
	private ICallbackListener _listener;
	
	public RequestTask(ICallbackListener listener) {
		_listener = listener;
	}

    @Override
    protected String doInBackground(String... params) {
		String urlAddress = params[0];
		String apiKey = params[1];
		
		HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        try {
            HttpGet request = new HttpGet(urlAddress);
            request.setHeader(HEADER_THINK_GAMING_API_KEY, apiKey);
			response = httpclient.execute(request);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            responseString = out.toString();
            
        } catch (ClientProtocolException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        
        if (_listener != null && result != null) {
        	if (_listener instanceof IOnGetStoresListener) {
        		try {
        			JSONObject jObject = new JSONObject(result); 
        			JSONArray jArray = jObject.getJSONArray("stores");
        			ThinkGamingStore[] stores = new ThinkGamingStore[jArray.length()];
        			for (int i=0; i < jArray.length(); i++) {
        				stores[i] = ThinkGamingStore.fromJSONObject(jArray.getJSONObject(i));
        			}
					
	        		((IOnGetStoresListener) _listener).onGetStoresCallback(true, stores);
	        		ThinkGamingStoreSDK.getInstance().onGetStoresCallback(true, stores);
				} catch (JSONException e) {
					e.printStackTrace();
	        		((IOnGetStoresListener) _listener).onGetStoresCallback(false, null);
	        		ThinkGamingStoreSDK.getInstance().onGetStoresCallback(false, null);
				}
        	} else if (_listener instanceof IOnGetProductsListener) {
        		try {
	        		JSONObject jObject = new JSONObject(result); 
	    			JSONArray jArray = jObject.getJSONArray("products");
	    			String storeId = jObject.getString("store_id");
	    			
	    			List<ThinkGamingProduct> products = new ArrayList<ThinkGamingProduct>();
	    			for (int i=0; i < jArray.length(); i++) {
	    				products.add(ThinkGamingProduct.fromJSONObject(jArray.getJSONObject(i), storeId));
	    			}
	    			
	    			products = ThinkGamingStoreSDK.getInstance().attachPlayStoreData(products);
	        		
	    			ThinkGamingProduct[] array = products.toArray(new ThinkGamingProduct[products.size()]);
	        		((IOnGetProductsListener) _listener).onGetProductsCallback(true, array);
	        		ThinkGamingStoreSDK.getInstance().onGetProductsCallback(true, array);
				} catch (JSONException e) {
					e.printStackTrace();
	        		((IOnGetProductsListener) _listener).onGetProductsCallback(false, null);
	        		ThinkGamingStoreSDK.getInstance().onGetProductsCallback(false, null);
				}
        	}
        }
    }
}
