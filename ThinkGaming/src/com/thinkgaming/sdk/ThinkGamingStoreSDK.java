package com.thinkgaming.sdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;
import com.thinkgaming.BillingCodes;
import com.thinkgaming.RequestTask;
import com.thinkgaming.data.ThinkGamingProduct;
import com.thinkgaming.data.ThinkGamingStore;
import com.thinkgaming.interfaces.*;

public class ThinkGamingStoreSDK {

	private static final String ThinkGamingAPIBase = "https://api.thinkgaming.com/api/v2";
	private static final String ThinkGamingAPIStorePath = "/stores";
	private static final String ThinkGamingAPIItemsPath = "/stores/";

	public static final int PurchaseIntentRequestCode = 791211;

	private static ThinkGamingStoreSDK _instance;

	public static ThinkGamingStoreSDK getInstance() {
		if (_instance == null) {
			_instance = new ThinkGamingStoreSDK();
		}
		return _instance;
	}

	private Activity _activity;
	private String _apiKey;
	private String _packageName;

	private HashMap<String, String> _ownedProducts;
	private HashMap<String, ThinkGamingStore> _stores;
	private HashMap<String, ThinkGamingProduct> _products;

	IInAppBillingService mService;

	ServiceConnection mServiceConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IInAppBillingService.Stub.asInterface(service);
			ThinkGamingLoggerSDK.getInstance().startSession(_activity, _apiKey);

			try {
				//		isBillingSupported
				confirmIsBillingAvailable(_activity);

				//		getOwnedItems
				getOwnedItems(null);

				//		getSkuDetails
			}
			catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	private ThinkGamingStoreSDK() {
		_ownedProducts = new HashMap<String, String>();
		_stores = new HashMap<String, ThinkGamingStore>();
		_products = new HashMap<String, ThinkGamingProduct>();
	}

