/*******************************************************************************
 * Copyright (c) 2007 - 2009 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Robert Munteanu
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import com.itsolut.mantis.binding.IssueHeaderData;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisCustomField;
import com.itsolut.mantis.core.model.MantisCustomFieldType;
import com.itsolut.mantis.core.model.MantisETA;
import com.itsolut.mantis.core.model.MantisPriority;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectCategory;
import com.itsolut.mantis.core.model.MantisProjectFilter;
import com.itsolut.mantis.core.model.MantisProjection;
import com.itsolut.mantis.core.model.MantisReproducibility;
import com.itsolut.mantis.core.model.MantisResolution;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisSearchFilter;
import com.itsolut.mantis.core.model.MantisSeverity;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicketStatus;
import com.itsolut.mantis.core.model.MantisVersion;
import com.itsolut.mantis.core.model.MantisViewState;

/**
 * @author Robert Munteanu
 * 
 */
public class MantisClient implements IMantisClient {

    private final MantisAxis1SOAPClient soapClient;

    private final MantisCache cache;

    public MantisClient(URL url, String username, String password, String httpUsername, String httpPassword,
            AbstractWebLocation webLocation) {

        soapClient = new MantisAxis1SOAPClient(url, username, password, httpUsername, httpPassword, webLocation);
        cache = new MantisCache(soapClient);
    }

    public MantisCache getCache(IProgressMonitor progressMonitor) throws MantisException {

        cache.refreshIfNeeded(Policy.monitorFor(progressMonitor));

        return cache;
    }

    public int createTicket(MantisTicket ticket, IProgressMonitor monitor) throws MantisException {

        throw new UnsupportedOperationException();
    }

    public byte[] getAttachmentData(int id, IProgressMonitor monitor) throws MantisException {

        throw new UnsupportedOperationException();
    }

    public List<MantisCustomFieldType> getCustomFieldTypes(IProgressMonitor monitor) throws MantisException {

        throw new UnsupportedOperationException();
    }

    public List<MantisCustomField> getCustomFieldsForProject(String projectName, IProgressMonitor monitor)
            throws MantisException {

        throw new UnsupportedOperationException();
    }

    public String[] getDevelopers(String project, IProgressMonitor monitor) {

        throw new UnsupportedOperationException();
    }

    public MantisETA[] getETA() {

        throw new UnsupportedOperationException();
    }

    public MantisPriority[] getPriorities() {

        throw new UnsupportedOperationException();
    }

    public MantisProject getProjectByName(String projectName, IProgressMonitor monitor) throws MantisException {

        throw new UnsupportedOperationException();
    }

    public MantisProjectCategory[] getProjectCategories(String projectName, IProgressMonitor monitor)
            throws MantisException {

        throw new UnsupportedOperationException();
    }

    public MantisProjectFilter[] getProjectFilters(String projectName, IProgressMonitor monitor) throws MantisException {

        throw new UnsupportedOperationException();
    }

    public MantisProjection[] getProjection() {

        throw new UnsupportedOperationException();
    }

    public MantisProject[] getProjects(IProgressMonitor monitor) throws MantisException {

        throw new UnsupportedOperationException();
    }

    public RepositoryVersion getRepositoryVersion(IProgressMonitor monitor) throws MantisException {

        throw new UnsupportedOperationException();
    }

    public MantisReproducibility[] getReproducibility() {

        throw new UnsupportedOperationException();
    }

    public MantisSeverity[] getSeverities() {

        throw new UnsupportedOperationException();
    }

    public MantisTicket getTicket(int ticketId, IProgressMonitor monitor) throws MantisException {

        cache.refreshIfNeeded(monitor);

        MantisTicket ticket = MantisConverter.convert(soapClient.getIssueData(ticketId, monitor), cache
                .getRepositoryVersion());

        Policy.advance(monitor, 1);

        return ticket;
    }

    public MantisResolution[] getTicketResolutions() {

        throw new UnsupportedOperationException();
    }

    public MantisTicketStatus[] getTicketStatus() {

        throw new UnsupportedOperationException();
    }

    public String[] getUsers(String project, IProgressMonitor monitor) {

        throw new UnsupportedOperationException();
    }

    public MantisVersion[] getVersions(String projectName, IProgressMonitor monitor) throws MantisException {

        throw new UnsupportedOperationException();
    }

    public MantisViewState[] getViewState() {

        throw new UnsupportedOperationException();
    }

    public boolean isCompleted(TaskData taskData, IProgressMonitor progressMonitor) throws MantisException {

        throw new UnsupportedOperationException();
    }

    public void putAttachmentData(int id, String name, byte[] data, IProgressMonitor monitor) throws MantisException {

        throw new UnsupportedOperationException();
    }

    public void search(MantisSearch query, List<MantisTicket> result, IProgressMonitor monitor) throws MantisException {

        cache.refreshIfNeeded(monitor);
        
        String projectName = null;
        String filterName = null;
        for (MantisSearchFilter filter : query.getFilters()) {
            if ("project".equals(filter.getFieldName())) {
                projectName = filter.getValues().get(0);

            } else if ("filter".equals(filter.getFieldName())) {
                filterName = filter.getValues().get(0);
            }
        }
        
        int projectId = cache.getProjectId(projectName);
        int filterId = cache.getProjectFilterId(projectId, filterName);
        
        IssueHeaderData[] issueHeaders = soapClient.getIssueHeaders(projectId, filterId, query.getLimit(), monitor);
        
        for ( IssueHeaderData issueHeader : issueHeaders)
            result.add(MantisConverter.convert(issueHeader, cache, projectName));
    }

    public void setData(MantisClientData data) {

        throw new UnsupportedOperationException();
    }

    public void updateAttributes(IProgressMonitor monitor, boolean force) throws MantisException {

        throw new UnsupportedOperationException();
    }

    public void updateTicket(MantisTicket ticket, String comment, IProgressMonitor monitor) throws MantisException {

        throw new UnsupportedOperationException();
    }

    public void validate(IProgressMonitor monitor) throws MantisException {

        monitor.beginTask("Validating", 2);

        try {

            // get and validate remote version
            String remoteVersion = soapClient.getVersion(monitor);
            RepositoryVersion.fromVersionString(remoteVersion);
            Policy.advance(monitor, 1);

            // test to see if the current user has proper access privileges,
            // since getVersion() does not require a valid user
            soapClient.getProjectData(monitor);
            Policy.advance(monitor, 1);
        } finally {

            monitor.done();
        }

    }

}
