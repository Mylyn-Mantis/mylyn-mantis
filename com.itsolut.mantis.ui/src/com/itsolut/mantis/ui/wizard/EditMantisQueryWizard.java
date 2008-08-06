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

import org.eclipse.mylyn.tasks.core.AbstractRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractEditQueryWizard;

/**
 * @author Steffen Pingel
 */
public class EditMantisQueryWizard extends AbstractEditQueryWizard {

//	private MantisCustomQueryPage queryPage;

	public EditMantisQueryWizard(TaskRepository repository, AbstractRepositoryQuery query) {
		super(repository, query);
	}

	@Override
	public void addPages() {
		page = new MantisCustomQueryPage(repository, query);
		page.setWizard(this);
		addPage(page);
	}

	@Override
	public boolean canFinish() {
		if (page.getNextPage() == null) {
			return page.isPageComplete();
		}
		return page.getNextPage().isPageComplete();
	}

//	@Override
//	public boolean performFinish() {
//		AbstractRepositoryQuery q = queryPage.getQuery();
//		if (q != null) {
//			TasksUiPlugin.getTaskListManager().getTaskList().deleteQuery(query);
//			TasksUiPlugin.getTaskListManager().getTaskList().addQuery(q);
//
//			AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
//					repository.getKind());
//			if (connector != null) {
//				TasksUiPlugin.getSynchronizationManager().synchronize(connector, q, null);
//			}
//		}
//
//		return true;
//	}

}