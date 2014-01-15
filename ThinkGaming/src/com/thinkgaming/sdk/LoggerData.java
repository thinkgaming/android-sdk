package com.thinkgaming.sdk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class LoggerData implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public LoggerData() {
		DataRows = new ArrayList<LoggerDataRow>();
		Parameters = new ArrayList<LoggerDataRow>();
	}
	
	List<LoggerDataRow> DataRows;
	List<LoggerDataRow> Parameters;
}