/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.ui.action;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonFormUtil;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskAttachment;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.Section;

import com.itsolut.mantis.core.IMantisClientManager;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.StatusFactory;
import com.itsolut.mantis.core.exception.MantisException;

/**
 * @author Robert Munteanu
 * 
 */
public class DeleteAttachmentJob extends Job {

    private final List<ITaskAttachment> attachments;
    private final TaskEditor editor;

    public DeleteAttachmentJob(List<ITaskAttachment> attachments, TaskEditor editor) {

        super("Deleting attachments.");
        this.attachments = attachments;
        this.editor = editor;
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {

        MantisRepositoryConnector connector = (MantisRepositoryConnector) TasksUi.getRepositoryManager().getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);

        IMantisClientManager clientManager = connector.getClientManager();

        monitor.beginTask("Deleting attachments", attachments.size() + 1);

        try {
            ITask task = null;

            for (ITaskAttachment attachment : attachments) {

                int attachmentId = Integer.parseInt(attachment.getTaskAttribute().getValue());
                task = attachment.getTask();

                try {
                    clientManager.getRepository(attachment.getTaskRepository()).deleteAttachment(attachmentId, monitor);
                } catch (MantisException e) {
                    return new StatusFactory().toStatus("Failed deleting attachment with id " + attachmentId, e, attachment.getTaskRepository());
                }

                monitor.worked(1);
            }

            // code adapted from BugzillaUpdateAttachmentAction
            TasksUiInternal.synchronizeTask(connector, task, true, new JobChangeAdapter() {

                @Override
                public void done(IJobChangeEvent event) {

                    PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

                        public void run() {

                            try {
                                if (editor == null)
                                    return;

                                editor.refreshPages();
                                editor.getEditorSite().getPage().activate(editor);
                                IFormPage formPage = editor.getActivePageInstance();
                                if (formPage instanceof AbstractTaskEditorPage) {
                                    AbstractTaskEditorPage taskEditorPage = (AbstractTaskEditorPage) formPage;
                                    Control control = taskEditorPage.getPart(AbstractTaskEditorPage.ID_PART_ATTACHMENTS).getControl();
                                    if (control instanceof Section) {
                                        Section section = (Section) control;
                                        CommonFormUtil.setExpanded(section, true);
                                    }
                                }
                            } finally {
                                if (editor != null) {
                                    editor.showBusy(false);
                                }
                            }
                        }
                    });
                }
            });

            // code adapted from BugzillaUpdateAttachmentAction
            monitor.worked(1);
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    if (editor != null) {
                        editor.showBusy(true);
                    }
                }
            });

            return Status.OK_STATUS;

        } catch (OperationCanceledException e) {

            return Status.CANCEL_STATUS;

        } finally {
            monitor.done();
        }

    }

}
