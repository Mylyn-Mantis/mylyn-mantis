/*******************************************************************************
 * Copyright (c) 2007 - IT Solutions and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - intial api and implementation
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.util.HashMap;
import java.util.Map;

public class MantisUserData {
	Map<String, String[]> usersPerProject = new HashMap<String, String[]>();
	Map<String, String[]> developersPerProject = new HashMap<String, String[]>();
	
}
