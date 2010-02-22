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

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

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
import com.itsolut.mantis.core.model.MantisTicketAttribute;
import com.itsolut.mantis.core.model.MantisTicketStatus;
import com.itsolut.mantis.core.model.MantisVersion;
import com.itsolut.mantis.core.model.MantisViewState;

/**
 * Holds the cached information for a complete Mantis installations.
 * 
 * <p>
 * All access calls should be guarded by a call to {@link #refreshIfNeeded(IProgressMonitor)}, since
 * this insures that
 * <ol>
 * <li>The cache is populated</li>
 * <li>Multi-threaded access is properly synchronised</li>
 * </ol>
 * </p>
 * 
 * @author Robert Munteanu
 * 
 */
public class MantisCache {

    static final int BUILT_IN_PROJECT_TASKS_FILTER_ID = -1;

    static final String BUILT_IN_PROJECT_TASKS_FILTER_FORMAT = "[Built-in] Latest %s tasks";

    private static final String SUBPROJECT_SEPARATOR = " » ";

    private static final String RESOLVED_STATUS_THRESHOLD = "bug_resolved_status_threshold";

    private static final String REPORTER_THRESHOLD = "report_bug_threshold";

    private static final String DEVELOPER_THRESHOLD = "update_bug_assign_threshold";

    private static final String DUE_DATE_VIEW_THRESOLD = "due_date_view_threshold";

    private static final String DUE_DATE_UPDATE_THRESOLD = "due_date_update_threshold";
    
    private static final String TIME_TRACKING_ENABLED = "time_tracking_enabled";
    
    static final int ACCESS_LEVEL_NOBODY = 100;

    private final Object sync = new Object();

    private MantisAxis1SOAPClient soapClient;

    private MantisCacheData cacheData = new MantisCacheData();

    private final NumberFormat formatter = new DecimalFormat("#.#");


    public MantisCache(MantisAxis1SOAPClient soapClient) {

        this.soapClient = soapClient;
    }

    public void setProjects(List<MantisProject> projects) {

        this.cacheData.projects = projects;
    }

    public List<MantisProject> getProjects() {

        return cacheData.projects;
    }

    void refreshIfNeeded(IProgressMonitor progressMonitor, String repositoryUrl) throws MantisException {

        synchronized (sync) {
            if (cacheData.lastUpdate == 0)
                refresh(progressMonitor, repositoryUrl);
        }

    }

