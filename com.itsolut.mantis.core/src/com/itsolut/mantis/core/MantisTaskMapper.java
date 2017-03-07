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

import org.eclipse.mylyn.tasks.core.ITask;
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
	public boolean applyTo(ITask task) {
		
		// cleanup old tasks with missing keys
		if ( task.getTaskKey() == null ) {
			task.setTaskKey(getTaskData().getRoot().getAttribute(TaskAttribute.TASK_KEY).getValue());
		}
		
		return super.applyTo(task);
	}

    @Override
    public PriorityLevel getPriorityLevel() {
        
        try {
            String priority = getTaskData().getRoot().getAttribute(Attribute.PRIORITY.getKey()).getValue();
            
            if ( priority == null ) // task was not refreshed since we the priority mapping in MantisAttributeMapper
                return null;
            
            return MantisPriorityLevel.fromPriorityId(Integer.parseInt(priority));
            
        } catch (NumberFormatException e) {
            
            MantisCorePlugin.warn("Failed getting the priority level for task with id " + getTaskData().getTaskId(), e);
            return null;
        }
    }
}