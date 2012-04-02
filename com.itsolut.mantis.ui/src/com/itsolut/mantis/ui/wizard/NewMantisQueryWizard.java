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
import org.eclipse.mylyn.tasks.ui.wizards.RepositoryQueryWizard;

import com.itsolut.mantis.core.IMantisClientManager;

/**
 * @author Steffen Pingel
 * @author David Carver
 */
public class NewMantisQueryWizard extends RepositoryQueryWizard {

	private final IRepositoryQuery query;
	
	public NewMantisQueryWizard(TaskRepository repository, IRepositoryQuery queryToEdit, IMantisClientManager clientManager) {
		super(repository);
		this.query = queryToEdit;
        
		setWindowTitle(query == null ? "Create Mantis Query" : "Edit Mantis Query");
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(TasksUiImages.BANNER_REPOSITORY);
		
		MantisCustomQueryPage queryPage = query != null ? new MantisCustomQueryPage(repository, query, clientManager) : new MantisCustomQueryPage(repository, clientManager); 
	    
		addPage(queryPage);
		
	}

	public NewMantisQueryWizard(TaskRepository repository, IMantisClientManager clientManager) {
	    this(repository, null, clientManager);
	}
}