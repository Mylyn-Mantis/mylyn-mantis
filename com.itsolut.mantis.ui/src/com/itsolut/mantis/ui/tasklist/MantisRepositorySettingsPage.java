/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *     Robert Munteanu - various changes
 *******************************************************************************/

package com.itsolut.mantis.ui.tasklist;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.RepositoryTemplate;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.swt.widgets.Composite;

import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConfiguration;
import com.itsolut.mantis.ui.internal.WikiLinkedErrorDialog;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 * @author David Carver / d_a_carver@yahoo.com - updated sample url.
 */
public class MantisRepositorySettingsPage extends AbstractRepositorySettingsPage {

    private static final String TITLE = "Mantis Repository Settings";

    private static final String DESCRIPTION = "Enter the path to your repository, for example : http://www.example.com/mantis/\nPlease validate the settings to ensure they are correct.";

    public MantisRepositorySettingsPage(TaskRepository taskRepository) {

        super(TITLE, DESCRIPTION, taskRepository);
        
        setNeedsAnonymousLogin(true); 
        setNeedsValidation(true);
        setNeedsHttpAuth(true);
        
        setNeedsEncoding(false);
        setNeedsTimeZone(false);
        setNeedsAdvanced(false);
    }
    
    @Override
    protected void createSettingControls(Composite parent) {

        super.createSettingControls(parent);
        addRepositoryTemplatesToServerUrlCombo();
    }

    @Override
    protected void createAdditionalControls(final Composite parent) {
    }

    @Override
    protected void repositoryTemplateSelected(RepositoryTemplate template) {

        repositoryLabelEditor.setStringValue(template.label);

        setUrl(template.repositoryUrl);
        setAnonymous(template.anonymous);
        getContainer().updateButtons();
    }

    @Override
    protected boolean isValidUrl(String name) {

        if ((name.startsWith(URL_PREFIX_HTTPS) || name.startsWith(URL_PREFIX_HTTP)) && !name.endsWith("/")) {
            try {
                new URL(name);
                return true;
            } catch (MalformedURLException e) {
            }
        }
        return false;
    }

    @Override
    public void applyTo(TaskRepository repository) {

        super.applyTo(repository);

        MantisRepositoryConfiguration.setCategoryIfNotSet(repository);
    }

    @Override
    public String getConnectorKind() {

        return MantisCorePlugin.REPOSITORY_KIND;
    }
    
    @Override
    protected void applyValidatorResult(Validator validator) {
    
        super.applyValidatorResult(validator);
        
        if ( validator.getStatus().getSeverity() != IStatus.ERROR)
            return;
        
        if ( validator.getStatus() instanceof RepositoryStatus ) {
            
            RepositoryStatus status = (RepositoryStatus) validator.getStatus();
            
            if ( status.getCode() != RepositoryStatus.ERROR_INTERNAL )
                return;
        }
        
        new WikiLinkedErrorDialog(getShell(), "Unexpected repository error", "The repository has returned an unknown error. Most likely there is an error in the repository configuration.", validator.getStatus()).open();
    }
}