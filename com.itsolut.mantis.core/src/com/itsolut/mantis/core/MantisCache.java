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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import com.itsolut.mantis.binding.CustomFieldDefinitionData;
import com.itsolut.mantis.binding.FilterData;
import com.itsolut.mantis.binding.ProjectData;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisCustomField;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectFilter;

/**
 * @author Robert Munteanu
 * 
 */
public class MantisCache {

    private long lastUpdate = 0;

    private List<MantisProject> projects = new ArrayList<MantisProject>();

    private Map<Integer, List<MantisProjectFilter>> projectFiltersById = new HashMap<Integer, List<MantisProjectFilter>>();

    private Map<Integer, List<MantisCustomField>> customFieldsByProjectId = new HashMap<Integer, List<MantisCustomField>>();
    
    private RepositoryVersion version;

    private MantisAxis1SOAPClient soapClient;

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

        for (MantisProject project : projects)
            cacheFilters(project.getValue(), soapClient.getProjectFilters(project.getValue(), monitor));

        for (MantisProject project : projects)
            cacheProjectCustomFields(project.getValue(), soapClient.getProjectCustomFields(project.getValue(), monitor));
        
        cacheRepositoryVersion(soapClient.getVersion(monitor));

        /*
         * 
         * String resolvedStatus = getSOAP().mc_config_get_string(getUsername(), getPassword(),
         * RESOLVED_STATUS_THRESHOLD);
         * data.setResolvedStatusThreshold(Integer.parseInt(resolvedStatus));
         * Policy.advance(subMonitor, 1);
         * 
         * // updateDueDateAttributes(subMonitor);
         * 
         * ObjectRef[] result = getSOAP().mc_enum_priorities(getUsername(), getPassword());
         * data.priorities = new ArrayList<MantisPriority>(result.length); for (ObjectRef item :
         * result) { data.priorities.add(parsePriority(item)); } Policy.advance(subMonitor, 1);
         * 
         * result = getSOAP().mc_enum_status(getUsername(), getPassword()); data.statuses = new
         * ArrayList<MantisTicketStatus>(result.length); for (ObjectRef item : result) {
         * data.statuses.add(parseTicketStatus(item)); } Policy.advance(subMonitor, 1);
         * 
         * result = getSOAP().mc_enum_severities(getUsername(), getPassword()); data.severities =
         * new ArrayList<MantisSeverity>(result.length); for (ObjectRef item : result) {
         * data.severities.add(parseSeverity(item)); } Policy.advance(subMonitor, 1);
         * 
         * result = getSOAP().mc_enum_resolutions(getUsername(), getPassword()); data.resolutions =
         * new ArrayList<MantisResolution>(result.length); for (ObjectRef item : result) {
         * data.resolutions.add(parseResolution(item)); } Policy.advance(subMonitor, 1);
         * 
         * result = getSOAP().mc_enum_reproducibilities(getUsername(), getPassword());
         * data.reproducibilities = new ArrayList<MantisReproducibility>(result.length); for
         * (ObjectRef item : result) { data.reproducibilities.add(parseReproducibility(item)); }
         * Policy.advance(subMonitor, 1);
         * 
         * result = getSOAP().mc_enum_projections(getUsername(), getPassword()); data.projections =
         * new ArrayList<MantisProjection>(result.length); for (ObjectRef item : result) {
         * data.projections.add(parseProjection(item)); } Policy.advance(subMonitor, 1);
         * 
         * result = getSOAP().mc_enum_etas(getUsername(), getPassword()); data.etas = new
         * ArrayList<MantisETA>(result.length); for (ObjectRef item : result) {
         * data.etas.add(parseETA(item)); } Policy.advance(subMonitor, 1);
         * 
         * result = getSOAP().mc_enum_view_states(getUsername(), getPassword()); data.viewStates =
         * new ArrayList<MantisViewState>(result.length); for (ObjectRef item : result) {
         * data.viewStates.add(parseViewState(item)); } Policy.advance(subMonitor, 1);
         * 
         * loadCustomFieldTypes(subMonitor);
         */
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

        this.version = RepositoryVersion.fromVersionString(version);
    }

}
