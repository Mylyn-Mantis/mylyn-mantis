/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Robert Munteanu
 *******************************************************************************/
package com.itsolut.mantis.core;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.mylyn.commons.net.Policy;

import com.itsolut.mantis.binding.AccountData;
import com.itsolut.mantis.binding.CustomFieldDefinitionData;
import com.itsolut.mantis.binding.FilterData;
import com.itsolut.mantis.binding.ObjectRef;
import com.itsolut.mantis.binding.ProjectData;
import com.itsolut.mantis.binding.ProjectVersionData;
import com.itsolut.mantis.core.AbstractMantisClient.DefaultConstantValues;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisCustomField;
import com.itsolut.mantis.core.model.MantisETA;
import com.itsolut.mantis.core.model.MantisPriority;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectCategory;
import com.itsolut.mantis.core.model.MantisProjectFilter;
import com.itsolut.mantis.core.model.MantisProjection;
import com.itsolut.mantis.core.model.MantisReproducibility;
import com.itsolut.mantis.core.model.MantisResolution;
import com.itsolut.mantis.core.model.MantisSeverity;
import com.itsolut.mantis.core.model.MantisTicketStatus;
import com.itsolut.mantis.core.model.MantisVersion;
import com.itsolut.mantis.core.model.MantisViewState;

/**
 * @author Robert Munteanu
 * 
 */
public class MantisCache {

    private static final String RESOLVED_STATUS_THRESHOLD = "bug_resolved_status_threshold";

    private static final String REPORTER_THRESHOLD = "report_bug_threshold";

    private static final String DEVELOPER_THRESHOLD = "update_bug_assign_threshold";

    private static final String DUE_DATE_VIEW_THRESHOLD = "due_date_view_threshold";

    private static final String DUE_DATE_UPDATE_THRESHOLD = "due_date_update_threshold";

    private long lastUpdate = 0;

    private List<MantisProject> projects = new ArrayList<MantisProject>();

    private Map<Integer, List<MantisProjectFilter>> projectFiltersById = new HashMap<Integer, List<MantisProjectFilter>>();

    private Map<Integer, List<MantisCustomField>> customFieldsByProjectId = new HashMap<Integer, List<MantisCustomField>>();

    private RepositoryVersion repositoryVersion;

    private MantisAxis1SOAPClient soapClient;

    private int resolvedStatus;

    private int dueDateViewThreshold;

    private int dueDateUpdateThreshold;

    private List<MantisPriority> priorities;

    private List<MantisTicketStatus> statuses;

    private List<MantisSeverity> severities;

    private List<MantisResolution> resolutions;

    private List<MantisReproducibility> reproducibilities;

    private List<MantisProjection> projections;

    private List<MantisETA> etas;

    private List<MantisViewState> viewStates;

    private Map<Integer, List<MantisProjectCategory>> categoriesByProjectId = new HashMap<Integer, List<MantisProjectCategory>>();

    private Map<Integer, List<MantisVersion>> versionsByProjectId = new HashMap<Integer, List<MantisVersion>>();

    private Map<Integer, List<String>> reportersByProjectId = new HashMap<Integer, List<String>>();

    private Map<Integer, List<String>> developersByProjectId = new HashMap<Integer, List<String>>();

    private int reporterThreshold;

    private int developerThreshold;

    public MantisCache(MantisAxis1SOAPClient soapClient) {

        this.soapClient = soapClient;
    }

    public void setProjects(List<MantisProject> projects) {

        this.projects = projects;
    }

    public List<MantisProject> getProjects() {

        return projects;
    }

    void refreshIfNeeded(IProgressMonitor progressMonitor) throws MantisException {

        if (lastUpdate != 0)
            refresh(progressMonitor);

    }

