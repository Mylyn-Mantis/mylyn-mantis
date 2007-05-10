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

package com.itsolut.mantis.ui;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylar.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylar.tasks.ui.OpenRepositoryTaskJob;
import org.eclipse.mylar.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.ui.wizard.EditMantisQueryWizard;
import com.itsolut.mantis.ui.wizard.MantisCustomQueryPage;
import com.itsolut.mantis.ui.wizard.MantisRepositorySettingsPage;
import com.itsolut.mantis.ui.wizard.NewMantisTaskWizard;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisRepositoryUi extends AbstractRepositoryConnectorUi {

	@Override
	public AbstractRepositorySettingsPage getSettingsPage() {
		return new MantisRepositorySettingsPage(this);
	}
	
	@Override
	public boolean hasRichEditor() {
		return true;
	}

	@Override
	public WizardPage getSearchPage(TaskRepository repository, IStructuredSelection selection) {
		return new MantisCustomQueryPage(repository);
	}

	@Override
	public boolean hasSearchPage() {
		return true;
	}
	
	@Override
	public IWizard getNewTaskWizard(TaskRepository repository) {
		return new NewMantisTaskWizard(repository);
	}
	
	@Override
	public IWizard getQueryWizard(TaskRepository repository, AbstractRepositoryQuery query) {
		return new EditMantisQueryWizard(repository, query);
	}

	@Override
	public String getRepositoryType() {
		return MantisCorePlugin.REPOSITORY_KIND;
	}

	@Override
	public boolean openRemoteTask(String repositoryUrl, String idString) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		String ticketUrl = repositoryUrl + IMantisClient.TICKET_URL + idString;
		OpenRepositoryTaskJob job = new OpenRepositoryTaskJob(MantisCorePlugin.REPOSITORY_KIND, repositoryUrl, idString, ticketUrl, page);
		job.schedule();
		return true;
	}
	
}
