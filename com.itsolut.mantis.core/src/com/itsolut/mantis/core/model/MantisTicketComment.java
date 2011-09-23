/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core.model;

import com.itsolut.mantis.core.util.MantisUtils;


/**
 * @author Robert Munteanu
 *
 */
public class MantisTicketComment {
    
    private String comment;
    private int timeTracking;
    
    public MantisTicketComment(String comment, int timeTracking) {

        this.comment = comment;
        this.timeTracking = timeTracking;
    }
    
    
    public String getComment() {

        return comment;
    }
    
    
    public int getTimeTracking() {

        return timeTracking;
    }
    
    public boolean hasContent() {
        
        return !MantisUtils.isEmpty(comment) || timeTracking > 0;
    }

}
