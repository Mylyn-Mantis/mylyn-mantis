/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import com.itsolut.mantis.core.*;
import com.itsolut.mantis.core.model.MantisRelationship;

public class TaskRelationshipChangeFinderTest extends TestCase {
	
	public void testNullArguments() {
		
		try {
			newChangeFinder().findChanges(null, Collections.<TaskAttribute> emptySet());
			fail("Should have thrown a RuntimeException");
		} catch (RuntimeException e) {
		}
		
		TaskData taskData = newTaskData();
		
		try {

			newChangeFinder().findChanges(taskData, null);
			fail("Should have thrown a RuntimeException");
		} catch (RuntimeException e) {
		}
	}

	private TaskRelationshipChangeFinder newChangeFinder() {
		return new TaskRelationshipChangeFinder(new MantisTaskDataHandler(null));
	}

	private TaskData newTaskData() {
	
		return new TaskData(new MantisAttributeMapper(new TaskRepository(MantisCorePlugin.REPOSITORY_KIND, "http://localhost")), MantisCorePlugin.REPOSITORY_KIND, "http://localhost", "-1");
	}
	
	public void testEmptyChangedAttributeDetectsNoChanges() {

		TaskData taskData = newTaskData();
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(taskData, Collections.<TaskAttribute> emptySet());
		
		assertEquals(0, changes.size());
	}
	
	public void testUpdatedSummaryDetectsNoChanges() {
		
		TaskData oldData = newTaskData();
		TaskAttribute oldSummary = oldData.getRoot().createAttribute(MantisAttributeMapper.Attribute.SUMMARY.getKey());
		oldSummary.setValue("Old status value");

		TaskData newData = newTaskData();
		TaskAttribute summary = newData.getRoot().createAttribute(MantisAttributeMapper.Attribute.SUMMARY.getKey());
		summary.setValue("New status value");
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(newData, Collections.<TaskAttribute> singleton(oldSummary));
		
		assertEquals(0, changes.size());
	}
	
	public void testNewParentDetectsChanges() {
		
		TaskData oldData = newTaskData();
		TaskAttribute oldParent = oldData.getRoot().createAttribute(MantisAttributeMapper.Attribute.PARENT_OF.getKey());
		oldParent.setValue("");

		TaskData newData = newTaskData();
		TaskAttribute parent = newData.getRoot().createAttribute(MantisAttributeMapper.Attribute.PARENT_OF.getKey());
		parent.setValue("55");
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(newData, Collections.<TaskAttribute> singleton(oldParent));
		
		assertEquals(1, changes.size());
		
		TaskRelationshipChange newParent = changes.get(0);
		assertEquals( TaskRelationshipChange.Direction.Added, newParent.getDirection());
		assertEquals(MantisRelationship.RelationType.PARENT, newParent.getRelationship().getType());
		assertEquals(-1, newParent.getRelationship().getId());
		assertEquals(55, newParent.getRelationship().getTargetId());
	}
	
	public void testNewWithNoAttributeParentDetectsChanges() {
		
		TaskData newData = newTaskData();
		TaskAttribute summary = newData.getRoot().createAttribute(MantisAttributeMapper.Attribute.PARENT_OF.getKey());
		summary.setValue("55");
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(newData, Collections.<TaskAttribute> emptySet());
		
		assertEquals(1, changes.size());
		
		TaskRelationshipChange newParent = changes.get(0);
		assertEquals( TaskRelationshipChange.Direction.Added, newParent.getDirection());
		assertEquals(MantisRelationship.RelationType.PARENT, newParent.getRelationship().getType());
		assertEquals(-1, newParent.getRelationship().getId());
		assertEquals(55, newParent.getRelationship().getTargetId());		
	}
	
	public void testRemovedParentDetectsChanges() {
		
		TaskData oldData = newTaskData();
		TaskAttribute oldParent = oldData.getRoot().createAttribute(MantisAttributeMapper.Attribute.PARENT_OF.getKey());
		oldParent.setValue("55");

		TaskData newData = newTaskData();
		TaskAttribute parent = newData.getRoot().createAttribute(MantisAttributeMapper.Attribute.PARENT_OF.getKey());
		parent.setValue("");
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(newData, Collections.<TaskAttribute> singleton(oldParent));
		
		assertEquals(1, changes.size());
		
		TaskRelationshipChange newParent = changes.get(0);
		assertEquals( TaskRelationshipChange.Direction.Removed, newParent.getDirection());
		assertEquals(MantisRelationship.RelationType.PARENT, newParent.getRelationship().getType());
		assertEquals(-1, newParent.getRelationship().getId());
		assertEquals(55, newParent.getRelationship().getTargetId());
	}
	
	public void testRemovedParentWithNoAttributeParentDetectsChanges() {
		
		TaskData oldData = newTaskData();
		TaskAttribute oldParent = oldData.getRoot().createAttribute(MantisAttributeMapper.Attribute.PARENT_OF.getKey());
		oldParent.setValue("55");

		TaskData newData = newTaskData();
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(newData, Collections.<TaskAttribute> singleton(oldParent));
		
		assertEquals(1, changes.size());
		
		TaskRelationshipChange newParent = changes.get(0);
		assertEquals( TaskRelationshipChange.Direction.Removed, newParent.getDirection());
		assertEquals(MantisRelationship.RelationType.PARENT, newParent.getRelationship().getType());
		assertEquals(-1, newParent.getRelationship().getId());
		assertEquals(55, newParent.getRelationship().getTargetId());		
	}
	
	public void testParentChangeWithFourValuesIsDetected() {
		
		TaskData oldData = newTaskData();
		TaskAttribute oldParent = oldData.getRoot().createAttribute(MantisAttributeMapper.Attribute.PARENT_OF.getKey());
		oldParent.setValues(Arrays.asList("55", "56"));

		TaskData newData = newTaskData();
		TaskAttribute parent = newData.getRoot().createAttribute(MantisAttributeMapper.Attribute.PARENT_OF.getKey());
		parent.setValues(Arrays.asList("57", "58"));
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(newData, Collections.<TaskAttribute> singleton(oldParent));
		
		assertEquals(4, changes.size());
		assertEquals(TaskRelationshipChange.Direction.Removed, changes.get(0).getDirection());
		assertEquals(TaskRelationshipChange.Direction.Removed, changes.get(1).getDirection());
		assertEquals(TaskRelationshipChange.Direction.Added, changes.get(2).getDirection());
		assertEquals(TaskRelationshipChange.Direction.Added, changes.get(3).getDirection());
	}

}