    void refresh(IProgressMonitor monitor, String repositoryUrl) throws MantisException {

        synchronized (sync) {

            long start = System.currentTimeMillis();

            SubMonitor subMonitor = SubMonitor.convert(monitor);

            try {
                cacheProjects(soapClient.getProjectData(monitor));

                subMonitor.beginTask("Refreshing repository configuration", cacheData.projects.size() * 6 + 15);

                cacheReporterThreshold(soapClient.getStringConfiguration(monitor, REPORTER_THRESHOLD));
                Policy.advance(subMonitor, 1);

                cacheDeveloperThreshold(soapClient.getStringConfiguration(monitor, DEVELOPER_THRESHOLD));
                Policy.advance(subMonitor, 1);

                cacheDueDateViewThreshold(soapClient.getStringConfiguration(monitor, DUE_DATE_VIEW_THRESOLD));
                Policy.advance(subMonitor, 1);

                cacheDueDateUpdateThreshold(soapClient.getStringConfiguration(monitor, DUE_DATE_UPDATE_THRESOLD));
                Policy.advance(subMonitor, 1);

                cacheTimeTrackingEnabled(soapClient.getStringConfiguration(monitor, TIME_TRACKING_ENABLED));
                Policy.advance(subMonitor, 1);
                
                for (MantisProject project : cacheData.projects) {
                    cacheFilters(project.getValue(), soapClient.getProjectFilters(project.getValue(), monitor));
                    Policy.advance(subMonitor, 1);

                    cacheProjectCustomFields(project.getValue(), soapClient.getProjectCustomFields(project.getValue(),
                            monitor));
                    Policy.advance(subMonitor, 1);

                    cacheProjectCategories(project.getValue(), soapClient.getProjectCategories(project.getValue(),
                            monitor));
                    Policy.advance(subMonitor, 1);

                    cacheProjectDevelopers(project.getValue(), soapClient.getProjectUsers(project.getValue(),
                            cacheData.developerThreshold, monitor));
                    Policy.advance(subMonitor, 1);

                    try {
                        cacheProjectReporters(project.getValue(), soapClient.getProjectUsers(project.getValue(),
                                cacheData.reporterThreshold, monitor));
                    } catch (MantisException e) {
                        if (!cacheData.reportersByProjectId.containsKey(project.getValue())) {
                            cacheData.reportersByProjectId.put(project.getValue(), new ArrayList<String>(
                                    cacheData.developersByProjectId.get(project.getValue())));
                            MantisCorePlugin.log(new Status(Status.WARNING, MantisCorePlugin.PLUGIN_ID,
                                    "Failed retrieving reporter information, using developers list for reporters.", e));
                        } else {
                            MantisCorePlugin.log(new Status(Status.WARNING, MantisCorePlugin.PLUGIN_ID,
                                    "Failed retrieving reporter information, using previously loaded values.", e));
                        }
                    }
                    Policy.advance(subMonitor, 1);

                    cacheProjectVersions(project.getValue(), soapClient.getProjectVersions(project.getValue(), monitor));
                    Policy.advance(subMonitor, 1);
                }

                cacheResolvedStatus(soapClient.getStringConfiguration(monitor, RESOLVED_STATUS_THRESHOLD));
                Policy.advance(subMonitor, 1);

                cacheRepositoryVersion(soapClient.getVersion(monitor));
                Policy.advance(subMonitor, 1);

                cachePriorities(soapClient.getPriorities(monitor));
                Policy.advance(subMonitor, 1);

                cacheStatuses(soapClient.getStatuses(monitor));
                Policy.advance(subMonitor, 1);

                cacheSeverities(soapClient.getSeverities(monitor));
                Policy.advance(subMonitor, 1);

                cacheResolutions(soapClient.getResolutions(monitor));
                Policy.advance(subMonitor, 1);

                cacheReproducibilites(soapClient.getReproducibilities(monitor));
                Policy.advance(subMonitor, 1);

                cacheProjections(soapClient.getProjections(monitor));
                Policy.advance(subMonitor, 1);

                cacheEtas(soapClient.getEtas(monitor));
                Policy.advance(subMonitor, 1);

                cacheViewStates(soapClient.getViewStates(monitor));
                Policy.advance(subMonitor, 1);

                cacheData.lastUpdate = System.currentTimeMillis();
            } finally {
                subMonitor.done();
                MantisCorePlugin.log(new Status(Status.INFO, MantisCorePlugin.PLUGIN_ID, "Repository sync for "
                        + repositoryUrl + " complete in " + format(start) + " seconds."));
            }

        }
    }

    private void cacheTimeTrackingEnabled(String stringValue) {
        
        cacheData.timeTrackingEnabled = parseMantisBoolean(stringValue);
    }

    private boolean parseMantisBoolean(String stringValue) {

        return "1".equals(stringValue);
    }

    private void cacheDueDateUpdateThreshold(String stringValue) {

        cacheData.dueDateUpdateThreshold = safeGetInt(stringValue, ACCESS_LEVEL_NOBODY);
    }

    private void cacheDueDateViewThreshold(String stringValue) {

        cacheData.dueDateViewThreshold = safeGetInt(stringValue, ACCESS_LEVEL_NOBODY);
    }

    private String format(long start) {

        double millis = (System.currentTimeMillis() - start) / (double) 1000;
        return formatter.format(millis);

    }

    private void cacheProjectVersions(int value, ProjectVersionData[] projectVerions) {

        List<MantisVersion> projectVersions = new ArrayList<MantisVersion>();

        for (ProjectVersionData version : projectVerions)
            projectVersions.add(MantisConverter.convert(version));

        cacheData.versionsByProjectId.put(value, projectVersions);

    }

