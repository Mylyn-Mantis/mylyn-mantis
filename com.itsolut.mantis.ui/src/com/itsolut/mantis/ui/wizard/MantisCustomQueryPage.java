/*******************************************************************************
 * Copyright (c) 2003 - 2006 University Of British Columbia and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: University Of British Columbia - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Chris Hane - adapted Trac implementation for Mantis
 *******************************************************************************/

package com.itsolut.mantis.ui.wizard;

import java.net.MalformedURLException;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectFilter;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisSearchFilter;
import com.itsolut.mantis.core.util.MantisUtils;
import com.itsolut.mantis.ui.util.MantisUIUtil;

/**
 * Mantis search page. Provides a form similar to the one the Bugzilla connector
 * uses.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 * 
 *         Dave Carver - 20070806 [ 1729675 ] Internal errors when project or
 *         filter not selected
 */
public class MantisCustomQueryPage extends AbstractRepositoryQueryPage {

	private static final String NO_FILTERS_AVAILABLE = "No filters available for this project. Make sure they are created and that you have the right to access them.";

	private static final String SELECT_FILTER_IN_PROJECT = "Select Filter in Project";

	private static final String TITLE = "Enter query parameters";

	private static final String DESCRIPTION = "Only filters created from the web interface are supported.";

	private static final String TITLE_QUERY_TITLE = "Query Title:";

	private static final String MAX_SEARCH_RESULTS = "Maximum results";

	private IRepositoryQuery query;

	private Text titleText;

	private Text searchLimit;

	private TaskRepository repository = null;

	protected Combo projectCombo = null;

	protected Combo filterCombo = null;

	protected Button updateRepository;

	public MantisCustomQueryPage(TaskRepository repository,
			IRepositoryQuery query) {

		super(TITLE, repository, query);

		this.repository = repository;
		this.query = query;

		setTitle(TITLE);
		setDescription(DESCRIPTION);
	}

	public MantisCustomQueryPage(TaskRepository repository) {

		this(repository, null);
	}

	public void createControl(Composite parent) {

        Composite control = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
        control.setLayoutData(gd);
        GridLayout layout = new GridLayout(1, false);
        control.setLayout(layout);

        createTitleGroup(control);

        projectCombo = new Combo(control, SWT.READ_ONLY);

        try {
            MantisRepositoryConnector connector = (MantisRepositoryConnector) TasksUi.getRepositoryManager()
            .getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);
            final IMantisClient client = connector.getClientManager().getRepository(repository);

            refreshProjectCombo(client, null);

            projectCombo.addSelectionListener(new SelectionListener() {

                public void widgetSelected(SelectionEvent e) {

                    try {
                        refreshFilterCombo(client, null);
                    } catch (MantisException e1) {
                        MantisCorePlugin.log(e1);
                    }
                }

                public void widgetDefaultSelected(SelectionEvent e) {

                    // nothing
                }
            });

            filterCombo = new Combo(control, SWT.READ_ONLY);
            filterCombo.add(SELECT_FILTER_IN_PROJECT);
            filterCombo.setText(filterCombo.getItem(0));

            filterCombo.addSelectionListener(new SelectionListener() {

                public void widgetSelected(SelectionEvent e) {

                    getWizard().getContainer().updateButtons();

                    Combo combo = (Combo) e.getSource();
                    updateDescription(combo);
                    
                    // skip auto fill-in if we don't have results
                    if (combo.getItemCount() <= 1)
                        return;

                    String text = combo.getText();

                    // skip auto fill-in if this is the 'suggestion' text
                    if (SELECT_FILTER_IN_PROJECT.equals(text))
                        return;

                    // set suggestion and select
                    titleText.setText(text);
                    titleText.selectAll();
                    // notify that we've changed the value
                    getContainer().updateButtons();

                }

                private void updateDescription(Combo combo) {
                	
                	if ( combo.getItemCount() > 1)
						setMessage(null, DialogPage.WARNING);
                	else if ( projectCombo.getSelectionIndex() != 0)
                		setMessage(NO_FILTERS_AVAILABLE, DialogPage.WARNING);
					
				}

				public void widgetDefaultSelected(SelectionEvent e) {

                    // nothing
                }
            });

            Label titleLabel = new Label(control, SWT.NONE);
            titleLabel.setText(MAX_SEARCH_RESULTS);

            searchLimit = new Text(control, SWT.BORDER);
            searchLimit.setText(MantisSearch.DEFAULT_SEARCH_LIMIT_STRING);

            updateRepository = new Button(control, SWT.PUSH);
            updateRepository.setText("Update Repository Configuration");
            updateRepository.addSelectionListener(new SelectionListener() {

                public void widgetDefaultSelected(SelectionEvent arg0) {

                    // nothing
                }

                public void widgetSelected(SelectionEvent arg0) {

                    try {
                        MantisUIUtil.updateRepositoryConfiguration(getContainer(), getRepository(), true);
                        refreshProjectCombo(client, projectCombo.getText());
                        refreshFilterCombo(client, filterCombo.getText());
                    } catch (MantisException e) {
                        MantisCorePlugin.log(e);
                    }
                }

            });

            if (query != null) {
                titleText.setText(query.getSummary());
                restoreSearchFilterFromQuery(query);
            }
        } catch (Exception e1) {
            MantisCorePlugin.log(e1);
        }

