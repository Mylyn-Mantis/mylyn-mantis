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

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.MantisTaskDataHandler;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.ui.MantisUIPlugin;
import com.itsolut.mantis.ui.util.MantisUIUtil;

/**
 * Wizard page for creating new Mantis tickets through a rich editor.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class NewMantisTaskPage extends WizardPage {

    private TaskRepository taskRepository;

    public Combo projectCombo;

    public NewMantisTaskPage(TaskRepository taskRepository) {
        super("New Task");
        setTitle("Mantis - New Issue");
        setDescription("Select the tickets project.");
        setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin("com.itsolut.mantis.ui",
        "icons/wizban/mantis_logo_button.gif"));

        this.taskRepository = taskRepository;
    }
    public void createControl(Composite parent) {

        Composite control = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
        control.setLayoutData(gd);
        GridLayout layout = new GridLayout(1, false);
        control.setLayout(layout);

    	
        projectCombo = new Combo(control, SWT.READ_ONLY);
        projectCombo.add("Select Project for new Issue");
        setControl(projectCombo);

        try {
            MantisRepositoryConnector connector = (MantisRepositoryConnector)TasksUi.getRepositoryManager().getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);
            IMantisClient client = connector.getClientManager().getRepository(taskRepository);

            for(MantisProject pd : client.getProjects())
                projectCombo.add(pd.getName());
            projectCombo.setText(projectCombo.getItem(0));

            projectCombo.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getContainer().updateButtons();
                }

                public void widgetDefaultSelected(SelectionEvent e) {
                    //nothing
                }
            });
        } catch (Exception e1) {
            MantisCorePlugin.log(e1);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        updateAttributesFromRepository();
    }

    @Override
    public boolean isPageComplete() {
        return projectCombo!=null && projectCombo.getSelectionIndex()!=0;
    }

    private void updateAttributesFromRepository() {

        MantisUIUtil.updateRepositoryConfiguration(getContainer(), taskRepository, false);
    }

    public String getSelectedProject() {
        return projectCombo.getText();
    }

}
