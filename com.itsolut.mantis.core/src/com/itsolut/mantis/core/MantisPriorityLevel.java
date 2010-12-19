package com.itsolut.mantis.core;

import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;

public class MantisPriorityLevel {
	
	/**
	 * 
	 * @param priority the priority id 
	 * @return the PriorityLevel, never null
	 */
	public static PriorityLevel fromPriorityId(int priority) {
	    
	    if ( priority >= DefaultConstantValues.Priority.URGENT.getValue() )
	        return PriorityLevel.P1;
	    if ( priority >= DefaultConstantValues.Priority.HIGH.getValue() )
	        return PriorityLevel.P2;
	    if ( priority >= DefaultConstantValues.Priority.NORMAL.getValue())
	        return PriorityLevel.P3;
	    if ( priority >= DefaultConstantValues.Priority.LOW.getValue())
	        return PriorityLevel.P4;

	    // default to none
	    return PriorityLevel.P5;
	}
}
