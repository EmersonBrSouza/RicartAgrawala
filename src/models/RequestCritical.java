package models;

import java.io.Serializable;

public class RequestCritical implements Serializable{


	private static final long serialVersionUID = 1L;
	private String sectionName;
	private String processID;
	private Integer timestamp;

	public RequestCritical (String sectionName, String processID, Integer timestamp) {
		this.sectionName = sectionName;
		this.processID = processID;
		this.timestamp = timestamp;
	}
	
	public String getSectionName() {
		return sectionName;
	}
	
	public void setSectionName(String sectionName) {
		this.sectionName = sectionName;
	}
	
	public String getProcessID() {
		return processID;
	}
	
	public void setProcessID(String processID) {
		this.processID = processID;
	}
	
	public Integer getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(Integer timestamp) {
		this.timestamp = timestamp;
	}
}
