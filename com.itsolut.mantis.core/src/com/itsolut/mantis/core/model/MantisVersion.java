/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.core.model;

import java.util.Date;

/**
 * @author Steffen Pingel
 */
public class MantisVersion extends MantisAttribute {

	private static final long serialVersionUID = 9018237956062697410L;

	private Date time;

	private String description;
	
	private boolean released;

	public MantisVersion(String name) {
		super(name);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}
	
	public void setReleased(boolean released){
		this.released = released;
	}
	
	public boolean isReleased(){
		return released;
	}

}
