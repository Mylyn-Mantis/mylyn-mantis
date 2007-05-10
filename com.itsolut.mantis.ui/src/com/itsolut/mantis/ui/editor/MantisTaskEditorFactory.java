/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.ui.editor;

import org.eclipse.mylar.context.core.MylarStatusHandler;
import org.eclipse.mylar.tasks.core.ITask;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.editors.ITaskEditorFactory;
import org.eclipse.mylar.tasks.ui.editors.NewTaskEditorInput;
import org.eclipse.mylar.tasks.ui.editors.RepositoryTaskEditorInput;
import org.eclipse.mylar.tasks.ui.editors.TaskEditor;
import org.eclipse.mylar.tasks.ui.editors.TaskEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.MantisTask;

/**
 * @author Steffen Pingel
 */
public class MantisTaskEditorFactory implements ITaskEditorFactory {

	public boolean canCreateEditorFor(ITask task) {
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
					&& MantisCorePlugin.REPOSITORY_KIND.equals(existingInput.getRepository().getKind());
		} else if (input instanceof NewTaskEditorInput) {
			NewTaskEditorInput newInput = (NewTaskEditorInput) input;
			return newInput.getTaskData() != null
					&& MantisCorePlugin.REPOSITORY_KIND.equals(newInput.getRepository().getKind());
		}
		return false;
	}

	public IEditorPart createEditor(TaskEditor parentEditor, IEditorInput editorInput) {
		if (editorInput instanceof RepositoryTaskEditorInput  || editorInput instanceof TaskEditorInput) {
			return new MantisTaskEditor(parentEditor);
		} else if (editorInput instanceof NewTaskEditorInput) {
			return new NewMantisTaskEditor(parentEditor);
		} 
		return null;
	}

	public IEditorInput createEditorInput(ITask task) {
		MantisTask tracTask = (MantisTask) task;
		TaskRepository repository = TasksUiPlugin.getRepositoryManager().getRepository(MantisCorePlugin.REPOSITORY_KIND,
				tracTask.getRepositoryUrl());
		try {
			return new RepositoryTaskEditorInput(repository, tracTask.getHandleIdentifier(), tracTask.getUrl());
		} catch (Exception e) {
			MylarStatusHandler.fail(e, "Could not create Mantis editor input", true);
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
