/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - Initial implementation for Mantis
 *******************************************************************************/
package com.itsolut.mantis.core;

import java.math.BigInteger;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.rpc.Call;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.AxisFault;
import org.apache.axis.configuration.FileProvider;
import org.apache.axis.encoding.Base64;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.Policy;
import org.xml.sax.SAXException;

import com.itsolut.mantis.binding.AccountData;
import com.itsolut.mantis.binding.AttachmentData;
import com.itsolut.mantis.binding.CustomFieldDefinitionData;
import com.itsolut.mantis.binding.CustomFieldValueForIssueData;
import com.itsolut.mantis.binding.CustomMantisConnectLocator;
import com.itsolut.mantis.binding.FilterData;
import com.itsolut.mantis.binding.IssueData;
import com.itsolut.mantis.binding.IssueHeaderData;
import com.itsolut.mantis.binding.IssueNoteData;
import com.itsolut.mantis.binding.MantisConnectPortType;
import com.itsolut.mantis.binding.ObjectRef;
import com.itsolut.mantis.binding.ProjectData;
import com.itsolut.mantis.binding.ProjectVersionData;
import com.itsolut.mantis.binding.RelationshipData;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.exception.MantisRemoteException;
import com.itsolut.mantis.core.model.MantisAttachment;
import com.itsolut.mantis.core.model.MantisComment;
import com.itsolut.mantis.core.model.MantisCustomField;
import com.itsolut.mantis.core.model.MantisCustomFieldType;
import com.itsolut.mantis.core.model.MantisETA;
import com.itsolut.mantis.core.model.MantisPriority;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectCategory;
import com.itsolut.mantis.core.model.MantisProjectFilter;
import com.itsolut.mantis.core.model.MantisProjection;
import com.itsolut.mantis.core.model.MantisRelationship;
import com.itsolut.mantis.core.model.MantisReproducibility;
import com.itsolut.mantis.core.model.MantisResolution;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisSearchFilter;
import com.itsolut.mantis.core.model.MantisSeverity;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicketAttribute;
import com.itsolut.mantis.core.model.MantisTicketStatus;
import com.itsolut.mantis.core.model.MantisVersion;
import com.itsolut.mantis.core.model.MantisViewState;
import com.itsolut.mantis.core.model.MantisTicket.Key;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * Represents a Mantis repository that is accessed through the MantisConnect
 * SOAP Interface.
 * 
 * @author Chris Hane
 */
public class MantisAxis1SOAPClient extends AbstractMantisClient {

    private static final String OLD_SF_NET_URL = "https://apps.sourceforge.net/mantisbt/";

    private static final String NEW_SF_NET_URL = "https://sourceforge.net/apps/mantisbt/";

    
    private transient MantisConnectPortType soap;

    private String httpUsername;

    private String httpPassword;

    private static final String REPORTER_THRESHOLD = "report_bug_threshold";

    private static final String DEVELOPER_THRESHOLD = "update_bug_assign_threshold";
    
    public MantisAxis1SOAPClient(URL url, String username, String password, String httpUsername,
            String httpPassword, AbstractWebLocation webLocation) {

        super(url, username, password, webLocation);
        
        this.httpUsername = httpUsername;
        this.httpPassword = httpPassword;

        try {
            soap = this.getSOAP();

            if (httpUsername != null && httpUsername.length() > 0 && httpPassword != null
                    && httpPassword.length() > 0) {
                ((Stub) soap)._setProperty(Call.USERNAME_PROPERTY, httpUsername);
                ((Stub) soap)._setProperty(Call.PASSWORD_PROPERTY, httpPassword);
            }
        } catch (MantisException e) {
        }
    }

    protected MantisConnectPortType getSOAP() throws MantisException {

        synchronized (this) {

            if (soap != null) {
                return soap;
            }

            try {
                FileProvider provider = new FileProvider(this.getClass().getClassLoader()
                        .getResourceAsStream("client-config.wsdd"));
                CustomMantisConnectLocator locator = new CustomMantisConnectLocator(provider);
                locator.setLocation(getLocation());

                soap = locator.getMantisConnectPort(repositoryUrl);
            } catch (ServiceException e) {
                MantisCorePlugin.log(e);
                throw new MantisRemoteException(e);
            }

            return soap;

        }

    }