    private void cacheProjectReporters(int projectId, AccountData[] projectUsers) {

        List<String> reporters = new ArrayList<String>();

        for (AccountData accountData : projectUsers)
            reporters.add(accountData.getName());

        cacheData.reportersByProjectId.put(projectId, reporters);

    }

    private void cacheProjectDevelopers(int projectId, AccountData[] projectDevelopers) {

        List<String> developers = new ArrayList<String>();

        for (AccountData accountData : projectDevelopers)
            developers.add(accountData.getName());

        cacheData.developersByProjectId.put(projectId, developers);

    }

    private void cacheReporterThreshold(String stringConfiguration) {

        cacheData.reporterThreshold = safeGetInt(stringConfiguration,
                DefaultConstantValues.Threshold.REPORT_BUG_THRESHOLD.getValue());

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

        cacheData.developerThreshold = safeGetInt(stringConfiguration,
                DefaultConstantValues.Threshold.UPDATE_BUG_ASSIGN_THRESHOLD.getValue());

    }

    private void cacheProjectCategories(int projectId, String[] projectCategories) {

        List<MantisProjectCategory> categories = new ArrayList<MantisProjectCategory>();

        // the SOAP API returns just the names, so we assign arbitrary ids
        int id = 0;

        for (String categoryName : projectCategories)
            categories.add(new MantisProjectCategory(categoryName, ++id));

        this.cacheData.categoriesByProjectId.put(projectId, categories);

    }

    private void cacheViewStates(ObjectRef[] viewStates) {

        this.cacheData.viewStates = new ArrayList<MantisViewState>();

        for (ObjectRef viewState : viewStates)
            this.cacheData.viewStates.add(new MantisViewState(viewState.getName(), viewState.getId().intValue()));

    }

    private void cacheEtas(ObjectRef[] etas) {

        this.cacheData.etas = new ArrayList<MantisETA>();

        for (ObjectRef eta : etas)
            this.cacheData.etas.add(new MantisETA(eta.getName(), eta.getId().intValue()));

    }

    private void cacheProjections(ObjectRef[] projections) {

        this.cacheData.projections = new ArrayList<MantisProjection>();

        for (ObjectRef projection : projections)
            this.cacheData.projections.add(new MantisProjection(projection.getName(), projection.getId().intValue()));

    }

    private void cacheReproducibilites(ObjectRef[] reproducibilities) {

        this.cacheData.reproducibilities = new ArrayList<MantisReproducibility>();

        for (ObjectRef reproducibility : reproducibilities)
            this.cacheData.reproducibilities.add(new MantisReproducibility(reproducibility.getName(), reproducibility
                    .getId().intValue()));

    }

    private void cacheResolutions(ObjectRef[] resolutions) {

        this.cacheData.resolutions = new ArrayList<MantisResolution>();

        for (ObjectRef resolution : resolutions)
            this.cacheData.resolutions.add(new MantisResolution(resolution.getName(), resolution.getId().intValue()));

    }

    private void cacheSeverities(ObjectRef[] severities) {

        this.cacheData.severities = new ArrayList<MantisSeverity>();

        for (ObjectRef severity : severities)
            this.cacheData.severities.add(new MantisSeverity(severity.getName(), severity.getId().intValue()));

    }

    private void cacheStatuses(ObjectRef[] statuses) {

        this.cacheData.statuses = new ArrayList<MantisTicketStatus>();

        for (ObjectRef status : statuses)
            this.cacheData.statuses.add(new MantisTicketStatus(status.getName(), status.getId().intValue()));

    }

    private void cachePriorities(ObjectRef[] prios) {

        cacheData.priorities = new ArrayList<MantisPriority>();
        for (ObjectRef prio : prios)
            cacheData.priorities.add(new MantisPriority(prio.getName(), prio.getId().intValue()));

    }

    private void cacheResolvedStatus(String resolvedStatus) {

        this.cacheData.resolvedStatus = Integer.parseInt(resolvedStatus);

    }

