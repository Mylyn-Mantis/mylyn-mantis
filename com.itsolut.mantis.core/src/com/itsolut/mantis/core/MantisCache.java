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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import biz.futureware.mantis.rpc.soap.client.AccountData;
import biz.futureware.mantis.rpc.soap.client.CustomFieldDefinitionData;
import biz.futureware.mantis.rpc.soap.client.FilterData;
import biz.futureware.mantis.rpc.soap.client.ObjectRef;
import biz.futureware.mantis.rpc.soap.client.ProjectData;
import biz.futureware.mantis.rpc.soap.client.ProjectVersionData;

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
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.model.MantisTicketAttribute;
import com.itsolut.mantis.core.model.MantisTicketStatus;
import com.itsolut.mantis.core.model.MantisUser;
import com.itsolut.mantis.core.model.MantisVersion;
import com.itsolut.mantis.core.model.MantisViewState;
import com.itsolut.mantis.core.soap.MantisConverter;

/**
 * Holds the cached information for a complete Mantis installations.
 * 
 * @author Robert Munteanu
 * 
 */
public class MantisCache {

    public static final int BUILT_IN_PROJECT_TASKS_FILTER_ID = -1;

    static final String BUILT_IN_PROJECT_TASKS_FILTER_FORMAT = "[Built-in] Latest %s tasks";

    private MantisCacheData cacheData = new MantisCacheData();

    public void setProjects(List<MantisProject> projects) {

        this.cacheData.setProjects( projects );
    }

    public List<MantisProject> getProjects() {

        return cacheData.getProjects();
    }

    public void cacheTimeTrackingEnabled(String stringValue) {
        
        cacheData.timeTrackingEnabled = parseMantisBoolean(stringValue);
    }

    private boolean parseMantisBoolean(String stringValue) {

        return "1".equals(stringValue);
    }

    public void cacheDueDateUpdateThreshold(int threshold) {

        cacheData.dueDateUpdateThreshold = threshold;
    }

    public void cacheDueDateViewThreshold(int threshold) {

        cacheData.dueDateViewThreshold = threshold;
    }
    
    public void cacheAssignedStatus(int status) {

        cacheData.bugAssignedStatus = status;
    }

    public void cacheSubmitStatus(int status) {
        
        cacheData.bugSubmitStatus = status;
    }

    public void cacheProjectVersions(int value, ProjectVersionData[] projectVerions) {

        List<MantisVersion> projectVersions = new ArrayList<MantisVersion>();

        for (ProjectVersionData version : projectVerions)
            projectVersions.add(MantisConverter.convert(version));

        cacheData.versionsByProjectId.put(value, projectVersions);

    }

    public void cacheProjectReporters(int projectId, AccountData[] projectUsers) {

        List<MantisUser> reporters = cacheUsers0(projectUsers);

        cacheData.getReportersByProjectId().putAll(projectId, reporters);
    }

    private List<MantisUser> cacheUsers0(AccountData[] projectUsers) {

        List<MantisUser> reporters = new ArrayList<MantisUser>();

        for (AccountData accountData : projectUsers) {
            
            String username = accountData.getName();
            MantisUser user = new MantisUser(accountData.getId().intValue(), username, accountData.getReal_name(), accountData.getEmail());
        
            cacheData.allUsers.put(username, user);
            reporters.add(user);
        }
        
        return reporters;
    }

    public void cacheProjectDevelopers(int projectId, AccountData[] projectDevelopers) {

        List<MantisUser> developers = cacheUsers0(projectDevelopers);

        cacheData.getDevelopersByProjectId().putAll(projectId, developers);
    }

    public void cacheReporterThreshold(int threshold) {

        cacheData.setReporterThreshold(threshold);
    }

    public void cacheDeveloperThreshold(int threshold) {

        cacheData.setDeveloperThreshold(threshold);
    }

