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
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.RepositoryTemplate;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;
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
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.IMantisClient.Version;
import com.itsolut.mantis.core.exception.MantisException;
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

	
	public MantisRepositorySettingsPage(String title, String description,
			TaskRepository taskRepository) {
		super(TITLE, DESCRIPTION, taskRepository);
		setNeedsAnonymousLogin(true);
		setNeedsEncoding(false);
		setNeedsTimeZone(false);
		setNeedsValidation(true);
		setNeedsAdvanced(true);
		setNeedsHttpAuth(true);
	}

	
	@Override
	public boolean isPageComplete() {
		// TODO Auto-generated method stub
		return super.isPageComplete();
	}
	
	@Override
	protected void createAdditionalControls(final Composite parent) {
		
		for (RepositoryTemplate template : TasksUiPlugin.getRepositoryTemplateManager().getTemplates(connector.getConnectorKind())) {
			serverUrlCombo.add(template.label);
		}
		
		serverUrlCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String text = serverUrlCombo.getText();
				RepositoryTemplate template = TasksUiPlugin.getRepositoryTemplateManager().getTemplate(connector.getConnectorKind(), text);
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
		// TODO Auto-generated method stub
		super.validateSettings();
	}
	

	@Override
	protected Validator getValidator(TaskRepository repository) {
		// TODO Auto-generated method stub
		return new MantisValidator(repository, Version.MC_1_0a5, getUserName(), getPassword());
	}


	@Override
	public String getConnectorKind() {
		return MantisCorePlugin.REPOSITORY_KIND;
	}
	
	// public for testing
	public class MantisValidator extends Validator {

		private final String repositoryUrl;

		private final TaskRepository taskRepository;

		private final Version version;
		private final String userName;
		private final String password;

		private Version result;

		public MantisValidator(TaskRepository taskRepository, Version version, String userName, String password) {
			this.repositoryUrl = taskRepository.getRepositoryUrl();
			this.taskRepository = taskRepository;
			this.version = version;
			this.userName = userName;
			this.password = password;
		}

		@Override
		public void run(IProgressMonitor monitor) throws CoreException {
			try {
				//validate(Provider.of(monitor));
				validate(monitor);
			} catch (MalformedURLException e) {
				throw new CoreException(RepositoryStatus.createStatus(repositoryUrl, IStatus.ERROR,
						MantisUIPlugin.PLUGIN_ID, INVALID_REPOSITORY_URL));
			} catch (MantisException e) {
				String message = "No Mantis repository found at url";
				if (e.getMessage() != null) {
					message += ": " + e.getMessage();
				}
				throw new CoreException(RepositoryStatus.createStatus(repositoryUrl, IStatus.ERROR,
						MantisUIPlugin.PLUGIN_ID, message));
			}
		}

		public void validate(IProgressMonitor monitor) throws MalformedURLException, MantisException {
			AbstractWebLocation location = new TaskRepositoryLocationFactory().createWebLocation(taskRepository);

				IMantisClient client = MantisClientFactory.createClient(location.getUrl(), version, userName, this.password, null);
				client.validate();
				setStatus(RepositoryStatus.createStatus(repositoryUrl, IStatus.INFO, MantisUIPlugin.PLUGIN_ID, "Authentication credentials are valid."));
		}
	}
	
}