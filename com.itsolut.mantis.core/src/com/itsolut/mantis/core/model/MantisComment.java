/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - Initial implementation for Mantis
 *******************************************************************************/
package com.itsolut.mantis.core.model;

import java.util.Date;

/**
 * @author Chris Hane
 */
public class MantisComment {

    private int id;

    private String reporter;

    private Date dateSubmitted;

    private Date lastModified;

    private String text;

    private int timeTracking;

    private boolean isPrivate;

    public MantisComment() {

    }

    public Date getDateSubmitted() {

        return dateSubmitted;
    }

    public void setDateSubmitted(Date dateSubmitted) {

        this.dateSubmitted = dateSubmitted;
    }

    public int getId() {

        return id;
    }

    public void setId(int id) {

        this.id = id;
    }

    public Date getLastModified() {

        return lastModified;
    }

    public void setLastModified(Date lastModified) {

        this.lastModified = lastModified;
    }

    public String getReporter() {

        return reporter;
    }

    public void setReporter(String reporter) {

        this.reporter = reporter;
    }

    public String getText() {

        return text;
    }

    public void setText(String text) {

        this.text = text;
    }

    public void setTimeTracking(int timeTracking) {

        this.timeTracking = timeTracking;

    }

    public int getTimeTracking() {

        return timeTracking;
    }

    public void setIsPrivate(boolean isPrivate) {

        this.isPrivate = isPrivate;
    }
    
    public boolean getIsPrivate() {

        return isPrivate;
    }
    
    @Override
    public String toString() {

        return "[" + text + "] " + reporter + " - " + dateSubmitted;
    }

}
