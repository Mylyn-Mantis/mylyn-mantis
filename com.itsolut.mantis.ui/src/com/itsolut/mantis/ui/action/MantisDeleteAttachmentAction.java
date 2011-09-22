/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.ui.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylyn.tasks.core.ITaskAttachment;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;


/**
 * @author Robert Munteanu
 *
 */
public class MantisDeleteAttachmentAction extends BaseSelectionListenerAction implements IActionDelegate {

    private List<ITaskAttachment> attachments = new ArrayList<ITaskAttachment>();

    public MantisDeleteAttachmentAction() {

        super("DeleteAttachmentAction");
    }

    public void run(IAction action) {

        String message = "";
        
        for ( ITaskAttachment attachment : attachments )
            message += " - " + attachment.getFileName() + " \n";
        
        boolean confirm = MessageDialog.openConfirm(null, "Attachment deletion", "Please confirm that the following attachments will be deleted:\n\n" + message);
        if ( !confirm )
            return;
        
        TaskEditor taskEditor = null;
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        IEditorPart activeEditor = page.getActiveEditor();
        if (activeEditor instanceof TaskEditor) 
            taskEditor = (TaskEditor) activeEditor;
        
        final DeleteAttachmentJob job = new DeleteAttachmentJob(attachments, taskEditor);
        job.setUser(true);
        job.schedule();
    }

    public void selectionChanged(IAction action, ISelection selection) {
        
        attachments.clear();
        action.setEnabled(false);

        IStructuredSelection structuredSelection = null;
        
        if (selection instanceof IStructuredSelection)
            structuredSelection = (IStructuredSelection) selection;
        
        if (structuredSelection == null || structuredSelection.isEmpty())
            return;
        
        for ( Object selectionItem : structuredSelection.toList() ) {
            if ( selectionItem instanceof ITaskAttachment ) {
                attachments.add((ITaskAttachment) selectionItem);
            }
        }
        
        action.setEnabled(attachments.size() > 0);
    }
}
