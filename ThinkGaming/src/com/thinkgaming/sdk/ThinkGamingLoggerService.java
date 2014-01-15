package com.thinkgaming.sdk;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.thinkgaming.PostTask;
import com.thinkgaming.interfaces.IOnPostLogListener;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

public class ThinkGamingLoggerService extends IntentService
			implements IOnPostLogListener {
	
	public static final String LOG_API_KEY = "log_api_key";
	public static final String LOG_EXTRA = "log_data_extra";
	
	private static final String ThinkGamingAPIBase = "https://api.thinkgaming.com/api/v2";
	private static final String ThinkGamingAPILogPath = "/log_activity";
	
	private String _apiKey;
	private HashMap<String, LoggerData> _payloads;
	private String _url;
	
	public ThinkGamingLoggerService() {
		this("com.thinkgaming.sdk.ThinkGamingLoggerService");
	}
	
	protected ThinkGamingLoggerService(String name) {
		super(name);
		_url = String.format("%s%s", ThinkGamingAPIBase, ThinkGamingAPILogPath);
		_payloads = new HashMap<String, LoggerData>();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			_apiKey = bundle.getString(LOG_API_KEY);

			LoggerData logData = (LoggerData)bundle.getSerializable(LOG_EXTRA);
//			System.out.println(logData);
			
			addLog(logData);
		}
		
		postLogs();
	}
	
	private void addLog(LoggerData logData) {
		synchronized(_payloads) {
			_payloads.put(UUID.randomUUID().toString(), logData);
		}
	}

	private void postLogs() {
		synchronized(_payloads) {
			if (_payloads.size() == 0) {
				return;
			}
			JSONObject logDataObject = new JSONObject();
			JSONArray payloadsArray = new JSONArray();
			for (String logKey : _payloads.keySet()) {
				try {
					JSONObject payloadObject = new JSONObject();
					for (LoggerDataRow dataRow : _payloads.get(logKey).DataRows) {
						payloadObject.put(dataRow.Name, dataRow.Value);
					}
					
					if (_payloads.get(logKey).Parameters != null && _payloads.get(logKey).Parameters.size() > 0) {
						JSONObject parameterObject = new JSONObject();
						for (LoggerDataRow dataRow : _payloads.get(logKey).Parameters) {
							parameterObject.put(dataRow.Name, dataRow.Value);
						}
						payloadObject.put("__TG__userData", parameterObject);
					}
					
					payloadsArray.put(payloadObject);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			try {	
				logDataObject.put("__TG__payload", payloadsArray);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			String logData = logDataObject.toString();
			
//			System.out.println("POST URL: " + _url);
//			System.out.println("API KEY: " + _apiKey);
//			System.out.println("DATA: " + logData);
			
			new PostTask(logData, this, _payloads.keySet()).execute(_url, _apiKey);
		}
	}

	@Override
	public void onPostLogSuccessfulCallback(Set<String> logKeys) {
		synchronized(_payloads) {
			try {
				for (String logKey : logKeys) {
					if (_payloads.containsKey(logKey)) {
						_payloads.remove(logKey);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				//Swallow a concurrent error for now: worst case we repost.
			}
		}
	}
}
