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

package com.itsolut.mantis.ui.wizard;

import static com.itsolut.mantis.ui.util.MantisUIUtil.newEnhancedFilteredTree;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylyn.commons.core.ICoreRunnable;
import org.eclipse.mylyn.commons.ui.CommonUiUtil;
import org.eclipse.mylyn.commons.workbench.EnhancedFilteredTree;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.itsolut.mantis.core.IMantisClientManager;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.ui.MantisUIPlugin;
import com.itsolut.mantis.ui.internal.MantisImages;
import com.itsolut.mantis.ui.util.MantisUIUtil;

/**
 * Wizard page selecting a repository project
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 * @author Robert Munteanu
 */
public class MantisProjectPage extends WizardPage {

    private TaskRepository taskRepository;
	private EnhancedFilteredTree tree;
    private IMantisClientManager clientManager;

    public MantisProjectPage(TaskRepository taskRepository, IMantisClientManager clientManager) {
        super("New Task");
        setTitle("Mantis - Select project");
        setDescription("Select the tickets project.");
        setImageDescriptor(MantisImages.WIZARD);

        this.taskRepository = taskRepository;
        this.clientManager = clientManager;
    }
    
    public void createControl(Composite parent) {

        Composite control = new Composite(parent, SWT.NULL);
        control.setLayout(new GridLayout());

		tree = newEnhancedFilteredTree(control);
		
		final TreeViewer projectTreeViewer = tree.getViewer();
		
		projectTreeViewer.setLabelProvider(new MantisProjectLabelProvider());
		
		projectTreeViewer.setContentProvider(new MantisProjectITreeContentProvider());
		
		projectTreeViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				if (getSelectedProject() == null) {
					setErrorMessage("Please select a project.");
				} else {
					setErrorMessage(null);
				}
				getWizard().getContainer().updateButtons();
			}

		});
		
		projectTreeViewer.setInput(getProjects());
		
		Button updateButton = new Button(control, SWT.LEFT | SWT.PUSH);
		updateButton.setText("Update attributes");
		updateButton.setLayoutData(new GridData());
		updateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
			    MantisUIUtil.updateRepositoryConfiguration(getContainer(), taskRepository, clientManager);
				projectTreeViewer.setInput(getProjects());
			}
		});
		
		setControl(tree);
    }


    private MantisProject[] getProjects() {

        final List<MantisProject> projects = new ArrayList<MantisProject>();
        
        try {
			
			CommonUiUtil.run(getContainer(), new ICoreRunnable() {
                
                public void run(IProgressMonitor monitor) throws CoreException {
            
                    try {
                        projects.addAll(clientManager.getRepository(taskRepository).getCache(monitor).getProjects());
                    } catch (MantisException e) {
                        throw new CoreException(new Status(Status.ERROR, MantisUIPlugin.PLUGIN_ID, "Failed getting projects : " + e.getMessage(), e));
                    }
                    
                }
            });
		} catch (CoreException e) {
            setMessage("Unable to load projects : " + e.getMessage()+ " .", DialogPage.ERROR);
        }
		
		return projects.toArray(new MantisProject[0]);
	}
    
	@Override
    public boolean isPageComplete() {
		return getSelectedProject() != null;
    }

    public MantisProject getSelectedProject() {
		IStructuredSelection selection = (IStructuredSelection) tree.getViewer().getSelection();
		return (MantisProject) selection.getFirstElement();
    }

}
