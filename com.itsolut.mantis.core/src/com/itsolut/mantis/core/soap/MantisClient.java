/*******************************************************************************
 * Copyright (c) 2004, 2010 Robert Munteanu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Robert Munteanu - initial API and implementation
 *******************************************************************************/


package com.itsolut.mantis.core.soap;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import org.apache.axis.encoding.Base64;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.commons.net.Policy;

import biz.futureware.mantis.rpc.soap.client.AccountData;
import biz.futureware.mantis.rpc.soap.client.IssueData;
import biz.futureware.mantis.rpc.soap.client.IssueHeaderData;
import biz.futureware.mantis.rpc.soap.client.IssueNoteData;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCache;
import com.itsolut.mantis.core.MantisCacheData;
import com.itsolut.mantis.core.RepositoryValidationResult;
import com.itsolut.mantis.core.RepositoryVersion;
import com.itsolut.mantis.core.TaskRelationshipChange;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisTicket;
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

    public int createTicket(MantisTicket ticket, IProgressMonitor monitor, List<TaskRelationshipChange> relationshipChanges) throws MantisException {

        cache.refreshIfNeeded(monitor, location.getUrl());

        IssueData issueData = MantisConverter.convert(ticket, this, getUserName());

        int issueId = soapClient.addIssue(issueData, monitor);

        ticket.setId(issueId);

        updateRelationsIfApplicable(ticket, relationshipChanges, monitor);

        return issueId;
    }

    private String getUserName() {

        // usual case
        AuthenticationCredentials credentials = location.getCredentials(AuthenticationType.REPOSITORY);
        
        // HTTP-only authentication
        if  ( credentials == null)
            credentials = location.getCredentials(AuthenticationType.HTTP);
        
        // no login specified is not supported ATM by the SOAP API, but there's no harm done either
        if ( credentials == null)
            return null;
        
        return credentials.getUserName();
    }

    private void updateRelationsIfApplicable(MantisTicket ticket, List<TaskRelationshipChange> relationshipChanges, IProgressMonitor monitor) throws MantisException {

        if (!cache.getRepositoryVersion().isHasProperTaskRelations())
            return;
        
        for ( TaskRelationshipChange relationshipChange : relationshipChanges) {
            
            switch ( relationshipChange.getDirection() ) {
                
                case Removed:
                    soapClient.deleteRelationship(ticket.getId(), relationshipChange.getRelationship().getId(), monitor);
                    break;
                case Added:
                    soapClient.addRelationship(ticket.getId(), MantisConverter.convert(relationshipChange.getRelationship()), monitor);
                    break;
            }
            
        }
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
        cache.registerAdditionalReporter(projectId, issueData.getReporter());
        
        if (issueData.getNotes() != null)
            for (IssueNoteData note : issueData.getNotes())
                cache.registerAdditionalReporter(projectId, note.getReporter());
        
        if ( issueData.getMonitors() != null )
            for ( AccountData issueMonitor : issueData.getMonitors() )
                cache.registerAdditionalReporter(projectId, issueMonitor);

    }

    public void putAttachmentData(int id, String name, byte[] data, IProgressMonitor monitor) throws MantisException {

        cache.refreshIfNeeded(monitor, location.getUrl());

        final byte[] encoded = cache.getRepositoryVersion().hasCorrectBase64Encoding() ?  data : Base64.encode(data).getBytes();

        soapClient.addIssueAttachment(id, name, encoded, monitor);
    }

    public void search(MantisSearch query, List<MantisTicket> result, IProgressMonitor monitor) throws MantisException {

        monitor.beginTask("", IProgressMonitor.UNKNOWN);
        try {
            cache.refreshIfNeeded(monitor, location.getUrl());

            String projectName = query.getProjectName();
            String filterName = query.getFilterName();

            int projectId = cache.getProjectId(projectName);
            int filterId = cache.getProjectFilterId(projectId, filterName);

            IssueHeaderData[] issueHeaders;

            if (filterId == MantisCache.BUILT_IN_PROJECT_TASKS_FILTER_ID)
                issueHeaders = soapClient.getIssueHeaders(projectId, query.getLimit(), monitor);
            else
                issueHeaders = soapClient.getIssueHeaders(projectId, filterId, query.getLimit(), monitor);

            for (IssueHeaderData issueHeader : issueHeaders)
                result.add(MantisConverter.convert(issueHeader, cache, projectName));
        } finally {
            monitor.done();
        }
    }

    public void updateAttributes(IProgressMonitor monitor) throws MantisException {

        cache.refresh(monitor, location.getUrl());
    }
    
    public void updateAttributesForTask(IProgressMonitor monitor, Integer ticketId) throws MantisException {
    	
    	IssueData issueData = soapClient.getIssueData(ticketId, monitor);
        
    	cache.refreshForProject(monitor, location.getUrl(), issueData.getProject().getId().intValue());
    }

    public void updateTicket(MantisTicket ticket, String comment, int timeTracking,  List<TaskRelationshipChange> changes, IProgressMonitor monitor) throws MantisException {

        cache.refreshIfNeeded(monitor, location.getUrl());

        IssueData issue = MantisConverter.convert(ticket, this, getUserName());
        issue.setId(BigInteger.valueOf(ticket.getId()));

        // add comment first because when updating the issue to resolved
        // comments can't be added
        addCommentIfApplicable(ticket.getId(), comment, timeTracking, monitor);
        updateRelationsIfApplicable(ticket, changes, monitor);

        soapClient.updateIssue(issue, monitor);
    }

    public void addIssueComment(int issueId, String comment, int timeTracking, IProgressMonitor monitor) throws MantisException {

        IssueNoteData ind = new IssueNoteData();
        
        ind.setDate_submitted(MantisUtils.transform(new Date()));
        ind.setLast_modified(MantisUtils.transform(new Date()));
        ind.setReporter(MantisConverter.convert(getUserName(), cache));
        ind.setTime_tracking(BigInteger.valueOf(timeTracking));
        ind.setText(comment);

        soapClient.addNote(issueId, ind, monitor);
    }
    
    private void addCommentIfApplicable(int issueId, String comment, int timeTracking, IProgressMonitor monitor) throws MantisException {

        if (MantisUtils.isEmpty(comment) && timeTracking == 0)
            return;

        addIssueComment(issueId, comment, timeTracking, monitor);
    }

    public RepositoryValidationResult validate(IProgressMonitor monitor) throws MantisException {

        monitor.beginTask("Validating", 2);

        try {

            // get and validate remote version
            String remoteVersion = soapClient.getVersion(monitor);
            RepositoryVersion version = RepositoryVersion.fromVersionString(remoteVersion);
            Policy.advance(monitor, 1);

            // test to see if the current user has proper access privileges,
            // since getVersion() does not require a valid user
            soapClient.getProjectData(monitor);
            Policy.advance(monitor, 1);
            
            return new RepositoryValidationResult(version);
            
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
        
        return cache.getCacheData().timeTrackingEnabled && cache.getRepositoryVersion().isHasTimeTrackingSupport();
    }
    
    public MantisCacheData getCacheData() {

        return cache.getCacheData();
    }

    public void setCacheData(MantisCacheData cacheData) {

        cache.setCacheData(cacheData);
    }
}