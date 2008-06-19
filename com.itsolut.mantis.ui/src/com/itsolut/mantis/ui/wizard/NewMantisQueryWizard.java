/*******************************************************************************
 * Copyright (c) 2008 - Standards for Technology in Automotive Retail and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Carver - Initial API and implementation.
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/

package com.itsolut.mantis.ui.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.mylyn.tasks.ui.wizards.RepositoryQueryWizard;

import com.itsolut.mantis.ui.MantisUIPlugin;

/**
 * @author Steffen Pingel
 * @author David Carver
 */
public class NewMantisQueryWizard extends RepositoryQueryWizard {

	private static final String TITLE = "New Mantis Query";

	private final TaskRepository repository;
	private IRepositoryQuery query;

	private MantisCustomQueryPage queryPage;
	
	public NewMantisQueryWizard(TaskRepository repository, IRepositoryQuery queryToEdit) {
		super(repository);
		this.repository = repository;
		this.query = queryToEdit;
		setWindowTitle("Edit Mantis Query");
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(TasksUiImages.BANNER_REPOSITORY);
		
	}

	public NewMantisQueryWizard(TaskRepository repository) {
		super(repository);
		this.repository = repository;

		setNeedsProgressMonitor(true);
		setWindowTitle(TITLE);
		setDefaultPageImageDescriptor(TasksUiImages.BANNER_REPOSITORY);
	}

	@Override
	public void addPages() {
		if (query != null) {
			queryPage = new MantisCustomQueryPage(repository, query);
		} else {
			queryPage = new MantisCustomQueryPage(repository);
		}
		queryPage.setWizard(this);
		addPage(queryPage);
	}

	@Override
	public boolean canFinish() {
		return queryPage.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		IWizardPage currentPage = queryPage; //getContainer().getCurrentPage();
		if (!(currentPage instanceof AbstractRepositoryQueryPage)) {
			StatusHandler.fail(new Status(IStatus.ERROR, MantisUIPlugin.PLUGIN_ID,
					"Current wizard page does not extends AbstractRepositoryQueryPage"));
			return false;
		}

		AbstractRepositoryQueryPage page = (AbstractRepositoryQueryPage) currentPage;
		IRepositoryQuery query = page.getQuery();
		if (query != null) {
			page.applyTo(query);
			TasksUiPlugin.getTaskList().notifyElementChanged((RepositoryQuery) query);
		} else {
			query = page.createQuery();
			TasksUiInternal.getTaskList().addQuery((RepositoryQuery) query);
		}
		AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(
				getTaskRepository().getConnectorKind());
		TasksUiInternal.synchronizeQuery(connector, (RepositoryQuery) query, null, true);
		return true;
	}
}