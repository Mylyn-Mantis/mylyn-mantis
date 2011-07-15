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

import static com.itsolut.mantis.core.ConfigurationKey.*;
import static com.itsolut.mantis.core.DefaultConstantValues.Attribute.ETA_ENABLED;
import static com.itsolut.mantis.core.DefaultConstantValues.Attribute.PROJECTION_ENABLED;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;

import org.apache.axis.encoding.Base64;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.osgi.util.NLS;

import biz.futureware.mantis.rpc.soap.client.AccountData;
import biz.futureware.mantis.rpc.soap.client.IssueData;
import biz.futureware.mantis.rpc.soap.client.IssueHeaderData;
import biz.futureware.mantis.rpc.soap.client.IssueNoteData;

import com.itsolut.mantis.core.DefaultConstantValues;
import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCache;
import com.itsolut.mantis.core.MantisCacheData;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.RepositoryValidationResult;
import com.itsolut.mantis.core.RepositoryVersion;
import com.itsolut.mantis.core.TaskRelationshipChange;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisETA;
import com.itsolut.mantis.core.model.MantisPriority;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjection;
import com.itsolut.mantis.core.model.MantisReproducibility;
import com.itsolut.mantis.core.model.MantisResolution;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisSeverity;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.model.MantisTicketStatus;
import com.itsolut.mantis.core.model.MantisViewState;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * The {@link MantisSoapClient} is a SOAP-based implementation of the {@link IMantisClient}
 * 
 * @author Robert Munteanu
 */
public class MantisSoapClient implements IMantisClient {

    private final MantisAxis1SoapClient soapClient;

    private final MantisCache cache;
    
    private final Object sync = new Object();

    private AbstractWebLocation location;
    
    private static final int ALL_PROJECTS = 0;
    
    private final NumberFormat formatter = new DecimalFormat("#.#");

    public MantisSoapClient(AbstractWebLocation webLocation) throws MantisException {

        soapClient = new MantisAxis1SoapClient(webLocation);
        cache = new MantisCache();
        location = webLocation;

    }

    public MantisCache getCache(IProgressMonitor progressMonitor) throws MantisException {

        refreshIfNeeded(Policy.monitorFor(progressMonitor), location.getUrl());

        return cache;
    }

    public int createTicket(MantisTicket ticket, IProgressMonitor monitor, List<TaskRelationshipChange> relationshipChanges) throws MantisException {

        refreshIfNeeded(monitor, location.getUrl());

        IssueData issueData = MantisConverter.convert(ticket, this, getUserName(), monitor);

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

        refreshIfNeeded(monitor, location.getUrl());

        return soapClient.getIssueAttachment(id, monitor);
    }

    public MantisTicket getTicket(int ticketId, IProgressMonitor monitor) throws MantisException {

        refreshIfNeeded(monitor, location.getUrl());

        IssueData issueData = soapClient.getIssueData(ticketId, monitor);

        registerAdditionalReporters(issueData);

        MantisTicket ticket = MantisConverter.convert(issueData, this, monitor);

        Policy.advance(monitor, 1);

        return ticket;
    }

    private void registerAdditionalReporters(IssueData issueData) {

        int projectId = issueData.getProject().getId().intValue();
        
        cache.registerAdditionalReporter(projectId, MantisConverter.convert(issueData.getReporter()));
        
        if ( issueData.getHandler() != null )
            cache.registerAdditionalReporter(projectId, MantisConverter.convert(issueData.getHandler()));
        
        if (issueData.getNotes() != null)
            for (IssueNoteData note : issueData.getNotes())
                cache.registerAdditionalReporter(projectId, MantisConverter.convert(note.getReporter()));
        
        if ( issueData.getMonitors() != null )
            for ( AccountData issueMonitor : issueData.getMonitors() )
                cache.registerAdditionalReporter(projectId, MantisConverter.convert(issueMonitor));

    }

    public void putAttachmentData(int id, String name, byte[] data, IProgressMonitor monitor) throws MantisException {

        refreshIfNeeded(monitor, location.getUrl());

        final byte[] encoded = cache.getRepositoryVersion().hasCorrectBase64Encoding() ?  data : Base64.encode(data).getBytes();

        soapClient.addIssueAttachment(id, name, encoded, monitor);
    }