    private void refresh(IProgressMonitor monitor) throws MantisException {

        cacheProjects(soapClient.getProjectData(monitor));

        cacheReporterThreshold(soapClient.getStringConfiguration(monitor, REPORTER_THRESHOLD));
        cacheDeveloperThreshold(soapClient.getStringConfiguration(monitor, DEVELOPER_THRESHOLD));

        for (MantisProject project : projects) {
            cacheFilters(project.getValue(), soapClient.getProjectFilters(project.getValue(), monitor));
            cacheProjectCustomFields(project.getValue(), soapClient.getProjectCustomFields(project.getValue(), monitor));
            cacheProjectCategories(project.getValue(), soapClient.getProjectCategories(project.getValue(), monitor));
            cacheProjectReporters(project.getValue(), soapClient.getProjectUsers(project.getValue(), reporterThreshold,
                    monitor));
            cacheProjectDevelopers(project.getValue(), soapClient.getProjectUsers(project.getValue(),
                    developerThreshold, monitor));
            cacheProjectVersions(project.getValue(), soapClient.getProjectVersions(project.getValue(), monitor));
        }

        cacheResolvedStatus(soapClient.getStringConfiguration(monitor, RESOLVED_STATUS_THRESHOLD));
        cacheRepositoryVersion(soapClient.getVersion(monitor));

        if (repositoryVersion.isHasDueDateSupport()) {
            cacheDueDateViewThreshold(soapClient.getStringConfiguration(monitor, DUE_DATE_VIEW_THRESHOLD));
            cacheDueDateUpdateThreshold(soapClient.getStringConfiguration(monitor, DUE_DATE_UPDATE_THRESHOLD));
        }

        cachePriorities(soapClient.getPriorities(monitor));
        cacheStatuses(soapClient.getStatuses(monitor));
        cacheSeverities(soapClient.getSeverities(monitor));
        cacheResolutions(soapClient.getResolutions(monitor));
        cacheReproducibilites(soapClient.getReproducibilities(monitor));
        cacheProjections(soapClient.getProjections(monitor));
        cacheEtas(soapClient.getEtas(monitor));
        cacheViewStates(soapClient.getViewStates(monitor));
    }

    private void cacheProjectVersions(int value, ProjectVersionData[] projectVerions) {

        List<MantisVersion> projectVersions = new ArrayList<MantisVersion>();

        for (ProjectVersionData version : projectVerions)
            projectVersions.add(MantisConverter.convert(version));

        versionsByProjectId.put(value, projectVersions);

    }

    private void cacheProjectReporters(int projectId, AccountData[] projectUsers) {

        List<String> reporters = new ArrayList<String>();

        for (AccountData accountData : projectUsers)
            reporters.add(accountData.getName());

        reportersByProjectId.put(projectId, reporters);

    }

    private void cacheProjectDevelopers(int projectId, AccountData[] projectDevelopers) {

        List<String> developers = new ArrayList<String>();

        for (AccountData accountData : projectDevelopers)
            developers.add(accountData.getName());

        developersByProjectId.put(projectId, developers);

    }

    private void cacheReporterThreshold(String stringConfiguration) {

        reporterThreshold = safeGetInt(stringConfiguration, DefaultConstantValues.Threshold.REPORT_BUG_THRESHOLD
                .getValue());

    }

    private int safeGetInt(String stringConfiguration, int defaultValue) {

        try {
            return Integer.parseInt(stringConfiguration);
        } catch (NumberFormatException e) {
            MantisCorePlugin.log(new Status(Status.WARNING, MantisCorePlugin.PLUGIN_ID,
                    "Failed parsing config option value " + stringConfiguration + ". Using default value.", e));
            return defaultValue;
        }
    }

    private void cacheDeveloperThreshold(String stringConfiguration) {

        developerThreshold = safeGetInt(stringConfiguration,
                DefaultConstantValues.Threshold.UPDATE_BUG_ASSIGN_THRESHOLD.getValue());

    }

    private void cacheProjectCategories(int projectId, String[] projectCategories) {

        List<MantisProjectCategory> categories = new ArrayList<MantisProjectCategory>();

        // the SOAP API returns just the names, so we assign arbitrary ids
        int id = 0;

        for (String categoryName : projectCategories)
            categories.add(new MantisProjectCategory(categoryName, ++id));

        this.categoriesByProjectId.put(projectId, categories);

    }

    private void cacheViewStates(ObjectRef[] viewStates) {

        this.viewStates = new ArrayList<MantisViewState>();

        for (ObjectRef viewState : viewStates)
            this.viewStates.add(new MantisViewState(viewState.getName(), viewState.getId().intValue()));

    }

    private void cacheEtas(ObjectRef[] etas) {

        this.etas = new ArrayList<MantisETA>();

        for (ObjectRef eta : etas)
            this.etas.add(new MantisETA(eta.getName(), eta.getId().intValue()));

    }

