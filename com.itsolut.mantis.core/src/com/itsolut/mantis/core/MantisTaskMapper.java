/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Robert Munteanu
 *******************************************************************************/
package com.itsolut.mantis.core;

import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;

import com.itsolut.mantis.core.MantisAttributeMapper.Attribute;


final class MantisTaskMapper extends TaskMapper {

    MantisTaskMapper(TaskData taskData) {

        super(taskData);
    }

	@Override
    public void setProduct(String product) {

        // ignore, set during task data initialization
    }

    @Override
    public PriorityLevel getPriorityLevel() {
        
        try {
            
            TaskAttribute priorityAttribute = getTaskData().getRoot().getAttribute(Attribute.PRIORITY.getKey());
            
            String value = priorityAttribute.getMetaData().getValue(MantisAttributeMapper.TASK_ATTRIBUTE_PRIORITY_ID);
            
            if ( value == null ) // task was not refreshed since we introduced the new meta attribute
                return null;
            
            int priorityLevel = Integer.parseInt(value);
            
            return MantisPriorityLevel.fromPriorityId(priorityLevel);
            
        } catch (NumberFormatException e) {
            
            MantisCorePlugin.warn("Failed getting the priority level", e);
            return null;
        }
    }
}