	private void confirmIsBillingAvailable(Context context) {
		if (mService == null) {
			final PackageManager packageManager = context.getPackageManager();
			final Intent intent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
			List<ResolveInfo> list = packageManager.queryIntentServices(intent, 0);
			if (list.size() == 0) {
				throw new RuntimeException("Think Gaming: Billing is not supported!");
			}
		} else {
			try {
				int result = mService.isBillingSupported(BillingCodes.BILLING_API_VERSION,
						_packageName, BillingCodes.INAPP);
				if (result != BillingCodes.BILLING_RESPONSE_RESULT_OK) {
					throw new RuntimeException("Think Gaming: Billing is not supported!");
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void init(Activity activity, String apiKey) throws RemoteException, RuntimeException {

		ThinkGamingLoggerSDK.getInstance().startSession(activity, apiKey);

		_activity = activity;
		_apiKey = apiKey;
		_packageName = activity.getApplicationContext().getPackageName();

		confirmIsBillingAvailable(activity);

		activity.bindService(
				new Intent("com.android.vending.billing.InAppBillingService.BIND"),
				mServiceConn, Context.BIND_AUTO_CREATE);

		if (_apiKey == null) {
			throw new RuntimeException("Think Gaming: SDK must be initialized with a valid API key!");
		}
	}

	public void unbind(Context context) {
		_packageName = null;

		if (mServiceConn != null) {
			context.unbindService(mServiceConn);
		}   
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		//For consuming purchase response
		if (resultCode == Activity.RESULT_OK && requestCode == PurchaseIntentRequestCode) {
			try {
				int responseCode = data.getIntExtra(BillingCodes.RESPONSE_CODE, 0);
				boolean didPurchase = responseCode == BillingCodes.BILLING_RESPONSE_RESULT_OK;

				String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
				JSONObject jo = new JSONObject(purchaseData);

				String sku = jo.getString("productId");
				ThinkGamingProduct product = _products.get(sku);
				
				if (product != null) {
					ThinkGamingLoggerSDK.getInstance().endLoggingBuyingProduct(product, didPurchase);
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public String[] getPreviouslyPurchasedSkus() {
		if (_apiKey == null) {
			throw new RuntimeException("Think Gaming: SDK must be initialized with a valid API key!");
		}

		if (mService == null) {
			return new String[] {};
		}

		try {
			getOwnedItems(null);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (_ownedProducts != null) {
			String[] array = new String[_ownedProducts.keySet().size()];
			array = _ownedProducts.keySet().toArray(array);
			return array;
		} else {
			return new String[] {};
		}
	}

	public void getStores(IOnGetStoresListener callback) {

		if (_apiKey == null) {
			throw new RuntimeException("Think Gaming: SDK must be initialized with a valid API key!");
		}

		try {
			String storeUri = String.format("%s%s", ThinkGamingAPIBase, ThinkGamingAPIStorePath);
			RequestTask task = new RequestTask(callback);
			task.execute(storeUri, _apiKey);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void getProducts(ThinkGamingStore store, IOnGetProductsListener callback) {
		if (_apiKey == null) {
			throw new RuntimeException("Think Gaming: SDK must be initialized with a valid API key!");
		}

		try {
			String productsUri = String.format("%s%s%s", ThinkGamingAPIBase, ThinkGamingAPIItemsPath, store.getStoreId());
			RequestTask task = new RequestTask(callback);
			task.execute(productsUri, _apiKey);

			ThinkGamingLoggerSDK.getInstance().startLoggingViewedStore(store);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public List<ThinkGamingProduct> attachPlayStoreData(List<ThinkGamingProduct> products) {
		if (mService == null) {
			return products;
		}
		
		try {
			ArrayList<String> skuList = new ArrayList<String>();

			for(int i = 0; i < products.size(); i++) {
				skuList.add(products.get(i).getgPlayId());
			}

			Bundle querySkus = new Bundle();
			querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

			Bundle skuDetails = mService.getSkuDetails(BillingCodes.BILLING_API_VERSION,
					_packageName, BillingCodes.INAPP, querySkus);

			int response = skuDetails.getInt("RESPONSE_CODE");
			if (response == 0) {
				ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");

				for (String thisResponse : responseList) {
					if (thisResponse != null) {
//						System.out.println(thisResponse);
					}
					
					JSONObject object = new JSONObject(thisResponse);
					String sku = object.getString("productId");
					String price = object.getString("price");
					String locale = object.getString("price_currency_code");
					String title = object.getString("title");

					for(ThinkGamingProduct product : products) {
						if (product.getgPlayId().equals(sku)) {
							product.setPrice(price);
							product.setStoreLocale(locale);
							product.setStoreTitle(title);
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return products;
	}

	public void purchaseProduct(ThinkGamingProduct product) {
		confirmIsBillingAvailable(_activity);

		if (product != null) {
			String gPlayId = product.getgPlayId();

			if (product.getgPlayReference() != null && gPlayId.length() > 0) {
				if (!_ownedProducts.containsKey(gPlayId)) {
					launchPurchaseIntent(product);
					ThinkGamingLoggerSDK.getInstance().startLoggingBuyingProduct(product);
				} else {
					throw new RuntimeException("Think Gaming: Product is already owned, it can't purchase again until consumed.");
				}
			} else {
				throw new RuntimeException("Think Gaming: Product has no associated Play Store Id, it can't be purchased until properly set up!");
			}
		}
	}

	private void launchPurchaseIntent(ThinkGamingProduct product) {
		try {
			String sku = product.getgPlayId();

			Bundle buyIntentBundle = mService.getBuyIntent(BillingCodes.BILLING_API_VERSION, _packageName,
					sku, BillingCodes.INAPP, "TG_PAYLOAD");
			switch (buyIntentBundle.getInt(BillingCodes.RESPONSE_CODE)) {
			case BillingCodes.BILLING_RESPONSE_RESULT_OK:
				PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
				_activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
						PurchaseIntentRequestCode, new Intent(),
						Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));
				break;
			case BillingCodes.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED:
				break;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void consumeProduct(String sku) {
		try {
			System.out.print("Consume: " + sku);
			String token = _ownedProducts.get(sku);
			int responseCode = mService.consumePurchase(BillingCodes.BILLING_API_VERSION, _packageName, token);
			if (responseCode == BillingCodes.BILLING_RESPONSE_RESULT_OK) {
				_ownedProducts.remove(sku);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void getOwnedItems(String continuationToken)
			throws RemoteException {

		Bundle ownedItems = mService.getPurchases(BillingCodes.BILLING_API_VERSION,
				_packageName, BillingCodes.INAPP, continuationToken);
		int response = ownedItems.getInt("RESPONSE_CODE");
		if (response == 0) {
			ArrayList<String> ownedSkus = 
					ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
			ArrayList<String> purchaseDataList = 
					ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
			continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");

			_ownedProducts.clear();

			for (int i = 0; i < ownedSkus.size(); ++i) {
				try {
					String sku = ownedSkus.get(i);
					String purchaseData = purchaseDataList.get(i);

					JSONObject jo = new JSONObject(purchaseData);
					String token = jo.getString("purchaseToken");

					_ownedProducts.put(sku, token);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} 

			if (continuationToken != null) {
				getOwnedItems(continuationToken);
			}
		}
	}

	public void onGetStoresCallback(Boolean success, ThinkGamingStore[] stores) {
		if (success) {
			for (ThinkGamingStore store : stores) {
				_stores.put(store.getStoreId(), store);
			}
		}
	}

	public void onGetProductsCallback(Boolean success, ThinkGamingProduct[] products) {
		if (success) {
			for (ThinkGamingProduct product : products) {
				_products.put(product.getgPlayId(), product);
			}
		}
	}
}
