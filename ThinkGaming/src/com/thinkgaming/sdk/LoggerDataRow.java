package com.thinkgaming.sdk;

import java.io.Serializable;

public class LoggerDataRow implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public LoggerDataRow(String name, Object value) {
		Name = name;
		Value = value;
	}
	
	String Name;
	Object Value;
}