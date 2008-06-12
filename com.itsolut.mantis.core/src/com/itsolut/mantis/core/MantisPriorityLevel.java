package com.itsolut.mantis.core;

import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;

public enum MantisPriorityLevel {
	NONE, LOW, NORMAL, HIGH, URGENT, IMMEDIATE;
	

	@Override
	public String toString() {
		switch (this) {
		case IMMEDIATE:
			return "P1";
		case URGENT:
			return "P2";
		case HIGH:
			return "P2";
		case NORMAL:
			return "P3";
		case LOW:
			return "P4";
		case NONE:
			return "P3";
		default:
			return "P5";
		}
	}

	public static PriorityLevel fromPriority(String priority) {
		if (priority == null)
			return null;
		if (priority.equals("immediate"))
			return PriorityLevel.P1;
		if (priority.equals("urgent"))
			return PriorityLevel.P2;
		if (priority.equals("high"))
			return PriorityLevel.P2;
		if (priority.equals("normal"))
			return PriorityLevel.P3;
		if (priority.equals("low"))
			return PriorityLevel.P4;
		if (priority.equals("none"))
			return PriorityLevel.P3;
		return null;
	}
	
	public static String getMylynPriority(String mantisPriority) {
		if (mantisPriority != null) {
			PriorityLevel priority = fromPriority(mantisPriority);
			if (priority != null) {
				return priority.toString();
			}
		}
		
		return PriorityLevel.P3.toString();
	}
	
}
