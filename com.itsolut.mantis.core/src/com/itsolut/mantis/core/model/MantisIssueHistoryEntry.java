package com.itsolut.mantis.core.model;

import java.util.Date;

public class MantisIssueHistoryEntry {

	private final Date date;
	private final String field;
	private final String author;
	private final String oldValue;
	private final String newValue;

	public MantisIssueHistoryEntry(Date date, String field, String author, String oldValue, String newValue) {
		this.date = date;
		this.field = field;
		this.author = author;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	
	public Date getDate() {
		return date;
	}

	public String getField() {
		return field;
	}

	public String getAuthor() {
		return author;
	}

	public String getOldValue() {
		return oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

}
