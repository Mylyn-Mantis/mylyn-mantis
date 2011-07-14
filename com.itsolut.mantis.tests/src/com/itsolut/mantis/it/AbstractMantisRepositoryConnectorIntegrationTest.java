/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.rpc.ServiceException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.data.TaskRelation;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;
import org.junit.Test;

import biz.futureware.mantis.rpc.soap.client.IssueData;
import biz.futureware.mantis.rpc.soap.client.ObjectRef;
import biz.futureware.mantis.rpc.soap.client.RelationshipData;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCache;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectFilter;
import com.itsolut.mantis.core.model.MantisRelationship;

public abstract class AbstractMantisRepositoryConnectorIntegrationTest extends AbstractIntegrationTest {

	@Test
	public void testGetTaskData() throws MalformedURLException, RemoteException, ServiceException, CoreException {

		int firstTaskId = createTask("First task", "Description");

		MantisRepositoryConnector connector = new MantisRepositoryConnector();

		TaskData taskData = connector.getTaskData(repositoryAccessor.getRepository(), String.valueOf(firstTaskId),
				new NullProgressMonitor());

		assertAttributeEquals(taskData.getRoot().getMappedAttribute(TaskAttribute.SUMMARY), "First task");
		assertAttributeEquals(taskData.getRoot().getMappedAttribute(TaskAttribute.DESCRIPTION), "Description");
		assertAttributeEquals(taskData.getRoot().getMappedAttribute(TaskAttribute.STATUS), String.valueOf(10)); // new

	}

	@Test
	public void testPerformQuery() throws MantisException, MalformedURLException, RemoteException, ServiceException {

		createTask("First task", "Description");
		createTask("Second task", "Description");

		MantisCache cache = repositoryAccessor.getClient().getCache(new NullProgressMonitor());
		MantisProject project = cache.getProjectById(DEFAULT_PROJECT_ID.intValue());

		List<MantisProjectFilter> projectFilters = cache.getProjectFilters(1);

		assertEquals(projectFilters.toString(), 1, projectFilters.size());

		MantisProjectFilter filter = projectFilters.get(0);

		final List<TaskData> hits = new ArrayList<TaskData>();

		MantisRepositoryConnector connector = new MantisRepositoryConnector();

		IRepositoryQuery query = getObjectsFactory().newQuery();
		query.setAttribute(IMantisClient.PROJECT_NAME, project.getName());
		query.setAttribute(IMantisClient.FILTER_NAME, filter.getName());
		TaskDataCollector resultCollector = new TaskDataCollector() {

			@Override
			public void accept(TaskData taskData) {

				hits.add(taskData);
			}
		};
		ISynchronizationSession event = getObjectsFactory().newSession();
		IStatus status = connector.performQuery(repositoryAccessor.getRepository(), query, resultCollector, event,
				new NullProgressMonitor());

		assertEquals("status", Status.OK_STATUS, status);
		assertEquals("hits.size", 2, hits.size());
		for (TaskData taskData : hits) {
			assertNotNull("summary", taskData.getRoot().getMappedAttribute(TaskAttribute.SUMMARY));
			assertNotNull("status", taskData.getRoot().getMappedAttribute(TaskAttribute.STATUS));
			assertTrue(taskData.isPartial());
		}
	}

	@Test
	public void testPerformQueryAfterUpdate() throws MantisException, MalformedURLException, RemoteException,
			ServiceException {

		int taskId = createTask("First task", "Description");

		MantisCache cache = repositoryAccessor.getClient().getCache(new NullProgressMonitor());
		MantisProject project = cache.getProjectById(DEFAULT_PROJECT_ID.intValue());

		List<MantisProjectFilter> projectFilters = cache.getProjectFilters(1);

		assertEquals(projectFilters.toString(), 1, projectFilters.size());

		MantisProjectFilter filter = projectFilters.get(0);

		final List<TaskData> hits = new ArrayList<TaskData>();

		MantisRepositoryConnector connector = new MantisRepositoryConnector();

		IRepositoryQuery query = getObjectsFactory().newQuery();
		query.setAttribute(IMantisClient.PROJECT_NAME, project.getName());
		query.setAttribute(IMantisClient.FILTER_NAME, filter.getName());
		TaskDataCollector resultCollector = new TaskDataCollector() {

			@Override
			public void accept(TaskData taskData) {

				hits.add(taskData);
			}
		};
		ISynchronizationSession event = getObjectsFactory().newSession();
		IStatus status = connector.performQuery(repositoryAccessor.getRepository(), query, resultCollector, event,
				new NullProgressMonitor());

		assertEquals("status", Status.OK_STATUS, status);
		assertEquals("hits.size", 1, hits.size());

		IssueData issue = new IssueData();
		issue.setSummary("First task - updated");
		issue.setDescription("Description");
		issue.setProject(new ObjectRef(DEFAULT_PROJECT_ID, ""));
		issue.setCategory(DEFAULT_CATEGORY_NAME);
		repositoryAccessor.getMantisConnectPort().mc_issue_update(getUsername(), getPassword(),
				BigInteger.valueOf(taskId), issue);

		hits.clear();

		event = getObjectsFactory().newSession();
		status = connector.performQuery(repositoryAccessor.getRepository(), query, resultCollector, event,
				new NullProgressMonitor());

		assertEquals("status", Status.OK_STATUS, status);
		assertEquals("hits.size", 1, hits.size());

		assertEquals("First task - updated", hits.get(0).getRoot().getMappedAttribute(TaskAttribute.SUMMARY).getValue());
	}

	@Test
	public void testGetTaskRelations() throws MalformedURLException, RemoteException, ServiceException,
			MantisException, CoreException {

		int taskId = createTask("First task", "Description");
		int secondTaskId = createTask("Second task", "Description");

		RelationshipData relation = new RelationshipData();
		relation.setTarget_id(BigInteger.valueOf(secondTaskId));
		relation.setType(new ObjectRef(BigInteger.valueOf(MantisRelationship.RelationType.PARENT.getMantisConstant()),
				""));

		repositoryAccessor.getMantisConnectPort().mc_issue_relationship_add(getUsername(), getPassword(),
				BigInteger.valueOf(taskId), relation);

		MantisRepositoryConnector connector = new MantisRepositoryConnector();

		TaskData taskData = connector.getTaskData(repositoryAccessor.getRepository(), String.valueOf(taskId),
				new NullProgressMonitor());

		Collection<TaskRelation> taskRelations = connector.getTaskRelations(taskData);

		assertEquals(1, taskRelations.size());
		assertTaskRelationEquals(TaskRelation.subtask(String.valueOf(secondTaskId)), taskRelations.iterator().next());
		
		TaskData childTaskData = connector.getTaskData(repositoryAccessor.getRepository(), String.valueOf(secondTaskId),
				new NullProgressMonitor());
		
		Collection<TaskRelation> childTaskRelations = connector.getTaskRelations(childTaskData);
		
		assertEquals(1, childTaskRelations.size());
		assertTaskRelationEquals(TaskRelation.parentTask(String.valueOf(taskId)), childTaskRelations.iterator().next());
	}
	
	private void assertTaskRelationEquals(TaskRelation expected, TaskRelation actual) {
		
		assertEquals("kind", expected.getKind(), actual.getKind());
		assertEquals("taskId", expected.getTaskId(), actual.getTaskId());
		assertEquals("direction", expected.getDirection(), actual.getDirection());
	}

	private void assertAttributeEquals(TaskAttribute mappedAttribute, String expectedValue) {

		assertNotNull("mappedAttribute is null", mappedAttribute);
		assertEquals(expectedValue, mappedAttribute.getValue());
	}

}
