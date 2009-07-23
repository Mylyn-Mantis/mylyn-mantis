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

import org.apache.axis.AxisFault;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.RepositoryTemplate;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.xml.sax.SAXException;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisClientFactory;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConfiguration;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.ui.MantisUIPlugin;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 * @author David Carver / d_a_carver@yahoo.com - updated sample url.
 */
public class MantisRepositorySettingsPage extends AbstractRepositorySettingsPage {
    
    private static final String TITLE = "Mantis Repository Settings";
    
    private static final String DESCRIPTION = "Example: http://mylyn-mantis.sourceforge.net/MantisTest/mc/mantisconnect.php";
    
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
    
    @Override protected void createAdditionalControls(final Composite parent) {

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
    
    @Override protected void repositoryTemplateSelected(RepositoryTemplate template) {

        repositoryLabelEditor.setStringValue(template.label);
        setUrl(template.repositoryUrl);
        setAnonymous(template.anonymous);
        
        getContainer().updateButtons();
    }
    
    @Override protected boolean isValidUrl(String name) {

        if ((name.startsWith(URL_PREFIX_HTTPS) || name.startsWith(URL_PREFIX_HTTP)) && !name.endsWith("/")) {
            try {
                new URL(name);
                return true;
            } catch (MalformedURLException e) {}
        }
        return false;
    }
    
    @Override protected Validator getValidator(TaskRepository repository) {

        return new MantisValidator(repository, getUserName(), getPassword(), getHttpAuthUserId(), getHttpAuthPassword());
    }
    
    @Override public void applyTo(TaskRepository repository) {

        super.applyTo(repository);
        
        MantisRepositoryConfiguration.setDownloadSubTasks(repository, retrieveSubTasksButton.getSelection());
        
    }
    
    @Override public String getConnectorKind() {

        return MantisCorePlugin.REPOSITORY_KIND;
    }
    
    // public for testing
    public class MantisValidator extends Validator {
        
        private static final String OLD_SF_NET_URL = "https://apps.sourceforge.net/mantisbt/";

		private static final String NEW_SF_NET_URL = "https://sourceforge.net/apps/mantisbt/";

		private final String repositoryUrl;
        
        private final TaskRepository taskRepository;
        
        private final String userName;
        private final String password;
        private final String httpUserName;
        private final String httpPassword;
        
        public MantisValidator(TaskRepository taskRepository, String userName, String password, String httpUserName, String httpPassword) {

            this.repositoryUrl = taskRepository.getRepositoryUrl();
            this.taskRepository = taskRepository;
            this.userName = userName;
            this.password = password;
            this.httpUserName = httpUserName;
            this.httpPassword = httpPassword;
        }
        
        @Override public void run(IProgressMonitor monitor) throws CoreException {

            try {
                //validate(Provider.of(monitor));
                validate(monitor);
            } catch (MalformedURLException e) {
                throw new CoreException(RepositoryStatus.createStatus(repositoryUrl, IStatus.ERROR, MantisUIPlugin.PLUGIN_ID, INVALID_REPOSITORY_URL));
            } catch (MantisException e) {
                String message = buildErrorMessage(e);
                throw new CoreException(RepositoryStatus.createStatus(repositoryUrl, IStatus.ERROR, MantisUIPlugin.PLUGIN_ID, message));
            }
        }

		private String buildErrorMessage(MantisException e) {
			
			StringBuilder message = new StringBuilder();
			
			if ( isSourceforgeRepoWithoutHttpAuth())
				message.append("For SF.net hosted apps, please make sure to use HTTP authentication only.").append('\n');
			
			if ( repositoryUrl.startsWith(OLD_SF_NET_URL))
				message.append("SF.net hosted apps have been moved to https://sourceforge.net/apps/mantisbt/").append('\n');
			
			message.append("Repository validation failed: ");
			
			if ( e.getCause() instanceof AxisFault ) {
				
				AxisFault axisFault = (AxisFault) e.getCause();
				
				if ( axisFault.getCause() instanceof SAXException)
					message.append("the repository has returned an invalid XML response : " + String.valueOf(axisFault.getCause().getMessage()) + " .");
				else if (e.getMessage() != null)
						message.append(" :").append(e.getMessage()).append('\n');

			} else if (e.getMessage() != null)
				message.append(" :").append(e.getMessage()).append('\n');

			
			return message.toString();
		}

		private boolean isSourceforgeRepoWithoutHttpAuth() {
			return repositoryUrl.startsWith(NEW_SF_NET_URL) &&  ( httpUserName.length() == 0 || httpPassword.length() == 0);
		}
        
        public void validate(IProgressMonitor monitor) throws MalformedURLException, MantisException {

            AbstractWebLocation location = new TaskRepositoryLocationFactory().createWebLocation(taskRepository);
            
            IMantisClient client = MantisClientFactory.createClient(location.getUrl(), this.userName, this.password, this.httpUserName, this.httpPassword, location);
            client.validate();
            setStatus(RepositoryStatus.createStatus(repositoryUrl, IStatus.INFO, MantisUIPlugin.PLUGIN_ID, "Authentication credentials are valid."));
        }
    }
    
}