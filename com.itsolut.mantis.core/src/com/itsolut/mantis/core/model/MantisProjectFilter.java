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

    public MantisProjectFilter(String name, int value) {

        this(name, value, null);
    }

    public MantisProjectFilter(String name, int value, String url) {

        super(name, value);

        this.url = url;
    }

    public String getUrl() {

        return url;
    }

    public void setUrl(String url) {

        this.url = url;
    }

}
