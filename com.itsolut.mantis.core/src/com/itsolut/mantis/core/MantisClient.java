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

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import org.apache.axis.encoding.Base64;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import com.itsolut.mantis.binding.IssueData;
import com.itsolut.mantis.binding.IssueHeaderData;
import com.itsolut.mantis.binding.IssueNoteData;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisRelationship;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicketStatus;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * @author Robert Munteanu
 * 
 */
public class MantisClient implements IMantisClient {

    private final MantisAxis1SOAPClient soapClient;

    private final MantisCache cache;

    private AbstractWebLocation location;

    public MantisClient(AbstractWebLocation webLocation) throws MantisException {

        soapClient = new MantisAxis1SOAPClient(webLocation);
        cache = new MantisCache(soapClient);
        location = webLocation;

    }

    public MantisCache getCache(IProgressMonitor progressMonitor) throws MantisException {

        cache.refreshIfNeeded(Policy.monitorFor(progressMonitor), location.getUrl());

        return cache;
    }

    public int createTicket(MantisTicket ticket, IProgressMonitor monitor) throws MantisException {

        cache.refreshIfNeeded(monitor, location.getUrl());

        IssueData issueData = MantisConverter.convert(ticket, this, getUserName());

        int issueId = soapClient.addIssue(issueData, monitor);

        ticket.setId(issueId);

        addRelationsIfApplicable(ticket, monitor);

        return issueId;
    }

    private String getUserName() {

        return location.getCredentials(AuthenticationType.REPOSITORY).getUserName();
    }

    private void addRelationsIfApplicable(MantisTicket ticket, IProgressMonitor monitor) throws MantisException {

        if (!cache.getRepositoryVersion().isHasProperTaskRelations())
            return;

        for (MantisRelationship relationship : ticket.getRelationships())
            soapClient.addRelationship(ticket.getId(), MantisConverter.convert(relationship), monitor);
    }

    public byte[] getAttachmentData(int id, IProgressMonitor monitor) throws MantisException {

        cache.refreshIfNeeded(monitor, location.getUrl());

        return soapClient.getIssueAttachment(id, monitor);
    }

    public MantisTicket getTicket(int ticketId, IProgressMonitor monitor) throws MantisException {

        cache.refreshIfNeeded(monitor, location.getUrl());

        IssueData issueData = soapClient.getIssueData(ticketId, monitor);

        registerAdditionalReporters(issueData);

        MantisTicket ticket = MantisConverter.convert(issueData, this, monitor);

        Policy.advance(monitor, 1);

        return ticket;
    }

    private void registerAdditionalReporters(IssueData issueData) {

        int projectId = issueData.getProject().getId().intValue();
        cache.registerAdditionalReporter(projectId, issueData.getReporter().getName());
        if (issueData.getNotes() == null)
            return;

        for (IssueNoteData note : issueData.getNotes())
            cache.registerAdditionalReporter(projectId, note.getReporter().getName());

    }

    public boolean isCompleted(TaskData taskData, IProgressMonitor progressMonitor) throws MantisException {

        cache.refreshIfNeeded(progressMonitor, location.getUrl());

        TaskAttribute status = taskData.getRoot().getAttribute(MantisAttributeMapper.Attribute.STATUS.getKey());
        String statusName = status.getValue();
        try {

            MantisTicketStatus mantisStatus = cache.getStatusByName(statusName);

            int resolvedStatusThreshold = cache.getResolvedStatus();

            return mantisStatus.getValue() >= resolvedStatusThreshold;

        } catch (MantisException e) {
            MantisCorePlugin.log(new Status(Status.WARNING, MantisCorePlugin.PLUGIN_ID,
                    "Unable to find the level for the status named " + statusName + " ."));
            return false;
        }

    }

    public void putAttachmentData(int id, String name, byte[] data, IProgressMonitor monitor) throws MantisException {

        cache.refreshIfNeeded(monitor, location.getUrl());

        boolean requiresBase64EncodedAttachment = cache.getRepositoryVersion().isRequiresBase64EncodedAttachment();

        final byte[] encoded = requiresBase64EncodedAttachment ? Base64.encode(data).getBytes() : data;

        soapClient.addIssueAttachment(id, name, encoded, monitor);
    }

    public void search(MantisSearch query, List<MantisTicket> result, IProgressMonitor monitor) throws MantisException {

        cache.refreshIfNeeded(monitor, location.getUrl());

        String projectName = query.getProjectName();
        String filterName = query.getFilterName();

        int projectId = cache.getProjectId(projectName);
        int filterId = cache.getProjectFilterId(projectId, filterName);

        IProgressMonitor subMonitor = Policy.subMonitorFor(monitor, 1);
        subMonitor.beginTask("Retrieving issue headers", 1);

        try {
            IssueHeaderData[] issueHeaders;

            if (filterId == MantisCache.BUILT_IN_PROJECT_TASKS_FILTER_ID)
                issueHeaders = soapClient.getIssueHeaders(projectId, filterId, subMonitor);
            else
                issueHeaders = soapClient.getIssueHeaders(projectId, filterId, query.getLimit(), monitor);

            for (IssueHeaderData issueHeader : issueHeaders)
                result.add(MantisConverter.convert(issueHeader, cache, projectName));

        } finally {
            subMonitor.done();
        }
    }

    public void updateAttributes(IProgressMonitor monitor) throws MantisException {

        cache.refresh(monitor, location.getUrl());
    }

    public void updateTicket(MantisTicket ticket, String comment, IProgressMonitor monitor) throws MantisException {

        cache.refreshIfNeeded(monitor, location.getUrl());

        IssueData issue = MantisConverter.convert(ticket, this, getUserName());
        issue.setId(BigInteger.valueOf(ticket.getId()));

        // add comment first because when updating the issue to resolved
        // comments can't be added
        addCommentIfApplicable(ticket.getId(), comment, monitor);

        soapClient.updateIssue(issue, monitor);
    }

    private void addCommentIfApplicable(int issueId, String comment, IProgressMonitor monitor) throws MantisException {

        if (MantisUtils.isEmpty(comment))
            return;

        final IssueNoteData ind = new IssueNoteData();
        ind.setDate_submitted(MantisUtils.transform(new Date()));
        ind.setLast_modified(MantisUtils.transform(new Date()));
        ind.setReporter(MantisConverter.convert(getUserName()));
        ind.setText(comment);

        soapClient.addNote(issueId, ind, monitor);
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

    public boolean isDueDateEnabled(IProgressMonitor monitor) throws MantisException {

        cache.refreshIfNeeded(monitor, location.getUrl());
        
        return cache.getRepositoryVersion().isHasDueDateSupport() && cache.dueDateIsEnabled(); 
    }
    
    public boolean isTimeTrackingEnabled(IProgressMonitor monitor) throws MantisException {

        cache.refreshIfNeeded(monitor, location.getUrl());
        
        return cache.getCacheData().timeTrackingEnabled;
    }
    
    public MantisCacheData getCacheData() {

        return cache.getCacheData();
    }

    public void setCacheData(MantisCacheData cacheData) {

        MantisCorePlugin.debug("Setting cache data with identity " + System.identityHashCode(cacheData)
                + " on client with identity " + System.identityHashCode(this) + " .", new RuntimeException());

        cache.setCacheData(cacheData);

    }

}
