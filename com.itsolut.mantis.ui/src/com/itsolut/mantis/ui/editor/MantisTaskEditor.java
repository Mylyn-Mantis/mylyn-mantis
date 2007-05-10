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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.mylar.context.core.MylarStatusHandler;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.mylar.tasks.ui.editors.AbstractRepositoryTaskEditor;
import org.eclipse.mylar.tasks.ui.editors.AbstractTaskEditorInput;
import org.eclipse.mylar.tasks.ui.editors.RepositoryTaskEditorInput;
import org.eclipse.mylar.tasks.ui.editors.RepositoryTaskOutlineNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.IProgressService;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.MantisAttributeFactory.Attribute;
import com.itsolut.mantis.core.exception.InvalidTicketException;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.ui.MantisUIPlugin;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisTaskEditor extends AbstractRepositoryTaskEditor {

	private static final String SUBMIT_JOB_LABEL = "Submitting to Mantis Repository";

	protected static final String LABEL_SECTION_STEPS = "Steps To Reproduce";
	protected TextViewer stepsToReproduceViewer;

	protected static final String LABEL_SECTION_ADDITIONAL = "Additional Information";
	protected TextViewer additionalViewer;

	public MantisTaskEditor(FormEditor editor) {
		super(editor);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) {
		if (!(input instanceof RepositoryTaskEditorInput))
			return;

		editorInput = (AbstractTaskEditorInput) input;
		repository = editorInput.getRepository();
		connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
				repository.getKind());

		setSite(site);
		setInput(input);

		taskOutlineModel = RepositoryTaskOutlineNode.parseBugReport(editorInput.getTaskData());

		isDirty = false;
		updateEditorTitle();
	}

	@Override
	public void submitToRepository() {
		if (isDirty()) {
			this.doSave(new NullProgressMonitor());
		}
		updateTask();
		submitButton.setEnabled(false);
		showBusy(true);

		final MantisTicket ticket;
		try {
			ticket = MantisRepositoryConnector.getMantisTicket(repository, getRepositoryTaskData());
		} catch (InvalidTicketException e) {
			MantisUIPlugin.handleMantisException(e);
			return;
		}
		final String comment = getNewCommentText();
		final AbstractRepositoryTask task = (AbstractRepositoryTask) TasksUiPlugin.getTaskListManager().getTaskList()
				.getTask(AbstractRepositoryTask.getHandle(repository.getUrl(), getRepositoryTaskData().getId()));
		final boolean attachContext = getAttachContext();

		JobChangeAdapter listener = new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (event.getJob().getResult().isOK()) {
							if (attachContext) {
								attachContext();
							}
							close();
						}
					}
				});
			}
		};

		Job submitJob = new Job(SUBMIT_JOB_LABEL) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IMantisClient server = ((MantisRepositoryConnector) connector).getClientManager().getRepository(repository);
					server.updateTicket(ticket, comment);
					if (task != null) {
						// XXX: HACK TO AVOID OVERWRITE WARNING
						task.setTaskData(null);
						TasksUiPlugin.getSynchronizationManager().synchronize(connector, task, true, null);
					}
					return Status.OK_STATUS;
				} catch (final Exception e) {
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						public void run() {
							if (!isDisposed() && !submitButton.isDisposed()) {
								MantisUIPlugin.handleMantisException(e);
								// MantisUiPlugin.handleMantisException(event.getResult());
								submitButton.setEnabled(true);
								MantisTaskEditor.this.showBusy(false);
							}
						}
					});
					return Status.CANCEL_STATUS;
				}
			}

		};

		submitJob.addJobChangeListener(listener);
		submitJob.schedule();

	}

	@Override
	protected void validateInput() {
	}

	private void attachContext() {
		String handle = AbstractRepositoryTask.getHandle(repository.getUrl(), getRepositoryTaskData().getId());
		final AbstractRepositoryTask modifiedTask = (AbstractRepositoryTask) TasksUiPlugin.getTaskListManager()
				.getTaskList().getTask(handle);

		IProgressService ps = PlatformUI.getWorkbench().getProgressService();
		try {
			ps.busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) {
					try {
						connector.attachContext(repository, modifiedTask, "");
					} catch (Exception e) {
						MylarStatusHandler.fail(e, "Failed to attach task context.\n\n" + e.getMessage(), true);
					}
				}
			});
		} catch (InvocationTargetException e) {
			MylarStatusHandler.fail(e.getCause(), "Failed to attach task context.\n\n" + e.getMessage(), true);
		} catch (InterruptedException ignore) {
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
