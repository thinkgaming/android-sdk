package com.thinkgaming.interfaces;

import java.util.Set;

public interface IOnPostLogListener extends ICallbackListener {
	void onPostLogSuccessfulCallback(Set<String> logKeys);
}