        setControl(control);
    }

	@Override
	public boolean canFlipToNextPage() {

		return false;
	}

	private void restoreSearchFilterFromQuery(IRepositoryQuery query)
			throws MalformedURLException, MantisException {

		for (MantisSearchFilter filter : MantisUtils.getMantisSearch(query)
				.getFilters())
			if ("project".equals(filter.getFieldName()))
				projectCombo.setText(filter.getValues().get(0));
			else if ("filter".equals(filter.getFieldName())) {
				MantisRepositoryConnector connector = (MantisRepositoryConnector) TasksUi
						.getRepositoryManager().getRepositoryConnector(
								MantisCorePlugin.REPOSITORY_KIND);
				IMantisClient client = connector.getClientManager()
						.getRepository(repository);
				for (MantisProjectFilter pd : client
						.getProjectFilters(projectCombo.getText()))
					filterCombo.add(pd.getName());

				filterCombo.setText(filter.getValues().get(0));

			}
	}

	private void createTitleGroup(Composite control) {

		if (inSearchContainer())
			return;

		Label titleLabel = new Label(control, SWT.NONE);
		titleLabel.setText(TITLE_QUERY_TITLE);

		titleText = new Text(control, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL
				| GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = 2;
		titleText.setLayoutData(gd);
		titleText.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {

				// ignore
			}

			public void keyReleased(KeyEvent e) {

				getContainer().updateButtons();
			}
		});
	}

	public TaskRepository getRepository() {

		return repository;
	}

	public void setRepository(TaskRepository repository) {

		this.repository = repository;
	}

	@Override
	public boolean isPageComplete() {

		return validate();
	}

	private boolean validate() {

		boolean returnsw = true;

		if (titleText == null)
			return false;

		if (titleText.getText().length() == 0)
			return false;

		if (projectCombo == null)
			return false;

		if (projectCombo.getItemCount() <= 1)
			return false;

		if (projectCombo.getText().contains("Select Project for new Issue"))
			return false;

		if (filterCombo != null
				&& filterCombo.getText().contains(SELECT_FILTER_IN_PROJECT))
			return false;

		try {
			if (searchLimit != null)
				Integer.parseInt(searchLimit.getText());
		} catch (NumberFormatException e) {
			return false;
		}

		return returnsw;
	}

	public String getQueryUrl(String repsitoryUrl) {

		MantisSearch search = new MantisSearch();

		search.addFilter(new MantisSearchFilter("project", projectCombo
				.getText()));
		search
				.addFilter(new MantisSearchFilter("filter", filterCombo
						.getText()));

		StringBuilder sb = new StringBuilder();
		sb.append(repsitoryUrl);
		sb.append(IMantisClient.QUERY_URL);
		sb.append(search.toUrl());
		return sb.toString();
	}

	@Override
	public void applyTo(IRepositoryQuery query) {

		query.setSummary(this.getQueryTitle());
		query.setAttribute(IMantisClient.SEARCH_LIMIT, searchLimit.getText());
		query.setUrl(this.getQueryUrl(repository.getRepositoryUrl()));
	}

	@Override
	public String getQueryTitle() {

		return (titleText != null) ? titleText.getText() : null;
	}

	/**
	 * @param client
	 *            the client
	 * @param valueToSet
	 *            the value to set for the combo, if not null. If null, the
	 *            first value will be set.
	 * @throws MantisException
	 *             error loading the project values
	 */
	private void refreshProjectCombo(IMantisClient client, String valueToSet)
			throws MantisException {

		projectCombo.removeAll();
		projectCombo.add("Select Project for new Issue");
		for (MantisProject pd : client.getProjects())
			projectCombo.add(pd.getName());
		if (valueToSet != null)
			projectCombo.setText(valueToSet);
		else
			projectCombo.setText(projectCombo.getItem(0));
	}

	/**
	 * @param client
	 *            the client client
	 * @param valueToSet
	 *            the value to set for the combo, if not null. If null, the
	 *            first value will be set.
	 * @throws MantisException
	 *             error loading the project filters
	 */
	private void refreshFilterCombo(IMantisClient client, String valueToSet)
			throws MantisException {

		filterCombo.remove(1, filterCombo.getItemCount() - 1);

		if (projectCombo.getSelectionIndex() > 0) {
			for (MantisProjectFilter pd : client.getProjectFilters(projectCombo
					.getText()))
				filterCombo.add(pd.getName());

			if (valueToSet != null)
				filterCombo.setText(valueToSet);

		}
		getWizard().getContainer().updateButtons();
	}

}
