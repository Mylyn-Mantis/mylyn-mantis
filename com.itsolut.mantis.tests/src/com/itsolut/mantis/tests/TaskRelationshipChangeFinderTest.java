/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import com.itsolut.mantis.core.MantisAttributeMapper;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.TaskRelationshipChange;
import com.itsolut.mantis.core.TaskRelationshipChangeFinder;

public class TaskRelationshipChangeFinderTest extends TestCase {
	
	public void testNullArguments() {
		
		try {
			new TaskRelationshipChangeFinder().findChanges(null, Collections.<TaskAttribute> emptySet());
			fail("Should have thrown a RuntimeException");
		} catch (RuntimeException e) {
		}
		
		TaskData taskData = newTaskData();
		
		try {

			new TaskRelationshipChangeFinder().findChanges(taskData, null);
			fail("Should have thrown a RuntimeException");
		} catch (RuntimeException e) {
		}
	}

	private TaskData newTaskData() {
	
		return new TaskData(new MantisAttributeMapper(new TaskRepository(MantisCorePlugin.REPOSITORY_KIND, "http://localhost")), MantisCorePlugin.REPOSITORY_KIND, "http://localhost", "-1");
	}
	
	public void testEmptyChangedAttributeDetectsNoChanges() {

		TaskData taskData = newTaskData();
		
		List<TaskRelationshipChange> changes = new TaskRelationshipChangeFinder().findChanges(taskData, Collections.<TaskAttribute> emptySet());
		
		assertEquals(0, changes.size());
	}
	
	public void testUpdatedSummaryDetectsNoChanges() {
		
		TaskData oldData = newTaskData();
		TaskAttribute oldSummary = oldData.getRoot().createAttribute(MantisAttributeMapper.Attribute.SUMMARY.toString());
		oldSummary.setValue("Old status value");

		TaskData newData = newTaskData();
		TaskAttribute summary = newData.getRoot().createAttribute(MantisAttributeMapper.Attribute.SUMMARY.toString());
		summary.setValue("New status value");
		
		List<TaskRelationshipChange> changes = new TaskRelationshipChangeFinder().findChanges(newData, Collections.<TaskAttribute> singleton(oldSummary));
		
		assertEquals(0, changes.size());
	}	

}