    public void cacheProjectCategories(int projectId, String[] projectCategories) {

        List<MantisProjectCategory> categories = new ArrayList<MantisProjectCategory>();

        // the SOAP API returns just the names, so we assign arbitrary ids
        int id = 0;

        for (String categoryName : projectCategories)
            categories.add(new MantisProjectCategory(categoryName, ++id));

        this.cacheData.categoriesByProjectId.put(projectId, categories);

    }

    public void cacheViewStates(ObjectRef[] viewStates) {

        this.cacheData.viewStates = new ArrayList<MantisViewState>();

        for (ObjectRef viewState : viewStates)
            this.cacheData.viewStates.add(new MantisViewState(viewState.getName(), viewState.getId().intValue()));

    }

    public void cacheEtas(ObjectRef[] etas) {

        this.cacheData.etas = new ArrayList<MantisETA>();

        for (ObjectRef eta : etas)
            this.cacheData.etas.add(new MantisETA(eta.getName(), eta.getId().intValue()));

    }

    public void cacheProjections(ObjectRef[] projections) {

        this.cacheData.projections = new ArrayList<MantisProjection>();

        for (ObjectRef projection : projections)
            this.cacheData.projections.add(new MantisProjection(projection.getName(), projection.getId().intValue()));

    }

    public void cacheReproducibilites(ObjectRef[] reproducibilities) {

        this.cacheData.reproducibilities = new ArrayList<MantisReproducibility>();

        for (ObjectRef reproducibility : reproducibilities)
            this.cacheData.reproducibilities.add(new MantisReproducibility(reproducibility.getName(), reproducibility
                    .getId().intValue()));

    }

    public void cacheResolutions(ObjectRef[] resolutions) {

        this.cacheData.resolutions = new ArrayList<MantisResolution>();

        for (ObjectRef resolution : resolutions)
            this.cacheData.resolutions.add(new MantisResolution(resolution.getName(), resolution.getId().intValue()));

    }

    public void cacheSeverities(ObjectRef[] severities) {

        this.cacheData.severities = new ArrayList<MantisSeverity>();

        for (ObjectRef severity : severities)
            this.cacheData.severities.add(new MantisSeverity(severity.getName(), severity.getId().intValue()));

    }

    public void cacheStatuses(ObjectRef[] statuses) {

        this.cacheData.statuses = new ArrayList<MantisTicketStatus>();

        for (ObjectRef status : statuses)
            this.cacheData.statuses.add(new MantisTicketStatus(status.getName(), status.getId().intValue()));

    }

    public void cachePriorities(ObjectRef[] prios) {

        cacheData.priorities = new ArrayList<MantisPriority>();
        for (ObjectRef prio : prios)
            cacheData.priorities.add(new MantisPriority(prio.getName(), prio.getId().intValue()));

    }

    public void cacheResolvedStatus(String resolvedStatus) {

        this.cacheData.resolvedStatus = Integer.parseInt(resolvedStatus);

    }

    public void cacheProjects(ProjectData[] projectData) {

        cacheData.getProjects().clear();

        for (ProjectData project : projectData) {
            cacheData.getProjects().add(new MantisProject(project.getName(),project.getId().intValue()));

            addSubProjectsIfApplicable(project);
        }

    }

    private void addSubProjectsIfApplicable(ProjectData pd) {

        if (pd.getSubprojects() == null || pd.getSubprojects().length == 0)
            return;

        for (ProjectData subProject : pd.getSubprojects()) {

            cacheData.getProjects().add(new MantisProject(subProject.getName(), subProject.getId().intValue(), pd.getId().intValue()));

            addSubProjectsIfApplicable(subProject);
        }

    }

    public void cacheFilters(int projectId, FilterData[] projectFilters) throws MantisException {

        List<MantisProjectFilter> filters = new ArrayList<MantisProjectFilter>();

        filters.add(addDefaultFilters(projectId));

        for (FilterData filter : projectFilters)
            filters.add(new MantisProjectFilter(filter.getName(), filter.getId().intValue(), filter.getUrl()));

        cacheData.projectFiltersById.put(projectId, filters);

    }

