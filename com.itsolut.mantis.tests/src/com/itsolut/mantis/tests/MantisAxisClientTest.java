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
import com.itsolut.mantis.core.MantisClientFactory;
import com.itsolut.mantis.core.IMantisClient.Version;
import com.itsolut.mantis.core.exception.MantisException;

/**
 * @author Steffen Pingel
 * @author Xiaoyang Guan
 */
public class MantisAxisClientTest extends AbstractMantisClientRepositoryTest {

	public MantisAxisClientTest() {
		super(Version.MC_1_0a5);
	}

	public void testupdateAttributes() throws Exception {

		String username = "reporter";
		String password = "reporter";

		IMantisClient mantisClient = MantisClientFactory.createClient(MantisTestConstants.TEST_MANTIS_HTTP_URL,
				username, password, null, null, null);
		assertNotNull("Mantis Client failed to be created.", mantisClient);

		mantisClient.updateAttributes(new NullProgressMonitor(), true);
	}
	
	public void testValidatePass() throws MantisException, MalformedURLException {

		String username = "reporter";
		String password = "reporter";

		IMantisClient mantisClient = MantisClientFactory.createClient(MantisTestConstants.TEST_MANTIS_HTTP_URL,
				username, password, null, null, null);
		assertNotNull("Mantis Client failed to be created.", mantisClient);

		mantisClient.validate();

	}
	
	public void testValidateFails() throws MalformedURLException {

		
		String username = "reporter";
		String password = "reporter12";

		IMantisClient mantisClient = MantisClientFactory.createClient(MantisTestConstants.TEST_MANTIS_HTTP_URL,
				username, password, null, null, null);
		assertNotNull("Mantis Client failed to be created", mantisClient);

		try {
			mantisClient.validate();
			fail("Expected MantisException");
		} catch ( MantisException e) {
			// pass
		}

	}

}
