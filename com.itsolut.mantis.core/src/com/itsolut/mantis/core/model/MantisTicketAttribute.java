/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.core.model;

import java.io.Serializable;

/**
 * @author Steffen Pingel
 */
public class MantisTicketAttribute implements Comparable<MantisTicketAttribute>, Serializable {

	private static final long serialVersionUID = -8611030780681519787L;

	private String name;

	private int value;

	public MantisTicketAttribute(String name, int value) {
	    
	    if ( name == null )
	        throw new IllegalArgumentException("Null name for " + getClass().getSimpleName() + " with id " + value);
	    
		this.name = name;
		this.value = value;
	}

	public int compareTo(MantisTicketAttribute o) {
		return value - o.value;
	}

	public String getName() {
		return name;
	}

	public int getValue() {
		return value;
	}

	public String getKey() {
		
		return String.valueOf(getValue());
	}
	
	@Override
	public String toString() {
		return name;
	}

}