    private MantisProjectFilter addDefaultFilters(int projectId) throws MantisException {

        String projectDisplayName = getProjectById(projectId).getName();
        String filterDisplayName = String.format(BUILT_IN_PROJECT_TASKS_FILTER_FORMAT, projectDisplayName);

        return new MantisProjectFilter(filterDisplayName, BUILT_IN_PROJECT_TASKS_FILTER_ID);
    }

    public void cacheProjectCustomFields(int projectId, CustomFieldDefinitionData[] customFieldData) {

        List<MantisCustomField> customFields = new ArrayList<MantisCustomField>();

        for (CustomFieldDefinitionData data : customFieldData)
            customFields.add(MantisConverter.convert(data));

        cacheData.customFieldsByProjectId.put(projectId, customFields);

    }

    public void cacheRepositoryVersion(String version) throws MantisException {

        this.cacheData.repositoryVersion = RepositoryVersion.fromVersionString(version);
    }

    public void cacheDefaultAttributeValue(Key attribute, int readValue) {

        cacheData.putDefaultValueForAttribute(attribute, readValue);
    }
    
    public RepositoryVersion getRepositoryVersion() {

        return cacheData.repositoryVersion;
    }

    public int getProjectId(String projectName) throws MantisException {

        for (MantisProject mantisProject : cacheData.getProjects())
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

    public MantisReproducibility getReproducibility(int reproducibilityId) throws MantisException {
        
        for (MantisReproducibility reproducibility : cacheData.reproducibilities)
            if (reproducibility.getValue() == reproducibilityId )
                return reproducibility;
        
        throw new MantisException("No reproducibility with id " + reproducibilityId + " .");
    }

    public MantisProjection getProjection(int projectionId) throws MantisException {
        
        for (MantisProjection projection : cacheData.projections)
            if (projection.getValue() == projectionId )
                return projection;
        
        throw new MantisException("No projection with id " + projectionId + " .");
    }

    public MantisETA getETA(int etaId) throws MantisException {
        
        for (MantisETA eta : cacheData.etas)
            if (eta.getValue() == etaId )
                return eta;
        
        throw new MantisException("No eta with id " + etaId + " .");
    }

    public MantisViewState getViewState(int viewStateId) throws MantisException {
        
        for (MantisViewState viewState : cacheData.viewStates)
            if (viewState.getValue() == viewStateId)
                return viewState;
        
        throw new MantisException("No viewState with name " + viewStateId + " .");
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

        for (MantisProject project : cacheData.getProjects())
            if (project.getName().equals(projectName))
                return project;

        throw new MantisException("No project with name " + projectName + " .");
    }

    public MantisProject getProjectById(int projectId) throws MantisException {

        for (MantisProject project : cacheData.getProjects())
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

    public MantisUser[] getDevelopersByProjectName(String projectName, IProgressMonitor monitor) throws MantisException {

        int projectId = getProjectId(projectName);

        List<MantisUser> developers = cacheData.getDevelopersByProjectId().get(projectId);

        return developers.toArray(new MantisUser[developers.size()]);

    }

    public MantisUser[] getUsersByProjectName(String projectName, IProgressMonitor monitor) throws MantisException {

        int projectId = getProjectId(projectName);

        List<MantisUser> reporters = cacheData.getReportersByProjectId().get(projectId);

        if (reporters == null)
            throw new MantisException("No reporters for project with id " + projectId + " ");

        return reporters.toArray(new MantisUser[reporters.size()]);
    }

    /**
     * Returns a user by the specified <tt>userName</tt>
     * 
     * @param userName
     * @return the matching user, possibly <code>null</code>
     */
    public MantisUser getUserByUsername(String userName) {
        
        return cacheData.allUsers.get(userName);
    }

    public MantisVersion[] getVersionsByProjectName(String projectName) throws MantisException {

        int projectId = getProjectId(projectName);

        List<MantisVersion> versions = cacheData.versionsByProjectId.get(projectId);

        if (versions == null)
            throw new MantisException("No versions for project with id " + projectId + " ");

        return versions.toArray(new MantisVersion[versions.size()]);
    }
    
    public String getSubmitStatus() throws MantisException {

        return getStatus(cacheData.bugSubmitStatus).getName();
    }
    
    public String getAssignedStatus() throws MantisException {

        return getStatus(cacheData.bugAssignedStatus).getName();
    }        

    public String getResolvedStatusName() throws MantisException {
        
        for ( MantisTicketStatus status : cacheData.statuses)
            if ( status.getValue() == cacheData.resolvedStatus)
                return status.getName();
        
        throw new MantisException("No status with id " + cacheData.resolvedStatus + " .");
    }

    public String getDefaultSeverityName() throws MantisException {
        
        return getSeverity(cacheData.getDefaultValueForAttribute(Key.SEVERITY)).getName();
    }
    
    public String getDefaultPriorityName() throws MantisException {
        
        return getPriority(cacheData.getDefaultValueForAttribute(Key.PRIORITY)).getName();
    }
    
    public String getDefaultEtaName() throws MantisException {
        
        return getETA(cacheData.getDefaultValueForAttribute(Key.ETA)).getName();
    }

    public String getDefaultProjectionName() throws MantisException {

        return getProjection(cacheData.getDefaultValueForAttribute(Key.PROJECTION)).getName();
    }
    
    public String getDefaultResolutionName() throws MantisException {
        
        return getResolution(cacheData.getDefaultValueForAttribute(Key.RESOLUTION)).getName();
    }
    
    public String getDefaultReproducibilityName() throws MantisException {
        
        return getReproducibility(cacheData.getDefaultValueForAttribute(Key.REPRODUCIBILITY)).getName();
    }
    
    public String getDefaultViewStateName() throws MantisException {
        
        return getViewState(cacheData.getDefaultValueForAttribute(Key.VIEW_STATE)).getName();
    }
    
    public String getDefaultStepsToReproduce() {
    	
    	return cacheData.getDefaultValueForStringAttribute(Key.STEPS_TO_REPRODUCE);
    }
    
    public String getDefaultAdditionalInfo() {
    	
    	return cacheData.getDefaultValueForStringAttribute(Key.ADDITIONAL_INFO);
    }
    
    public MantisResolution getBugResolutionFixedThreshold() throws MantisException {
    	
    	return getResolution(cacheData.getBugResolutionFixedThreshold());
    }
    
    public boolean isEtaEnabled() {
    	
    	return cacheData.isEtaEnabled();
    }
    
    public boolean isProjectionEnabled() {
    	
    	return cacheData.isProjectionEnabled();
    }
    
    public MantisCacheData getCacheData() {

        return cacheData;
    }

    public void setCacheData(MantisCacheData cacheData) {

        this.cacheData = cacheData;
    }

    /**
     * Since for large user counts the reporter retrieval fails, we provide a hook for registering
     * additional reporter users as they are discovered, e.g. in IssueData
     * 
     * @param projectId
     *            the project id
     * @param accountData
     *            the name of the reporter
     */
    public void registerAdditionalReporter(int projectId, AccountData accountData) {
        
        // empty account
        if ( accountData.getId().equals(BigInteger.ZERO) )
            return;

        MantisUser user = new MantisUser(accountData.getId().intValue(), accountData.getName(), accountData.getReal_name(), accountData.getEmail());
        
        cacheData.allUsers.put(accountData.getName(), user);
        
        if ( cacheData.getReportersByProjectId().containsEntry(projectId, user) )
            return;
        
        cacheData.getReportersByProjectId().put(projectId, user);
    }

    public boolean dueDateIsEnabled() {
        
        return cacheData.dueDateViewThreshold < DefaultConstantValues.Role.NOBODY.getValue() && cacheData.dueDateUpdateThreshold < DefaultConstantValues.Role.NOBODY.getValue();
    }

    public void copyReportersFromDevelopers(int projectId) {

        cacheData.getReportersByProjectId().replaceValues(projectId, cacheData.getDevelopersByProjectId().get(projectId));
    }
}