    public void validate(IProgressMonitor monitor) throws MantisException {

        monitor.beginTask("Validating", 2);
        
        try {
            
            // get and validate remote version
            String remoteVersion = getSOAP().mc_version();
            RepositoryVersion.fromVersionString(remoteVersion);
            Policy.advance(monitor, 1);

            // test to see if the current user has proper access privileges,
            // since mc_version() does not require a valid user
            getSOAP().mc_projects_get_user_accessible(username, password);
            Policy.advance(monitor, 1);

        } catch (RemoteException e) {
            MantisCorePlugin.log(e);
            throw wrap(e);
        } finally {
            monitor.done();
        }

    }

    private MantisRemoteException wrap(RemoteException e) {

        
        StringBuilder message = new StringBuilder();
        
        if ( isSourceforgeRepoWithoutHttpAuth())
            message.append("For SF.net hosted apps, please make sure to use HTTP authentication only.").append('\n');
        
        if ( repositoryUrl.toExternalForm().startsWith(OLD_SF_NET_URL))
            message.append("SF.net hosted apps have been moved to https://sourceforge.net/apps/mantisbt/").append('\n');
        
        message.append("Repository validation failed: ");
        
        if ( e instanceof AxisFault ) {
            
            AxisFault axisFault = (AxisFault) e;
            
            if ( axisFault.getCause() instanceof SAXException)
                message.append("the repository has returned an invalid XML response : " + String.valueOf(axisFault.getCause().getMessage()) + " .");
            else if (e.getMessage() != null)
                    message.append(" :").append(e.getMessage()).append('\n');

        } else if (e.getMessage() != null)
            message.append(" :").append(e.getMessage()).append('\n');
        
        return new MantisRemoteException(message.toString(), e);
        
    }
    
    private boolean isSourceforgeRepoWithoutHttpAuth() {
        return repositoryUrl.toExternalForm().startsWith(NEW_SF_NET_URL) &&  ( httpUsername.length() == 0 || httpPassword.length() == 0);
    }

    public MantisTicket getTicket(int id, IProgressMonitor monitor) throws MantisException {

        IssueData issue;
        try {
            issue = getSOAP().mc_issue_get(username, password, BigInteger.valueOf(id));
            Policy.advance(monitor, 1);
        } catch (RemoteException e) {
            MantisCorePlugin.log(e);
            throw new MantisRemoteException(e);
        }
        MantisTicket ticket = parseTicket(issue);
        //
        // String[] actions = getActions(id);
        // ticket.setActions(actions);
        //
        // ticket.setResolutions(getDefaultTicketResolutions());

        return ticket;
    }

    // local cache
    private MantisProject[] projects = null;

    public MantisProject[] getProjects(IProgressMonitor monitor) throws MantisException {

        if (projects == null) {
            ProjectData[] pds;
            try {
                pds = getSOAP().mc_projects_get_user_accessible(username, password);
                Policy.advance(monitor, 1);
            } catch (RemoteException e) {
                MantisCorePlugin.log(e);
                throw new MantisRemoteException(e);
            }

            projects = new MantisProject[countProjects(pds)];
            addProjects(0, pds, 0);
        }
        

        return projects;
    }

    private int addProjects(int offset, ProjectData[] pds, int level) {

        StringBuilder buf = new StringBuilder(level);
        // if(level>0){
        // for(int x=level; x>0; x--){
        // buf.append(" ");
        // }
        // buf.append(" -> ");
        // }

        for (ProjectData pd : pds) {
            projects[offset++] = new MantisProject(buf.toString() + pd.getName(), pd.getId()
                    .intValue());
            offset = addProjects(offset, pd.getSubprojects(), level + 1); // add
            // sub
            // projects
            // if
            // there
            // are
            // any...
        }
        return offset;
    }

    private int countProjects(ProjectData[] pds) {

        int cnt = 0;

        for (ProjectData pd : pds) {
            cnt++;
            cnt += countProjects(pd.getSubprojects());
        }

        return cnt;
    }

    private ObjectRef getProject(String name, IProgressMonitor monitor) throws MantisException {

        for (MantisProject mp : this.getProjects(monitor)) {
            if (mp.getName().equals(name)) {
                return new ObjectRef(BigInteger.valueOf(mp.getValue()), mp.getName());
            }
        }
        return null;
    }

    private final Map<String, MantisProjectCategory[]> categories = new HashMap<String, MantisProjectCategory[]>(
            3);

