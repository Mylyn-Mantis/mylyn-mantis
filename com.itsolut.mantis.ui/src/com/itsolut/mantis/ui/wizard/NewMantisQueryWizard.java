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

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;

/**
 * @author Steffen Pingel
 * @author David Carver
 */
public class NewMantisQueryWizard extends Wizard {

	private static final String TITLE = "New Mantis Query";

	private final TaskRepository repository;

	private MantisCustomQueryPage queryPage;

	public NewMantisQueryWizard(TaskRepository repository) {
		this.repository = repository;

		setNeedsProgressMonitor(true);
		setWindowTitle(TITLE);
		setDefaultPageImageDescriptor(TasksUiImages.BANNER_REPOSITORY);
	}

	@Override
	public void addPages() {
		queryPage = new MantisCustomQueryPage(repository);
		queryPage.setWizard(this);
		addPage(queryPage);
	}

	@Override
	public boolean canFinish() {
		return queryPage.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		RepositoryQuery query = queryPage.getQuery();
		if (query != null) {
			TasksUiPlugin.getTaskList().addQuery(query);
			AbstractRepositoryConnector connector = (AbstractRepositoryConnector) TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
					repository.getConnectorKind());
			if (connector != null) {
				TasksUiPlugin.getSynchronizationScheduler().synchronize(repository);
			}
		}
		return true;
	}

}