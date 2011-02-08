/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;

public class TaskRelationshipChangeFinder {

    public List<TaskRelationshipChange> findChanges(TaskData taskData, Set<TaskAttribute> changedAttributes) {

        Assert.isNotNull(taskData);
        Assert.isNotNull(changedAttributes);
        
        return Collections.emptyList();
    }
}
