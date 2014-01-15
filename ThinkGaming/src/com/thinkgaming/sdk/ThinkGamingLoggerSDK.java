package com.thinkgaming.sdk;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.apache.http.conn.util.InetAddressUtils;

import com.thinkgaming.data.ThinkGamingProduct;
import com.thinkgaming.data.ThinkGamingStore;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.Settings.Secure;

public class ThinkGamingLoggerSDK {
	
	private static final String COM_THINKGAMING_SDK = "com.thinkgaming.sdk";

	private static final String TG_COHORT_ID = "TG_cohortId";
	private static final String TG_FIRST_LAUNCH_DATE = "TG_firstLaunchDate";
	
	private static final String ThinkGamingViewStoreLogId = "viewed_store";
	private static final String ThinkGamingTappedItemLogId = "tapped_purchase";
	private static final String ThinkGamingCompletedPurchaseLogId = "completed_purchase";
	
	private static ThinkGamingLoggerSDK _instance;
	
	public static ThinkGamingLoggerSDK getInstance() {
		if (_instance == null) {
			_instance = new ThinkGamingLoggerSDK();
		}
		return _instance;
	}
	
	private Context _context;
	private String _apiKey;
	private String _mediaSourceId;
	
	private ThinkGamingLoggerSDK() {
		
	}
	
	public void startSession(Context context, String apiKey) {
		startSession(context, apiKey, null);
	}
	
	public void startSession(Context context, String apiKey, String mediaSourceId) {
		_context = context;
		_apiKey = apiKey;
		_mediaSourceId = mediaSourceId;
		
		if (getFirstLaunchDate() == -1) {
			//There is no vendorId or advertisingId for Android
			//I could use Android Id
//			String deviceId = Secure.getString(_context.getContentResolver(), Secure.ANDROID_ID);
			
			long firstLaunchDate = Calendar.getInstance().getTimeInMillis() / 1000;
			SharedPreferences prefs = _context.getSharedPreferences(COM_THINKGAMING_SDK, Context.MODE_PRIVATE);
			Editor editor = prefs.edit();
			editor.putLong(TG_FIRST_LAUNCH_DATE, firstLaunchDate);
			editor.commit();
			
			HashMap<String, Object> parameters = new HashMap<String, Object>();
			parameters.put("__TG__firstLaunchDate", firstLaunchDate);
			
			logEvent("__TG__firstLaunch", parameters);
		} else {
			logEvent("__TG__sessionStart");
		}
	}
	
	public void flushEvents() {
		Intent logIntent = new Intent(_context, ThinkGamingLoggerService.class);
		_context.startService(logIntent);
	}
	
	public void logEvent(String eventName) {
		logEvent(eventName, null, false, false);
	}
	
	public void logEvent(String eventName, HashMap<String, Object> parameters) {
		logEvent(eventName, parameters, false, false);
	}
	
	public void startTimedEvent(String eventName) {
		logEvent(eventName, null, true, false);
	}

	public void startTimedEvent(String eventName, HashMap<String, Object> parameters) {
		logEvent(eventName, parameters, true, false);
	}
	
	public void endTimedEvent(String eventName) {
		logEvent(eventName, null, true, true);
	}

	public void endTimedEvent(String eventName, HashMap<String, Object> parameters) {
		logEvent(eventName, parameters, true, true);
	}
	
	public void startLoggingViewedStore(ThinkGamingStore store) {
		if (store != null) {
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("store_id", store.getStoreId());
			startTimedEvent(ThinkGamingViewStoreLogId, params);
		}
	}
	
	public void startLoggingBuyingProduct(ThinkGamingProduct product) {
		if (product != null) {
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("gplay_id", product.getgPlayId());
			params.put("price_id", product.getPriceId());
			params.put("message_id", product.getMessageId());
			startTimedEvent(ThinkGamingTappedItemLogId, params);
		}
	}
	
