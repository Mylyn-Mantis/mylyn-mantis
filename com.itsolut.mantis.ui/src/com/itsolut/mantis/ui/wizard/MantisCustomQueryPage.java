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

import static com.itsolut.mantis.ui.util.MantisUIUtil.newEnhancedFilteredTree;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonUiUtil;
import org.eclipse.mylyn.internal.provisional.commons.ui.EnhancedFilteredTree;
import org.eclipse.mylyn.internal.provisional.commons.ui.ICoreRunnable;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCache;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectFilter;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.util.MantisUtils;
import com.itsolut.mantis.ui.MantisUIPlugin;
import com.itsolut.mantis.ui.util.MantisUIUtil;

/**
 * Mantis search page. Provides a form similar to the one the Bugzilla connector uses.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 * 
 *         Dave Carver - 20070806 [ 1729675 ] Internal errors when project or filter not selected
 */
@SuppressWarnings("restriction")
public class MantisCustomQueryPage extends AbstractRepositoryQueryPage {

    private static final String NO_FILTERS_AVAILABLE = "No filters available for this project. Make sure they are created and that you have the right to access them.";

    private static final String SELECT_FILTER_IN_PROJECT = "Select Filter in Project";

    private static final String TITLE = "Enter query parameters";

    private static final String DESCRIPTION = "Select a project and a filter to populate the task list. Custom filters can be created from the web interface.";

    private static final String TITLE_QUERY_TITLE = "Query Title:";

    private static final String MAX_SEARCH_RESULTS = "Maximum results";

    private IRepositoryQuery query;

    private Text titleText;

    private Text searchLimit;

    private TaskRepository repository = null;

    protected Combo filterCombo = null;

    protected Button updateRepository;

    private EnhancedFilteredTree tree;

    public MantisCustomQueryPage(TaskRepository repository, IRepositoryQuery query) {

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
        GridLayout layout = new GridLayout(2, false);
        control.setLayout(layout);

        createTitleGroup(control);

        Label projectLabel = new Label(control, SWT.NONE);
        projectLabel.setLayoutData(new GridData(SWT.NONE, SWT.TOP, false, false));
        projectLabel.setText("Select project");

        createProjectTree(control);

        try {
            Label comboLabel = new Label(control, SWT.NONE);
            comboLabel.setText("Select filter");

            filterCombo = new Combo(control, SWT.READ_ONLY);
            filterCombo.add(SELECT_FILTER_IN_PROJECT);
            filterCombo.setText(filterCombo.getItem(0));

            filterCombo.addSelectionListener(new SelectionListener() {

                public void widgetSelected(SelectionEvent e) {

                    updateButtonsIfNeeded();

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
                    if (getSearchContainer() == null) {
                        titleText.setText(text);
                        titleText.selectAll();
                    }
                    updateButtonsIfNeeded();

                }

                private void updateDescription(Combo combo) {

                    if (combo.getItemCount() > 1)
                        setMessage(null, DialogPage.WARNING);
                    else if (getSelectedProject() == null)
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
            GridData buttonGridData = new GridData(SWT.LEFT, SWT.NULL, false, false);
            buttonGridData.horizontalSpan = 2;
            updateRepository.setLayoutData(buttonGridData);
            updateRepository.setText("Update Repository Configuration");
            updateRepository.addSelectionListener(new SelectionListener() {

                public void widgetDefaultSelected(SelectionEvent arg0) {

                    // nothing
                }

                public void widgetSelected(SelectionEvent arg0) {

                    MantisUIUtil.updateRepositoryConfiguration(getRunnableContext(), getRepository());
                    refreshFilterCombo();

                }

            });

            if (query != null) {
                titleText.setText(query.getSummary());
                restoreSearchFilterFromQuery(query);
            }
        } catch (Exception e1) {

            // Axis does send very verbose errors which break the layout if
            // displayed directly
            String sanitizedString = String.valueOf(e1.getMessage());

            if (sanitizedString.length() > 50)
                sanitizedString = sanitizedString.substring(0, 47) + "...";

            setMessage("Unable to build query page. Please check your repository settings.\nError details: "
                    + sanitizedString, DialogPage.ERROR);
        }

        setControl(control);
    }

    private IMantisClient getMantisClient() throws MantisException {

        MantisRepositoryConnector connector = (MantisRepositoryConnector) TasksUi.getRepositoryManager()
                .getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);
        final IMantisClient client = connector.getClientManager().getRepository(repository);
        return client;
    }

    private void createProjectTree(Composite control) {

        tree = newEnhancedFilteredTree(control);

        TreeViewer projectTreeViewer = tree.getViewer();

        projectTreeViewer.setLabelProvider(new MantisProjectLabelProvider());

        projectTreeViewer.setContentProvider(new MantisProjectITreeContentProvider());

        projectTreeViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {

                if (getSelectedProject() == null) {
                    setErrorMessage("Please select a project.");
                } else {
                    setErrorMessage(null);
                }

                refreshFilterCombo();
            }

        });

        refreshProjectTree(projectTreeViewer);

    }

    private void refreshProjectTree(TreeViewer projectTreeViewer) {

        projectTreeViewer.setInput(getProjects());
    }

