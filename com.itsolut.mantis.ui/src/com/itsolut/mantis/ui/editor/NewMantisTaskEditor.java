/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

package com.itsolut.mantis.ui.editor;

import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.AbstractTaskContainer;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.editors.AbstractNewRepositoryTaskEditor;
import org.eclipse.mylar.tasks.ui.search.SearchHitCollector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.MantisRepositoryQuery;
import com.itsolut.mantis.core.MantisTask;
import com.itsolut.mantis.core.MantisAttributeFactory.Attribute;
import com.itsolut.mantis.core.exception.InvalidTicketException;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisSearchFilter;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisSearchFilter.CompareOperator;
import com.itsolut.mantis.ui.MantisUIPlugin;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class NewMantisTaskEditor extends AbstractNewRepositoryTaskEditor {

	private static final String SUBMIT_JOB_LABEL = "Submitting to Mantis repository";

	protected static final String LABEL_SECTION_STEPS = "Steps To Reproduce";
	protected TextViewer stepsToReproduceViewer;

	protected static final String LABEL_SECTION_ADDITIONAL = "Additional Information";
	protected TextViewer additionalViewer;

	public NewMantisTaskEditor(FormEditor editor) {
		super(editor);
	}

	@Override
	public void submitToRepository() {
		if (!prepareSubmit()) {
			return;
		}

		final MantisRepositoryConnector connector = (MantisRepositoryConnector) TasksUiPlugin.getRepositoryManager()
				.getRepositoryConnector(repository.getKind());

		updateTask();

		final MantisTicket ticket;
		try {
			ticket = MantisRepositoryConnector.getMantisTicket(repository, getRepositoryTaskData());
		} catch (InvalidTicketException e) {
			MantisUIPlugin.handleMantisException(e);
			submitButton.setEnabled(true);
			showBusy(false);
			return;
		}
		
		//TODO: let user select
		final AbstractTaskContainer category = getCategory();
//		final AbstractTaskContainer category = TasksUiPlugin.getTaskListManager().getTaskList().getRootCategory();
		Job submitJob = new Job(SUBMIT_JOB_LABEL) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IMantisClient server = connector.getClientManager().getRepository(repository);
					int id = server.createTicket(ticket);

					MantisTask newTask = new MantisTask(AbstractRepositoryTask.getHandle(repository.getUrl(), id),
							MantisRepositoryConnector.getTicketDescription(ticket), true);
					
					if (category != null) {
						TasksUiPlugin.getTaskListManager().getTaskList().addTask(newTask, category);
					} else {
						TasksUiPlugin.getTaskListManager().getTaskList().addTask(newTask);
					}

					TasksUiPlugin.getSynchronizationScheduler().synchNow(0, Collections.singletonList(repository));

					return Status.OK_STATUS;
				} catch (Exception e) {
					return MantisCorePlugin.toStatus(e);
				}
			}
		};

		submitJob.addJobChangeListener(getSubmitJobListener());
		submitJob.schedule();
	}

	@Override
	public SearchHitCollector getDuplicateSearchCollector(String searchString) {
		MantisSearchFilter filter = new MantisSearchFilter("description");
		filter.setOperator(CompareOperator.CONTAINS);
		filter.addValue(searchString);

		MantisSearch search = new MantisSearch();
		search.addFilter(filter);

		// TODO copied from MantisCustomQueryPage.getQueryUrl()
		StringBuilder sb = new StringBuilder();
		sb.append(repository.getUrl());
		sb.append(IMantisClient.QUERY_URL);
		sb.append(search.toUrl());

		MantisRepositoryQuery query = new MantisRepositoryQuery(repository.getUrl(), sb.toString(), "<Duplicate Search>",
				TasksUiPlugin.getTaskListManager().getTaskList());

		SearchHitCollector collector = new SearchHitCollector(TasksUiPlugin.getTaskListManager().getTaskList(),
				repository, query);
		return collector;
	}

	@Override
	protected void handleSubmitError(final IJobChangeEvent event) {
		MantisUIPlugin.handleMantisException(event.getJob().getResult());
		
		if (!isDisposed() && !submitButton.isDisposed()) {
			submitButton.setEnabled(true);
			showBusy(false);
		}
	}

	@Override
	protected String getPluginId() {
		return MantisUIPlugin.PLUGIN_ID;
	}

	@Override
	protected void createAttachmentLayout(Composite composite) {
		FormToolkit toolkit = this.getManagedForm().getToolkit();
		Section section = createSection(composite, LABEL_SECTION_STEPS);
		section.setExpanded(false);

		Composite stepsToReproduceComposite = toolkit.createComposite(section);
		GridLayout descriptionLayout = new GridLayout();

		stepsToReproduceComposite.setLayout(descriptionLayout);
		GridData stepsToReproduceData = new GridData(GridData.FILL_BOTH);
		stepsToReproduceData.grabExcessVerticalSpace = true;
		stepsToReproduceComposite.setLayoutData(stepsToReproduceData);
		section.setClient(stepsToReproduceComposite);

		stepsToReproduceViewer = addTextEditor(repository, stepsToReproduceComposite,
				getRepositoryTaskData().getAttributeValue(Attribute.STEPS_TO_REPRODUCE.getMantisKey()), true, SWT.FLAT | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		stepsToReproduceViewer.setEditable(true);

		GridData descriptionTextData = new GridData(GridData.FILL_BOTH);
		stepsToReproduceViewer.getControl().setLayoutData(descriptionTextData);
		stepsToReproduceViewer.getControl().setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		stepsToReproduceViewer.getTextWidget().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String changed = stepsToReproduceViewer.getTextWidget().getText();
				String original = getRepositoryTaskData().getAttributeValue(Attribute.STEPS_TO_REPRODUCE.getMantisKey());
				if (original==null || !(original.equals(changed))) {
					getRepositoryTaskData().getAttribute(Attribute.STEPS_TO_REPRODUCE.getMantisKey()).setValue(changed);
					markDirty(true);
				}
				validateInput();
			}
		});

		toolkit.paintBordersFor(stepsToReproduceComposite);
		
		////////////////
		Section sectionAddtional = createSection(composite, LABEL_SECTION_ADDITIONAL);
		sectionAddtional.setExpanded(false);

		Composite additionalComposite = toolkit.createComposite(sectionAddtional);
		GridLayout additionalLayout = new GridLayout();

		additionalComposite.setLayout(additionalLayout);
		GridData additionalData = new GridData(GridData.FILL_BOTH);
		additionalData.grabExcessVerticalSpace = true;
		additionalComposite.setLayoutData(additionalData);
		sectionAddtional.setClient(additionalComposite);

		additionalViewer = addTextEditor(repository, additionalComposite,
				getRepositoryTaskData().getAttributeValue(Attribute.ADDITIONAL_INFO.getMantisKey()), true, SWT.FLAT | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		additionalViewer.setEditable(true);

		GridData additionalTextData = new GridData(GridData.FILL_BOTH);
		additionalViewer.getControl().setLayoutData(additionalTextData);
		additionalViewer.getControl().setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		additionalViewer.getTextWidget().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String changed = additionalViewer.getTextWidget().getText();
				String original = getRepositoryTaskData().getAttributeValue(Attribute.ADDITIONAL_INFO.getMantisKey());
				if (original==null || !(original.equals(changed))) {
					getRepositoryTaskData().getAttribute(Attribute.ADDITIONAL_INFO.getMantisKey()).setValue(changed);
					markDirty(true);
				}
				validateInput();
			}
		});

		toolkit.paintBordersFor(additionalComposite);
		///////////////////
		
		super.createAttachmentLayout(composite);
	}
}
