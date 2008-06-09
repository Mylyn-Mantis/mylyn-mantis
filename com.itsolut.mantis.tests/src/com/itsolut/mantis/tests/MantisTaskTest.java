/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import junit.framework.TestCase;

import com.itsolut.mantis.core.MantisTask;

/**
 * @author Steffen Pingel
 * @author David Carver
 */
public class MantisTaskTest extends TestCase {

	/**
	 * Check for the values that indicate a task is completed.  This
	 * is used by Mylyn to determine if a task is Completed and can be
	 * filtered.
	 * 
	 * @author David Carver
	 */
	public void testIsCompleted() {
		assertTrue(MantisTask.isCompleted("closed"));
		assertTrue(MantisTask.isCompleted("resolved"));
		assertFalse(MantisTask.isCompleted("Closed"));
		assertFalse(MantisTask.isCompleted("Resolved"));
	}

	/**
	 * Return the priorities as stored in Mantis.  Currently works with
	 * the Priority Enums in the MantisTask class.
	 * @author David Carver
	 */
	public void testGetMylynPriority() {
		assertEquals("P1", MantisTask.getMylynPriority("immediate").toString());
		assertEquals("P2", MantisTask.getMylynPriority("urgent").toString());
		assertEquals("P2", MantisTask.getMylynPriority("high").toString());
		assertEquals("P3", MantisTask.getMylynPriority("normal").toString());
		assertEquals("P4", MantisTask.getMylynPriority("low").toString());
	}
	
	/**
	 * Test to make sure the base url is being returned as expected.
	 * @author David Carver
	 */
	public void testGetRepositoryBaseUrl() {
		String baseUrl = "http://mylyn-mantis.sourceforge.net/Mantis/mc/mantisconnect.php";
		
		assertEquals("http://mylyn-mantis.sourceforge.net/Mantis/", MantisTask.getRepositoryBaseUrl(baseUrl));
	}
}
