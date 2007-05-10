/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *******************************************************************************/

package com.itsolut.mantis.core;

import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.RepositoryTaskAttribute;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisTask extends AbstractRepositoryTask {

	public MantisTask(String handle, String label, boolean newTask) {
		super(handle, label, newTask);

		setUrl(AbstractRepositoryTask.getRepositoryUrl(handle) + IMantisClient.TICKET_URL
				+ AbstractRepositoryTask.getTaskId(handle));
	}

	@Override
	public boolean isCompleted() {
		if (taskData != null) {
			return isCompleted(taskData.getStatus());
		} else {
			return super.isCompleted();
		}
	}

	@Override
	public String getRepositoryKind() {
		return MantisCorePlugin.REPOSITORY_KIND;
	}

	@Override
	public String getOwner() {
		if (taskData != null && taskData.getAttribute(RepositoryTaskAttribute.USER_ASSIGNED) != null) {
			return taskData.getAttributeValue(RepositoryTaskAttribute.USER_ASSIGNED);
		} else {
			return super.getOwner();
		}
	}
	
	public static boolean isCompleted(String status) {
		return "closed".equals(status) || "resolved".equals(status);
	}

}
