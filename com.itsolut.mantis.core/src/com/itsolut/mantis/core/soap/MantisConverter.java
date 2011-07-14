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

package com.itsolut.mantis.core.soap;

import java.math.BigInteger;
import java.util.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;

import biz.futureware.mantis.rpc.soap.client.AccountData;
import biz.futureware.mantis.rpc.soap.client.AttachmentData;
import biz.futureware.mantis.rpc.soap.client.CustomFieldDefinitionData;
import biz.futureware.mantis.rpc.soap.client.CustomFieldValueForIssueData;
import biz.futureware.mantis.rpc.soap.client.IssueData;
import biz.futureware.mantis.rpc.soap.client.IssueHeaderData;
import biz.futureware.mantis.rpc.soap.client.IssueNoteData;
import biz.futureware.mantis.rpc.soap.client.ObjectRef;
import biz.futureware.mantis.rpc.soap.client.ProjectVersionData;
import biz.futureware.mantis.rpc.soap.client.RelationshipData;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCache;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.RepositoryVersion;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.*;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * @author Robert Munteanu
 * 
 */
public class MantisConverter {

    public static MantisCustomField convert(CustomFieldDefinitionData customFieldData) {

        MantisCustomField customField = new MantisCustomField();
        customField.setId(customFieldData.getField().getId().intValue());
        customField.setName(customFieldData.getField().getName());
        customField.setType(MantisCustomFieldType.fromMantisConstant(customFieldData.getType().intValue()));
        customField.setDefaultValue(customFieldData.getDefault_value());
        if (customFieldData.getPossible_values() != null)
            customField.setPossibleValues(customFieldData.getPossible_values().split("\\|"));

        return customField;
    }

    public static MantisVersion convert(ProjectVersionData versionData) {

        MantisVersion version = new MantisVersion(versionData.getName());
        version.setDescription(versionData.getDescription());
        version.setTime(versionData.getDate_order().getTime());
        version.setReleased(versionData.getReleased());

        return version;
    }

    public static MantisTicket convert(IssueData issue, MantisClient mantisClient, IProgressMonitor monitor) throws MantisException {

        MantisTicket ticket = new MantisTicket(issue.getId().intValue());
        ticket.setCreated(issue.getDate_submitted().getTime());
        ticket.setLastChanged(issue.getLast_updated().getTime());

        ticket.putBuiltinValue(Key.PROJECT, issue.getProject().getName());

        ticket.putBuiltinValue(Key.SUMMARY, issue.getSummary());
        ticket.putBuiltinValue(Key.DESCRIPTION, issue.getDescription());
        ticket.putBuiltinValue(Key.CATEOGRY, issue.getCategory());

        ticket.putBuiltinValue(Key.RESOLUTION, issue.getResolution().getName());
        ticket.putBuiltinValue(Key.SEVERITY, issue.getSeverity().getName());
        ticket.putBuiltinValue(Key.PRIORITY, issue.getPriority().getName());
        ticket.putBuiltinValue(Key.REPRODUCIBILITY, issue.getReproducibility().getName());
        ticket.putBuiltinValue(Key.PROJECTION, issue.getProjection().getName());
        ticket.putBuiltinValue(Key.ETA, issue.getEta().getName());
        ticket.putBuiltinValue(Key.VIEW_STATE, issue.getView_state().getName());
        ticket.putBuiltinValue(Key.STATUS, issue.getStatus().getName());
        ticket.putBuiltinValue(Key.VERSION, issue.getVersion());
        ticket.putBuiltinValue(Key.FIXED_IN, issue.getFixed_in_version());
        ticket.putBuiltinValue(Key.TARGET_VERSION, issue.getTarget_version());
        if (mantisClient.isDueDateEnabled(monitor) && issue.getDue_date() != null)
            ticket.putBuiltinValue(Key.DUE_DATE, String.valueOf(issue.getDue_date().getTimeInMillis()));
        
        if ( issue.getStatus().getId().intValue() >= mantisClient.getCache(monitor).getResolvedStatus() )
        	ticket.putBuiltinValue(Key.COMPLETION_DATE, String.valueOf(issue.getLast_updated().getTimeInMillis()));

        ticket.putBuiltinValue(Key.ADDITIONAL_INFO, issue.getAdditional_information());
        ticket.putBuiltinValue(Key.STEPS_TO_REPRODUCE, issue.getSteps_to_reproduce());

        ticket.putBuiltinValue(Key.REPORTER, issue.getReporter().getName());
        if (issue.getHandler() != null)
            ticket.putBuiltinValue(Key.ASSIGNED_TO, issue.getHandler().getName());
        
        boolean supportsTimeTracking = mantisClient.isTimeTrackingEnabled(monitor);

        if (issue.getNotes() != null)
            for (IssueNoteData ind : issue.getNotes())
                ticket.addComment(convert(ind, supportsTimeTracking));

        if (issue.getAttachments() != null)
            for (AttachmentData ad : issue.getAttachments())
                ticket.addAttachment(convert(ad));

        if (issue.getRelationships() != null)
            for (RelationshipData rel : issue.getRelationships())
                ticket.addRelationship(convert(rel, mantisClient.getCache(monitor).getRepositoryVersion()));

        if (issue.getCustom_fields() != null)
            for (CustomFieldValueForIssueData customFieldValue : issue.getCustom_fields())
                ticket.putCustomFieldValue(customFieldValue.getField().getName(), customFieldValue.getValue());
        
        if ( issue.getMonitors() != null ) {
            List<User> monitors = new ArrayList<User>();
            for ( AccountData issueMonitor : issue.getMonitors() )
                monitors.add(convert(issueMonitor));
            
            ticket.setMonitors(monitors);
        }
        
        MantisCorePlugin.debug(NLS.bind("Converted IssueData to {0}." , ticket), null);

        return ticket;

    }

