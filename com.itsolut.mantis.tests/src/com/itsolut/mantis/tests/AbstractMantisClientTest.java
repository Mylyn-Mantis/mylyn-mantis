/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import java.net.Proxy;

import junit.framework.TestCase;

import org.eclipse.mylyn.context.tests.support.TestUtil;
import org.eclipse.mylyn.context.tests.support.TestUtil.Credentials;
import org.eclipse.mylyn.context.tests.support.TestUtil.PrivilegeLevel;
import com.itsolut.mantis.core.MantisClientFactory;

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

	private PrivilegeLevel level;

	public AbstractMantisClientTest(Version version, PrivilegeLevel level) {
		this.version = version;
		this.level = level;
	}

	public AbstractMantisClientTest(Version version) {
		this(version, PrivilegeLevel.USER);
	}

	public AbstractMantisClientTest() {
		this(null, PrivilegeLevel.USER);
	}


	public IMantisClient connect(String url, String username, String password, Proxy proxy) throws Exception {
		return connect(url, username, password, proxy, version);
	}

	public IMantisClient connect(String url, String username, String password, Proxy proxy, Version version)
			throws Exception {
		this.repositoryUrl = url;
		this.username = username;
		this.password = password;
		this.repository = MantisClientFactory.createClient(url, version, username, password, proxy);

		return this.repository;
	}

}
