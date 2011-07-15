/*******************************************************************************
 * Copyright (c) 2011 Robert Munteanu and others.
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
 * Holds well-know configuration keys for MantisBT
 * 
 * @author Robert Munteanu
 *
 */
public enum ConfigurationKey {
    
    RESOLVED_STATUS_THRESHOLD("bug_resolved_status_threshold"),
    REPORTER_THRESHOLD("report_bug_threshold"),
    DEVELOPER_THRESHOLD("update_bug_assign_threshold"),
    DUE_DATE_VIEW_THRESOLD("due_date_view_threshold"),
    DUE_DATE_UPDATE_THRESOLD("due_date_update_threshold"),
    TIME_TRACKING_ENABLED("time_tracking_enabled"),
    BUG_SUBMIT_STATUS("bug_submit_status"),
    BUG_ASSIGNED_STATUS("bug_assigned_status");
    
    private final String value;

    private ConfigurationKey(String value) {
        
        this.value = value;
    }
    
    
    public String getValue() {

        return value;
    }
}
