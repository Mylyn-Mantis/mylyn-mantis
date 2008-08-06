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

package com.itsolut.mantis.ui.wizard;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.mylyn.tasks.core.RepositoryTemplate;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisClientFactory;
import com.itsolut.mantis.core.IMantisClient.Version;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.exception.MantisLoginException;
import com.itsolut.mantis.ui.MantisUIPlugin;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 * @author David Carver / d_a_carver@yahoo.com - updated sample url.
 */
public class MantisRepositorySettingsPage extends AbstractRepositorySettingsPage {

	private static final String MESSAGE_FAILURE_UNKNOWN = "Unknown error occured. Check that server url and credentials are valid.";

	private static final String TITLE = "Mantis Repository Settings";

	private static final String DESCRIPTION = "Example: http://mylyn-mantis.sourceforge.net/MantisTest/mc/mantisconnect.php";

	private Combo accessTypeCombo;

	/** Supported access types. */
	private Version[] versions;

	public MantisRepositorySettingsPage(AbstractRepositoryConnectorUi repositoryUi) {
		super(TITLE, DESCRIPTION, repositoryUi);

		setNeedsAnonymousLogin(false);
		setNeedsEncoding(false);
		setNeedsTimeZone(false);
	}

	
	@Override
	protected void createAdditionalControls(final Composite parent) {
		
		for (RepositoryTemplate template : connector.getTemplates()) {
			serverUrlCombo.add(template.label);
		}
		serverUrlCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String text = serverUrlCombo.getText();
				RepositoryTemplate template = connector.getTemplate(text);
				if (template != null) {
					repositoryLabelEditor.setStringValue(template.label);
					setUrl(template.repositoryUrl);
					setAnonymous(template.anonymous);

					try {
						Version version = Version.valueOf(template.version);
						setMantisVersion(version);
					} catch (RuntimeException ex) {
						setMantisVersion(Version.MC_1_0a5);
					}

					getContainer().updateButtons();
					return;
				}
			}
		});
		
		Label accessTypeLabel = new Label(parent, SWT.NONE);
		accessTypeLabel.setText("Access Type: ");
		accessTypeCombo = new Combo(parent, SWT.READ_ONLY);
		
		versions = Version.values();
		for (Version version : versions) {
			accessTypeCombo.add(version.toString());
		}
		if (repository != null) {
			setMantisVersion(Version.fromVersion(repository.getVersion()));
		} else {
			setMantisVersion(null);
		}
		accessTypeCombo.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				setMantisVersion(getMantisVersion());
				getWizard().getContainer().updateButtons();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				// ignore
			}
		});
		
		// initialize the private field that holds the version
		setMantisVersion(getMantisVersion()); 
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

	public Version getMantisVersion() {
		return versions[accessTypeCombo.getSelectionIndex()];
	}

	public void setMantisVersion(Version version) {
		if (version == null) {
			// select "Automatic"
			accessTypeCombo.select(0);
		} else {
			int i = accessTypeCombo.indexOf(version.toString());
			if (i != -1) {
				accessTypeCombo.select(i);
			}
			setVersion(version.name());
		}
	}

	@Override
	protected void validateSettings() {

		try {
			final String serverUrl = getServerUrl();
			final Version version = getMantisVersion();
			final String username = getUserName();
			final String password = getPassword();
			// TODO is there a way to get the proxy without duplicating code and
			// creating a task repository?

			if (version == null) {
				MessageDialog.openInformation(null, MantisUIPlugin.TITLE_MESSAGE_DIALOG, "Repository Connector Version is required.");
				return;
			}

			if (username.length() == 0 || password.length() == 0) {
				MessageDialog.openInformation(null, MantisUIPlugin.TITLE_MESSAGE_DIALOG, "Authentication credentials are needed.");
				return;
			}
			
			final Proxy proxy = createTaskRepository().getProxy();
			
			getWizard().getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Validating server settings", IProgressMonitor.UNKNOWN);
					
					try {
						IMantisClient client = MantisClientFactory.createClient(serverUrl, version, username, password, proxy);
						client.validate();

					} catch (Exception e) {
						MantisUIPlugin.handleMantisException(e);
					} finally {
						monitor.done();
					}
				}
			});

			if (username.length() > 0) {
				MessageDialog.openInformation(null, MantisUIPlugin.TITLE_MESSAGE_DIALOG, "Authentication credentials are valid.");
			} else {
				MessageDialog.openInformation(null, MantisUIPlugin.TITLE_MESSAGE_DIALOG, "Repository is valid.");
			}
			
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof MalformedURLException) {
				MessageDialog.openWarning(null, MantisUIPlugin.TITLE_MESSAGE_DIALOG, "Repository url is invalid.");
				
			} else if (e.getCause() instanceof MantisLoginException) {
				MessageDialog.openWarning(null, MantisUIPlugin.TITLE_MESSAGE_DIALOG, "Unable to authenticate with repository. Login credentials invalid.");
				
			} else if (e.getCause() instanceof MantisException) {
				String message = "No O repository found at url";
				if (e.getCause().getMessage() != null) {
					message += ": " + e.getCause().getMessage();
				}
				MessageDialog.openWarning(null, MantisUIPlugin.TITLE_MESSAGE_DIALOG, message);
				
			} else {
				MessageDialog.openWarning(null, MantisUIPlugin.TITLE_MESSAGE_DIALOG, MESSAGE_FAILURE_UNKNOWN);
				
			}
		} catch (InterruptedException e) {
			MessageDialog.openWarning(null, MantisUIPlugin.TITLE_MESSAGE_DIALOG, MESSAGE_FAILURE_UNKNOWN);
		}
		
		super.getWizard().getContainer().updateButtons();
	}

	@Override
	protected Validator getValidator(TaskRepository repository) {
		// TODO Auto-generated method stub
		return null;
	}
}