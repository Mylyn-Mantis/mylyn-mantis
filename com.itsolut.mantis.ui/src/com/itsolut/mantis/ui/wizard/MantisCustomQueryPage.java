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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.mylyn.commons.workbench.EnhancedFilteredTree;
import org.eclipse.mylyn.commons.workbench.forms.SectionComposite;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.IMantisClientManager;
import com.itsolut.mantis.core.MantisCache;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectFilter;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.util.MantisUtils;
import com.itsolut.mantis.ui.MantisUIPlugin;

/**
 * Mantis search page. Provides a form similar to the one the Bugzilla connector uses.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 * 
 *         Dave Carver - 20070806 [ 1729675 ] Internal errors when project or filter not selected
 */
public class MantisCustomQueryPage extends AbstractRepositoryQueryPage2 {

    private static final String NO_FILTERS_AVAILABLE = "No filters available for this project. Make sure they are created and that you have the right to access them.";

    private static final String TITLE = "Enter query parameters";

    private static final String DESCRIPTION = "Select a project and a filter to populate the task list. Custom filters can be created from the web interface.";

    private static final String MAX_SEARCH_RESULTS = "Maximum results";

    private Text searchLimit;

    private TaskRepository repository;

    protected List projectFilters;

    private EnhancedFilteredTree tree;

    private final Map<FilterKey, String> filterKeyToUrl = new HashMap<FilterKey, String>();

    private final IMantisClientManager clientManager;

    public MantisCustomQueryPage(TaskRepository repository, IRepositoryQuery query, IMantisClientManager clientManager) {

        super(TITLE, repository, query);

        this.repository = repository;
        this.clientManager = clientManager;

        setTitle(TITLE);
        setDescription(DESCRIPTION);
    }
    
    public MantisCustomQueryPage(TaskRepository repository, IMantisClientManager clientManager) {

        this(repository, null, clientManager);
    }

    protected void createPageContent(SectionComposite sectionComposite) {
    	
    	Composite parent = sectionComposite.getContent();
    	
        Composite control = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        control.setLayoutData(gd);
        GridLayout layout = new GridLayout(2, true);
        control.setLayout(layout);

        Label projectLabel = new Label(control, SWT.NONE);
        projectLabel.setLayoutData(new GridData(SWT.NONE, SWT.TOP, true, false));
        projectLabel.setText("Select project");
        
        Label comboLabel = new Label(control, SWT.NONE);
        comboLabel.setLayoutData(new GridData(SWT.NONE, SWT.TOP, true, false));
        comboLabel.setText("Select filter");

        createProjectTree(control);

        projectFilters = new List(control, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
        projectFilters.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        projectFilters.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {

                updateButtonsIfNeeded();

                List combo = (List) e.getSource();
                updateDescription(combo);

                String text = getSelected(combo);
                
                if ( text == null ) 
                	return;

                // set suggestion
                if (!inSearchContainer())
					setQueryTitle(text);
                
                updateButtonsIfNeeded();
            }

            private void updateDescription(List combo) {

                if (combo.getItemCount() > 1)
                    setMessage(null, DialogPage.WARNING);
                else if (getSelectedProject() == null)
                    setMessage(NO_FILTERS_AVAILABLE, DialogPage.WARNING);

            }
        });


		Composite searchLimitComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(searchLimitComposite);

        Label titleLabel = new Label(searchLimitComposite, SWT.NONE);
        titleLabel.setText(MAX_SEARCH_RESULTS);

