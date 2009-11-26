/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import java.net.MalformedURLException;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.exception.MantisException;

/**
 * @author Steffen Pingel
 * @author Xiaoyang Guan
 */
public class MantisAxisClientTest extends AbstractMantisClientRepositoryTest {

	public void testupdateAttributes() throws Exception {

		String username = "reporter";
		String password = "reporter";

		IMantisClient mantisClient = newMantisClient(MantisTestConstants.TEST_MANTIS_HTTP_URL, username, password);

		mantisClient.updateAttributes(new NullProgressMonitor());
	}

	public void testValidatePass() throws MantisException, MalformedURLException {

		String username = "reporter";
		String password = "reporter";

		IMantisClient mantisClient = newMantisClient(MantisTestConstants.TEST_MANTIS_HTTP_URL, username, password);

		mantisClient.validate(new NullProgressMonitor());

	}

	public void testValidateFails() throws MalformedURLException, MantisException {

		String username = "reporter";
		String password = "reporter12";

		IMantisClient mantisClient = newMantisClient(MantisTestConstants.TEST_MANTIS_HTTP_URL, username, password);

		try {
			mantisClient.validate(new NullProgressMonitor());
			fail("Expected MantisException");
		} catch (MantisException e) {
			// pass
		}
	}

}
