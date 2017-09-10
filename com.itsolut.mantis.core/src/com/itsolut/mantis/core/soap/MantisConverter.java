/*******************************************************************************
 * Copyright (c) 2007 - 2009 IT Solutions, Inc. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Robert Munteanu
 *******************************************************************************/

package com.itsolut.mantis.core.soap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.*;

import org.eclipse.core.runtime.IProgressMonitor;

import biz.futureware.mantis.rpc.soap.client.*;

import com.google.common.collect.Lists;
import com.itsolut.mantis.core.*;
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
        if (customFieldData.getPossible_values() != null && customFieldData.getPossible_values().trim().length() > 0)
            customField.setPossibleValues(customFieldData.getPossible_values().split("\\|"));

        return customField;
    }

    public static MantisVersion convert(ProjectVersionData versionData) {

        MantisVersion version = new MantisVersion(versionData.getName());
        version.setDescription(versionData.getDescription());
        if ( versionData.getDate_order() != null)
        	version.setTime(MantisUtils.transform(versionData.getDate_order()));
        version.setReleased(versionData.getReleased());

        return version;
    }

    public static MantisTicket convert(IssueData issue, MantisSoapClient mantisClient, IProgressMonitor monitor) throws MantisException {

        MantisTicket ticket = new MantisTicket(issue.getId().intValue());
        ticket.setCreated(issue.getDate_submitted().getTime());
        ticket.setLastChanged(MantisUtils.transform(issue.getLast_updated()));

        ticket.putBuiltinValue(Key.PROJECT, issue.getProject().getName());

        ticket.putBuiltinValue(Key.SUMMARY, issue.getSummary());
        ticket.putBuiltinValue(Key.DESCRIPTION, issue.getDescription());
        ticket.putBuiltinValue(Key.CATEOGRY, issue.getCategory());

        ticket.putBuiltinValue(Key.RESOLUTION, issue.getResolution().getName());
        ticket.putBuiltinValue(Key.SEVERITY, issue.getSeverity().getName());
        ticket.putBuiltinValue(Key.PRIORITY, issue.getPriority().getName());
        ticket.putBuiltinValue(Key.REPRODUCIBILITY, issue.getReproducibility().getName());
        if ( issue.getProjection() != null )
        	ticket.putBuiltinValue(Key.PROJECTION, issue.getProjection().getName());
        if ( issue.getEta() != null )
        	ticket.putBuiltinValue(Key.ETA, issue.getEta().getName());
        ticket.putBuiltinValue(Key.VIEW_STATE, issue.getView_state().getName());
        ticket.putBuiltinValue(Key.STATUS, issue.getStatus().getName());
        ticket.putBuiltinValue(Key.VERSION, issue.getVersion());
        ticket.putBuiltinValue(Key.FIXED_IN, issue.getFixed_in_version());
        ticket.putBuiltinValue(Key.TARGET_VERSION, issue.getTarget_version());
        if (mantisClient.isDueDateEnabled(monitor) && issue.getDue_date() != null)
            ticket.putBuiltinValue(Key.DUE_DATE, String.valueOf(MantisUtils.transform(issue.getDue_date()).getTime()));

        if (issue.getStatus().getId().intValue() >= mantisClient.getCache(monitor).getResolvedStatus())
            ticket.putBuiltinValue(Key.COMPLETION_DATE, String.valueOf(MantisUtils.transform(issue.getLast_updated()).getTime()));

        ticket.putBuiltinValue(Key.ADDITIONAL_INFO, issue.getAdditional_information());
        ticket.putBuiltinValue(Key.STEPS_TO_REPRODUCE, issue.getSteps_to_reproduce());

        ticket.putBuiltinValue(Key.REPORTER, issue.getReporter().getName());
        if (issue.getHandler() != null)
            ticket.putBuiltinValue(Key.ASSIGNED_TO, issue.getHandler().getName());
        
        if( mantisClient.getCache(monitor).isEnableProfiles() ) {
            ticket.putBuiltinValue(Key.PLATFORM, issue.getPlatform());
            ticket.putBuiltinValue(Key.OS, issue.getOs());
            ticket.putBuiltinValue(Key.OS_BUILD, issue.getOs_build());
        }

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
                putCustomFieldValue(ticket, customFieldValue, mantisClient.getCache(monitor).getCustomFieldByProjectIdAndFieldName(issue.getProject().getId().intValue(), customFieldValue.getField().getName()));

        if (issue.getMonitors() != null) {
            List<MantisUser> monitors = new ArrayList<MantisUser>();
            for (AccountData issueMonitor : issue.getMonitors())
                monitors.add(convert(issueMonitor));

            ticket.setMonitors(monitors);
        }
        
        if ( mantisClient.getCache(monitor).getRepositoryVersion().isHasTagSupport() ) {
            ObjectRef[] tags = issue.getTags() != null ? issue.getTags() : new ObjectRef[0];
            List<MantisTag> tagIds = Lists.newArrayListWithExpectedSize(tags.length);
            for ( ObjectRef tag : tags )
                tagIds.add(new MantisTag(tag.getName(), tag.getId().intValue()));
            
            ticket.setTags(tagIds);
        }

        return ticket;

    }

    private static void putCustomFieldValue(MantisTicket ticket, CustomFieldValueForIssueData customFieldValue, MantisCustomField customField) {
        
        String value;
        if ( customField.getType() == MantisCustomFieldType.DATE ) {
            value = MantisUtils.convertFromCustomFieldDate(customFieldValue.getValue());
        } else {
            value = customFieldValue.getValue();
        }
        
        ticket.putCustomFieldValue(customFieldValue.getField().getName(), value);
    }

    public static MantisUser convert(AccountData accountData) {

        return new MantisUser(accountData.getId().intValue(), accountData.getName(), accountData.getReal_name(), accountData.getEmail());
    }

    private static MantisAttachment convert(AttachmentData ad) {

        MantisAttachment ma = new MantisAttachment();
        ma.setContentType(ad.getContent_type());
        ma.setCreated(MantisUtils.transform(ad.getDate_submitted()));
        ma.setDownloadURL(ad.getDownload_url().getPath());
        ma.setFilename(ad.getFilename());
        ma.setSize(ad.getSize().intValue());
        ma.setId(ad.getId().intValue());
        if ( ad.getUser_id() != null)
            ma.setUserId(ad.getUser_id().intValue());

        return ma;

    }

    private static MantisComment convert(IssueNoteData ind, boolean supportsTimeTracking) {

        MantisComment comment = new MantisComment();
        comment.setId(ind.getId().intValue());
        comment.setReporter(ind.getReporter().getName());
        comment.setText(ind.getText());
        comment.setDateSubmitted(MantisUtils.transform(ind.getDate_submitted()));
        comment.setLastModified(MantisUtils.transform(ind.getLast_modified()));
        if (supportsTimeTracking)
            comment.setTimeTracking(ind.getTime_tracking().intValue());
        boolean isPrivate = ind.getView_state().getId().intValue() == DefaultConstantValues.ViewState.PRIVATE.getValue();
        comment.setIsPrivate(isPrivate);

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

        ticket.putBuiltinValue(Key.RESOLUTION, String.valueOf(cache.getResolution(ihd.getResolution().intValue()).getValue()));
        ticket.putBuiltinValue(Key.PRIORITY, String.valueOf(cache.getPriority(ihd.getPriority().intValue()).getValue()));
        ticket.putBuiltinValue(Key.SEVERITY, String.valueOf(cache.getSeverity(ihd.getSeverity().intValue()).getValue()));
        ticket.putBuiltinValue(Key.STATUS, String.valueOf(cache.getStatus(ihd.getStatus().intValue()).getValue()));

        if (ihd.getStatus().intValue() >= cache.getResolvedStatus())
            ticket.putBuiltinValue(Key.COMPLETION_DATE, String.valueOf(MantisUtils.transform(ihd.getLast_updated()).getTime()));

        // DC: Added so that it isn't necessary to retrieve all tasks one at time
        // to see if they have changed since the last synchronization.
        // This cuts down on the number of soap requests that need to be made to the server.
        ticket.setLastChanged(MantisUtils.transform(ihd.getLast_updated()));

        return ticket;
    }

    public static IssueData convert(MantisTicket ticket, IMantisClient client, String username, IProgressMonitor monitor) throws MantisException {

        MantisCache cache = client.getCache(monitor);

        ObjectRef project = new ObjectRef(BigInteger.valueOf(cache.getProjectByName(ticket.getValue(Key.PROJECT)).getValue()), ticket.getValue(Key.PROJECT));

        IssueData issue = new IssueData();

        issue.setSummary(ticket.getValue(Key.SUMMARY));
        issue.setDescription(ticket.getValue(Key.DESCRIPTION));
        issue.setSeverity(getValueAsObjectRef(ticket, Key.SEVERITY));
        issue.setResolution(getValueAsObjectRef(ticket, Key.RESOLUTION));
        issue.setPriority(getValueAsObjectRef(ticket, Key.PRIORITY));
        issue.setReproducibility(getValueAsObjectRef(ticket, Key.REPRODUCIBILITY));
        if (cache.isProjectionEnabled())
            issue.setProjection(getValueAsObjectRef(ticket, Key.PROJECTION));
        if (cache.isEtaEnabled())
            issue.setEta(getValueAsObjectRef(ticket, Key.ETA));
        issue.setView_state(getValueAsObjectRef(ticket, Key.VIEW_STATE));
        
        if( cache.isEnableProfiles() ) {
            issue.setPlatform(ticket.getValue(Key.PLATFORM));
            issue.setOs(ticket.getValue(Key.OS));
            issue.setOs_build(ticket.getValue(Key.OS_BUILD));
        }

        issue.setProject(project);
        issue.setCategory(ticket.getValue(Key.CATEOGRY));

        issue.setVersion(ticket.getValueAndFilterNone(Key.VERSION));
        issue.setFixed_in_version(ticket.getValueAndFilterNone(Key.FIXED_IN));
        if (cache.getRepositoryVersion().isHasTargetVersionSupport())
            issue.setTarget_version(ticket.getValueAndFilterNone(Key.TARGET_VERSION));

        if (client.isDueDateEnabled(monitor)) {
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

        if (!MantisUtils.isEmpty(ticket.getValue(Key.ASSIGNED_TO)))
            issue.setHandler(convert(ticket.getValue(Key.ASSIGNED_TO), cache));
        issue.setLast_updated(MantisUtils.transform(new Date()));

        setIssueMonitors(ticket, issue, cache, username);
        setCustomFields(ticket, project, issue, cache);
        setTags(ticket, issue);
        
        return issue;
    }

    private static ObjectRef getValueAsObjectRef(MantisTicket ticket, Key key) {

        return new ObjectRef(new BigInteger(ticket.getValue(key)), "");
    }

    public static AccountData convert(String userName, MantisCache cache) throws MantisException {

        AccountData accountData = new AccountData();
        if (userName.length() == 0)
            return accountData;

        MantisUser user = cache.getUserByUsername(userName);

        if (user == null)
            throw new MantisException("Could not find user for username " + userName);

        accountData.setId(BigInteger.valueOf(user.getValue()));
        accountData.setName(user.getKey());
        accountData.setEmail(user.getEmail());
        accountData.setReal_name(user.getRealName());

        return accountData;
    }

    private static void setIssueMonitors(MantisTicket ticket, IssueData issue, MantisCache cache, String username) throws MantisException {

        boolean addSelf = Boolean.valueOf(ticket.getValue(Key.ADD_SELF_TO_MONITORS));
        List<String> monitorList = MantisUtils.fromCsvString(ticket.getValue(Key.MONITORS));
        if (addSelf && !monitorList.contains(username))
            monitorList.add(username);

        List<AccountData> monitors = new ArrayList<AccountData>();
        for (String monitorUsername : monitorList)
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
    
    private static void setTags(MantisTicket ticket, IssueData issue) {
        
        if ( ticket.getTags() == null )
            return;
        
        List<ObjectRef> tagRefs = Lists.newArrayList();
        for ( MantisTag tag : ticket.getTags() )
            tagRefs.add(new ObjectRef(BigInteger.valueOf(tag.getValue()), tag.getName()));
        
        issue.setTags(tagRefs.toArray(new ObjectRef[tagRefs.size()]));
    }

    private static CustomFieldValueForIssueData extractCustomFieldValue(ObjectRef project, Map.Entry<String, String> entry, MantisCache cache) throws MantisException {

        String customFieldName = entry.getKey();
        MantisCustomField customField = cache.getCustomFieldByProjectIdAndFieldName(project.getId().intValue(),
                customFieldName);
        
        ObjectRef customFieldRef = new ObjectRef(BigInteger.valueOf(customField.getId()), customField.getName());
        String value = entry.getValue();
        if ( customField.getType() == MantisCustomFieldType.DATE ) 
            value = MantisUtils.convertToCustomFieldDate(value);
            
        return new CustomFieldValueForIssueData(customFieldRef, value);
    }

    public static RelationshipData convert(MantisRelationship relationship) {

        ObjectRef relationType = new ObjectRef(BigInteger.valueOf(relationship.getType().getMantisConstant()), "");
        RelationshipData relationshipData = new RelationshipData();
        relationshipData.setId(BigInteger.valueOf(relationship.getId()));
        relationshipData.setType(relationType);
        relationshipData.setTarget_id(BigInteger.valueOf(relationship.getTargetId()));
        return relationshipData;
    }

    public static <T extends MantisTicketAttribute> List<T> convert(ObjectRef[] objectRef, Class<T> attributeType) throws MantisException {

        if (objectRef == null || objectRef.length == 0)
            return Collections.emptyList();

        try {
            List<T> attributes = Lists.newArrayListWithExpectedSize(objectRef.length);

            Constructor<T> contructor = attributeType.getConstructor(String.class, int.class);

            for (ObjectRef ref : objectRef)
                attributes.add(contructor.newInstance(ref.getName(), ref.getId().intValue()));

            return attributes;
        } catch (SecurityException e) {
            throw new MantisException("Unable to convert ObjectRef to attribute of type " + attributeType.getName(), e);
        } catch (NoSuchMethodException e) {
            throw new MantisException("Unable to convert ObjectRef to attribute of type " + attributeType.getName(), e);
        } catch (InstantiationException e) {
            throw new MantisException("Unable to convert ObjectRef to attribute of type " + attributeType.getName(), e);
        } catch (IllegalAccessException e) {
            throw new MantisException("Unable to convert ObjectRef to attribute of type " + attributeType.getName(), e);
        } catch (InvocationTargetException e) {
            throw new MantisException("Unable to convert ObjectRef to attribute of type " + attributeType.getName(), e);
        }
    }

    public static List<MantisProject> convert(ProjectData[] projectData) {
        
        if ( projectData == null )
            return Collections.emptyList();

        List<MantisProject> projects = Lists.newArrayList();

        addSubProjects(projects, projectData, null);

        return projects;
    }

    private static void addSubProjects(List<MantisProject> projects, ProjectData[] projectData, Integer parentProjectId) {

        for (ProjectData projectDataItem : projectData) {

            MantisProject project = new MantisProject(projectDataItem.getName(), projectDataItem.getId().intValue(), parentProjectId);

            projects.add(project);

            if (projectDataItem.getSubprojects() != null)
                addSubProjects(projects, projectDataItem.getSubprojects(), projectDataItem.getId().intValue());
        }
    }

    public static List<MantisProjectFilter> convert(FilterData[] projectFilters) {

        if (projectFilters == null)
            return Collections.emptyList();

        List<MantisProjectFilter> filters = Lists.newArrayListWithCapacity(projectFilters.length);

        for (FilterData filterData : projectFilters)
            filters.add(new MantisProjectFilter(filterData.getName(), filterData.getId().intValue(), filterData.getUrl(), filterData.getProject_id().intValue()));

        return filters;
    }

    public static List<MantisCustomField> convert(CustomFieldDefinitionData[] projectCustomFields) {

        if (projectCustomFields == null)
            return Collections.emptyList();

        List<MantisCustomField> customFields = Lists.newArrayListWithCapacity(projectCustomFields.length);
        for (CustomFieldDefinitionData projectCustomField : projectCustomFields)
            customFields.add(convert(projectCustomField));

        return customFields;
    }

    public static List<MantisVersion> convert(ProjectVersionData[] projectVersions) {

        if (projectVersions == null)
            return Collections.emptyList();

        List<MantisVersion> versions = Lists.newArrayListWithCapacity(projectVersions.length);
        for (ProjectVersionData projectVersion : projectVersions)
            versions.add(convert(projectVersion));

        return versions;
    }

    public static List<MantisUser> convert(AccountData[] projectUsers) {

        if (projectUsers == null)
            return Collections.emptyList();

        List<MantisUser> users = Lists.newArrayListWithCapacity(projectUsers.length);
        for (AccountData projectCustomField : projectUsers)
            users.add(convert(projectCustomField));

        return users;
    }

    /**
     * @param allTags
     * @return
     */
    public static List<MantisTag> convert(List<TagData> allTags) {

        List<MantisTag> mantisTags = Lists.newArrayListWithExpectedSize(allTags.size());
        for ( TagData tagData : allTags )
            mantisTags.add(new MantisTag(tagData.getName(), tagData.getId().intValue()));
        
        return mantisTags;
    }

	public static MantisIssueHistoryEntry convert(HistoryData entry) {
		if ( entry.getField().isEmpty()  ) {
			return null;
		}
		return new MantisIssueHistoryEntry(MantisUtils.parseDate(entry.getDate().longValue() * 1000), entry.getField(), entry.getUsername(), 
				entry.getOld_value(), entry.getNew_value());
	}

}
