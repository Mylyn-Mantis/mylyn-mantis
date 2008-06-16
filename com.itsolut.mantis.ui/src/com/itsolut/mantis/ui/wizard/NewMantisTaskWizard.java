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
import org.eclipse.mylyn.tasks.ui.wizards.NewTaskWizard;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorInput;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard for creating new Mantis tickets through a rich editor..
 * 
 * @author Steffen Pingel
 */
public class NewMantisTaskWizard extends NewTaskWizard {

	public NewMantisTaskWizard(TaskRepository taskRepository,
			ITaskMapping taskSelection) {
		super(taskRepository, taskSelection);

		newTaskPage = new NewMantisTaskPage(taskRepository);
		
		setWindowTitle("New Repository Task");
		setDefaultPageImageDescriptor(TasksUiImages.BANNER_REPOSITORY);
		
		setNeedsProgressMonitor(true);
	}

	private TaskRepository taskRepository;

	private NewMantisTaskPage newTaskPage;


	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	@Override
	public void addPages() {
		addPage(newTaskPage);
	}

//	@Override
//	public boolean performFinish() {
//		TaskEditorInput editorInput = new TaskEditorInput(taskRepository, (ITask)this.newTaskPage.getRepositoryTaskData());
//		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
//		TasksUiUtil.openEditor(editorInput, TaskEditor.ID_EDITOR, page);
//		
//		return true;
//	}

}
