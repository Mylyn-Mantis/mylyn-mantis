/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.itsolut.mantis.core.MantisRepositoryConnector;

public class MantisRepositoryConnectorTest  {

	private static final String REPOSITORY_ROOT = "http://mylyn-mantis.sourceforge.net/MantisTest/";

	private MantisRepositoryConnector connector;

	private final String taskId = "12";

	private final String expectedUrl = REPOSITORY_ROOT + "view.php?id=" + taskId;

	@Before
	public void setUp() throws Exception {

		connector = new MantisRepositoryConnector();

	}

	@Test
	public void testGetUrl11x() {

		assertEquals("Wrong url for Mantis 1.1.x", expectedUrl, connector.getTaskUrl(
				REPOSITORY_ROOT + "api/soap/mantisconnect.php", taskId));
	}

	@Test
	public void testGetTaskIdFromTaskUrl() {

		String taskId = "84";

		String url = REPOSITORY_ROOT + "view.php?id=" + taskId;

		assertEquals("Failed to extract task id", taskId, connector.getTaskIdFromTaskUrl(url));

	}

	@Test
	public void testGetRepositoryUrlFromTaskUrl() {

		String taskId = "84";

		String url = REPOSITORY_ROOT + "view.php?id=" + taskId;

		assertEquals("Failed to extract the repository path", REPOSITORY_ROOT,
				connector.getRepositoryUrlFromTaskUrl(url));
	}


}
