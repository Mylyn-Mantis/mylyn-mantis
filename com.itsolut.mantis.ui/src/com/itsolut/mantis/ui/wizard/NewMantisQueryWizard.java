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

import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.mylyn.tasks.ui.wizards.RepositoryQueryWizard;

/**
 * @author Steffen Pingel
 * @author David Carver
 */
public class NewMantisQueryWizard extends RepositoryQueryWizard {

	private static final String TITLE = "New Mantis Query";

	private final TaskRepository repository;
	private IRepositoryQuery query;

	
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
	    
	    AbstractRepositoryQueryPage queryPage = query != null ? new MantisCustomQueryPage(repository, query) : new MantisCustomQueryPage(repository); 
	    
		queryPage.setWizard(this);
		addPage(queryPage);
	}
    
}