    public MantisProjectCategory[] getProjectCategories(String projectName, IProgressMonitor monitor) throws MantisException {

        if (categories.containsKey(projectName)) {
            return categories.get(projectName);
        }

        ObjectRef project = getProject(projectName, monitor);
        String[] list;
        try {
            list = getSOAP().mc_project_get_categories(username, password, project.getId());
            Policy.advance(monitor, 1);
        } catch (Exception e) {
            MantisCorePlugin.log(e);
            return new MantisProjectCategory[0];
        }
        MantisProjectCategory[] data = new MantisProjectCategory[list.length];
        for (int x = 0; x < list.length; x++) {
            data[x] = new MantisProjectCategory(list[x], x);
        }
        this.categories.put(projectName, data);
        return data;
    }

    private final Map<String, MantisProjectFilter[]> filters = new HashMap<String, MantisProjectFilter[]>(
            3);

    public MantisProjectFilter[] getProjectFilters(String projectName, IProgressMonitor monitor) throws MantisException {

        // cached value
        if (filters.containsKey(projectName))
            return filters.get(projectName);

        try {

            ObjectRef project = getProject(projectName, monitor);

            // somehow we get the wrong value ... debugging
            if (project == null)
                throw new MantisException("No project can be found for with name " + projectName
                        + " . Currently cached projects : " + Arrays.toString(projects) + " .");

            // get from remote
            FilterData[] list = getSOAP().mc_filter_get(username, password, project.getId());
            Policy.advance(monitor, 1);

            // convert
            MantisProjectFilter[] data = new MantisProjectFilter[list.length];
            for (int x = 0; x < list.length; x++) {
                data[x] = new MantisProjectFilter(list[x].getName(), list[x].getId().intValue());
            }

            // cache
            this.filters.put(projectName, data);

            return data;
        } catch (RemoteException e) {

            // fail gracefully
            MantisCorePlugin.log(e);
            return new MantisProjectFilter[0];
        } catch (MantisException e) {

            // fail gracefully
            MantisCorePlugin.log(e);
            return new MantisProjectFilter[0];
        }

    }

    private FilterData getFilter(String projectName, String filterName, IProgressMonitor monitor) throws MantisException {

        for (MantisProjectFilter filter : getProjectFilters(projectName, monitor)) {
            if (filter.getName().equals(filterName)) {
                FilterData fd = new FilterData();
                fd.setId(BigInteger.valueOf(filter.getValue()));
                return fd;
            }
        }
        return null;
    }

    public void search(MantisSearch query, List<MantisTicket> tickets, IProgressMonitor monitor) throws MantisException {
        
        try {
            
            String projectName = null;
            String filterName = null;
            for (MantisSearchFilter filter : query.getFilters()) {
                if ("project".equals(filter.getFieldName())) {
                    projectName = filter.getValues().get(0);

                } else if ("filter".equals(filter.getFieldName())) {
                    filterName = filter.getValues().get(0);
                }
            }

            ObjectRef project = getProject(projectName, monitor);
            FilterData filter = getFilter(projectName, filterName, monitor);

            if (project == null || filter == null)
                throw new MantisException(
                        "Unable to create query . Please make sure that the repository credentials and the query parameters are valid.");

            IssueHeaderData[] ihds = getSOAP().mc_filter_get_issue_headers(username, password,
                    project.getId(), // project
                    filter.getId(), // filter
                    BigInteger.valueOf(1), // start page
                    BigInteger.valueOf(query.getLimit())); // # per page
            
            Policy.advance(monitor, 1);
            
            for (IssueHeaderData ihd : ihds) {
                // only read the attributes that are important for the tasklist
                MantisTicket ticket = new MantisTicket(ihd.getId().intValue());

                ticket.putBuiltinValue(Key.PROJECT, project.getName());
                ticket.putBuiltinValue(Key.SUMMARY, ihd.getSummary());
                ticket.putBuiltinValue(Key.ID, ihd.getId().toString());

                MantisResolution resolution = data.getResolution(ihd.getResolution().intValue());
                if (resolution != null) {
                    ticket.putBuiltinValue(Key.RESOLUTION, resolution.getName());
                }

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
                // DC: Added so that it isn't necessary to retrieve all tasks
                // one at time
                // to see if they have changed since the last synchronization.
                // This cuts down on the number of soap requests that need to be
                // made
                // to the server.
                ticket.setLastChanged(ihd.getLast_updated().getTime());

                tickets.add(ticket);
            }

        } catch (RemoteException e) {
            throw new MantisRemoteException(e);
        }
    }

