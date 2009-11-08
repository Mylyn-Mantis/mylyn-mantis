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

import java.util.Calendar;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.itsolut.mantis.binding.AttachmentData;
import com.itsolut.mantis.binding.CustomFieldDefinitionData;
import com.itsolut.mantis.binding.CustomFieldValueForIssueData;
import com.itsolut.mantis.binding.IssueData;
import com.itsolut.mantis.binding.IssueHeaderData;
import com.itsolut.mantis.binding.IssueNoteData;
import com.itsolut.mantis.binding.ProjectVersionData;
import com.itsolut.mantis.binding.RelationshipData;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisAttachment;
import com.itsolut.mantis.core.model.MantisComment;
import com.itsolut.mantis.core.model.MantisCustomField;
import com.itsolut.mantis.core.model.MantisCustomFieldType;
import com.itsolut.mantis.core.model.MantisPriority;
import com.itsolut.mantis.core.model.MantisRelationship;
import com.itsolut.mantis.core.model.MantisResolution;
import com.itsolut.mantis.core.model.MantisSeverity;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicketStatus;
import com.itsolut.mantis.core.model.MantisVersion;
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

    public static MantisTicket convert(IssueData issue, RepositoryVersion version) {

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
        if (version.isHasDueDateSupport())
            ticket.putBuiltinValue(Key.DUE_DATE, String.valueOf(issue.getDue_date().getTimeInMillis()));

        ticket.putBuiltinValue(Key.ADDITIONAL_INFO, issue.getAdditional_information());
        ticket.putBuiltinValue(Key.STEPS_TO_REPRODUCE, issue.getSteps_to_reproduce());

        ticket.putBuiltinValue(Key.REPORTER, issue.getReporter().getName());
        if (issue.getHandler() != null) {
            ticket.putBuiltinValue(Key.ASSIGNED_TO, issue.getHandler().getName());
        }

        if (issue.getNotes() != null)
            for (IssueNoteData ind : issue.getNotes())
                ticket.addComment(convert(ind));

        if (issue.getAttachments() != null)
            for (AttachmentData ad : issue.getAttachments())
                ticket.addAttachment(convert(ad));

        if (issue.getRelationships() != null)
            for (RelationshipData rel : issue.getRelationships())
                ticket.addRelationship(convert(rel, version));

        if (issue.getCustom_fields() != null)
            for (CustomFieldValueForIssueData customFieldValue : issue.getCustom_fields())
                ticket.putCustomFieldValue(customFieldValue.getField().getName(), customFieldValue.getValue());

        return ticket;

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

    private static MantisComment convert(IssueNoteData ind) {

        MantisComment comment = new MantisComment();
        comment.setId(ind.getId().intValue());
        comment.setReporter(ind.getReporter().getName());
        comment.setText(ind.getText());
        comment.setDateSubmitted(MantisUtils.transform(ind.getDate_submitted()));
        comment.setLastModified(MantisUtils.transform(ind.getLast_modified()));

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

    public static MantisTicket convert(IssueHeaderData ihd, MantisCache cache, String projectName) {

            MantisTicket ticket = new MantisTicket(ihd.getId().intValue());
            
            ticket.putBuiltinValue(Key.PROJECT, projectName);
            ticket.putBuiltinValue(Key.SUMMARY, ihd.getSummary());
            ticket.putBuiltinValue(Key.ID, ihd.getId().toString());

            ticket.putBuiltinValue(Key.RESOLUTION, cache.getResolution(ihd.getResolution().intValue()).getName());

            MantisPriority priority = data.getPriority(ihd.getPriority().intValue());
            if (priority != null) {
                ticket.putBuiltinValue(Key.PRIORITY, priority.getName());
            }

            MantisSeverity severity = data.getSeverity(ihd.getSeverity().intValue());
            if (severity != null) {
                ticket.putBuiltinValue(Key.SEVERITY, severity.getName());
            }

            MantisTicketStatus status = data.getStatus(ihd.getStatus().intValue());
            if (status != null) {
                ticket.putBuiltinValue(Key.STATUS, status.getName());
            }
            // DC: Added so that it isn't necessary to retrieve all tasks one at time
            // to see if they have changed since the last synchronization.
            // This cuts down on the number of soap requests that need to be made
            // to the server.
            ticket.setLastChanged(ihd.getLast_updated().getTime());
    }

}
