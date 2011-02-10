/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.util.*;

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.osgi.util.NLS;

import com.itsolut.mantis.core.MantisAttributeMapper.Attribute;
import com.itsolut.mantis.core.TaskRelationshipChange.Direction;
import com.itsolut.mantis.core.model.MantisRelationship;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * 
 * @author Robert Munteanu
 */
public class TaskRelationshipChangeFinder {

    private final MantisTaskDataHandler _mantisTaskDataHandler;

    public TaskRelationshipChangeFinder(MantisTaskDataHandler mantisTaskDataHandler) {

        _mantisTaskDataHandler = mantisTaskDataHandler;
    }

    /**
     * Returns a sorted list of detected changes, possibly empty
     * 
     * <p>The list will contain all the found deletions and then all the found additions. This will
     * insure that the change list can be processed as-is, without additional sorting.</p>
     * 
     * @param taskData
     * @param changedAttributes
     * @return a sorted list of detected changes, possibly empty
     */
    public List<TaskRelationshipChange> findChanges(TaskData taskData, Set<TaskAttribute> changedAttributes) {

        Assert.isNotNull(taskData);
        Assert.isNotNull(changedAttributes);

        List<TaskRelationshipChange> changes = new ArrayList<TaskRelationshipChange>();
        int taskId = Integer.parseInt(taskData.getTaskId());

        for (Attribute relationAttribute : MantisAttributeMapper.taskRelationAttributes()) {

            TaskAttribute parentAttribute = taskData.getRoot().getAttribute(relationAttribute.getKey());
            TaskAttribute oldAttribute = null;
            for (TaskAttribute attribute : changedAttributes) {
                if (attribute.getId().equals(relationAttribute.getKey())) {
                    oldAttribute = attribute;
                    break;
                }
            }
            
            if (oldAttribute == null)
                continue;
            
            List<String> newValues = parentAttribute != null ? parentAttribute.getValues() : Collections.<String> emptyList();
            Map<String,String> oldIdToValues = findOldValues(oldAttribute);

            changes.addAll(findRemovedValues(taskId, relationAttribute, newValues, oldIdToValues));
            changes.addAll(findAddedValues(taskId, relationAttribute, newValues, new ArrayList<String>(oldIdToValues.values())));
        }

        // place deletions first, as this is the natural processing order 
        Collections.sort(changes, new Comparator<TaskRelationshipChange>() {

            public int compare(TaskRelationshipChange o1, TaskRelationshipChange o2) {

                if ( o1.getDirection() == o2.getDirection() )
                    return 0;
                
                if ( o1.getDirection() == Direction.Added )
                    return 1;
                
                return -1;
            }
        });
        
        return changes;
    }

    private List<String> fromCsvString(String value) {
        
        if ( MantisUtils.isEmpty(value) )
            return Collections.emptyList();

        String[] raw = value.split("\\,");
        List<String> values = new ArrayList<String>(raw.length);
        for (String rawValue : raw)
            values.add(rawValue.trim());

        return values;
    }
    
    private Map<String,String> findOldValues(TaskAttribute oldAttribute) {
        
        List<String> oldValues = oldAttribute.getValues();
        List<String> oldIds = fromCsvString(oldAttribute.getMetaData().getValue(MantisAttributeMapper.TASK_ATTRIBUTE_RELATIONSHIP_IDS));

        Assert.isTrue(oldValues.size() == oldIds.size(), NLS.bind("Inconsistency when reading old attribute values. oldValues: {0}, oldIds: {1}.", oldValues, oldIds));
        
        Map<String,String> oldValuesById = new HashMap<String, String>();
        
        for ( int i = 0; i < oldValues.size(); i++ )
            oldValuesById.put(oldIds.get(i), oldValues.get(i));
        
        return oldValuesById;
        
    }

    private List<TaskRelationshipChange> findRemovedValues(int taskId, Attribute relationAttribute,
            List<String> newValues, Map<String, String> oldIdToValues) {

        List<TaskRelationshipChange> changed = new ArrayList<TaskRelationshipChange>();

        for (Map.Entry<String,String> oldValueEntry : oldIdToValues.entrySet()) {

            if (!(newValues.contains(oldValueEntry.getValue())))
                changed.add(new TaskRelationshipChange(Direction.Removed, createRelationship(relationAttribute, Integer.parseInt(oldValueEntry.getKey()),
                        oldValueEntry.getValue())));
        }
        
        return changed;
    }

    private List<TaskRelationshipChange> findChanges(int taskId, Attribute relationAttribute, List<String> toValues,
            List<String> fromValues, Direction direction) {

        List<TaskRelationshipChange> changed = new ArrayList<TaskRelationshipChange>();

        for (String fromValue : fromValues) {

            if (MantisUtils.isEmpty(fromValue))
                continue;

            if (!(toValues.contains(fromValue)))
                changed.add(new TaskRelationshipChange(direction, createRelationship(relationAttribute, 0,
                        fromValue)));
        }
        return changed;
    }

    private MantisRelationship createRelationship(Attribute relationAttribute, int relationshipId, String targetId) {

        MantisRelationship relationship = new MantisRelationship();

        relationship.setId(relationshipId);
        relationship.setTargetId(Integer.parseInt(targetId));
        relationship.setType(_mantisTaskDataHandler.getRelationTypeForAttribute(relationAttribute));

        return relationship;

    }

    private List<TaskRelationshipChange> findAddedValues(int taskId, Attribute relationAttribute,
            List<String> newValues, List<String> oldValues) {

        return findChanges(taskId, relationAttribute, oldValues, newValues, Direction.Added);
    }

}
