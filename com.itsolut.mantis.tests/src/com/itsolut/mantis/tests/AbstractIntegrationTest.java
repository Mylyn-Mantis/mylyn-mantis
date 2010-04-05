/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;

import com.itsolut.mantis.binding.IssueData;
import com.itsolut.mantis.binding.ObjectRef;
import com.itsolut.mantis.core.MantisClientFactory;

import junit.framework.TestCase;

/**
 * @author Robert Munteanu
 */
public abstract class AbstractIntegrationTest extends TestCase {

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

	protected final void setUp() throws Exception {

		MantisClientFactory.getDefault().setTaskRepositoryLocationFactory(new TaskRepositoryLocationFactory());

		repositoryAccessor = new MantisRepositoryAccessor(getUsername(), getPassword(), getRepositoryUrlWithOverride());
		repositoryAccessor.init();
		
		postSetUp();
	}

	protected void postSetUp() {
		
	}

	protected final void tearDown() throws Exception {

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