    private MantisTicket parseTicket(IssueData issue) throws MantisException {

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

        ticket.putBuiltinValue(Key.ADDITIONAL_INFO, issue.getAdditional_information());
        ticket.putBuiltinValue(Key.STEPS_TO_REPRODUCE, issue.getSteps_to_reproduce());

        ticket.putBuiltinValue(Key.REPORTER, issue.getReporter().getName());
        if (issue.getHandler() != null) {
            ticket.putBuiltinValue(Key.ASSIGNED_TO, issue.getHandler().getName());
        }

        if (issue.getNotes() != null)
            for (IssueNoteData ind : issue.getNotes())
                parseNote(ticket, ind);

        if (issue.getAttachments() != null)
            for (AttachmentData ad : issue.getAttachments())
                parseAttachment(ticket, ad);

        if (issue.getRelationships() != null)
            for (RelationshipData rel : issue.getRelationships())
                parseRelation(ticket, rel);

        
        if (issue.getCustom_fields() != null)
            for (CustomFieldValueForIssueData customFieldValue : issue.getCustom_fields())
                ticket.putCustomFieldValue(customFieldValue.getField().getName(), customFieldValue
                        .getValue());

        return ticket;
    }

    private void parseAttachment(MantisTicket ticket, AttachmentData ad) {

        MantisAttachment ma = new MantisAttachment();
        ma.setContentType(ad.getContent_type());
        ma.setCreated(MantisUtils.transform(ad.getDate_submitted()));
        ma.setDownloadURL(ad.getDownload_url().getPath());
        ma.setFilename(ad.getFilename());
        ma.setSize(ad.getSize().intValue());
        ma.setId(ad.getId().intValue());
        ticket.addAttachment(ma);
    }

    private void parseNote(MantisTicket ticket, IssueNoteData ind) {

        MantisComment comment = new MantisComment();
        comment.setId(ind.getId().intValue());
        comment.setReporter(ind.getReporter().getName());
        comment.setText(ind.getText());
        comment.setDateSubmitted(MantisUtils.transform(ind.getDate_submitted()));
        comment.setLastModified(MantisUtils.transform(ind.getLast_modified()));
        ticket.addComment(comment);
    }

    private void parseRelation(MantisTicket ticket, RelationshipData relationData)
            throws MantisException {

        MantisRelationship relationship = new MantisRelationship();
        relationship.setId(relationData.getId().intValue());
        relationship.setTargetId(relationData.getTarget_id().intValue());
        if (getRepositoryVersion(new NullProgressMonitor()).isHasProperTaskRelations())
            relationship.setType(MantisRelationship.RelationType.fromRelationId(relationData
                    .getType().getId()));
        else
            relationship.setType(MantisRelationship.RelationType.fromRelation(relationData
                    .getType().getName()));
        ticket.addRelationship(relationship);
    }

