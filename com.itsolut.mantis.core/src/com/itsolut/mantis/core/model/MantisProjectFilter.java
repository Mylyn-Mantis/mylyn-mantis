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

/**
 * @author Chris Hane
 */
public class MantisProjectFilter extends MantisTicketAttribute {

    private static final long serialVersionUID = 2392206019389785563L;

    private String url;
    private int projectId;

    public MantisProjectFilter(String name, int value, String url, int projectId) {

        super(name, value);

        this.url = url;
        this.projectId = projectId;
    }

    public String getUrl() {

        return url;
    }

    public void setUrl(String url) {

        this.url = url;
    }
    
    public int getProjectId() {

        return projectId;
    }
}
