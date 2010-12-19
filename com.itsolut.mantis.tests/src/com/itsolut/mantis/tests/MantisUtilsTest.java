/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import junit.framework.TestCase;

import com.itsolut.mantis.core.util.MantisUtils;

/**
 * @author Steffen Pingel
 * @author David Carver
 */
public class MantisUtilsTest extends TestCase {

	/**
	 * Test to make sure the base url is being returned as expected.
	 * @author David Carver
	 */
	public void testGetRepositoryBaseUrl() {
		String baseUrl = "http://mylyn-mantis.sourceforge.net/Mantis/mc/mantisconnect.php";
		
		assertEquals("http://mylyn-mantis.sourceforge.net/Mantis/", MantisUtils.getRepositoryBaseUrl(baseUrl));
	}
}
