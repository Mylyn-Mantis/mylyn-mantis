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
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.rpc.Call;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import org.apache.axis.AxisFault;
import org.apache.axis.configuration.FileProvider;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.internal.provisional.commons.soap.AbstractSoapClient;
import org.xml.sax.SAXException;

import com.itsolut.mantis.binding.AccountData;
import com.itsolut.mantis.binding.CustomFieldDefinitionData;
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

/**
 * Represents a Mantis repository that is accessed through the MantisConnect
 * SOAP Interface.
 * 
 * @author Chris Hane
 */
public class MantisAxis1SOAPClient extends AbstractSoapClient {

    private transient MantisConnectPortType soap;

    private AbstractWebLocation location;

    public MantisAxis1SOAPClient(AbstractWebLocation webLocation) throws MantisException {

        this.location = webLocation;

        soap = this.getSOAP();

        configureHttpAuthentication();

    }
        
    private void configureHttpAuthentication() {

        // TODO: test http authentication, see if we still need this

        AuthenticationCredentials httpCredentials = location.getCredentials(AuthenticationType.HTTP);
        if (httpCredentials == null)
            return;

        Stub stub = (Stub) soap;
        stub._setProperty(Call.USERNAME_PROPERTY, httpCredentials.getUserName());
        stub._setProperty(Call.PASSWORD_PROPERTY, httpCredentials.getPassword());
    }

    private boolean doesNotHaveHttpAuth() {

        return location.getCredentials(AuthenticationType.HTTP) == null;
    }

    @Override
    protected AbstractWebLocation getLocation() {

        return location;
    }

    private String getUsername() {

        return location.getCredentials(AuthenticationType.REPOSITORY).getUserName();
    }

    private String getPassword() {

        return location.getCredentials(AuthenticationType.REPOSITORY).getPassword();
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

                soap = locator.getMantisConnectPort(new URL(location.getUrl()));
            } catch (ServiceException e) {
                throw new MantisRemoteException(e);
            } catch (MalformedURLException e) {
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

        return mantisException.getMessage() != null && mantisException.getMessage().toLowerCase().indexOf("access denied") != -1;

        
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

    private MantisRemoteException wrap(RemoteException e) {

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

        if (location.getUrl().startsWith(SourceForgeConstants.OLD_SF_NET_URL))
            message.append("SF.net hosted apps have been moved to https://sourceforge.net/apps/mantisbt/").append('\n');

        for (ProjectData pd : pds) {
            cnt++;
            cnt += countProjects(pd.getSubprojects());
        }

        return cnt;
    }

            if (axisFault.getCause() instanceof SAXException)
                message.append("The repository has returned an invalid XML response : "
                        + String.valueOf(axisFault.getCause().getMessage()) + " .");
            else if (e.getMessage() != null)
                message.append(e.getMessage());

        } else if (e.getMessage() != null)
            message.append(e.getMessage());

    private final Map<String, MantisProjectCategory[]> categories = new HashMap<String, MantisProjectCategory[]>(
            3);

    public MantisProjectCategory[] getProjectCategories(String projectName, IProgressMonitor monitor) throws MantisException {

        if (categories.containsKey(projectName)) {
            return categories.get(projectName);
        }

        return location.getUrl().startsWith(SourceForgeConstants.NEW_SF_NET_URL) && doesNotHaveHttpAuth();
    }

    private final Map<String, MantisProjectFilter[]> filters = new HashMap<String, MantisProjectFilter[]>(
            3);

    public MantisProjectFilter[] getProjectFilters(String projectName, IProgressMonitor monitor) throws MantisException {

        // cached value
        if (filters.containsKey(projectName))
            return filters.get(projectName);

        try {

            ObjectRef project = getProject(projectName, monitor);

    public byte[] getIssueAttachment(final int attachmentID, final IProgressMonitor monitor) throws MantisException {

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

                return getSOAP()
                        .mc_issue_attachment_get(getUsername(), getPassword(), BigInteger.valueOf(attachmentID));
            }
            
            String resolvedStatus = getSOAP().mc_config_get_string(username, password, RESOLVED_STATUS_THRESHOLD);
            data.setResolvedStatusThreshold(Integer.parseInt(resolvedStatus));
            Policy.advance(subMonitor, 1);

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

    public void addIssueAttachment(final int ticketID, final String filename, final byte[] data,
            final IProgressMonitor monitor) throws MantisException {

    private void loadCustomFieldTypes(IProgressMonitor monitor) throws RemoteException,
            MantisException {

        ObjectRef[] mcEnumCustomFieldTypes = getSOAP().mc_enum_custom_field_types(username,
                password);
        Policy.advance(monitor, 1);

                getSOAP().mc_issue_attachment_add(getUsername(), getPassword(), BigInteger.valueOf(ticketID), filename,
                        "bug", data);

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

    public IssueHeaderData[] getIssueHeaders(final int projectId, final int filterId, final int limit,
            IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<IssueHeaderData[]>() {

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

    public int addIssue(final IssueData issue, IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<BigInteger>() {

            data.setCustomFields(project.getValue(), projectCustomFields);
        } catch (RemoteException e) {
            MantisCorePlugin.log("Failed retrieving custom fields for project " + project.getName()
                    + " .", e);
        }
    }

                BigInteger result = getSOAP().mc_issue_add(getUsername(), getPassword(), issue);

        MantisViewState item = new MantisViewState(or.getName(), or.getId().intValue());
        return item;
    }

    private MantisETA parseETA(ObjectRef or) {

        }).intValue();

    }

    public void addRelationship(final int ticketId, final RelationshipData relationshipData, IProgressMonitor monitor)
            throws MantisException {

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

                getSOAP().mc_issue_relationship_add(getUsername(), getPassword(), BigInteger.valueOf(ticketId),
                        relationshipData);
                return null;

            }
        });
            if (getRepositoryVersion(monitor).isHasProperTaskRelations())
                createRelationships(ticket, monitor, id);
        } catch (RemoteException e) {
            MantisCorePlugin.log(e);
            throw new MantisRemoteException(e);
        }

        ticket.setId(id.intValue());

        return ticket.getId();
    }

    public void addNote(final int issueId, final IssueNoteData ind, IProgressMonitor monitor) throws MantisException {

        call(monitor, new Callable<Void>() {

            public Void call() throws Exception {

                getSOAP().mc_issue_note_add(getUsername(), getPassword(), BigInteger.valueOf(issueId), ind);
                return null;

            }
        });

    }

    public void updateIssue(final IssueData issue, IProgressMonitor monitor) throws MantisException {

        call(monitor, new Callable<Void>() {

            public Void call() throws Exception {

                getSOAP().mc_issue_update(getUsername(), getPassword(), issue.getId(), issue);
                return null;

            }
        });

    }

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