    private void cacheProjects(ProjectData[] projectData) {

        cacheData.projects.clear();

        for (ProjectData pd : projectData) {
            cacheData.projects.add(new MantisProject(pd.getName(), pd.getName(), pd.getId().intValue()));

            addSubProjectsIfApplicable(pd, pd.getName());
        }

    }

    private void addSubProjectsIfApplicable(ProjectData pd, String accumulatedName) {

        if (pd.getSubprojects() == null || pd.getSubprojects().length == 0)
            return;

        for (ProjectData subProject : pd.getSubprojects()) {

            String projectName = accumulatedName + SUBPROJECT_SEPARATOR + subProject.getName();

            cacheData.projects.add(new MantisProject(subProject.getName(), projectName, subProject.getId().intValue()));

            addSubProjectsIfApplicable(subProject, projectName);
        }

    }

    private void cacheFilters(int projectId, FilterData[] projectFilters) throws MantisException {

        List<MantisProjectFilter> filters = new ArrayList<MantisProjectFilter>();

        filters.add(addDefaultFilters(projectId));

        for (FilterData filter : projectFilters)
            filters.add(new MantisProjectFilter(filter.getName(), filter.getId().intValue()));

        cacheData.projectFiltersById.put(projectId, filters);

    }

    private MantisProjectFilter addDefaultFilters(int projectId) throws MantisException {

        String projectDisplayName = getProjectById(projectId).getDisplayName();
        String filterDisplayName = String.format(BUILT_IN_PROJECT_TASKS_FILTER_FORMAT, projectDisplayName);

        return new MantisProjectFilter(filterDisplayName, BUILT_IN_PROJECT_TASKS_FILTER_ID);
    }

    private void cacheProjectCustomFields(int projectId, CustomFieldDefinitionData[] customFieldData) {

        List<MantisCustomField> customFields = new ArrayList<MantisCustomField>();

        for (CustomFieldDefinitionData data : customFieldData)
            customFields.add(MantisConverter.convert(data));

        cacheData.customFieldsByProjectId.put(projectId, customFields);

    }

    private void cacheRepositoryVersion(String version) throws MantisException {

        this.cacheData.repositoryVersion = RepositoryVersion.fromVersionString(version);
    }

    public RepositoryVersion getRepositoryVersion() {

        return cacheData.repositoryVersion;
    }

    public int getProjectId(String projectName) throws MantisException {

        for (MantisProject mantisProject : cacheData.projects)
            if (mantisProject.getName().equals(projectName))
                return mantisProject.getValue();

        throw new MantisException("No project with the name " + projectName + " .");
    }

    public int getProjectFilterId(int projectId, String filterName) throws MantisException {

        List<MantisProjectFilter> filters = cacheData.projectFiltersById.get(projectId);

        for (MantisProjectFilter filter : filters)
            if (filter.getName().equals(filterName))
                return filter.getValue();

        throw new MantisException("No filter with name " + filterName + " for project with id " + projectId + " .");

    }

    public MantisResolution getResolution(int intValue) throws MantisException {

        for (MantisResolution resolution : cacheData.resolutions)
            if (resolution.getValue() == intValue)
                return resolution;

        throw new MantisException("No resolution with id " + intValue + " .");
    }

    public MantisPriority getPriority(int intValue) throws MantisException {

        for (MantisPriority priority : cacheData.priorities)
            if (priority.getValue() == intValue)
                return priority;

        throw new MantisException("No priority with id " + intValue + " .");
    }

    public MantisSeverity getSeverity(int intValue) throws MantisException {

        for (MantisSeverity severity : cacheData.severities)
            if (severity.getValue() == intValue)
                return severity;

        throw new MantisException("No severity with id " + intValue + " .");
    }

    public MantisTicketStatus getStatus(int intValue) throws MantisException {

        for (MantisTicketStatus status : cacheData.statuses)
            if (status.getValue() == intValue)
                return status;

        throw new MantisException("No status with id " + intValue + " .");
    }

    public ObjectRef getProjectAsObjectRef(String projectName) throws MantisException {

        return toObjectRef(getProjectByName(projectName));

    }

