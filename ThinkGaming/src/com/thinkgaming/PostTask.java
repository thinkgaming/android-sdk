package com.thinkgaming;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import com.thinkgaming.interfaces.ICallbackListener;
import com.thinkgaming.interfaces.IOnPostLogListener;

import android.os.AsyncTask;

public class PostTask extends AsyncTask<String, String, String>{
	
	private static final String HEADER_THINK_GAMING_API_KEY = "X-ThinkGaming-API-Key";
	
	private Set<String> _logKeys;
	private String _postObject;
	private ICallbackListener _listener;
	
	public PostTask(String postObject, ICallbackListener listener, Set<String> logKeys) {
		_logKeys = logKeys;
		_postObject = postObject;
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
        	HttpPost post = new HttpPost(urlAddress);
        	post.setHeader(HEADER_THINK_GAMING_API_KEY, apiKey);
        	post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/json");
            
            StringEntity se = new StringEntity(_postObject);

            //sets the post request as the resulting string
            post.setEntity(se);
        	
            response = httpclient.execute(post);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            responseString = out.toString();
            
//            System.out.println(responseString);
            
            return responseString;
            
        } catch (ClientProtocolException e) {
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        
        if (result != null && _listener != null) {
        	if (_listener instanceof IOnPostLogListener) {
        		((IOnPostLogListener) _listener).onPostLogSuccessfulCallback(_logKeys);
        	}
        }
    }
}
