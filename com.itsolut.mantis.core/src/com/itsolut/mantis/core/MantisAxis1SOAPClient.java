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
import java.util.concurrent.Callable;

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
import com.itsolut.mantis.core.exception.MantisLocalException;
import com.itsolut.mantis.core.exception.MantisRemoteException;

/**
 * Represents a Mantis repository that is accessed through the MantisConnect SOAP Interface.
 * 
 * @author Chris Hane
 */
@SuppressWarnings("restriction")
public class MantisAxis1SOAPClient extends AbstractSoapClient {

    private transient MantisConnectPortType soap;

    private AbstractWebLocation location;

    public MantisAxis1SOAPClient(AbstractWebLocation webLocation) throws MantisException {

        this.location = webLocation;

        soap = this.getSOAP();

        configureHttpAuthentication();

    }

    private void configureHttpAuthentication() {

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
                FileProvider provider = new FileProvider(this.getClass().getClassLoader().getResourceAsStream(
                        "client-config.wsdd"));
                CustomMantisConnectLocator locator = new CustomMantisConnectLocator(provider);
                locator.setLocation(getLocation());

                soap = locator.getMantisConnectPort(new URL(location.getUrl()));
            } catch (ServiceException e) {
                throw new MantisRemoteException(e);
            } catch (MalformedURLException e) {
                throw new MantisLocalException(e);
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

        return mantisException.getMessage() != null
                && mantisException.getMessage().toLowerCase().indexOf("access denied") != -1;

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

    private MantisRemoteException wrap(RemoteException e) {

        StringBuilder message = new StringBuilder();

        if (isSourceforgeRepoWithoutHttpAuth())
            message.append("For SF.net hosted apps, please make sure to use HTTP authentication only.").append('\n');

        if (location.getUrl().startsWith(SourceForgeConstants.OLD_SF_NET_URL))
            message.append("SF.net hosted apps have been moved to https://sourceforge.net/apps/mantisbt/").append('\n');

        if (e instanceof AxisFault) {

            AxisFault axisFault = (AxisFault) e;

            if (axisFault.getCause() instanceof SAXException)
                message.append("The repository has returned an invalid XML response : "
                        + String.valueOf(axisFault.getCause().getMessage()) + " .");
            else if (e.getMessage() != null)
                message.append(e.getMessage());

        } else if (e.getMessage() != null)
            message.append(e.getMessage());

        return new MantisRemoteException(message.toString(), e);

    }

    private boolean isSourceforgeRepoWithoutHttpAuth() {

        return location.getUrl().startsWith(SourceForgeConstants.NEW_SF_NET_URL) && doesNotHaveHttpAuth();
    }

    public IssueData getIssueData(final int issueId, IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<IssueData>() {

            public IssueData call() throws MantisException, RemoteException {

                return getSOAP().mc_issue_get(getUsername(), getPassword(), BigInteger.valueOf(issueId));
            }

        });
    }

    public byte[] getIssueAttachment(final int attachmentID, final IProgressMonitor monitor) throws MantisException {

        byte[] attachment = call(monitor, new Callable<byte[]>() {

            public byte[] call() throws Exception {

                return getSOAP()
                        .mc_issue_attachment_get(getUsername(), getPassword(), BigInteger.valueOf(attachmentID));
            }

        });

        return attachment;

    }

    public void addIssueAttachment(final int ticketID, final String filename, final byte[] data,
            final IProgressMonitor monitor) throws MantisException {

        call(monitor, new Callable<Void>() {

            public Void call() throws Exception {

                getSOAP().mc_issue_attachment_add(getUsername(), getPassword(), BigInteger.valueOf(ticketID), filename,
                        "bug", data);

                return null;
            }
        });

    }

    public IssueHeaderData[] getIssueHeaders(final int projectId, final int filterId, final int limit,
            IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<IssueHeaderData[]>() {

            public IssueHeaderData[] call() throws Exception {

                return getSOAP().mc_filter_get_issue_headers(getUsername(), getPassword(),
                        BigInteger.valueOf(projectId), // project
                        BigInteger.valueOf(filterId), // filter
                        BigInteger.ONE, // start page
                        BigInteger.valueOf(limit)); // # per page

            }

        });
    }

    public IssueHeaderData[] getIssueHeaders(final int projectId, final int limit, IProgressMonitor monitor)
            throws MantisException {

        return call(monitor, new Callable<IssueHeaderData[]>() {

            public IssueHeaderData[] call() throws Exception {

                return getSOAP().mc_project_get_issue_headers(getUsername(), getPassword(),
                        BigInteger.valueOf(projectId), BigInteger.ONE, BigInteger.valueOf(limit));
            }

        });
    }

    public int addIssue(final IssueData issue, IProgressMonitor monitor) throws MantisException {

        return call(monitor, new Callable<BigInteger>() {

            public BigInteger call() throws Exception {

                BigInteger result = getSOAP().mc_issue_add(getUsername(), getPassword(), issue);

                return result;

            }

        }).intValue();

    }

    public void addRelationship(final int ticketId, final RelationshipData relationshipData, IProgressMonitor monitor)
            throws MantisException {

        call(monitor, new Callable<Void>() {

            public Void call() throws Exception {

                getSOAP().mc_issue_relationship_add(getUsername(), getPassword(), BigInteger.valueOf(ticketId),
                        relationshipData);
                return null;

            }
        });
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