    private ObjectRef toObjectRef(MantisTicketAttribute attribute) {

        return new ObjectRef(BigInteger.valueOf(attribute.getValue()), attribute.getName());
    }

    public ObjectRef getSeverityAsObjectRef(String severityName) throws MantisException {

        for (MantisSeverity severity : cacheData.severities)
            if (severity.getName().equals(severityName))
                return toObjectRef(severity);

        throw new MantisException("No severity with name " + severityName + " .");
    }

    public ObjectRef getResolutionAsObjectRef(String resolutionName) throws MantisException {

        for (MantisResolution reslution : cacheData.resolutions)
            if (reslution.getName().equals(resolutionName))
                return toObjectRef(reslution);

        throw new MantisException("No resolution with name " + resolutionName + " .");
    }

    public ObjectRef getPriorityAsObjectRef(String priorityName) throws MantisException {

        for (MantisPriority priority : cacheData.priorities)
            if (priority.getName().equals(priorityName))
                return toObjectRef(priority);

        throw new MantisException("No priority with name " + priorityName + " .");
    }

    public ObjectRef getReproducibilityAsObjectRef(String reproducibilityName) throws MantisException {

        for (MantisReproducibility reproducibility : cacheData.reproducibilities)
            if (reproducibility.getName().equals(reproducibilityName))
                return toObjectRef(reproducibility);

        throw new MantisException("No reproducibility with name " + reproducibilityName + " .");
    }

    public ObjectRef getProjectionAsObjectRef(String projectionName) throws MantisException {

        for (MantisProjection projection : cacheData.projections)
            if (projection.getName().equals(projectionName))
                return toObjectRef(projection);

        throw new MantisException("No projection with name " + projectionName + " .");
    }

    public ObjectRef getEtaAsObjectRef(String etaName) throws MantisException {

        for (MantisETA eta : cacheData.etas)
            if (eta.getName().equals(etaName))
                return toObjectRef(eta);

        throw new MantisException("No eta with name " + etaName + " .");
    }

    public ObjectRef getViewStateAsObjectRef(String viewStateName) throws MantisException {

        for (MantisViewState viewState : cacheData.viewStates)
            if (viewState.getName().equals(viewStateName))
                return toObjectRef(viewState);

        throw new MantisException("No viewState with name " + viewStateName + " .");
    }

    public ObjectRef getStatusAsObjectRef(String statusName) throws MantisException {

        return toObjectRef(getStatusByName(statusName));
    }

    public MantisCustomField getCustomFieldByProjectIdAndFieldName(int projectId, String customFieldName)
            throws MantisException {

        for (MantisCustomField customField : getCustomFieldsByProjectId(projectId))
            if (customField.getName().equals(customFieldName))
                return customField;

        throw new MantisException("No custom field with name " + customFieldName + " for the project with id "
                + projectId + " .");
    }

    private List<MantisCustomField> getCustomFieldsByProjectId(int projectId) throws MantisException {

        List<MantisCustomField> projectCustomFields = cacheData.customFieldsByProjectId.get(projectId);
        if (projectCustomFields == null)
            throw new MantisException("No custom fields for the project with id = " + projectId + " .");
        return projectCustomFields;
    }

    public int getResolvedStatus() {

        return cacheData.resolvedStatus;
    }

    public MantisTicketStatus getStatusByName(String statusName) throws MantisException {

        for (MantisTicketStatus status : cacheData.statuses)
            if (status.getName().equals(statusName))
                return status;

        throw new MantisException("No status with name " + statusName + " .");
    }

    public List<MantisCustomField> getCustomFieldsByProjectName(String projectName) throws MantisException {

        return getCustomFieldsByProjectId(getProjectId(projectName));
    }

    public MantisSeverity[] getSeverities() {

        return cacheData.severities.toArray(new MantisSeverity[cacheData.severities.size()]);
    }

    public MantisResolution[] getTicketResolutions() {

        return cacheData.resolutions.toArray(new MantisResolution[cacheData.resolutions.size()]);
    }

