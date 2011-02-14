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
	
	private static final String RELATED_TASK_ID_1 = "55";
	private static final String RELATED_TASK_ID_2 = "56";
	
	private static final List<String> RELATED_TASK_IDS_1_2 = Arrays.asList(RELATED_TASK_ID_1, RELATED_TASK_ID_2);
	
	private static final String RELATED_TASK_ID_3 = "57";
	private static final String RELATED_TASK_ID_4 = "58";
	
	private static final List<String> RELATED_TASK_IDS_3_4 = Arrays.asList(RELATED_TASK_ID_3, RELATED_TASK_ID_4);
	
	private static final String OLD_RELATION_ID = "22";
	private static final String OLD_RELATION_IDS = "22,23";
	
	public void testNullArguments() {
		
		try {
			newChangeFinder().findChanges(null, Collections.<TaskAttribute> emptySet());
			fail("Should have thrown a RuntimeException");
		} catch (RuntimeException e) {
		}
		
		TaskData taskData = newExistingTaskData();
		
		try {

			newChangeFinder().findChanges(taskData, null);
			fail("Should have thrown a RuntimeException");
		} catch (RuntimeException e) {
		}
	}

	private TaskRelationshipChangeFinder newChangeFinder() {
		return new TaskRelationshipChangeFinder(new MantisTaskDataHandler(null));
	}

	private TaskData newExistingTaskData() {
	
		return new TaskData(new MantisAttributeMapper(new TaskRepository(MantisCorePlugin.REPOSITORY_KIND, "http://localhost")), MantisCorePlugin.REPOSITORY_KIND, "http://localhost", "-1");
	}

	private TaskData newUnsubmittedTaskData() {
		
		return new TaskData(new MantisAttributeMapper(new TaskRepository(MantisCorePlugin.REPOSITORY_KIND, "http://localhost")), MantisCorePlugin.REPOSITORY_KIND, "http://localhost", "");
	}
	
	public void testInconsistentTaskStatusAndAttributes() {
		
		TaskAttribute oldParent = newParentOfAttribute(newExistingTaskData(), Arrays.asList(RELATED_TASK_ID_1), OLD_RELATION_ID);

		TaskData newData = newUnsubmittedTaskData();
		newParentOfAttribute(newData, Arrays.asList(RELATED_TASK_ID_1 + "," + RELATED_TASK_ID_2), "");
		
		try {
			newChangeFinder().findChanges(newData, Collections.<TaskAttribute> singleton(oldParent));
			fail("Should have thrown a RuntimeException");
		} catch (RuntimeException e) {
		}
	}
	
	public void testEmptyChangedAttributeDetectsNoChanges() {
		
		TaskData taskData = newExistingTaskData();
		newParentOfAttribute(taskData, Arrays.asList(RELATED_TASK_ID_1), "");
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(taskData, Collections.<TaskAttribute> emptySet());
		
		assertEquals(0, changes.size());
	}
	
	private TaskAttribute newParentOfAttribute(TaskData taskData, List<String> values, String relationshipIdsMetaValue) {
		
		TaskAttribute attribute = taskData.getRoot().createAttribute(MantisAttributeMapper.Attribute.PARENT_OF.getKey());
		attribute.setValues(values);
		attribute.getMetaData().putValue(MantisAttributeMapper.TASK_ATTRIBUTE_RELATIONSHIP_IDS, relationshipIdsMetaValue);
		
		return attribute; 
	}
	
	public void testUpdatedSummaryDetectsNoChanges() {

		TaskAttribute oldSummary = newExistingTaskData().getRoot().createAttribute(MantisAttributeMapper.Attribute.SUMMARY.getKey());
		oldSummary.setValue("Old status value");

		TaskData newData = newExistingTaskData();
		newData.getRoot().createAttribute(MantisAttributeMapper.Attribute.SUMMARY.getKey()).setValue("New");
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(newData, Collections.<TaskAttribute> singleton(oldSummary));
		
		assertEquals(0, changes.size());
	}
	
	public void testNewParentDetectsChanges() {

		TaskAttribute oldParent = newParentOfAttribute(newExistingTaskData(), Collections. <String> emptyList(), "");

		TaskData newData = newExistingTaskData();
		newParentOfAttribute(newData, Arrays.asList(RELATED_TASK_ID_1), "");
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(newData, Collections.<TaskAttribute> singleton(oldParent));
		
		assertEquals(1, changes.size());
		
		TaskRelationshipChange newParent = changes.get(0);
		assertEquals(TaskRelationshipChange.Direction.Added, newParent.getDirection());
		assertEquals(MantisRelationship.RelationType.PARENT, newParent.getRelationship().getType());
		assertEquals(0, newParent.getRelationship().getId());
		assertEquals(RELATED_TASK_ID_1, String.valueOf(newParent.getRelationship().getTargetId()));
	}
	
	public void testRemovedParentDetectsChanges() {
		
		TaskAttribute oldParent = newParentOfAttribute(newExistingTaskData(), Arrays.asList(RELATED_TASK_ID_1), OLD_RELATION_ID);

		TaskData newData = newExistingTaskData();
		newParentOfAttribute(newData, Collections. <String> emptyList(), "");
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(newData, Collections.<TaskAttribute> singleton(oldParent));
		
		assertEquals(1, changes.size());
		
		TaskRelationshipChange newParent = changes.get(0);
		assertEquals( TaskRelationshipChange.Direction.Removed, newParent.getDirection());
		assertEquals(MantisRelationship.RelationType.PARENT, newParent.getRelationship().getType());
		assertEquals(OLD_RELATION_ID, String.valueOf(newParent.getRelationship().getId()));
		assertEquals(RELATED_TASK_ID_1, String.valueOf(newParent.getRelationship().getTargetId()));
	}
	
	public void testRemovedParentWithNoAttributeParentDetectsChanges() {
		
		TaskAttribute oldParent = newParentOfAttribute(newExistingTaskData(), Arrays.asList(RELATED_TASK_ID_1), OLD_RELATION_ID);

		TaskData newData = newExistingTaskData();
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(newData, Collections.<TaskAttribute> singleton(oldParent));
		
		assertEquals(1, changes.size());
		
		TaskRelationshipChange newParent = changes.get(0);
		assertEquals( TaskRelationshipChange.Direction.Removed, newParent.getDirection());
		assertEquals(MantisRelationship.RelationType.PARENT, newParent.getRelationship().getType());
		assertEquals(OLD_RELATION_ID, String.valueOf(newParent.getRelationship().getId()));
		assertEquals(RELATED_TASK_ID_1, String.valueOf(newParent.getRelationship().getTargetId()));		
	}
	
	public void testParentChangeWithFourValuesIsDetected() {
		
		TaskAttribute oldParent = newParentOfAttribute(newExistingTaskData(), RELATED_TASK_IDS_1_2, OLD_RELATION_IDS);

		TaskData newData = newExistingTaskData();
		newParentOfAttribute(newData, RELATED_TASK_IDS_3_4, "");
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(newData, Collections.<TaskAttribute> singleton(oldParent));
		
		assertEquals(4, changes.size());
		assertEquals(TaskRelationshipChange.Direction.Removed, changes.get(0).getDirection());
		assertEquals(TaskRelationshipChange.Direction.Removed, changes.get(1).getDirection());
		assertEquals(TaskRelationshipChange.Direction.Added, changes.get(2).getDirection());
		assertEquals(TaskRelationshipChange.Direction.Added, changes.get(3).getDirection());
	}
	
	public void testNewTaskWithParentDetectsChanges() {

		TaskData newData = newUnsubmittedTaskData();
		newParentOfAttribute(newData, Arrays.asList(RELATED_TASK_ID_1), "");
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(newData, Collections.<TaskAttribute> emptySet());
		
		assertEquals(1, changes.size());
		
		TaskRelationshipChange newParent = changes.get(0);
		assertEquals(TaskRelationshipChange.Direction.Added, newParent.getDirection());
		assertEquals(MantisRelationship.RelationType.PARENT, newParent.getRelationship().getType());
		assertEquals(0, newParent.getRelationship().getId());
		assertEquals(RELATED_TASK_ID_1, String.valueOf(newParent.getRelationship().getTargetId()));
	}

	public void testParentChangeWithCSVValueDetectsChanges() {
		
		TaskAttribute oldParent = newParentOfAttribute(newExistingTaskData(), Arrays.asList(RELATED_TASK_ID_1), OLD_RELATION_ID);

		TaskData newData = newExistingTaskData();
		newParentOfAttribute(newData, Arrays.asList(RELATED_TASK_ID_1 + "," + RELATED_TASK_ID_2), "");
		
		List<TaskRelationshipChange> changes = newChangeFinder().findChanges(newData, Collections.<TaskAttribute> singleton(oldParent));
		
		assertEquals(1, changes.size());
		
		TaskRelationshipChange newParent = changes.get(0);
		assertEquals(TaskRelationshipChange.Direction.Added, newParent.getDirection());
		assertEquals(MantisRelationship.RelationType.PARENT, newParent.getRelationship().getType());
		assertEquals(0, newParent.getRelationship().getId());
		assertEquals(RELATED_TASK_ID_2, String.valueOf(newParent.getRelationship().getTargetId()));
	}
}