    @Override
    public synchronized void updateAttributes(IProgressMonitor monitor) throws MantisException {

        SubMonitor subMonitor = SubMonitor.convert(monitor, "Updating attributes", 15);

        try {
            
            ProjectData[] projectData = getSOAP().mc_projects_get_user_accessible(username,
                    password);
            projects = new MantisProject[countProjects(projectData)];
            addProjects(0, projectData, 0);
            Policy.advance(subMonitor, 1);

            // load project-specific data
            subMonitor.setWorkRemaining(projects.length * 2 + 11);

            for (MantisProject project : projects) {
                loadProjectFilters(subMonitor, project);
                loadProjectCustomFields(subMonitor, project);
            }

            // get and parse repository version
            String versionString = getSOAP().mc_version();
            RepositoryVersion version = RepositoryVersion.fromVersionString(versionString);
            data.setRepositoryVersion(version);
            Policy.advance(subMonitor, 1);

            ObjectRef[] result = getSOAP().mc_enum_priorities(username, password);
            data.priorities = new ArrayList<MantisPriority>(result.length);
            for (ObjectRef item : result) {
                data.priorities.add(parsePriority(item));
            }
            Policy.advance(subMonitor, 1);

            result = getSOAP().mc_enum_status(username, password);
            data.statuses = new ArrayList<MantisTicketStatus>(result.length);
            for (ObjectRef item : result) {
                data.statuses.add(parseTicketStatus(item));
            }
            Policy.advance(subMonitor, 1);

            result = getSOAP().mc_enum_severities(username, password);
            data.severities = new ArrayList<MantisSeverity>(result.length);
            for (ObjectRef item : result) {
                data.severities.add(parseSeverity(item));
            }
            Policy.advance(subMonitor, 1);

            result = getSOAP().mc_enum_resolutions(username, password);
            data.resolutions = new ArrayList<MantisResolution>(result.length);
            for (ObjectRef item : result) {
                data.resolutions.add(parseResolution(item));
            }
            Policy.advance(subMonitor, 1);

            result = getSOAP().mc_enum_reproducibilities(username, password);
            data.reproducibilities = new ArrayList<MantisReproducibility>(result.length);
            for (ObjectRef item : result) {
                data.reproducibilities.add(parseReproducibility(item));
            }
            Policy.advance(subMonitor, 1);

            result = getSOAP().mc_enum_projections(username, password);
            data.projections = new ArrayList<MantisProjection>(result.length);
            for (ObjectRef item : result) {
                data.projections.add(parseProjection(item));
            }
            Policy.advance(subMonitor, 1);

            result = getSOAP().mc_enum_etas(username, password);
            data.etas = new ArrayList<MantisETA>(result.length);
            for (ObjectRef item : result) {
                data.etas.add(parseETA(item));
            }
            Policy.advance(subMonitor, 1);

            result = getSOAP().mc_enum_view_states(username, password);
            data.viewStates = new ArrayList<MantisViewState>(result.length);
            for (ObjectRef item : result) {
                data.viewStates.add(parseViewState(item));
            }
            Policy.advance(subMonitor, 1);
            
            loadCustomFieldTypes(subMonitor);


        } catch (RemoteException e) {
            MantisCorePlugin.log(e);
            throw new MantisRemoteException(e);
        }
    }

    private MantisCustomField parseCustomFieldData(CustomFieldDefinitionData customFieldData) {

        MantisCustomField customField = new MantisCustomField();
        customField.setId(customFieldData.getField().getId().intValue());
        customField.setName(customFieldData.getField().getName());
        customField.setType(MantisCustomFieldType.fromMantisConstant(customFieldData.getType()
                .intValue()));
        customField.setDefaultValue(customFieldData.getDefault_value());
        if ( customFieldData.getPossible_values() != null)
            customField.setPossibleValues(customFieldData.getPossible_values().split("\\|"));

        return customField;
    }

    private void loadCustomFieldTypes(IProgressMonitor monitor) throws RemoteException,
            MantisException {

        ObjectRef[] mcEnumCustomFieldTypes = getSOAP().mc_enum_custom_field_types(username,
                password);
        Policy.advance(monitor, 1);

        List<MantisCustomFieldType> customFieldTypes = new ArrayList<MantisCustomFieldType>(
                mcEnumCustomFieldTypes.length);

        for (ObjectRef objectRef : mcEnumCustomFieldTypes) {

            MantisCustomFieldType customFieldType = MantisCustomFieldType
                    .fromMantisConstant(objectRef.getId().intValue());
            if (customFieldType == null) {
                MantisCorePlugin.log(new Status(IStatus.WARNING, MantisCorePlugin.PLUGIN_ID,
                        "Unknown custom field type " + objectRef.getId() + " ("
                                + objectRef.getName() + " ). Ignoring."));
                continue;
            }

            customFieldTypes.add(customFieldType);
        }

        data.setCustomFieldTypes(customFieldTypes);
    }

    private void loadProjectFilters(SubMonitor subMonitor, MantisProject project)
            throws MantisException {

        try {
            FilterData[] filterData = getSOAP().mc_filter_get(username, password,
                    BigInteger.valueOf(project.getValue()));
            Policy.advance(subMonitor, 1);
            
            MantisProjectFilter[] data = new MantisProjectFilter[filterData.length];

            for (int x = 0; x < filterData.length; x++) {
                data[x] = new MantisProjectFilter(filterData[x].getName(), filterData[x].getId()
                        .intValue());
            }

            filters.put(project.getName(), data);
            
        } catch (RemoteException e) {
            MantisCorePlugin.log("Failed retrieving filters for project " + project.getName()
                    + " .", e);
        }
    }