    public MantisTicketStatus[] getTicketStatus() {

        return cacheData.statuses.toArray(new MantisTicketStatus[cacheData.statuses.size()]);
    }

    public MantisPriority[] getPriorities() {

        return cacheData.priorities.toArray(new MantisPriority[cacheData.priorities.size()]);
    }

    public MantisReproducibility[] getReproducibility() {

        return cacheData.reproducibilities.toArray(new MantisReproducibility[cacheData.reproducibilities.size()]);
    }

    public MantisProjection[] getProjection() {

        return cacheData.projections.toArray(new MantisProjection[cacheData.projections.size()]);
    }

    public MantisETA[] getETA() {

        return cacheData.etas.toArray(new MantisETA[cacheData.etas.size()]);
    }

    public MantisViewState[] getViewState() {

        return cacheData.viewStates.toArray(new MantisViewState[cacheData.viewStates.size()]);
    }

    public MantisProject getProjectByName(String projectName) throws MantisException {

        for (MantisProject project : cacheData.projects)
            if (project.getName().equals(projectName))
                return project;

        throw new MantisException("No project with name " + projectName + " .");
    }

    public MantisProject getProjectById(int projectId) throws MantisException {

        for (MantisProject project : cacheData.projects)
            if (project.getValue() == projectId)
                return project;

        throw new MantisException("No project with id " + projectId + " .");
    }

    public List<MantisProjectFilter> getProjectFilters(int projectId) throws MantisException {

        List<MantisProjectFilter> filters = cacheData.projectFiltersById.get(projectId);

        if (filters == null)
            throw new MantisException("No filters for project with id " + projectId + " .");

        return filters;
    }

    public List<MantisProjectCategory> getProjectCategories(String projectName) throws MantisException {

        int projectId = getProjectId(projectName);
        List<MantisProjectCategory> categories = cacheData.categoriesByProjectId.get(projectId);

        if (categories == null)
            throw new MantisException("No categories for project with id " + projectId + " .");

        return categories;
    }

    public String[] getDevelopersByProjectName(String projectName, IProgressMonitor monitor) throws MantisException {

        int projectId = getProjectId(projectName);

        List<String> developers = cacheData.developersByProjectId.get(projectId);

        if (developers == null)
            throw new MantisException("No developers for project with id " + projectId + " ");

        return developers.toArray(new String[developers.size()]);

    }

    public String[] getUsersByProjectName(String projectName, IProgressMonitor monitor) throws MantisException {

        int projectId = getProjectId(projectName);

        List<String> reporters = cacheData.reportersByProjectId.get(projectId);

        if (reporters == null)
            throw new MantisException("No reporters for project with id " + projectId + " ");

        return reporters.toArray(new String[reporters.size()]);
    }

    public MantisVersion[] getVersionsByProjectName(String projectName) throws MantisException {

        int projectId = getProjectId(projectName);

        List<MantisVersion> versions = cacheData.versionsByProjectId.get(projectId);

        if (versions == null)
            throw new MantisException("No versions for project with id " + projectId + " ");

        return versions.toArray(new MantisVersion[versions.size()]);
    }

    MantisCacheData getCacheData() {

        synchronized (sync) {
            return cacheData;
        }

    }

    void setCacheData(MantisCacheData cacheData) {

        synchronized (sync) {
            this.cacheData = cacheData;
        }

    }

    /**
     * Since for large user counts the reporter retrieval fails, we provide a hook for registering
     * additional reporter users as they are discovered, e.g. in IssueData
     * 
     * @param projectId
     *            the project id
     * @param reporterName
     *            the name of the reporter
     */
    void registerAdditionalReporter(int projectId, String reporterName) {

        if (cacheData.reportersByProjectId.get(projectId).contains(reporterName))
            return;

        cacheData.reportersByProjectId.get(projectId).add(reporterName);

    }

    boolean dueDateIsEnabled() {
        
        return cacheData.dueDateViewThreshold < ACCESS_LEVEL_NOBODY && cacheData.dueDateUpdateThreshold < ACCESS_LEVEL_NOBODY;
    }
}
