/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisClientFactory;

/**
 * Test cases for classes that implement Mantis Client
 * 
 * @author Steffen Pingel
 */
public class AbstractMantisClientRepositoryTest extends AbstractMantisClientTest {

	public AbstractMantisClientRepositoryTest(IMantisClient.Version version) {
		super(version);
	}

	public void testValidateHTTP() throws Exception {
		String username = "reporter";
		String password = "reporter";
			
		IMantisClient mantisClient = MantisClientFactory.createClient(MantisTestConstants.TEST_MANTIS_HTTP_URL, IMantisClient.Version.MC_1_0a5, username, password, null);
		mantisClient.validate();
	}

}