	public void endLoggingBuyingProduct(ThinkGamingProduct product, boolean didPurchase) {
		if (product != null) {
			String result = didPurchase
					? "didPurchase"
					: "didNotPurchase";
			
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("gplay_id", product.getgPlayId());
			params.put("price_id", product.getPriceId());
			params.put("message_id", product.getMessageId());
			params.put("product_id", product.getgPlayId());
			params.put("price", product.getPrice());
			params.put("price_locale", product.getStoreLocale());
			params.put("title", product.getStoreTitle());
			params.put("result", result);
			endTimedEvent(ThinkGamingCompletedPurchaseLogId, params);
		}
	}

	private void logEvent(String eventName, HashMap<String, Object> parameters, boolean timed, boolean stopTimer) {
		
		HashMap<String, Object> dict = new HashMap<String, Object>(); 
		
		long time = Calendar.getInstance().getTimeInMillis() / 1000;

		dict.put("__TG__Platform", "Android");
		dict.put("__TG__timestamp", time);
		dict.put("__TG__apiKey", _apiKey);    
		dict.put("__TG__eventName", eventName);
		dict.put("__TG__timed", timed ? 1 : 0);
		dict.put("__TG__stopTimer", stopTimer ? 1 : 0);
		
		int cohortId = getCohortId();
		dict.put("__TG__cohortID", cohortId);

		String ipAddress = getIPAddress();
		if (ipAddress != null && ipAddress.length() > 0) {
			dict.put("__TG__IPAddress", ipAddress);
		}
		
		String locale = Locale.getDefault().toString();
		if (locale != null && locale.length() > 0) {
			dict.put("__TG__locale", locale);
		}
		
		String deviceId = Secure.getString(_context.getContentResolver(), Secure.ANDROID_ID); 
		if (deviceId != null && deviceId.length() > 0) {
			dict.put("__TG__userID", deviceId);
		}
		
		if (_mediaSourceId != null && _mediaSourceId.length() > 0) {
			dict.put("__TG__mediaSourceID", _mediaSourceId);
		}
		
		LoggerData payload = new LoggerData();
		for (String key : dict.keySet()) {
			LoggerDataRow row = new LoggerDataRow(key, dict.get(key));
			payload.DataRows.add(row);
		}
		
		if (parameters != null && parameters.keySet().size() > 0) {
			for (String key : parameters.keySet()) {
				LoggerDataRow row = new LoggerDataRow(key, parameters.get(key));
				payload.Parameters.add(row);
			}
		}
		
		Intent logIntent = new Intent(_context, ThinkGamingLoggerService.class);
		logIntent.putExtra(ThinkGamingLoggerService.LOG_API_KEY, _apiKey);
		logIntent.putExtra(ThinkGamingLoggerService.LOG_EXTRA, payload);
		_context.startService(logIntent);
	}
	
	private long getFirstLaunchDate() {
		SharedPreferences prefs = _context.getSharedPreferences(COM_THINKGAMING_SDK, Context.MODE_PRIVATE);
		return prefs.getLong(TG_FIRST_LAUNCH_DATE, -1);
	}
	
	private int getCohortId() {
		SharedPreferences prefs = _context.getSharedPreferences(COM_THINKGAMING_SDK, Context.MODE_PRIVATE);
		int cohortId = prefs.getInt(TG_COHORT_ID, -1);
		
		if (cohortId == -1) {
			cohortId = new Random().nextInt(99);
			Editor editor = prefs.edit();
			editor.putInt(TG_COHORT_ID, cohortId);
			editor.commit();
		}
		return cohortId;
	}

	private static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase(Locale.US);
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr); 
                        if (isIPv4) {
                            return sAddr;
                        } else {
                            int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                            return delim<0 ? sAddr : sAddr.substring(0, delim);
                        }
                    }
                }
            }
        } catch (Exception e) {
        	//Swallow for now.
        	e.printStackTrace();
        }
        return "";
    }
}
