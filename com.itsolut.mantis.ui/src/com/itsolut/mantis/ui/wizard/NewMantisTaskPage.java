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

package com.itsolut.mantis.ui.wizard;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylyn.tasks.core.AbstractAttributeFactory;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisTaskDataHandler;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.ui.MantisUIPlugin;

/**
 * Wizard page for creating new Mantis tickets through a rich editor.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class NewMantisTaskPage extends WizardPage {

	private TaskRepository taskRepository;

	private RepositoryTaskData taskData;
	public Combo projectCombo;

	public NewMantisTaskPage(TaskRepository taskRepository) {
		super("New Task");

		setTitle("Mantis - New Issue");
		setDescription("Select the tickets project.");

		this.taskRepository = taskRepository;
	}

	public void createControl(Composite parent) {
//		Text text = new Text(parent, SWT.WRAP);
//		text.setEditable(false);
//		setControl(text);
//		text.setText("Click Finish");
//		
		projectCombo = new Combo(parent, SWT.READ_ONLY);
		projectCombo.add("Select Project for new Issue");
		setControl(projectCombo);

		try {
			MantisRepositoryConnector connector = (MantisRepositoryConnector)TasksUiPlugin.getRepositoryManager().getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);
			IMantisClient client = connector.getClientManager().getRepository(taskRepository);
			
			for(MantisProject pd : client.getProjects()){
				projectCombo.add(pd.getName());
			}
			projectCombo.setText(projectCombo.getItem(0));
			
			projectCombo.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					if(projectCombo.getSelectionIndex()>0){
						
						taskData.setAttributeValue(Key.PROJECT.getKey(), projectCombo.getText());
						MantisRepositoryConnector connector = (MantisRepositoryConnector) TasksUiPlugin.getRepositoryManager()
						                                                                               .getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);
						try{
							MantisTaskDataHandler.createProjectSpecificAttributes(taskData, connector.getClientManager().getRepository(taskRepository));
						} catch (MalformedURLException ex) {
							MantisUIPlugin.handleMantisException(ex);
							return;
						}
					}
					getWizard().getContainer().updateButtons();
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					//nothing
				}
			});
		} catch (Exception e1) {
			MantisCorePlugin.log(e1);
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		updateAttributesFromRepository();
	}

	@Override
	public boolean isPageComplete() {
		return taskData != null && projectCombo!=null && projectCombo.getSelectionIndex()!=0;
	}

	private void updateAttributesFromRepository() {
		
		MantisRepositoryConnector connector = (MantisRepositoryConnector) TasksUiPlugin.getRepositoryManager()
				.getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);
		final IMantisClient client;
		try {
			client = connector.getClientManager().getRepository(taskRepository);
		} catch (MalformedURLException e) {
			MantisUIPlugin.handleMantisException(e);
			return;
		}

		if (!client.hasAttributes()) {
			try {
				IRunnableWithProgress runnable = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							client.updateAttributes(monitor, true);
						} catch (MantisException e) {
							throw new InvocationTargetException(e);
						}
					}
				};

				getContainer().run(true, true, runnable);
			} catch (InvocationTargetException e) {
				MantisUIPlugin.handleMantisException(e.getCause());
				return;
			} catch (InterruptedException e) {
				return;
			}
		}

		MantisTaskDataHandler offlineHandler = (MantisTaskDataHandler) connector.getTaskDataHandler();
		AbstractAttributeFactory attributeFactory = offlineHandler.getAttributeFactory(taskRepository.getUrl(), taskRepository.getConnectorKind(), AbstractTask.DEFAULT_TASK_KIND);
		this.taskData = new RepositoryTaskData(attributeFactory, 
				                               MantisCorePlugin.REPOSITORY_KIND,
				                               taskRepository.getUrl(),
				                               TasksUiPlugin.getDefault().getNextNewRepositoryTaskId(), 
				                               AbstractTask.DEFAULT_TASK_KIND);
		
		this.taskData.setNew(true);
		
		//default setting
		taskData.setAttributeValue(Key.PROJECT.getKey(), projectCombo.getText());
		
		try {
			MantisTaskDataHandler.createDefaultAttributes(taskData.getAttributeFactory(), taskData, client, false);
		} catch (CoreException e) {
			MantisUIPlugin.handleMantisException(e.getCause());
		}
	}

	public RepositoryTaskData getRepositoryTaskData() {
		return taskData;
	}

}
