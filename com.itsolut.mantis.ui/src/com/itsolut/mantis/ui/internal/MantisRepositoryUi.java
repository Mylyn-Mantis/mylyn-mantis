/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2007, 2008 - IT Solutions, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *     David Carver - STAR - Mylyn 3.0 migration.
 *******************************************************************************/

package com.itsolut.mantis.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskComment;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentModel;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.LegendElement;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskRepositoryPage;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskSearchPage;
import org.eclipse.mylyn.tasks.ui.wizards.TaskAttachmentPage;
import org.eclipse.osgi.util.NLS;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.itsolut.mantis.core.IMantisClientManager;
import com.itsolut.mantis.core.MantisClientFactory;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryLocations;
import com.itsolut.mantis.core.SourceForgeConstants;
import com.itsolut.mantis.core.StatusFactory;
import com.itsolut.mantis.ui.MantisUIPlugin;
import com.itsolut.mantis.ui.tasklist.MantisRepositorySettingsPage;
import com.itsolut.mantis.ui.wizard.MantisCustomQueryPage;
import com.itsolut.mantis.ui.wizard.NewMantisQueryWizard;
import com.itsolut.mantis.ui.wizard.NewMantisTaskWizard;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 * @author Chris Hane
 * @author dcarver
 */
public class MantisRepositoryUi extends AbstractRepositoryConnectorUi {
    
    @Inject
    private StatusFactory statusFactory;

    @Inject
    private IMantisClientManager clientManager;

    @Inject
    private MantisClientFactory clientFactory;
    
    @Inject
    private MantisHyperlinkFinder hyperlinkFinder;
    
    public MantisRepositoryUi() {
        
        Injector injector = Guice.createInjector(new MantisUiPluginModule());
        
        injector.injectMembers(MantisUIPlugin.getDefault());
        injector.injectMembers(this);
    }

    @Override
    public String getConnectorKind() {

        return MantisCorePlugin.REPOSITORY_KIND;
    }

    @Override
    public IWizard getNewTaskWizard(TaskRepository repository, ITaskMapping selection) {

        return new NewMantisTaskWizard(repository, selection, clientManager);
    }

    @Override
    public IWizard getQueryWizard(TaskRepository repository, IRepositoryQuery queryToEdit) {

        if (queryToEdit != null) {
            return new NewMantisQueryWizard(repository, queryToEdit, clientManager);
        }

        return new NewMantisQueryWizard(repository, clientManager);
    }

    @Override
    public ITaskRepositoryPage getSettingsPage(TaskRepository taskRepository) {

        return new MantisRepositorySettingsPage(taskRepository, statusFactory, clientFactory);
    }
    
    @Override
    public IWizardPage getTaskAttachmentPage(TaskAttachmentModel model) {
    
        TaskAttachmentPage taskAttachmentPage = new TaskAttachmentPage(model);
        taskAttachmentPage.setNeedsDescription(false);
        return taskAttachmentPage;
    }

    @Override
    public boolean hasSearchPage() {

        return true;
    }

    @Override
    public ITaskSearchPage getSearchPage(TaskRepository repository, IStructuredSelection selection) {

        return new MantisCustomQueryPage(repository, clientManager);
    }

    @Override
    public List<LegendElement> getLegendElements() {

        List<LegendElement> legendItems = new ArrayList<LegendElement>();
        legendItems.add(LegendElement.createTask("block", MantisImages.OVERLAY_CRITICAL));
        legendItems.add(LegendElement.createTask("major", MantisImages.OVERLAY_MAJOR));
        legendItems.add(LegendElement.createTask("feature", MantisImages.OVERLAY_ENHANCEMENT));
        legendItems.add(LegendElement.createTask("trivial", MantisImages.OVERLAY_MINOR));
        return legendItems;

    }

    @Override
    public String getAccountCreationUrl(TaskRepository taskRepository) {

        if (taskRepository.getUrl().startsWith(SourceForgeConstants.NEW_SF_NET_URL))
            return SourceForgeConstants.SIGNUP_URL;

        return MantisRepositoryLocations.create(taskRepository.getRepositoryUrl()).getSignupLocation();
    }

    @Override
    public String getAccountManagementUrl(TaskRepository taskRepository) {

        return MantisRepositoryLocations.create(taskRepository.getRepositoryUrl()).getAccountManagementLocation();
    }

    @Override
    public ImageDescriptor getTaskKindOverlay(ITask task) {

        String severity = task.getTaskKind();
        if (severity != null) {
            if ("block".equals(severity)) {
                return MantisImages.OVERLAY_CRITICAL;
            } else if ("major".equals(severity) || "crash".equals(severity)) {
                return MantisImages.OVERLAY_MAJOR;
            } else if ("feature".equals(severity)) {
                return MantisImages.OVERLAY_ENHANCEMENT;
            } else if ("trivial".equals(severity) || "minor".equals(severity)) {
                return MantisImages.OVERLAY_MINOR;
            } else {
                return null;
            }
        }
        return super.getTaskKindOverlay(task);
    }

    @Override
    public IHyperlink[] findHyperlinks(TaskRepository repository, ITask task, String text, int lineOffset, int regionOffset) {

        return hyperlinkFinder.findHyperlinks(repository, task, text, lineOffset, regionOffset);
    }

    @Override
    public String getReplyText(TaskRepository taskRepository, ITask task, ITaskComment taskComment, boolean includeTask) {

        if (taskComment == null) 
            return NLS.bind("(In reply to bug #{0})", task.getTaskId());
        
        if (includeTask)
            return NLS.bind("(In reply to bug #{0} comment ~{1})", task.getTaskId(), taskComment.getTaskAttribute().getValue());

        return NLS.bind("(In reply to comment ~{0})", taskComment.getTaskAttribute().getValue());
    }
}
