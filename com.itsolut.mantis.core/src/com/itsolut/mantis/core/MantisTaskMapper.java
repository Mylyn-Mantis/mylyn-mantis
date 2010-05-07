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

import java.util.Date;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;

import com.itsolut.mantis.core.exception.MantisException;

final class MantisTaskMapper extends TaskMapper {

    MantisTaskMapper(TaskData taskData) {

        super(taskData);
    }

    @Override
    public Date getCompletionDate() {

        try {
            boolean completed = getClient().isCompleted(getTaskData(), new NullProgressMonitor());

            if (completed)
                return getModificationDate();

            return null;
        } catch (MantisException e) {
            MantisCorePlugin.error(e);
            return null;
        }
    }

    @Override
    public void setCompletionDate(Date dateCompleted) {

        // ignore
    }

    @Override
    public void setProduct(String product) {

        // ignore, set during task data initialization
    }

    @Override
    public PriorityLevel getPriorityLevel() {

        return MantisPriorityLevel.fromPriority(getPriority());
    }

    private IMantisClient getClient() throws MantisException {

        return MantisCorePlugin.getDefault().getConnector().getClientManager().getRepository(
                getTaskData().getAttributeMapper().getTaskRepository());
    }
}