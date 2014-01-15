package com.thinkgaming.interfaces;

import com.thinkgaming.data.ThinkGamingStore;

public interface IOnGetStoresListener extends ICallbackListener {
	void onGetStoresCallback(Boolean success, ThinkGamingStore[] stores);
}
