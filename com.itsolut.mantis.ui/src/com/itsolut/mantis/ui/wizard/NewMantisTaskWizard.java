/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package com.itsolut.mantis.ui.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylar.internal.tasks.ui.TaskListImages;
import org.eclipse.mylar.internal.tasks.ui.TaskListPreferenceConstants;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.TasksUiUtil;
import org.eclipse.mylar.tasks.ui.editors.NewTaskEditorInput;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard for creating new Mantis tickets through a rich editor..
 * 
 * @author Steffen Pingel
 */
public class NewMantisTaskWizard extends Wizard implements INewWizard {

	private TaskRepository taskRepository;

	private NewMantisTaskPage newTaskPage;

	public NewMantisTaskWizard(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;

		newTaskPage = new NewMantisTaskPage(taskRepository);
		
		setWindowTitle("New Repository Task");
		setDefaultPageImageDescriptor(TaskListImages.BANNER_REPOSITORY);
		
		setNeedsProgressMonitor(true);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		addPage(newTaskPage);
	}

	@Override
	public boolean performFinish() {
		NewTaskEditorInput editorInput = new NewTaskEditorInput(taskRepository, newTaskPage.getRepositoryTaskData());
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		TasksUiUtil.openEditor(editorInput, TaskListPreferenceConstants.TASK_EDITOR_ID, page);
		
		return true;
	}

}
