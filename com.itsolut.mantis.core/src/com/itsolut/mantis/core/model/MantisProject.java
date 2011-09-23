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
public class MantisProject extends MantisTicketAttribute {
    
    public static final MantisProject ALL_PROJECTS = new MantisProject("All Projects", 0, null);

    private static final long serialVersionUID = -7316456033389981356L;
    private Integer parentProjectId;

    public MantisProject(String name, int value) {

        super(name, value);
    }

    public MantisProject(String name, int value, Integer parentProjectId) {

        this(name, value);
        
        this.parentProjectId = parentProjectId;
    }
    
    public Integer getParentProjectId() {

        return parentProjectId;
    }
}
