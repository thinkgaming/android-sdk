package com.thinkgaming.interfaces;

import com.thinkgaming.data.ThinkGamingProduct;

public interface IOnGetProductsListener extends ICallbackListener {
	void onGetProductsCallback(Boolean success, ThinkGamingProduct[] products);
}
