/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.exception.MantisException;

/**
 * Test cases for classes that implement Mantis Client
 * 
 * @author Steffen Pingel
 */
public class AbstractMantisClientRepositoryTest extends AbstractMantisClientTest {

	public void testValidate() throws MantisException {
		
		IMantisClient client = newMantisClient(MantisTestConstants.TEST_MANTIS_HTTP_URL, "reporter", "reporter");
		client.validate(new NullProgressMonitor());
	}
}