    /**
     * @param issueMonitor
     * @return
     */
    private static User convert(AccountData issueMonitor) {

        return new User(issueMonitor.getId().intValue(), issueMonitor.getName(), issueMonitor.getReal_name(), issueMonitor.getEmail());
    }

    private static MantisAttachment convert(AttachmentData ad) {

        MantisAttachment ma = new MantisAttachment();
        ma.setContentType(ad.getContent_type());
        ma.setCreated(MantisUtils.transform(ad.getDate_submitted()));
        ma.setDownloadURL(ad.getDownload_url().getPath());
        ma.setFilename(ad.getFilename());
        ma.setSize(ad.getSize().intValue());
        ma.setId(ad.getId().intValue());

        return ma;

    }

    private static MantisComment convert(IssueNoteData ind, boolean supportsTimeTracking) {

        MantisComment comment = new MantisComment();
        comment.setId(ind.getId().intValue());
        comment.setReporter(ind.getReporter().getName());
        comment.setText(ind.getText());
        comment.setDateSubmitted(MantisUtils.transform(ind.getDate_submitted()));
        comment.setLastModified(MantisUtils.transform(ind.getLast_modified()));
        if ( supportsTimeTracking)
            comment.setTimeTracking(ind.getTime_tracking().intValue());

        return comment;

    }

    private static MantisRelationship convert(RelationshipData relationData, RepositoryVersion repositoryVersion) {

        MantisRelationship relationship = new MantisRelationship();
        relationship.setId(relationData.getId().intValue());
        relationship.setTargetId(relationData.getTarget_id().intValue());
        if (repositoryVersion.isHasProperTaskRelations())
            relationship.setType(MantisRelationship.RelationType.fromRelationId(relationData.getType().getId()));
        else
            relationship.setType(MantisRelationship.RelationType.fromRelation(relationData.getType().getName()));

        return relationship;
    }

    public static MantisTicket convert(IssueHeaderData ihd, MantisCache cache, String projectName) throws MantisException {

            MantisTicket ticket = new MantisTicket(ihd.getId().intValue());
            
            ticket.putBuiltinValue(Key.PROJECT, projectName);
            ticket.putBuiltinValue(Key.SUMMARY, ihd.getSummary());
            ticket.putBuiltinValue(Key.ID, ihd.getId().toString());

            ticket.putBuiltinValue(Key.RESOLUTION, cache.getResolution(ihd.getResolution().intValue()).getName());
            ticket.putBuiltinValue(Key.PRIORITY, cache.getPriority(ihd.getPriority().intValue()).getName());
            ticket.putBuiltinValue(Key.SEVERITY, cache.getSeverity(ihd.getSeverity().intValue()).getName());
            ticket.putBuiltinValue(Key.STATUS, cache.getStatus(ihd.getStatus().intValue()).getName());
            
            if ( ihd.getStatus().intValue() >= cache.getResolvedStatus() )
            	ticket.putBuiltinValue(Key.COMPLETION_DATE, String.valueOf(ihd.getLast_updated().getTimeInMillis()));
            

            // DC: Added so that it isn't necessary to retrieve all tasks one at time
            // to see if they have changed since the last synchronization.
            // This cuts down on the number of soap requests that need to be made to the server.
            ticket.setLastChanged(ihd.getLast_updated().getTime());
            
            MantisCorePlugin.debug(NLS.bind("Converted IssueHeaderData to {0}." , ticket), new RuntimeException());
            
            return ticket;
    }
    
