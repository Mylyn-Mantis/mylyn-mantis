/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.it;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.junit.After;
import org.junit.Before;

import com.itsolut.mantis.tests.MantisRepositoryAccessor;
import com.itsolut.mantis.tests.MylynObjectsFactory;
import com.itsolut.mantis.tests.RepositoryConfiguration;

import biz.futureware.mantis.rpc.soap.client.IssueData;
import biz.futureware.mantis.rpc.soap.client.ObjectRef;

/**
 * @author Robert Munteanu
 */
public abstract class AbstractIntegrationTest  {

	protected static final BigInteger DEFAULT_PROJECT_ID = BigInteger.ONE;
	protected static final String DEFAULT_CATEGORY_NAME = "General";
	
	protected MantisRepositoryAccessor repositoryAccessor;
	private MylynObjectsFactory objectsFactory;
	
	private String getRepositoryUrlWithOverride() {

		return System.getProperty("mantis.test." + getRepositoryConfiguration() + ".url",
				getRepositoryConfiguration().getDefaultUrl());
	}

	protected abstract RepositoryConfiguration getRepositoryConfiguration();

	protected String getOverrideKey() {
		return getClass().getSimpleName();
	}

	protected String getPassword() {
		return "root";
	}

	protected String getUsername() {
		return "administrator";
	}

	@Before
	public final void setUp() throws Exception {

		repositoryAccessor = new MantisRepositoryAccessor(getUsername(), getPassword(), getRepositoryUrlWithOverride());
		repositoryAccessor.init();
		
		objectsFactory = new MylynObjectsFactory();
		
		postSetUp();
	}

	protected void postSetUp() {
		
	}

	@After
	public final void tearDown() throws Exception {

		try {
			preTearDown();
		} finally {
			repositoryAccessor.deleteIssues();
		}
	}

	protected void preTearDown() {
		
	}
	
	protected MylynObjectsFactory getObjectsFactory() {
		return objectsFactory;
	}

	protected int createTask(String summary, String description) throws MalformedURLException, ServiceException,
			RemoteException {

		IssueData issue = new IssueData();
		issue.setSummary(summary);
		issue.setDescription(description);
		issue.setProject(new ObjectRef(DEFAULT_PROJECT_ID, ""));
		issue.setCategory(DEFAULT_CATEGORY_NAME);

		int newTaskId = repositoryAccessor.getMantisConnectPort()
				.mc_issue_add(getUsername(), getPassword(), issue)
				.intValue();

		repositoryAccessor.registerIssueToDelete(newTaskId);

		return newTaskId;
	}

}