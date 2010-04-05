/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.core.TaskTask;
import org.eclipse.mylyn.internal.tasks.core.data.TextTaskAttachmentSource;
import org.eclipse.mylyn.internal.tasks.core.sync.SynchronizationSession;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentSource;

import com.itsolut.mantis.core.MantisCorePlugin;

/**
 * The <tt>MylynObjectsFactory</tt> creates objects usually provided by the Mylyn Task framework.
 * 
 * <p>
 * Since access to these objects is usually restricted and they are not published API, it adds the benefit of reducing
 * the project's exposure to internal changes.</p>
 * 
 * @author Robert Munteanu
 */
@SuppressWarnings("restriction")
public class MylynObjectsFactory {

	private int queryCounter;

	public ITask newTask(String repositoryUrl, String taskId) {
		return new TaskTask(MantisCorePlugin.REPOSITORY_KIND, repositoryUrl, taskId);
	}

	public AbstractTaskAttachmentSource newTaskAttachmentSource(String contents) {
		return new TextTaskAttachmentSource("Attachment contents");
	}

	public RepositoryQuery newQuery() {
		return new RepositoryQuery(MantisCorePlugin.REPOSITORY_KIND, "test-" + ++queryCounter);
	}

	public SynchronizationSession newSession() {
		return new SynchronizationSession();
	}

}