    /**
     * @return a suitable, visible {@link IRunnableContext}
     */
    private IRunnableContext getRunnableContext() {

    	
    	// we trust the search container to always have a runnable context
    	if ( getSearchContainer() != null )
    		return getSearchContainer().getRunnableContext();
    	
        // task #155 : when 'New query' is invoked from the the repositories page the shell is not visible
        final boolean[] shellVisibleHolder = new boolean[] { false } ;
        
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				if  ( getShell() != null )
					shellVisibleHolder[0] = getShell().isVisible();
			}
		});
        
		IRunnableContext container = getContainer();
        
		// only get the platform service if the shell is invisible or the container is null
        if ( !shellVisibleHolder[0] || container == null )
        	container = PlatformUI.getWorkbench().getProgressService();
        
        return container;
    }

    private MantisProject[] getProjects() {

        final List<MantisProject> projects = new ArrayList<MantisProject>();

        try {
            MantisRepositoryConnector connector = (MantisRepositoryConnector) TasksUi.getRepositoryManager()
                    .getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);
            final IMantisClient client = connector.getClientManager().getRepository(repository);
            

            CommonUiUtil.run(getRunnableContext(), new ICoreRunnable() {

                public void run(IProgressMonitor monitor) throws CoreException {
            
                    try {
                        projects.addAll(client.getCache(monitor).getProjects());
                    } catch (MantisException e) {
                        throw new CoreException(new Status(Status.ERROR, MantisUIPlugin.PLUGIN_ID,
                                "Failed getting projects : " + e.getMessage(), e));
                    }
                }
            });
        } catch (CoreException e) {
            setMessage("Unable to load projects : " + e.getMessage() + " .", DialogPage.ERROR);
        } catch (MantisException e) {
            setMessage("Unable to load projects : " + e.getMessage() + " .", DialogPage.ERROR);
        }

        return projects.toArray(new MantisProject[0]);
    }

    public MantisProject getSelectedProject() {

        IStructuredSelection selection = (IStructuredSelection) tree.getViewer().getSelection();
        return (MantisProject) selection.getFirstElement();
    }

    @Override
    public boolean canFlipToNextPage() {

        return false;
    }

    private void restoreSearchFilterFromQuery(IRepositoryQuery query) throws MalformedURLException, MantisException {

        MantisCache cache = getMantisClient().getCache(new NullProgressMonitor());

        MantisSearch search = MantisUtils.getMantisSearch(query);

        tree.getViewer().setSelection(new StructuredSelection(cache.getProjectByName(search.getProjectName())));
        
        for (MantisProjectFilter pd : cache.getProjectFilters(getSelectedProject().getValue()))
            filterCombo.add(pd.getName());

        filterCombo.setText(search.getFilterName());
        searchLimit.setText(String.valueOf(search.getLimit()));
        
    }

    private void createTitleGroup(Composite control) {

        if (inSearchContainer())
            return;

        Label titleLabel = new Label(control, SWT.NONE);
        titleLabel.setText(TITLE_QUERY_TITLE);

        titleText = new Text(control, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
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

    @Override
    public void setPageComplete(boolean complete) {

        super.setPageComplete(complete);

        if (getSearchContainer() != null)
            getSearchContainer().setPerformActionEnabled(complete);
    }

    private boolean validate() {

        boolean returnsw = true;

        if (getSearchContainer() == null) {

            if (titleText == null)
                return false;

            if (titleText.getText().length() == 0)
                return false;
        }

        if (tree == null || getSelectedProject() == null)
            return false;

        if (filterCombo != null && filterCombo.getText().contains(SELECT_FILTER_IN_PROJECT))
            return false;

        try {
            if (searchLimit != null)
                Integer.parseInt(searchLimit.getText());
        } catch (NumberFormatException e) {
            return false;
        }

        return returnsw;
    }

    @Override
    public void applyTo(IRepositoryQuery query) {

        query.setSummary(this.getQueryTitle());
        query.setAttribute(IMantisClient.SEARCH_LIMIT, searchLimit.getText());
        query.setAttribute(IMantisClient.PROJECT_NAME, getSelectedProject().getName());
        query.setAttribute(IMantisClient.FILTER_NAME, filterCombo.getText());
    }

    @Override
    public String getQueryTitle() {

        return (titleText != null) ? titleText.getText() : null;
    }

    private void refreshFilterCombo() {

        try {
            MantisCache cache = getMantisClient().getCache(new NullProgressMonitor());
            String valueToSet = filterCombo.getText();

            filterCombo.remove(1, filterCombo.getItemCount() - 1);

            if (getSelectedProject() == null)
                return;

            for (MantisProjectFilter pd : cache.getProjectFilters(getSelectedProject().getValue()))
                filterCombo.add(pd.getName());

            if (valueToSet != null)
                filterCombo.setText(valueToSet);

            if (filterCombo.getSelectionIndex() == -1)
                filterCombo.setText(SELECT_FILTER_IN_PROJECT);

        } catch (MantisException e) {
            setErrorMessage("Failed updating attributes " + e.getMessage() + " .");
        }

        updateButtonsIfNeeded();
    }

    private void updateButtonsIfNeeded() {

        if (getContainer() != null)
            getContainer().updateButtons();

        setPageComplete(isPageComplete());
    }

}