        searchLimit = new Text(searchLimitComposite, SWT.BORDER);
        searchLimit.setText(MantisSearch.DEFAULT_SEARCH_LIMIT_STRING);
    }

    private IMantisClient getMantisClient() throws MantisException {

        return clientManager.getRepository(repository);
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
    }

    private void refreshProjectTree() {

        tree.getViewer().setInput(getProjects());
    }

    private MantisProject[] getProjects() {

        try {
			final ArrayList<MantisProject> projects = new ArrayList<MantisProject>();
			projects.add(MantisProject.ALL_PROJECTS);
			projects.addAll(getMantisClient().getCache(new NullProgressMonitor()).getProjects());
			
			return projects.toArray(new MantisProject[projects.size()]);
		} catch (MantisException e) {
			String message = "Failed loading projects : " + e.getMessage();
			MantisUIPlugin.handleError(e, message, false);
			setErrorMessage(message);
			return new MantisProject[0];
		}
    }

    public MantisProject getSelectedProject() {

        IStructuredSelection selection = (IStructuredSelection) tree.getViewer().getSelection();
        return (MantisProject) selection.getFirstElement();
    }

    @Override
    public boolean canFlipToNextPage() {

        return false;
    }

    private static void select(org.eclipse.swt.widgets.List list, String text) {
    	for ( int i = 0 ; i < list.getItemCount(); i++ )
			if ( list.getItem(i).equals(text) )
    			list.setSelection(i);
    }
    
    private static String getSelected(org.eclipse.swt.widgets.List list) {
    	
    	String[] selection = list.getSelection();
    	if ( selection.length == 0 )
    		return null;
    	
    	return selection[0];
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

        if (getSearchContainer() == null) {

            if (getQueryTitle() == null)
                return false;

            if (getQueryTitle().length() == 0)
                return false;
        }

        if (tree == null || getSelectedProject() == null)
            return false;

        if (projectFilters == null || getSelected(projectFilters) == null )
            return false;

        try {
            if (searchLimit != null)
                Integer.parseInt(searchLimit.getText());
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    @Override
    public void applyTo(IRepositoryQuery query) {

        query.setSummary(getQueryTitle());
        query.setUrl(filterKeyToUrl.get(new FilterKey(getSelectedProject().getValue(), getSelected(projectFilters)))); // possibly null
        query.setAttribute(IMantisClient.SEARCH_LIMIT, searchLimit.getText());
        query.setAttribute(IMantisClient.PROJECT_NAME, getSelectedProject().getName());
        query.setAttribute(IMantisClient.FILTER_NAME, getSelected(projectFilters));
    }

    private void refreshFilterCombo() {

        try {
            MantisCache cache = getMantisClient().getCache(new NullProgressMonitor());
            String valueToSet = getSelected(projectFilters);

            projectFilters.removeAll();

            if (getSelectedProject() == null)
                return;

            int selectedProjectId = getSelectedProject().getValue();
            for (MantisProjectFilter pd : cache.getProjectFilters(selectedProjectId)) {
                projectFilters.add(pd.getName());
                filterKeyToUrl.put(new FilterKey(selectedProjectId, pd.getName()), pd.getUrl());
            }

            if (valueToSet != null)
                select(projectFilters, valueToSet);

        } catch (MantisException e) {
            setErrorMessage("Failed updating attributes " + e.getMessage() + " .");
        }

        updateButtonsIfNeeded();
    }

    @Override
    protected boolean restoreState(IRepositoryQuery query) {
    
        try {
			MantisCache cache = getMantisClient().getCache(new NullProgressMonitor());

			MantisSearch search = MantisUtils.getMantisSearch(query);

			tree.getViewer().setSelection(new StructuredSelection(cache.getProjectByName(search.getProjectName())));

			select(projectFilters, search.getFilterName());
			searchLimit.setText(String.valueOf(search.getLimit()));
			
			return true;
			
		} catch (MantisException e) {
			MantisUIPlugin.handleError(e, "Failed restoring query page state from query " + query, false);
		
			return false;
		}
    }
    
    private void updateButtonsIfNeeded() {

        if (getContainer() != null)
            getContainer().updateButtons();

        setPageComplete(isPageComplete());
    }
    
	@Override
	protected void doRefreshControls() {
	
		refreshProjectTree();
		refreshFilterCombo();	
	}

	@Override
	protected boolean hasRepositoryConfiguration() {
		
		try {
			return getMantisClient().getCacheData().hasBeenRefreshed();
		} catch (MantisException e) {
			return false;
		}
	}

    private static final class FilterKey {

        private int projectId;
        private String filterName;

        public FilterKey(int projectId, String filterName) {

            this.projectId = projectId;
            this.filterName = filterName;
        }

        @Override
        public int hashCode() {

            final int prime = 31;
            int result = 1;
            result = prime * result + ((filterName == null) ? 0 : filterName.hashCode());
            result = prime * result + projectId;
            return result;
        }

        @Override
        public boolean equals(Object obj) {

            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FilterKey other = (FilterKey) obj;
            if (filterName == null) {
                if (other.filterName != null)
                    return false;
            } else if (!filterName.equals(other.filterName))
                return false;
            if (projectId != other.projectId)
                return false;
            return true;
        }
        
        
    }
}
