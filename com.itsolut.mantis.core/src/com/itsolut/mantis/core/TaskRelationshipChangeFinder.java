/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import com.itsolut.mantis.core.MantisAttributeMapper.Attribute;
import com.itsolut.mantis.core.TaskRelationshipChange.Direction;
import com.itsolut.mantis.core.model.MantisRelationship;
import com.itsolut.mantis.core.util.MantisUtils;

public class TaskRelationshipChangeFinder {

    private final MantisTaskDataHandler _mantisTaskDataHandler;

    public TaskRelationshipChangeFinder(MantisTaskDataHandler mantisTaskDataHandler) {

        _mantisTaskDataHandler = mantisTaskDataHandler;
    }

    public List<TaskRelationshipChange> findChanges(TaskData taskData, Set<TaskAttribute> changedAttributes) {

        Assert.isNotNull(taskData);
        Assert.isNotNull(changedAttributes);

        List<TaskRelationshipChange> changes = new ArrayList<TaskRelationshipChange>();
        int taskId = Integer.parseInt(taskData.getTaskId());

        for (Attribute relationAttribute : MantisAttributeMapper.taskRelationAttributes()) {

            TaskAttribute parentAttribute = taskData.getRoot().getAttribute(relationAttribute.getKey());
            List<String> newValues = parentAttribute != null ? parentAttribute.getValues() : new ArrayList<String>();
            List<String> oldValues = findOldValues(relationAttribute, changedAttributes);

            changes.addAll(findRemovedValues(taskId, relationAttribute, newValues, oldValues));
            changes.addAll(findAddedValues(taskId, relationAttribute, newValues, oldValues));
        }

        return changes;
    }

    private List<String> findOldValues(Attribute relationAttribute, Set<TaskAttribute> changedAttributes) {

        List<String> oldValues = new ArrayList<String>();

        for (TaskAttribute oldAttribute : changedAttributes) {
            if (MantisUtils.isEmpty(oldAttribute.getValue()))
                continue;
            if (oldAttribute.getId().equals(relationAttribute.getKey())) {
                oldValues.addAll(oldAttribute.getValues());
                break;
            }
        }
        return oldValues;
    }

    private List<TaskRelationshipChange> findRemovedValues(int taskId, Attribute relationAttribute,
            List<String> newValues, List<String> oldValues) {

        return findChanges(taskId, relationAttribute, newValues, oldValues, Direction.Removed);
    }

    private List<TaskRelationshipChange> findChanges(int taskId, Attribute relationAttribute, List<String> toValues,
            List<String> fromValues, Direction direction) {

        List<TaskRelationshipChange> changed = new ArrayList<TaskRelationshipChange>();
        
        for (String fromValue : fromValues) {

            if (MantisUtils.isEmpty(fromValue))
                continue;

            if (!(toValues.contains(fromValue))) {
                changed.add(new TaskRelationshipChange(direction, createRelationship(relationAttribute, taskId,
                        fromValue)));
            }
        }
        return changed;
    }

    private MantisRelationship createRelationship(Attribute relationAttribute, int taskId, String targetId) {

        MantisRelationship relationship = new MantisRelationship();
        
        relationship.setType(_mantisTaskDataHandler.getRelationTypeForAttribute(relationAttribute));
        relationship.setTargetId(Integer.parseInt(targetId));
        relationship.setId(taskId);

        return relationship;

    }

    private List<TaskRelationshipChange> findAddedValues(int taskId, Attribute relationAttribute,
            List<String> newValues, List<String> oldValues) {
        
        return findChanges(taskId, relationAttribute, oldValues, newValues, Direction.Added);
    }

}