    private void loadProjectCustomFields(SubMonitor subMonitor, MantisProject project)
            throws MantisException {

        try {
            CustomFieldDefinitionData[] customFields = getSOAP().mc_project_get_custom_fields(
                    username, password, BigInteger.valueOf(project.getValue()));
            Policy.advance(subMonitor, 1);
            
            List<MantisCustomField> projectCustomFields = new ArrayList<MantisCustomField>(
                    customFields.length);

            for (CustomFieldDefinitionData customFieldData : customFields)
                projectCustomFields.add(parseCustomFieldData(customFieldData));

            data.setCustomFields(project.getValue(), projectCustomFields);
        } catch (RemoteException e) {
            MantisCorePlugin.log("Failed retrieving custom fields for project " + project.getName()
                    + " .", e);
        }
    }

    private MantisViewState parseViewState(ObjectRef or) {

        MantisViewState item = new MantisViewState(or.getName(), or.getId().intValue());
        return item;
    }

    private MantisETA parseETA(ObjectRef or) {

        MantisETA item = new MantisETA(or.getName(), or.getId().intValue());
        return item;
    }

    private MantisProjection parseProjection(ObjectRef or) {

        MantisProjection item = new MantisProjection(or.getName(), or.getId().intValue());
        return item;
    }

    private MantisReproducibility parseReproducibility(ObjectRef or) {

        MantisReproducibility item = new MantisReproducibility(or.getName(), or.getId().intValue());
        return item;
    }

    private MantisResolution parseResolution(ObjectRef or) {

        MantisResolution item = new MantisResolution(or.getName(), or.getId().intValue());
        return item;
    }

    private MantisSeverity parseSeverity(ObjectRef or) {

        MantisSeverity item = new MantisSeverity(or.getName(), or.getId().intValue());
        return item;
    }

    private MantisTicketStatus parseTicketStatus(ObjectRef or) {

        MantisTicketStatus item = new MantisTicketStatus(or.getName(), or.getId().intValue());
        return item;
    }

    private MantisPriority parsePriority(ObjectRef or) {

        MantisPriority item = new MantisPriority(or.getName(), or.getId().intValue());
        return item;
    }

    // private MantisVersion parseVersion(Map<?, ?> result) {
    // MantisVersion version = new MantisVersion((String) result.get("name"));
    // version.setTime(MantisUtils.parseDate((Integer) result.get("time")));
    // version.setDescription((String) result.get("description"));
    // return version;
    // }

    public byte[] getAttachmentData(int attachmentID, IProgressMonitor monitor) throws MantisException {

        try {
            byte[] attachment = getSOAP().mc_issue_attachment_get(username, password,
                    BigInteger.valueOf(attachmentID));
            Policy.advance(monitor, 1);
            
            return attachment;
            
        } catch (RemoteException e) {
            throw new MantisRemoteException(e);
        }
    }

    public void putAttachmentData(int ticketID, String filename, byte[] data, IProgressMonitor monitor)
            throws MantisException {

        boolean requiresBase64EncodedAttachment = getRepositoryVersion(monitor)
                .isRequiresBase64EncodedAttachment();

        try {

            // TODO - we should determine the encoding
            if (requiresBase64EncodedAttachment)
                data = Base64.encode(data).getBytes();

            getSOAP().mc_issue_attachment_add(username, password, BigInteger.valueOf(ticketID),
                    filename, "bug", data);
            Policy.advance(monitor, 1);
            
        } catch (RemoteException e) {
            MantisCorePlugin.log(e);
            throw new MantisRemoteException(e);
        }
    }

    public int createTicket(MantisTicket ticket, IProgressMonitor monitor) throws MantisException {

        IssueData issue = createSOAPIssue(ticket, monitor);

        BigInteger id;
        try {
            id = getSOAP().mc_issue_add(username, password, issue);
            Policy.advance(monitor, 1);

            if (getRepositoryVersion(monitor).isHasProperTaskRelations())
                createRelationships(ticket, monitor, id);
        } catch (RemoteException e) {
            MantisCorePlugin.log(e);
            throw new MantisRemoteException(e);
        }

        ticket.setId(id.intValue());

        return ticket.getId();
    }

    private void createRelationships(MantisTicket ticket, IProgressMonitor monitor, BigInteger id)
            throws RemoteException, MantisException {

        MantisRelationship[] relationships = ticket.getRelationships();
        for (MantisRelationship relationship : relationships) {

            RelationshipData relationshipData = toRelationshipData(relationship);
            getSOAP().mc_issue_relationship_add(username, password, id, relationshipData);
            Policy.advance(monitor, 1);
        }
    }

