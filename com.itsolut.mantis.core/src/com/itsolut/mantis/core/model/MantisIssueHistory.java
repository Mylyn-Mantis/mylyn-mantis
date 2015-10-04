package com.itsolut.mantis.core.model;

import java.util.List;

import com.google.common.collect.Lists;


public class MantisIssueHistory {

	private final int issueId;

	private final List<MantisIssueHistoryEntry> entries = Lists.newArrayList();
	
	public MantisIssueHistory(int issueId) {
		this.issueId = issueId;
	}
	
	public int getIssueId() {
		return issueId;
	}

	public void addEntry(MantisIssueHistoryEntry entry) {
		this.entries.add(entry);
	}
	
	public List<MantisIssueHistoryEntry> getEntries() {
		return entries;
	}
}
