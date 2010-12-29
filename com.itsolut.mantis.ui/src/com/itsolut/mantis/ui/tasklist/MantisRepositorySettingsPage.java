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

package com.itsolut.mantis.ui.tasklist;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.RepositoryTemplate;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisClientFactory;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConfiguration;
import com.itsolut.mantis.core.RepositoryCapability;
import com.itsolut.mantis.core.RepositoryValidationResult;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.ui.MantisUIPlugin;
import com.itsolut.mantis.ui.internal.WikiLinkedErrorDialog;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 * @author David Carver / d_a_carver@yahoo.com - updated sample url.
 */
public class MantisRepositorySettingsPage extends AbstractRepositorySettingsPage {

    private static final String TITLE = "Mantis Repository Settings";

    private static final String DESCRIPTION = "Example: http://www.example.com/mantis/\nDo not enter the path to the SOAP API.";

    private Button retrieveSubTasksButton;

    public MantisRepositorySettingsPage(String title, String description, TaskRepository taskRepository) {

        super(TITLE, DESCRIPTION, taskRepository);
        setNeedsAnonymousLogin(true);
        setNeedsEncoding(false);
        setNeedsTimeZone(false);
        setNeedsValidation(true);
        setNeedsAdvanced(true);
        setNeedsHttpAuth(true);
    }

    @Override
    protected void createAdditionalControls(final Composite parent) {

        addRepositoryTemplatesToServerUrlCombo();

        Label downloadAttachmentsLabel = new Label(parent, SWT.NONE);
        downloadAttachmentsLabel.setText("Group sub-tasks");
        retrieveSubTasksButton = new Button(parent, SWT.CHECK | SWT.LEFT);
        retrieveSubTasksButton.setText("Enabled");
        if (repository != null)
            retrieveSubTasksButton.setSelection(MantisRepositoryConfiguration.isDownloadSubTasks(repository));
        else
            retrieveSubTasksButton.setSelection(true);

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
    protected Validator getValidator(TaskRepository repository) {

        return new MantisValidator(repository);
    }

    @Override
    public void applyTo(TaskRepository repository) {

        super.applyTo(repository);

        MantisRepositoryConfiguration.setDownloadSubTasks(repository, retrieveSubTasksButton.getSelection());
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
    
    public class MantisValidator extends Validator {

        private final String repositoryUrl;

        private final TaskRepository taskRepository;

        public MantisValidator(TaskRepository taskRepository) {

            this.repositoryUrl = taskRepository.getRepositoryUrl();
            this.taskRepository = taskRepository;
        }

        @Override
        public void run(IProgressMonitor monitor) throws CoreException {

            try {
                validate(monitor);
            } catch (MantisException e) {
                throw new CoreException(MantisCorePlugin.getDefault().getStatusFactory().toStatus(null, e, taskRepository));
            } catch (MalformedURLException e) {
                throw new CoreException(MantisCorePlugin.getDefault().getStatusFactory().toStatus(null, e, taskRepository));
            }
        }

        public void validate(IProgressMonitor monitor) throws MalformedURLException, MantisException {

            AbstractWebLocation location = new TaskRepositoryLocationFactory().createWebLocation(taskRepository);

            IMantisClient client = MantisClientFactory.getDefault().createClient(location);
            RepositoryValidationResult validate = client.validate(monitor);
            if ( !validate.getVersion().getMissingCapabilities().isEmpty() ) {
            	setStatusFromMissingCapabilities(validate);
            	return;
            	
            }
            setStatus(RepositoryStatus.createStatus(repositoryUrl, IStatus.INFO, MantisUIPlugin.PLUGIN_ID,
                    "Authentication credentials are valid."));
        }

		private void setStatusFromMissingCapabilities( RepositoryValidationResult validate) {
			
			StringBuilder message = new StringBuilder();
			message.append("You are using version ").append(validate.getVersion().getDescription()).append(" which has known problems : ");
			for ( RepositoryCapability capability : validate.getVersion().getMissingCapabilities() )
				message.append(capability.getDescriptionForMissingCapability()).append(" ,");
			message.deleteCharAt(message.length() -1);
			message.append(". Please consider upgrading to the latest stable version.");
			
			setStatus(RepositoryStatus.createStatus(repositoryUrl, WARNING, MantisUIPlugin.PLUGIN_ID, message.toString()));
		}
    }

}