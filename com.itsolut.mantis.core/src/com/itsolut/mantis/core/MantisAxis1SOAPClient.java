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
import java.util.concurrent.Callable;

import javax.xml.rpc.Call;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.AxisFault;
import org.apache.axis.configuration.FileProvider;
import org.apache.axis.encoding.Base64;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.Policy;
import org.xml.sax.SAXException;

import com.itsolut.mantis.binding.AccountData;
import com.itsolut.mantis.binding.AttachmentData;
import com.itsolut.mantis.binding.CustomFieldDefinitionData;
import com.itsolut.mantis.binding.CustomFieldValueForIssueData;
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
 * Represents a Mantis repository that is accessed through the MantisConnect SOAP Interface.
 * 
 * @author Chris Hane
 */
@SuppressWarnings("restriction")
public class MantisAxis1SOAPClient extends AbstractMantisClient {

    private static final String OLD_SF_NET_URL = "https://apps.sourceforge.net/mantisbt/";

    private static final String NEW_SF_NET_URL = "https://sourceforge.net/apps/mantisbt/";

    private transient MantisConnectPortType soap;

    private String httpUsername;

    private String httpPassword;

    public MantisAxis1SOAPClient(URL url, String username, String password, String httpUsername, String httpPassword,
            AbstractWebLocation webLocation) {

        super(url, username, password, webLocation);

        this.httpUsername = httpUsername;
        this.httpPassword = httpPassword;

        try {
            soap = this.getSOAP();

            if (httpUsername != null && httpUsername.length() > 0 && httpPassword != null && httpPassword.length() > 0) {
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
                FileProvider provider = new FileProvider(this.getClass().getClassLoader().getResourceAsStream(
                        "client-config.wsdd"));
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

    @Override
    protected boolean doLogin(IProgressMonitor monitor) {

        return true;
    }

    @Override
    protected boolean isAuthenticationException(Exception exception) {

        if (!(exception instanceof MantisException))
            return false;

        MantisException mantisException = (MantisException) exception;

        return mantisException.getMessage() != null && mantisException.getMessage().indexOf("Access denied") != -1;

    }

    @Override
    protected <T> T call(IProgressMonitor monitor, Callable<T> runnable) throws MantisException {

        try {
            return super.call(monitor, runnable);
        } catch (MantisException e) {
            throw e;
        } catch (RemoteException e) {
            throw wrap(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected <T> T callOnce(IProgressMonitor monitor, Callable<T> runnable) throws MantisException {

        try {
            return super.callOnce(monitor, runnable);
        } catch (MantisException e) {
            throw e;
        } catch (RemoteException e) {
            throw wrap(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MantisRemoteException wrap(Exception e) {

        StringBuilder message = new StringBuilder();

        if (isSourceforgeRepoWithoutHttpAuth())
            message.append("For SF.net hosted apps, please make sure to use HTTP authentication only.").append('\n');

        if (repositoryUrl.toExternalForm().startsWith(OLD_SF_NET_URL))
            message.append("SF.net hosted apps have been moved to https://sourceforge.net/apps/mantisbt/").append('\n');

        message.append("Repository validation failed: ");

        if (e instanceof AxisFault) {

            AxisFault axisFault = (AxisFault) e;

            if (axisFault.getCause() instanceof SAXException)
                message.append("the repository has returned an invalid XML response : "
                        + String.valueOf(axisFault.getCause().getMessage()) + " .");
            else if (e.getMessage() != null)
                message.append(" :").append(e.getMessage()).append('\n');

        } else if (e.getMessage() != null)
            message.append(" :").append(e.getMessage()).append('\n');

        return new MantisRemoteException(message.toString(), e);

    }

    private boolean isSourceforgeRepoWithoutHttpAuth() {

        return repositoryUrl.toExternalForm().startsWith(NEW_SF_NET_URL)
                && (httpUsername.length() == 0 || httpPassword.length() == 0);
    }

    public IssueData getIssueData(final int issueId, IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<IssueData>() {

            public IssueData call() throws MantisException, RemoteException {

                return getSOAP().mc_issue_get(getUsername(), getPassword(), BigInteger.valueOf(issueId));
            }

        });
    }

    public void search(final MantisSearch query, List<MantisTicket> tickets, final IProgressMonitor monitor)
            throws MantisException {

        String projectName = null;
        String filterName = null;
        for (MantisSearchFilter filter : query.getFilters()) {
            if ("project".equals(filter.getFieldName())) {
                projectName = filter.getValues().get(0);

            } else if ("filter".equals(filter.getFieldName())) {
                filterName = filter.getValues().get(0);
            }
        }

        final ObjectRef project = getProject(projectName, monitor);
        final FilterData filter = getFilter(projectName, filterName, monitor);

        if (project == null || filter == null)
            throw new MantisException(
                    "Unable to create query . Please make sure that the repository credentials and the query parameters are valid.");

        IssueHeaderData[] ihds = call(monitor, new Callable<IssueHeaderData[]>() {

            public IssueHeaderData[] call() throws Exception {

                IssueHeaderData[] headers = getSOAP().mc_filter_get_issue_headers(getUsername(), getPassword(),
                        project.getId(), // project
                        filter.getId(), // filter
                        BigInteger.valueOf(1), // start page
                        BigInteger.valueOf(query.getLimit())); // # per page

                Policy.advance(monitor, 1);

                return headers;
            }

        });

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

    public byte[] getAttachmentData(final int attachmentID, final IProgressMonitor monitor) throws MantisException {

        byte[] attachment = call(monitor, new Callable<byte[]>() {

            public byte[] call() throws Exception {

                byte[] response = getSOAP().mc_issue_attachment_get(getUsername(), getPassword(),
                        BigInteger.valueOf(attachmentID));
                Policy.advance(monitor, 1);
                return response;
            }

        });

        return attachment;

    }

    public void putAttachmentData(final int ticketID, final String filename, byte[] data, final IProgressMonitor monitor)
            throws MantisException {

        boolean requiresBase64EncodedAttachment = getRepositoryVersion(monitor).isRequiresBase64EncodedAttachment();

        final byte[] encoded = requiresBase64EncodedAttachment ? Base64.encode(data).getBytes() : data;

        call(monitor, new Callable<Void>() {

            public Void call() throws Exception {

                getSOAP().mc_issue_attachment_add(getUsername(), getPassword(), BigInteger.valueOf(ticketID), filename,
                        "bug", encoded);
                Policy.advance(monitor, 1);

                return null;
            }
        });

    }
    
    public IssueHeaderData[] getIssueHeaders(final int projectId, final int filterId, final int limit, IProgressMonitor monitor) throws MantisException {
        
        return call(monitor, new Callable<IssueHeaderData[]>() {

            public IssueHeaderData[] call() throws Exception {

                return getSOAP().mc_filter_get_issue_headers(getUsername(), getPassword(),
                        BigInteger.valueOf(projectId), // project
                        BigInteger.valueOf(filterId), // filter
                        BigInteger.valueOf(1), // start page
                        BigInteger.valueOf(limit)); // # per page

            }

        });
    }

    public int createTicket(MantisTicket ticket, final IProgressMonitor monitor) throws MantisException {

        final IssueData issue = createSOAPIssue(ticket, monitor);

        BigInteger id = call(monitor, new Callable<BigInteger>() {

            public BigInteger call() throws Exception {

                BigInteger result = getSOAP().mc_issue_add(getUsername(), getPassword(), issue);
                Policy.advance(monitor, 1);

                return result;

            }

        });

        if (getRepositoryVersion(monitor).isHasProperTaskRelations())
            createRelationships(ticket, monitor, id);

        ticket.setId(id.intValue());

        return ticket.getId();
    }

    private void createRelationships(MantisTicket ticket, final IProgressMonitor monitor, final BigInteger id)
            throws MantisException {

        final MantisRelationship[] relationships = ticket.getRelationships();

        if (relationships.length == 0)
            return;

        call(monitor, new Callable<Void>() {

            public Void call() throws Exception {

                for (MantisRelationship relationship : relationships) {

                    RelationshipData relationshipData = toRelationshipData(relationship);
                    getSOAP().mc_issue_relationship_add(getUsername(), getPassword(), id, relationshipData);
                    Policy.advance(monitor, 1);
                }

                return null;
            }
        });

    }

    private RelationshipData toRelationshipData(MantisRelationship relationship) {

        ObjectRef relationType = new ObjectRef(BigInteger.valueOf(relationship.getType().getMantisConstant()), "");
        RelationshipData relationshipData = new RelationshipData();
        relationshipData.setType(relationType);
        relationshipData.setTarget_id(BigInteger.valueOf(relationship.getTargetId()));
        return relationshipData;
    }

    private IssueData createSOAPIssue(MantisTicket ticket, IProgressMonitor monitor) throws MantisException {

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

        if (getRepositoryVersion(monitor).isHasDueDateSupport()) {
            String dueDate = ticket.getValue(Key.DUE_DATE);
            if (dueDate == null) {
                issue.setDue_date(null);
            } else {
                long dueDateMillis = Long.parseLong(dueDate);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(dueDateMillis);
                issue.setDue_date(calendar);
            }
        }

        issue.setSteps_to_reproduce(ticket.getValue(Key.STEPS_TO_REPRODUCE));
        issue.setAdditional_information(ticket.getValue(Key.ADDITIONAL_INFO));

        issue.setStatus(newRef(data.statuses, Key.STATUS, ticket));

        if (MantisUtils.isEmpty(ticket.getValue(Key.REPORTER))) {
            issue.setReporter(createReport(getUsername()));
        } else {
            issue.setReporter(createReport(ticket.getValue(Key.REPORTER)));
        }

        issue.setHandler(createReport(ticket.getValue(Key.ASSIGNED_TO)));
        issue.setLast_updated(MantisUtils.transform(new Date()));

        setCustomFields(ticket, project, issue);

        return issue;
    }

    private void setCustomFields(MantisTicket ticket, ObjectRef project, IssueData issue) {

        if (ticket.getCustomFieldValues().isEmpty())
            return;

        List<CustomFieldValueForIssueData> customFieldValues = new ArrayList<CustomFieldValueForIssueData>(ticket
                .getCustomFieldValues().size());

        for (Map.Entry<String, String> entry : ticket.getCustomFieldValues().entrySet())
            customFieldValues.add(extractCustomFieldValue(project, entry));

        issue.setCustom_fields(customFieldValues.toArray(new CustomFieldValueForIssueData[0]));
    }

    private CustomFieldValueForIssueData extractCustomFieldValue(ObjectRef project, Map.Entry<String, String> entry) {

        String customFieldName = entry.getKey();
        MantisCustomField customField = data.getCustomFieldByProjectIdAndFieldName(project.getId().intValue(),
                customFieldName);
        ObjectRef customFieldRef = new ObjectRef(BigInteger.valueOf(customField.getId()), customField.getName());
        CustomFieldValueForIssueData customFieldValueForIssueData = new CustomFieldValueForIssueData(customFieldRef,
                entry.getValue());
        return customFieldValueForIssueData;
    }

    private AccountData createReport(String name) {

        AccountData data = new AccountData();
        data.setName(name);
        return data;
    }

    private ObjectRef newRef(List<? extends MantisTicketAttribute> atttributes, Key key, MantisTicket ticket)
            throws MantisException {

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

    public void updateTicket(final MantisTicket ticket, final String comment, final IProgressMonitor monitor)
            throws MantisException {

        final IssueData issue = createSOAPIssue(ticket, monitor);
        issue.setId(BigInteger.valueOf(ticket.getId()));

        // add comment...
        final IssueNoteData ind = new IssueNoteData();
        ind.setDate_submitted(MantisUtils.transform(new Date()));
        ind.setLast_modified(MantisUtils.transform(new Date()));
        ind.setReporter(createReport(getUsername()));
        ind.setText(comment);

        call(monitor, new Callable<Void>() {

            public Void call() throws Exception {

                // add comment first because when updating the issue to resolved
                // comments can't be added
                if (!MantisUtils.isEmpty(comment)) {
                    BigInteger id = getSOAP().mc_issue_note_add(getUsername(), getPassword(), issue.getId(), ind);
                    Policy.advance(monitor, 1);
                    ind.setId(id);
                    parseNote(ticket, ind);
                }

                getSOAP().mc_issue_update(getUsername(), getPassword(), issue.getId(), issue);
                Policy.advance(monitor, 1);

                return null;
            }

        });
    }

    public ProjectData[] getProjectData(IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<ProjectData[]>() {

            public ProjectData[] call() throws Exception {

                return getSOAP().mc_projects_get_user_accessible(getUsername(), getPassword());
            }
        });
    }

    public FilterData[] getProjectFilters(final int projectId, IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<FilterData[]>() {

            public FilterData[] call() throws Exception {

                return getSOAP().mc_filter_get(getUsername(), getPassword(), BigInteger.valueOf(projectId));
            }
        });

    }

    public CustomFieldDefinitionData[] getProjectCustomFields(final int projectId, IProgressMonitor monitor)
            throws MantisException {

        return call(monitor, new Callable<CustomFieldDefinitionData[]>() {

            public CustomFieldDefinitionData[] call() throws Exception {

                return getSOAP().mc_project_get_custom_fields(getUsername(), getPassword(),
                        BigInteger.valueOf(projectId));
            }

        });

    }

    public String getVersion(IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<String>() {

            public String call() throws Exception {

                return getSOAP().mc_version();
            }
        });

    }

    public String getStringConfiguration(IProgressMonitor monitor, final String configurationParameter)
            throws MantisException {

        return call(monitor, new Callable<String>() {

            public String call() throws Exception {

                return getSOAP().mc_config_get_string(getUsername(), getPassword(), configurationParameter);
            }
        });
    }

    public ObjectRef[] getPriorities(IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<ObjectRef[]>() {

            public ObjectRef[] call() throws Exception {

                return getSOAP().mc_enum_priorities(getUsername(), getPassword());

            }
        });

    }

    public ObjectRef[] getStatuses(IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<ObjectRef[]>() {

            public ObjectRef[] call() throws Exception {

                return getSOAP().mc_enum_status(getUsername(), getPassword());

            }
        });

    }

    public ObjectRef[] getSeverities(IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<ObjectRef[]>() {

            public ObjectRef[] call() throws Exception {

                return getSOAP().mc_enum_severities(getUsername(), getPassword());

            }
        });

    }

    public ObjectRef[] getResolutions(IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<ObjectRef[]>() {

            public ObjectRef[] call() throws Exception {

                return getSOAP().mc_enum_resolutions(getUsername(), getPassword());

            }
        });

    }

    public ObjectRef[] getReproducibilities(IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<ObjectRef[]>() {

            public ObjectRef[] call() throws Exception {

                return getSOAP().mc_enum_reproducibilities(getUsername(), getPassword());

            }
        });

    }

    public ObjectRef[] getProjections(IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<ObjectRef[]>() {

            public ObjectRef[] call() throws Exception {

                return getSOAP().mc_enum_projections(getUsername(), getPassword());

            }
        });

    }

    public ObjectRef[] getEtas(IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<ObjectRef[]>() {

            public ObjectRef[] call() throws Exception {

                return getSOAP().mc_enum_etas(getUsername(), getPassword());

            }
        });

    }

    public ObjectRef[] getViewStates(IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<ObjectRef[]>() {

            public ObjectRef[] call() throws Exception {

                return getSOAP().mc_enum_view_states(getUsername(), getPassword());

            }
        });

    }

    public String[] getProjectCategories(final int value, IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<String[]>() {

            public String[] call() throws Exception {

                return getSOAP().mc_project_get_categories(getUsername(), getPassword(), BigInteger.valueOf(value));

            }
        });
    }

    public AccountData[] getProjectUsers(final int projectId, final int reporterThreshold, IProgressMonitor monitor)
            throws MantisException {

        return call(monitor, new Callable<AccountData[]>() {

            public AccountData[] call() throws Exception {

                return getSOAP().mc_project_get_users(getUsername(), getPassword(), BigInteger.valueOf(projectId),
                        BigInteger.valueOf(reporterThreshold));

            }
        });
    }

    public ProjectVersionData[] getProjectVersions(final int projectId, IProgressMonitor monitor)
            throws MantisException {

        return call(monitor, new Callable<ProjectVersionData[]>() {

            public ProjectVersionData[] call() throws Exception {

                return getSOAP().mc_project_get_versions(getUsername(), getPassword(), BigInteger.valueOf(projectId));
            }
        });
    }

}