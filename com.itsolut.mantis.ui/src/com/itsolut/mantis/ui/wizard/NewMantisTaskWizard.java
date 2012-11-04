/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package com.itsolut.mantis.ui.wizard;

import static com.itsolut.mantis.core.MantisAttributeMapper.Attribute.PROJECT;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylyn.tasks.core.*;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.ui.wizards.NewTaskWizard;
import org.eclipse.ui.INewWizard;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.IMantisClientManager;
import com.itsolut.mantis.core.MantisCacheData;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.ui.util.MantisUIUtil;

/**
 * Wizard for creating new Mantis tickets through a rich editor..
 * 
 * @author Steffen Pingel
 */
public class NewMantisTaskWizard extends NewTaskWizard implements INewWizard {

    private final TaskRepository taskRepository;

    private MantisProjectPage newTaskPage;

    private IMantisClientManager clientManager;

    private ProductOnlyTaskMapping mapping;

    public NewMantisTaskWizard(TaskRepository taskRepository,
			ITaskMapping taskSelection, IMantisClientManager clientManager) {
		super(taskRepository, taskSelection);
		
		this.taskRepository = taskRepository;
        this.clientManager = clientManager;

		
		setWindowTitle("New Repository Task");
		setDefaultPageImageDescriptor(TasksUiImages.BANNER_REPOSITORY);
		
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
	    
	    IStructuredSelection selection = MantisUIUtil.getCurrentSelection();
         
         if ( selection != null && ! selection.isEmpty() ) {
             
             Object selectedElement = selection.getFirstElement();
             String projectName = null;
             if ( selectedElement instanceof ITask ) {
                ITask task = (ITask) selectedElement;
                projectName = task.getAttribute(PROJECT.getKey());
             } else if ( selectedElement instanceof IRepositoryQuery) {
                 IRepositoryQuery query = (IRepositoryQuery) selectedElement;
                 projectName = query.getAttribute(IMantisClient.PROJECT_NAME);
             } else if (selectedElement instanceof IAdaptable) {
                 IAdaptable adaptable = (IAdaptable) selectedElement;
                 ITask task = (ITask) adaptable.getAdapter(ITask.class);
                 if (task != null)
                    projectName = task.getAttribute(PROJECT.getKey());
             }
             
             if ( projectName != null && ! MantisProject.ALL_PROJECTS.getName().equals(projectName) ) {
            	 
				try {
					MantisCacheData cacheData = clientManager.getRepository( taskRepository).getCacheData();
					for (MantisProject project : cacheData.getProjects()) {
						if (project.getName().equals(projectName)) {
							mapping = new ProductOnlyTaskMapping(projectName);
							return;
						}
					}
				} catch (MantisException e) {
					// ignore the attempt and fall back to using a project page
				}
             }
         }
         
		newTaskPage = new MantisProjectPage(taskRepository, clientManager);
		addPage(newTaskPage);
	}
	
	@Override
	public ITaskMapping getTaskSelection() {
	
	    ITaskMapping selection = super.getTaskSelection();
	    if ( selection == null )
	        selection = mapping;
	    
	    return selection;
	}
	

	@Override
	protected ITaskMapping getInitializationData() {
	    
       if (getTaskSelection() != null && getTaskSelection().getProduct() != null)
           return getTaskSelection();
	    
		final MantisProject project = newTaskPage.getSelectedProject();
		
		return new ProductOnlyTaskMapping(project.getName());
	}
	
	private static class ProductOnlyTaskMapping extends TaskMapping {
	    
	    private final String product;

        public ProductOnlyTaskMapping(String product) {

            this.product = product;
        }
        
        public String getProduct() {

            return product;
        }
        
        @Override
        public String toString() {
        
            return getClass().getSimpleName()+"{ product: " + product + "}";
        }
	}
}
