/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import junit.framework.TestCase;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.IMantisClient.Version;

/**
 * Provides a base implementation for test cases that access trac repositories.
 * 
 * @author Steffen Pingel
 * @author David Carver
 */
public abstract class AbstractMantisClientTest extends TestCase {

	public String repositoryUrl;

	public IMantisClient repository;

	public String username;

	public String password;

	public Version version;

	public AbstractMantisClientTest(Version version) {

		this.version = version;
	}

	public AbstractMantisClientTest() {
		this(null);
	}

}