    public void search(MantisSearch query, List<MantisTicket> result, IProgressMonitor monitor) throws MantisException {

        monitor.beginTask("", IProgressMonitor.UNKNOWN);
        try {
            refreshIfNeeded(monitor, location.getUrl());

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

        refresh(monitor, location.getUrl());
    }
    
    public void updateAttributesForTask(IProgressMonitor monitor, Integer ticketId) throws MantisException {
    	
    	IssueData issueData = soapClient.getIssueData(ticketId, monitor);
        
    	refreshForProject(monitor, location.getUrl(), issueData.getProject().getId().intValue());
    }

    public void updateTicket(MantisTicket ticket, String comment, int timeTracking,  List<TaskRelationshipChange> changes, IProgressMonitor monitor) throws MantisException {

        refreshIfNeeded(monitor, location.getUrl());

        IssueData issue = MantisConverter.convert(ticket, this, getUserName(), monitor);
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

        refreshIfNeeded(monitor, location.getUrl());
        
        return cache.getRepositoryVersion().isHasDueDateSupport() && cache.dueDateIsEnabled(); 
    }
    
    public boolean isTimeTrackingEnabled(IProgressMonitor monitor) throws MantisException {

        refreshIfNeeded(monitor, location.getUrl());
        
        return cache.getCacheData().timeTrackingEnabled && cache.getRepositoryVersion().isHasTimeTrackingSupport();
    }
    
    public MantisCacheData getCacheData() {

        return cache.getCacheData();
    }

    public void setCacheData(MantisCacheData cacheData) {

        cache.setCacheData(cacheData);
    }
    
    public void refreshIfNeeded(IProgressMonitor progressMonitor, String repositoryUrl) throws MantisException {

        synchronized (sync) {
            if (!cache.getCacheData().hasBeenRefreshed())
                refresh(progressMonitor, repositoryUrl);
        }

    }

    public void refresh(IProgressMonitor monitor, String repositoryUrl) throws MantisException {

        refresh0(monitor, repositoryUrl, ALL_PROJECTS);
    }

    private void refresh0(IProgressMonitor monitor, String repositoryUrl, int projectId) throws MantisException {
        
        synchronized (sync) {

            long start = System.currentTimeMillis();

            SubMonitor subMonitor = SubMonitor.convert(monitor);

            try {
                // TODO: recursive
                cache.cacheProjects(MantisConverter.convert(soapClient.getProjectData(monitor)));

                int projectsToRefresh =  projectId == ALL_PROJECTS ? cache.getCacheData().getProjects().size()  : 1 ;
                
                subMonitor.beginTask("Refreshing repository configuration", projectsToRefresh * 6 + 29);

                cache.cacheReporterThreshold(safeGetInt(soapClient.getStringConfiguration(monitor, REPORTER_THRESHOLD.getValue()), DefaultConstantValues.Threshold.REPORT_BUG_THRESHOLD.getValue()));
                Policy.advance(subMonitor, 1);

                cache.cacheDeveloperThreshold(safeGetInt(soapClient.getStringConfiguration(monitor, DEVELOPER_THRESHOLD.getValue()), DefaultConstantValues.Threshold.UPDATE_BUG_ASSIGN_THRESHOLD.getValue()));
                Policy.advance(subMonitor, 1);
                
                cache.cacheAssignedStatus(safeGetInt(soapClient.getStringConfiguration(monitor, BUG_ASSIGNED_STATUS.getValue()), DefaultConstantValues.Status.ASSIGNED.getValue()));
                Policy.advance(subMonitor, 1);

                cache.cacheSubmitStatus(safeGetInt(soapClient.getStringConfiguration(monitor, BUG_SUBMIT_STATUS.getValue()), DefaultConstantValues.Status.NEW.getValue()));
                Policy.advance(subMonitor, 1);
                
                try {
                    cache.cacheDueDateViewThreshold(safeGetInt(soapClient.getStringConfiguration(monitor, DUE_DATE_VIEW_THRESOLD.getValue()), DefaultConstantValues.Role.NOBODY.getValue()));
                } catch (MantisException e) {
                    MantisCorePlugin.warn("Failed retrieving configuration value: " + e.getMessage() + " . Using default value.");
                    cache.cacheDueDateViewThreshold(DefaultConstantValues.Role.NOBODY.getValue());
                } finally {
                    Policy.advance(subMonitor, 1);
                }

                try {
                    String mantisValue = soapClient.getStringConfiguration(monitor, DUE_DATE_UPDATE_THRESOLD.getValue());
                    cache.cacheDueDateUpdateThreshold(safeGetInt(mantisValue,DefaultConstantValues.Role.NOBODY.getValue()));
                } catch (MantisException e) {
                    MantisCorePlugin.warn("Failed retrieving configuration value: " + e.getMessage() + " . Using default value.");
                    cache.cacheDueDateUpdateThreshold(DefaultConstantValues.Role.NOBODY.getValue());
                } finally {
                    Policy.advance(subMonitor, 1);
                }

                try {
                    cache.cacheTimeTrackingEnabled(soapClient.getStringConfiguration(monitor, TIME_TRACKING_ENABLED.getValue()));
                } catch (MantisException e) {
                    MantisCorePlugin.warn("Failed retrieving configuration value: " + e.getMessage() + " . Using default value.");
                    cache.cacheTimeTrackingEnabled(Boolean.FALSE.toString());
                } finally {
                    Policy.advance(subMonitor, 1);
                }
                
                for (MantisProject project : cache.getProjects()) {
                    
                    if ( projectId != ALL_PROJECTS && projectId != project.getValue() )
                        continue;
                    
                    cache.cacheFilters(project.getValue(), MantisConverter.convert(soapClient.getProjectFilters(project.getValue(), monitor)));
                    Policy.advance(subMonitor, 1);

                    cache.cacheProjectCustomFields(project.getValue(), MantisConverter.convert(soapClient.getProjectCustomFields(project.getValue(),
                            monitor)));
                    Policy.advance(subMonitor, 1);

                    cache.cacheProjectCategories(project.getValue(), soapClient.getProjectCategories(project.getValue(),
                            monitor));
                    Policy.advance(subMonitor, 1);

                    cache.cacheProjectDevelopers(project.getValue(), MantisConverter.convert(soapClient.getProjectUsers(project.getValue(),
                            cache.getCacheData().getDeveloperThreshold(), monitor)));
                    Policy.advance(subMonitor, 1);

                    try {
                        cache.cacheProjectReporters(project.getValue(), MantisConverter.convert(soapClient.getProjectUsers(project.getValue(),
                                cache.getCacheData().getReporterThreshold(), monitor)));
                    } catch (MantisException e) {
                        if ( cache.getCacheData().getReportersByProjectId().containsKey(project.getValue()) ) {
                            MantisCorePlugin.warn("Failed retrieving reporter information, using previously loaded values.", e);
                        } else {
                            cache.copyReportersFromDevelopers(project.getValue());
                            MantisCorePlugin.warn("Failed retrieving reporter information, using developers list for reporters.", e);
                        }
                    }
                    Policy.advance(subMonitor, 1);

                    cache.cacheProjectVersions(project.getValue(), MantisConverter.convert(soapClient.getProjectVersions(project.getValue(), monitor)));
                    Policy.advance(subMonitor, 1);
                }

                cache.cacheResolvedStatus(soapClient.getStringConfiguration(monitor, RESOLVED_STATUS_THRESHOLD.getValue()));
                Policy.advance(subMonitor, 1);
                
                cache.cacheRepositoryVersion(soapClient.getVersion(monitor));
                Policy.advance(subMonitor, 1);
                
                cache.cachePriorities(MantisConverter.convert( soapClient.getPriorities(monitor), MantisPriority.class));
                Policy.advance(subMonitor, 1);

                cache.cacheStatuses(MantisConverter.convert(soapClient.getStatuses(monitor), MantisTicketStatus.class));
                Policy.advance(subMonitor, 1);

                cache.cacheSeverities(MantisConverter.convert(soapClient.getSeverities(monitor), MantisSeverity.class));
                Policy.advance(subMonitor, 1);

                cache.cacheResolutions(MantisConverter.convert(soapClient.getResolutions(monitor), MantisResolution.class));
                Policy.advance(subMonitor, 1);

                cache.cacheReproducibilites(MantisConverter.convert(soapClient.getReproducibilities(monitor), MantisReproducibility.class));
                Policy.advance(subMonitor, 1);

                cache.cacheProjections(MantisConverter.convert(soapClient.getProjections(monitor), MantisProjection.class));
                Policy.advance(subMonitor, 1);

                cache.cacheEtas(MantisConverter.convert(soapClient.getEtas(monitor), MantisETA.class));
                Policy.advance(subMonitor, 1);

                cache.cacheViewStates(MantisConverter.convert(soapClient.getViewStates(monitor), MantisViewState.class));
                Policy.advance(subMonitor, 1);
                
                cache.cacheDefaultAttributeValue(Key.SEVERITY, safeGetThreshold(monitor, "default_bug_severity", DefaultConstantValues.Attribute.BUG_SEVERITY));
                Policy.advance(subMonitor, 1);

                cache.cacheDefaultAttributeValue(Key.PRIORITY, safeGetThreshold(monitor, "default_bug_priority", DefaultConstantValues.Attribute.BUG_PRIORITY));
                Policy.advance(subMonitor, 1);

                cache.cacheDefaultAttributeValue(Key.ETA, safeGetThreshold(monitor, "default_bug_eta", DefaultConstantValues.Attribute.BUG_ETA));
                Policy.advance(subMonitor, 1);

                cache.cacheDefaultAttributeValue(Key.REPRODUCIBILITY, safeGetThreshold(monitor, "default_bug_reproducibility", DefaultConstantValues.Attribute.BUG_REPRODUCIBILITY));
                Policy.advance(subMonitor, 1);

                cache.cacheDefaultAttributeValue(Key.RESOLUTION, safeGetThreshold(monitor, "default_bug_resolution", DefaultConstantValues.Attribute.BUG_RESOLUTION));
                Policy.advance(subMonitor, 1);

                cache.cacheDefaultAttributeValue(Key.PROJECTION, safeGetThreshold(monitor, "default_bug_projection", DefaultConstantValues.Attribute.BUG_PROJECTION));
                Policy.advance(subMonitor, 1);
                
                cache.cacheDefaultAttributeValue(Key.VIEW_STATE, safeGetThreshold(monitor, "default_bug_view_status", DefaultConstantValues.Attribute.BUG_VIEW_STATUS));
                Policy.advance(subMonitor, 1);
                
                cache.getCacheData().putDefaultValueForStringAttribute(Key.STEPS_TO_REPRODUCE, soapClient.getStringConfiguration(monitor, "default_bug_steps_to_reproduce"));
                Policy.advance(subMonitor, 1);
                
                cache.getCacheData().putDefaultValueForStringAttribute(Key.ADDITIONAL_INFO, soapClient.getStringConfiguration(monitor, "default_bug_additional_info"));
                Policy.advance(subMonitor, 1);
                
                cache.getCacheData().setBugResolutionFixedThreshold(safeGetThreshold(monitor, "bug_resolution_fixed_threshold", DefaultConstantValues.Attribute.BUG_RESOLUTION_FIXED_THRESHOLD));
                
                cache.getCacheData().setEtaEnabled(safeGetBoolean(subMonitor, "enable_eta", ETA_ENABLED ));
                
                cache.getCacheData().setProjectionEnabled(safeGetBoolean(subMonitor, "enable_projection", PROJECTION_ENABLED ));
                
                cache.getCacheData().setLastUpdate( System.currentTimeMillis() );
            } finally {
                subMonitor.done();
                MantisCorePlugin.debug(NLS.bind("Repository sync for {0} complete in {1} seconds.", repositoryUrl,
                        format(start)), null);
            }
        }
    }
    
    private int safeGetThreshold(IProgressMonitor monitor, String configName, DefaultConstantValues.Attribute attribute) {
        
        try {
            return safeGetInt(soapClient.getStringConfiguration(monitor, configName), attribute.getValue());
        } catch ( MantisException e ) {
            MantisCorePlugin.warn("Unable to retrieve configuration value '" + configName + "' . Using default value '" + attribute.getValue() + "'");
            return attribute.getValue();
        }
    }
    
    private int safeGetInt(String stringConfiguration, int defaultValue) {

        try {
            return Integer.parseInt(stringConfiguration);
        } catch (NumberFormatException e) {
            MantisCorePlugin.warn("Failed parsing config option value " + stringConfiguration
                    + ". Using default value.", e);
            return defaultValue;
        }
    }

    private boolean safeGetBoolean(IProgressMonitor monitor, String configName, DefaultConstantValues.Attribute attribute) {
        
        try {
            int intValue = safeGetInt(soapClient.getStringConfiguration(monitor, configName), attribute.getValue());
            return intValue == 1;
        } catch ( MantisException e ) {
            MantisCorePlugin.warn("Unable to retrieve configuration value '" + configName + "' . Using default value '" + attribute.getValue() + "'");
            return attribute.getValue() == 1;
        }
    }
    
    private String format(long start) {

        double millis = (System.currentTimeMillis() - start) / (double) 1000;
        return formatter.format(millis);

    }

    public void refreshForProject(IProgressMonitor monitor, String url, int projectId) throws MantisException {
        
        refresh0(monitor, url, projectId);
    }
}