    private void cacheProjections(ObjectRef[] projections) {

        this.projections = new ArrayList<MantisProjection>();

        for (ObjectRef projection : projections)
            this.projections.add(new MantisProjection(projection.getName(), projection.getId().intValue()));

    }

    private void cacheReproducibilites(ObjectRef[] reproducibilities) {

        this.reproducibilities = new ArrayList<MantisReproducibility>();

        for (ObjectRef reproducibility : reproducibilities)
            this.reproducibilities.add(new MantisReproducibility(reproducibility.getName(), reproducibility.getId()
                    .intValue()));

    }

    private void cacheResolutions(ObjectRef[] resolutions) {

        this.resolutions = new ArrayList<MantisResolution>();

        for (ObjectRef resolution : resolutions)
            this.resolutions.add(new MantisResolution(resolution.getName(), resolution.getId().intValue()));

    }

    private void cacheSeverities(ObjectRef[] severities) {

        this.severities = new ArrayList<MantisSeverity>();

        for (ObjectRef severity : severities)
            this.severities.add(new MantisSeverity(severity.getName(), severity.getId().intValue()));

    }

    private void cacheStatuses(ObjectRef[] statuses) {

        this.statuses = new ArrayList<MantisTicketStatus>();

        for (ObjectRef status : statuses)
            this.statuses.add(new MantisTicketStatus(status.getName(), status.getId().intValue()));

    }

    private void cachePriorities(ObjectRef[] prios) {

        priorities = new ArrayList<MantisPriority>();
        for (ObjectRef prio : prios)
            priorities.add(new MantisPriority(prio.getName(), prio.getId().intValue()));

    }

    private void cacheResolvedStatus(String resolvedStatus) {

        this.resolvedStatus = Integer.parseInt(resolvedStatus);

    }

    private void cacheDueDateViewThreshold(String viewThreshold) {

        dueDateViewThreshold = Integer.parseInt(viewThreshold);
    }

    private void cacheDueDateUpdateThreshold(String updateThreshold) {

        dueDateUpdateThreshold = Integer.parseInt(updateThreshold);
    }

    private void cacheProjects(ProjectData[] projectData) {

        // TODO Test sub-projects
        projects.clear();

        for (ProjectData pd : projectData) {
            projects.add(new MantisProject(pd.getName(), pd.getId().intValue()));

            for (ProjectData subProject : pd.getSubprojects())
                projects.add(new MantisProject(subProject.getName(), subProject.getId().intValue()));
        }

    }

    private void cacheFilters(int projectId, FilterData[] projectFilters) {

        List<MantisProjectFilter> filters = new ArrayList<MantisProjectFilter>();
        for (FilterData filter : projectFilters)
            filters.add(new MantisProjectFilter(filter.getName(), filter.getId().intValue()));

        projectFiltersById.put(projectId, filters);

    }

    private void cacheProjectCustomFields(int projectId, CustomFieldDefinitionData[] customFieldData) {

        List<MantisCustomField> customFields = new ArrayList<MantisCustomField>();

        for (CustomFieldDefinitionData data : customFieldData)
            customFields.add(MantisConverter.convert(data));

        customFieldsByProjectId.put(projectId, customFields);

    }

    private void cacheRepositoryVersion(String version) throws MantisException {

        this.repositoryVersion = RepositoryVersion.fromVersionString(version);
    }

    public RepositoryVersion getRepositoryVersion() {

        return repositoryVersion;
    }

    public int getProjectId(String projectName) throws MantisException {

        for ( MantisProject mantisProject : projects)
            if ( mantisProject.getName().equals(projectName))
                return mantisProject.getValue();
        
        throw new MantisException("No project with the name " + projectName + " .");
    }
    
    public int getProjectFilterId(int projectId, String filterName) throws MantisException {

        List<MantisProjectFilter> filters = projectFiltersById.get(projectId);
        
        for( MantisProjectFilter filter : filters)
            if ( filter.getName().equals(filterName))
                return filter.getValue();
        
        throw new MantisException("No filter with name " + filterName + " for project with id " + projectId + " .");
        
    }

    public MantisResolution getResolution(int intValue) throws MantisException {

        for ( MantisResolution resolution : resolutions)
            if ( resolution.getValue() == intValue)
                return resolution;
        
        throw new MantisException("No resoution with id " + intValue + " .");
    }

}
