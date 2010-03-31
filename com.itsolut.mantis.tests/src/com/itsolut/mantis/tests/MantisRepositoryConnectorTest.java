/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import junit.framework.TestCase;

import com.itsolut.mantis.core.MantisRepositoryConnector;

public class MantisRepositoryConnectorTest extends TestCase {

	private static final String REPOSITORY_ROOT = "http://mylyn-mantis.sourceforge.net/MantisTest/";

	private MantisRepositoryConnector connector;

	private final String taskId = "12";

	private final String expectedUrl = REPOSITORY_ROOT + "view.php?id=" + taskId;

	@Override
	protected void setUp() throws Exception {

		super.setUp();

		connector = new MantisRepositoryConnector();

	}

	public void testGetUrl10x() {

		assertEquals("Wrong url for Mantis 1.0.x", expectedUrl, connector.getTaskUrl(
				REPOSITORY_ROOT + "mc/mantisconnect.php", taskId));
	}

	public void testGetUrl11x() {



		assertEquals("Wrong url for Mantis 1.1.x", expectedUrl, connector.getTaskUrl(
				REPOSITORY_ROOT + "api/soap/mantisconnect.php", taskId));
	}

	public void testGetTaskIdFromTaskUrl() {

		String taskId = "84";

		String url = REPOSITORY_ROOT + "view.php?id=" + taskId;

		assertEquals("Failed to extract task id", taskId, connector.getTaskIdFromTaskUrl(url));

	}

	public void testGetRepositoryUrlFromTaskUrl() {

		String taskId = "84";

		String url = REPOSITORY_ROOT + "view.php?id=" + taskId;

		assertEquals("Failed to extract the repository path", REPOSITORY_ROOT,
				connector.getRepositoryUrlFromTaskUrl(url));
	}


}
