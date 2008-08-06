/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.ui.editor;

import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorFactory;
import org.eclipse.mylyn.tasks.ui.editors.NewTaskEditorInput;
import org.eclipse.mylyn.tasks.ui.editors.RepositoryTaskEditorInput;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorInput;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;


import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.MantisTask;

/**
 * @author Steffen Pingel
 */
public class MantisTaskEditorFactory extends AbstractTaskEditorFactory {

	public boolean canCreateEditorFor(AbstractTask task) {
		if (task instanceof MantisTask) {
			TaskRepository repository = TasksUiPlugin.getRepositoryManager().getRepository(
					MantisCorePlugin.REPOSITORY_KIND, ((MantisTask) task).getRepositoryUrl());
			return MantisRepositoryConnector.hasRichEditor(repository);
		}
		return task instanceof MantisTask;
	}

	public boolean canCreateEditorFor(IEditorInput input) {
		if (input instanceof RepositoryTaskEditorInput) {
			RepositoryTaskEditorInput existingInput = (RepositoryTaskEditorInput) input;
			return existingInput.getTaskData() != null
					&& MantisCorePlugin.REPOSITORY_KIND.equals(existingInput.getRepository().getConnectorKind());
		} else if (input instanceof NewTaskEditorInput) {
			NewTaskEditorInput newInput = (NewTaskEditorInput) input;
			return newInput.getTaskData() != null
					&& MantisCorePlugin.REPOSITORY_KIND.equals(newInput.getRepository().getConnectorKind());
		}
		return false;
	}

	public IEditorPart createEditor(TaskEditor parentEditor, IEditorInput editorInput) {
		if (editorInput instanceof RepositoryTaskEditorInput) {
			RepositoryTaskEditorInput taskInput = (RepositoryTaskEditorInput) editorInput;
			if (taskInput.getTaskData().isNew()) {
				return new NewMantisTaskEditor(parentEditor);
			} else {
				return new MantisTaskEditor(parentEditor);
			}
		} else if (editorInput instanceof TaskEditorInput) {
			return new MantisTaskEditor(parentEditor);
		}
		return null;
	}

	public IEditorInput createEditorInput(AbstractTask task) {
		MantisTask tracTask = (MantisTask) task;
		TaskRepository repository = TasksUiPlugin.getRepositoryManager().getRepository(MantisCorePlugin.REPOSITORY_KIND,
				tracTask.getRepositoryUrl());
		try {
			return new RepositoryTaskEditorInput(repository, tracTask.getTaskId(), tracTask.getUrl());
		} catch (Exception e) {
			StatusHandler.fail(e, "Could not create Mantis editor input", true);
		}
		return null;
	}

	public String getTitle() {
		return "Mantis";
	}

	public boolean providesOutline() {
		return true;
	}
}
