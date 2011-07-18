/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Map;

import javax.xml.rpc.ServiceException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.itsolut.mantis.core.DefaultConstantValues;
import com.itsolut.mantis.core.MantisRepositoryConfiguration;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.MantisTaskDataHandler;
import com.itsolut.mantis.core.StatusFactory;
import com.itsolut.mantis.tests.MantisRepositoryAccessor;

public abstract class AbstractMantisTaskDataHandlerTest extends AbstractIntegrationTest {

	@Test
	public void testUnableToCloneNullTask() {
		
		AbstractTaskDataHandler taskDataHandler = newTaskDataHandler();
		
		assertFalse(taskDataHandler.canInitializeSubTaskData(repositoryAccessor.getRepository(), null));
	}

	private MantisTaskDataHandler newTaskDataHandler() {
		return new MantisTaskDataHandler(MantisRepositoryAccessor.clientManager, new StatusFactory());
	}
	
	@Test
	public void testUnableToCloneTaskWithoutProperKey() {
		
		assertFalse(newTaskDataHandler().canInitializeSubTaskData(repositoryAccessor.getRepository(), getObjectsFactory().newTask(repositoryAccessor.getLocation().getUrl(), "1")));
	}

	@Test
	public void testAbleToCloneTaskWithProperKey() {
		
		ITask task = getObjectsFactory().newTask(repositoryAccessor.getLocation().getUrl(), "1");
		TaskRepository repository = repositoryAccessor.getRepository();
		MantisRepositoryConfiguration.setSupportsSubTasks(repository, true);
		try {
			assertTrue(newTaskDataHandler().canInitializeSubTaskData(repository, task));
		} finally {
			MantisRepositoryConfiguration.setSupportsSubTasks(repository, false);
		}
	}
	
	@Test
	public void getTaskData() throws MalformedURLException, RemoteException, ServiceException, CoreException {

		int firstTaskId = createTask("First task", "Description");

		TaskData taskData = new MantisTaskDataHandler(MantisRepositoryAccessor.clientManager, new StatusFactory())
			.getTaskData(repositoryAccessor.getRepository(), String.valueOf(firstTaskId), new NullProgressMonitor());
		
		Map<String, String> expectedValues = Maps.newHashMap();
		expectedValues.put(TaskAttribute.PRODUCT, "Test project");
		expectedValues.put(TaskAttribute.SUMMARY, "First task");
		expectedValues.put(TaskAttribute.DESCRIPTION, "Description");
		expectedValues.put(TaskAttribute.STATUS, String.valueOf(DefaultConstantValues.Status.NEW.getValue()));
		expectedValues.put(TaskAttribute.RESOLUTION, String.valueOf(DefaultConstantValues.Resolution.OPEN.getValue()));
		expectedValues.put(TaskAttribute.PRIORITY, String.valueOf(DefaultConstantValues.Priority.NORMAL.getValue()));
		expectedValues.put(TaskAttribute.SEVERITY, String.valueOf(DefaultConstantValues.Severity.MINOR.getValue()));
		
		for ( Map.Entry<String, String> expectedValueEntry : expectedValues.entrySet() )
			assertAttributeEquals(taskData.getRoot().getMappedAttribute(expectedValueEntry.getKey()), expectedValueEntry.getValue());
	}
	

	private void assertAttributeEquals(TaskAttribute mappedAttribute, String expectedValue) {

		assertNotNull("mappedAttribute is null", mappedAttribute);
		assertEquals(expectedValue, mappedAttribute.getValue());
	}

}
