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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import com.itsolut.mantis.binding.IssueData;
import com.itsolut.mantis.binding.ObjectRef;
import com.itsolut.mantis.core.MantisClientFactory;
import com.itsolut.mantis.core.MantisRepositoryConnector;

import junit.framework.TestCase;

public abstract class AbstractMantisRepositoryConnectorIntegrationTest extends TestCase {

	private MantisRepositoryAccessor repositoryAccessor;

	protected abstract String getRepositoryUrl();

	private String getRepositoryUrlWithOverride() {

		return System.getProperty("mantis.test." + getOverrideKey() + ".url", getRepositoryUrl());
	}

	protected String getOverrideKey() {

		return getClass().getSimpleName();
	}

	protected String getPassword() {
		return "root";
	}

	protected String getUsername() {

		return "administrator";
	}

	public void testGetTaskData() throws MalformedURLException, RemoteException, ServiceException, CoreException {

		int firstTaskId = createTask("First task", "Description");

		MantisRepositoryConnector connector = new MantisRepositoryConnector();

		TaskData taskData = connector.getTaskData(repositoryAccessor.getRepository(), String.valueOf(firstTaskId),
				new NullProgressMonitor());

		assertAttributeEquals(taskData.getRoot().getMappedAttribute(TaskAttribute.SUMMARY), "First task");
		assertAttributeEquals(taskData.getRoot().getMappedAttribute(TaskAttribute.DESCRIPTION), "Description");
		assertAttributeEquals(taskData.getRoot().getMappedAttribute(TaskAttribute.STATUS), "new");
		
	}

	private void assertAttributeEquals(TaskAttribute mappedAttribute, String expectedValue) {

		assertNotNull("mappedAttribute is null", mappedAttribute);
		assertEquals(expectedValue, mappedAttribute.getValue());
	}

	protected int createTask(String summary, String description) throws MalformedURLException, ServiceException,
			RemoteException {

		IssueData issue = new IssueData();
		issue.setSummary(summary);
		issue.setDescription(description);
		issue.setProject(new ObjectRef(BigInteger.ONE, ""));
		issue.setCategory("General");

		int newTaskId = repositoryAccessor.getMantisConnectPort()
				.mc_issue_add(getUsername(), getPassword(), issue)
				.intValue();

		repositoryAccessor.registerIssueToDelete(newTaskId);

		return newTaskId;
	}

	protected void setUp() throws Exception {
		
		MantisClientFactory.getDefault().setTaskRepositoryLocationFactory(new TaskRepositoryLocationFactory());

		repositoryAccessor = new MantisRepositoryAccessor(getUsername(), getPassword(), getRepositoryUrlWithOverride());
		repositoryAccessor.init();

	}

	protected void tearDown() throws Exception {

		repositoryAccessor.deleteIssues();
	}

}
