/*******************************************************************************
 * Copyright (c) 2004, 2010 Robert Munteanu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Robert Munteanu - initial API and implementation
 *******************************************************************************/

package com.itsolut.mantis.core;

/**
 * @author Robert Munteanu
 */
public enum RepositoryCapability {
    
    TARGET_VERSION("the target_version field will be erased"), 
    TASK_RELATIONS("task relation are not displayed or editable"), 
    CORRECT_BASE64_ENCODING("attachments are incorrectly encoded and will be corrupted when downloaded from the web interface"), 
    DUE_DATE("the due_date field will not be available"), 
    TIME_TRACKING("time tracking will not be supported");
    
    private String descriptionForMissingCapability;

	private RepositoryCapability(String descriptionForMissingCapability) {
    	
    	this.descriptionForMissingCapability = descriptionForMissingCapability;
    }
	
	public String getDescriptionForMissingCapability() {
	
		return descriptionForMissingCapability;
	}
    
}