    public static IssueData convert(MantisTicket ticket, IMantisClient client, String username) throws MantisException {

        MantisCache cache = client.getCache(new NullProgressMonitor());
        
        ObjectRef project = cache.getProjectAsObjectRef(ticket.getValue(Key.PROJECT));

        IssueData issue = new IssueData();
        issue.setSummary(ticket.getValue(Key.SUMMARY));
        issue.setDescription(ticket.getValue(Key.DESCRIPTION));
        issue.setSeverity(getValueAsObjectRef(ticket, Key.SEVERITY));
        issue.setResolution(getValueAsObjectRef(ticket, Key.RESOLUTION));
        issue.setPriority(getValueAsObjectRef(ticket, Key.PRIORITY));
        issue.setReproducibility(getValueAsObjectRef(ticket, Key.REPRODUCIBILITY));
        if ( cache.isProjectionEnabled() )
        	issue.setProjection(getValueAsObjectRef(ticket, Key.PROJECTION));
        if ( cache.isEtaEnabled() )
        	issue.setEta(getValueAsObjectRef(ticket, Key.ETA));
        issue.setView_state(getValueAsObjectRef(ticket, Key.VIEW_STATE));

        issue.setProject(project);
        issue.setCategory(ticket.getValue(Key.CATEOGRY));

        issue.setVersion(ticket.getValueAndFilterNone(Key.VERSION));
        issue.setFixed_in_version(ticket.getValueAndFilterNone(Key.FIXED_IN));
        if (cache.getRepositoryVersion().isHasTargetVersionSupport())
            issue.setTarget_version(ticket.getValueAndFilterNone(Key.TARGET_VERSION));

        if (client.isDueDateEnabled(new NullProgressMonitor())) {
            String dueDate = ticket.getValue(Key.DUE_DATE);
            if (dueDate == null || dueDate.length() == 0) {
                issue.setDue_date(null);
            } else {
                long dueDateMillis = Long.parseLong(dueDate);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date(dueDateMillis));
                issue.setDue_date(calendar);
            }
        }

        issue.setSteps_to_reproduce(ticket.getValue(Key.STEPS_TO_REPRODUCE));
        issue.setAdditional_information(ticket.getValue(Key.ADDITIONAL_INFO));

        issue.setStatus(getValueAsObjectRef(ticket, Key.STATUS));

        if (MantisUtils.isEmpty(ticket.getValue(Key.REPORTER))) {
            issue.setReporter(convert(username, cache));
        } else {
            issue.setReporter(convert(ticket.getValue(Key.REPORTER), cache));
        }

        issue.setHandler(convert(ticket.getValue(Key.ASSIGNED_TO), cache));
        issue.setLast_updated(MantisUtils.transform(new Date()));

        setIssueMonitors(ticket, project, issue, cache, username);
        setCustomFields(ticket, project, issue, cache);

        return issue;
    }

    private static ObjectRef getValueAsObjectRef(MantisTicket ticket, Key key) {

        return new ObjectRef(new BigInteger(ticket.getValue(key)), "");
    }

    public static AccountData convert(String userId, MantisCache cache) {
        
        AccountData accountData = new AccountData();
        if ( userId.length() == 0 )
            return accountData;
        accountData.setId(new BigInteger(userId));
        User user = cache.getUserByUsername(userId);
        if ( user != null ) {
            accountData.setName(user.getName());
            accountData.setEmail(user.getEmail());
            accountData.setReal_name(user.getRealName());
        }
        
        return accountData;
    }

    private static void setIssueMonitors(MantisTicket ticket, ObjectRef project, IssueData issue, MantisCache cache, String username) {

        boolean addSelf = Boolean.valueOf(ticket.getValue(Key.ADD_SELF_TO_MONITORS));
        List<String> monitorList = MantisUtils.fromCsvString(ticket.getValue(Key.MONITORS));
        if ( addSelf && !monitorList.contains(username) )
            monitorList.add(username);

        List<AccountData> monitors = new ArrayList<AccountData>();
        for ( String monitorUsername : monitorList )
            monitors.add(convert(monitorUsername, cache));
        issue.setMonitors(monitors.toArray(new AccountData[monitors.size()]));
    }
    
    private static void setCustomFields(MantisTicket ticket, ObjectRef project, IssueData issue, MantisCache cache) throws MantisException {

        if (ticket.getCustomFieldValues().isEmpty())
            return;

        List<CustomFieldValueForIssueData> customFieldValues = new ArrayList<CustomFieldValueForIssueData>(ticket
                .getCustomFieldValues().size());

        for (Map.Entry<String, String> entry : ticket.getCustomFieldValues().entrySet())
            customFieldValues.add(extractCustomFieldValue(project, entry, cache));

        issue.setCustom_fields(customFieldValues.toArray(new CustomFieldValueForIssueData[0]));
    }

    private static CustomFieldValueForIssueData extractCustomFieldValue(ObjectRef project, Map.Entry<String, String> entry, MantisCache cache) throws MantisException {

        String customFieldName = entry.getKey();
        MantisCustomField customField = cache.getCustomFieldByProjectIdAndFieldName(project.getId().intValue(),
                customFieldName);
        ObjectRef customFieldRef = new ObjectRef(BigInteger.valueOf(customField.getId()), customField.getName());
        CustomFieldValueForIssueData customFieldValueForIssueData = new CustomFieldValueForIssueData(customFieldRef,
                entry.getValue());
        return customFieldValueForIssueData;
    }
    
    public static RelationshipData convert(MantisRelationship relationship) {

        ObjectRef relationType = new ObjectRef(BigInteger.valueOf(relationship.getType().getMantisConstant()), "");
        RelationshipData relationshipData = new RelationshipData();
        relationshipData.setId(BigInteger.valueOf(relationship.getId()));
        relationshipData.setType(relationType);
        relationshipData.setTarget_id(BigInteger.valueOf(relationship.getTargetId()));
        return relationshipData;
    }

}