    private RelationshipData toRelationshipData(MantisRelationship relationship) {

        ObjectRef relationType = new ObjectRef(BigInteger.valueOf(relationship.getType()
                .getMantisConstant()), "");
        RelationshipData relationshipData = new RelationshipData();
        relationshipData.setType(relationType);
        relationshipData.setTarget_id(BigInteger.valueOf(relationship.getTargetId()));
        return relationshipData;
    }

    private IssueData createSOAPIssue(MantisTicket ticket, IProgressMonitor monitor)
            throws MantisException {

        ObjectRef project = getProject(ticket.getValue(Key.PROJECT), monitor);

        IssueData issue = new IssueData();
        issue.setSummary(ticket.getValue(Key.SUMMARY));
        issue.setDescription(ticket.getValue(Key.DESCRIPTION));
        // issue.setDate_submitted(ticket.getValue(Key.DATE_SUBMITTED));
        issue.setSeverity(newRef(data.severities, Key.SEVERITY, ticket));
        issue.setResolution(newRef(data.resolutions, Key.RESOLUTION, ticket));
        issue.setPriority(newRef(data.priorities, Key.PRIORITY, ticket));
        issue.setReproducibility(newRef(data.reproducibilities, Key.REPRODUCIBILITY, ticket));
        issue.setProjection(newRef(data.projections, Key.PROJECTION, ticket));
        issue.setEta(newRef(data.etas, Key.ETA, ticket));
        issue.setView_state(newRef(data.viewStates, Key.VIEW_STATE, ticket));

        issue.setProject(project);
        issue.setCategory(ticket.getValue(Key.CATEOGRY));

        issue.setVersion(ticket.getValueAndFilterNone(Key.VERSION));
        issue.setFixed_in_version(ticket.getValueAndFilterNone(Key.FIXED_IN));
        if (getRepositoryVersion(monitor).isHasProperTaskRelations())
            issue.setTarget_version(ticket.getValueAndFilterNone(Key.TARGET_VERSION));

        issue.setSteps_to_reproduce(ticket.getValue(Key.STEPS_TO_REPRODUCE));
        issue.setAdditional_information(ticket.getValue(Key.ADDITIONAL_INFO));

        issue.setStatus(newRef(data.statuses, Key.STATUS, ticket));

        if (MantisUtils.isEmpty(ticket.getValue(Key.REPORTER))) {
            issue.setReporter(createReport(username));
        } else {
            issue.setReporter(createReport(ticket.getValue(Key.REPORTER)));
        }

        issue.setHandler(createReport(ticket.getValue(Key.ASSIGNED_TO)));
        issue.setLast_updated(MantisUtils.transform(new Date()));

        setCustomFields(ticket, project, issue);

        return issue;
    }


    
    private void setCustomFields(MantisTicket ticket, ObjectRef project, IssueData issue) {

        if ( ticket.getCustomFieldValues().isEmpty())
            return;
        
        List<CustomFieldValueForIssueData> customFieldValues = new ArrayList<CustomFieldValueForIssueData>(
                ticket.getCustomFieldValues().size());

        for (Map.Entry<String, String> entry : ticket.getCustomFieldValues().entrySet())
            customFieldValues.add(extractCustomFieldValue(project, entry));
        
        issue.setCustom_fields(customFieldValues.toArray(new CustomFieldValueForIssueData[0]));
    }
    
    private CustomFieldValueForIssueData extractCustomFieldValue(ObjectRef project,
            Map.Entry<String, String> entry) {

        String customFieldName = entry.getKey();
        MantisCustomField customField = data.getCustomFieldByProjectIdAndFieldName(project.getId()
                .intValue(), customFieldName);
        ObjectRef customFieldRef = new ObjectRef(BigInteger.valueOf(customField.getId()),
                customField.getName());
        CustomFieldValueForIssueData customFieldValueForIssueData = new CustomFieldValueForIssueData(
                customFieldRef, entry.getValue());
        return customFieldValueForIssueData;
    }
    

    private AccountData createReport(String name) {

        AccountData data = new AccountData();
        data.setName(name);
        return data;
    }

    private ObjectRef newRef(List<? extends MantisTicketAttribute> atttributes, Key key,
            MantisTicket ticket) throws MantisException {

        ObjectRef ref = new ObjectRef();

        ref.setName(ticket.getValue(key));

        for (MantisTicketAttribute attribute : atttributes) {
            if (attribute.getName().equals(ref.getName())) {
                ref.setId(BigInteger.valueOf(attribute.getValue()));
                return ref;
            }
        }

        // throw new
        // MantisException("Could not find id for value["+ref.getName()+"] in key["+key.getKey()+"]");
        return null;

    }

    public void updateTicket(MantisTicket ticket, String comment, IProgressMonitor monitor)
            throws MantisException {

        IssueData issue = createSOAPIssue(ticket, monitor);
        issue.setId(BigInteger.valueOf(ticket.getId()));

        // add comment...
        IssueNoteData ind = new IssueNoteData();
        ind.setDate_submitted(MantisUtils.transform(new Date()));
        ind.setLast_modified(MantisUtils.transform(new Date()));
        ind.setReporter(createReport(username));
        ind.setText(comment);
        try {
            // add comment first because when updating the issue to resolved
            // comments can't be added
            if (!MantisUtils.isEmpty(comment)) {
                BigInteger id = getSOAP().mc_issue_note_add(username, password, issue.getId(), ind);
                Policy.advance(monitor, 1);
                ind.setId(id);
                parseNote(ticket, ind);
            }

            getSOAP().mc_issue_update(username, password, issue.getId(), issue);
            Policy.advance(monitor, 1);

        } catch (RemoteException e) {
            MantisCorePlugin.log(e);
            throw new MantisRemoteException(e);
        }
    }

    protected void updateUsers(String project, IProgressMonitor monitor) throws MantisException {

        try {
            ObjectRef projectRef = getProject(project, monitor);

            int reporterAccessLevel = safeGetIntConfigurationValue(REPORTER_THRESHOLD,
                    DefaultConstantValues.Threshold.REPORT_BUG_THRESHOLD.getValue(), monitor);
            int developerAccessLevel = safeGetIntConfigurationValue(DEVELOPER_THRESHOLD,
                    DefaultConstantValues.Threshold.UPDATE_BUG_ASSIGN_THRESHOLD.getValue(), monitor);

            AccountData[] accounts = getSOAP().mc_project_get_users(username, password,
                    projectRef.getId(), BigInteger.valueOf(reporterAccessLevel));
            Policy.advance(monitor, 1);
            
            AccountData[] developerAccounts;
            
            if (reporterAccessLevel == developerAccessLevel)
                developerAccounts = accounts;
            else {
                developerAccounts = getSOAP().mc_project_get_users(username, password,
                        projectRef.getId(), BigInteger.valueOf(developerAccessLevel));
                Policy.advance(monitor, 1);
            }

            String[] users = new String[accounts.length];
            for (int i = 0; i < accounts.length; i++)
                users[i] = accounts[i].getName();

            String[] devUsers = new String[developerAccounts.length];

            for (int i = 0; i < developerAccounts.length; i++)
                devUsers[i] = developerAccounts[i].getName();

            userData.usersPerProject.put(project, users);
            userData.developersPerProject.put(project, devUsers);
        } catch (RemoteException e) {
            MantisCorePlugin.log(e);
            throw new MantisRemoteException(e);
        }
    }

	private int safeGetIntConfigurationValue(String optionName, int defaultValue, IProgressMonitor monitor) throws RemoteException,
			MantisException {
		try {	
			int value = Integer.parseInt(getSOAP().mc_config_get_string(username, password, optionName));
			Policy.advance(monitor, 1);
			
			return value;
		} catch (NumberFormatException e) {
			MantisCorePlugin.log(new Status(Status.WARNING, MantisCorePlugin.PLUGIN_ID, "Failed parsing config option " + optionName + " using default value.", e));
		}
		return defaultValue;
	}

    public MantisVersion[] getVersions(String project, IProgressMonitor monitor) throws MantisException {

        List<MantisVersion> versions = new ArrayList<MantisVersion>();
        try {
            ObjectRef projectRef = getProject(project, monitor);

            ProjectVersionData[] data = getSOAP().mc_project_get_versions(username, password,
                    projectRef.getId());
            Policy.advance(monitor, 1);

            /* Convert the ProjectVersionData's into MantisVersions */
            for (ProjectVersionData v : data) {
                MantisVersion version = new MantisVersion(v.getName());
                version.setDescription(v.getDescription());

                Calendar cal = v.getDate_order();
                version.setTime(cal.getTime());
                versions.add(version);

                version.setReleased(v.getReleased());
            }
        } catch (RemoteException e) {
            MantisCorePlugin.log(e);
            throw new MantisRemoteException(e);
        }
        return versions.toArray(new MantisVersion[versions.size()]);
    }

}