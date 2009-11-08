/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisCustomField;
import com.itsolut.mantis.core.model.MantisCustomFieldType;
import com.itsolut.mantis.core.model.MantisETA;
import com.itsolut.mantis.core.model.MantisPriority;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjection;
import com.itsolut.mantis.core.model.MantisReproducibility;
import com.itsolut.mantis.core.model.MantisResolution;
import com.itsolut.mantis.core.model.MantisSeverity;
import com.itsolut.mantis.core.model.MantisTicketStatus;
import com.itsolut.mantis.core.model.MantisViewState;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public abstract class AbstractMantisClient implements IMantisClient {
	
	static interface DefaultConstantValues {
		
		public enum Role {
			
			REPORTER(25), DEVELOPER(55);
			
			private final int value;
			
			private Role(int value) {
				this.value = value;
			}
			
			public int getValue() {
				return value;
			}
		}
		
		public enum Threshold {
			
			REPORT_BUG_THRESHOLD(Role.REPORTER.getValue()),
			UPDATE_BUG_ASSIGN_THRESHOLD(Role.DEVELOPER.getValue());
			
			private final int value;
			
			private Threshold(int value) {
				this.value = value;
			}
			
			public int getValue() {
				return value;
			}
		}
	
		
	}
	
	

	protected String username;

	protected String password;

	protected URL repositoryUrl;


	protected MantisClientData data;
	
	protected MantisUserData userData;

	private AbstractWebLocation location;
	
	public AbstractMantisClient(URL repositoryUrl, String username, String password, AbstractWebLocation webLocation) {
		this.repositoryUrl = repositoryUrl;
		this.username = username;
		this.password = password;
		this.location = webLocation;
		
		this.data = new MantisClientData();
		this.userData = new MantisUserData();
	}

	protected boolean hasAuthenticationCredentials() {
		return username != null && username.length() > 0;
	}

	public MantisPriority[] getPriorities() {
		return (data.priorities != null) ? data.priorities.toArray(new MantisPriority[0]) : null;
	}

	public MantisSeverity[] getSeverities() {
		return (data.severities != null) ? data.severities.toArray(new MantisSeverity[0]) : null;
	}
	
	public MantisResolution[] getTicketResolutions() {
		return (data.resolutions != null) ? data.resolutions.toArray(new MantisResolution[0]) : null;
	}

	public MantisTicketStatus[] getTicketStatus() {
		return (data.statuses != null) ? data.statuses.toArray(new MantisTicketStatus[0]) : null;
	}

	public MantisReproducibility[] getReproducibility() {
		return (data.reproducibilities != null) ? data.reproducibilities.toArray(new MantisReproducibility[0]) : null;
	}

	public MantisETA[] getETA() {
		return (data.etas != null) ? data.etas.toArray(new MantisETA[0]) : null;
	}

	public MantisViewState[] getViewState() {
		return (data.viewStates != null) ? data.viewStates.toArray(new MantisViewState[0]) : null;
	}

	public MantisProjection[] getProjection() {
		return (data.projections != null) ? data.projections.toArray(new MantisProjection[0]) : null;
	}
	
	public String[] getUsers(String project, IProgressMonitor monitor) {
		if(!userData.usersPerProject.containsKey(project)) {
			try {
				updateUsers(project, monitor);
			} catch (MantisException e) {
				MantisCorePlugin.log(e);
				StatusHandler.log(MantisCorePlugin.toStatus(e));
			}
		}
			
		return userData.usersPerProject.get(project);
	}
	
	public String[] getDevelopers(String project, IProgressMonitor monitor) {

        if (!userData.developersPerProject.containsKey(project)) {
            try {
                updateUsers(project, monitor);
            } catch (MantisException e) {
                MantisCorePlugin.log(e);
                StatusHandler.log(MantisCorePlugin.toStatus(e));
            }
        }

        return userData.developersPerProject.get(project);

    }
	
	public MantisProject getProjectByName(String projectName, IProgressMonitor monitor) throws MantisException {
	    
	    for ( MantisProject project : getProjects(monitor))
	        if ( project.getName().equals(projectName))
	            return project;
	    
	    throw new MantisException("Unable to find project by name " + projectName + " .");
	};

	/**
     * Returns true, if the repository details are cached. If this method
     * returns true, invoking <tt>updateAttributes(monitor, false)</tt> will
     * return without opening a connection.
     * 
     * @see #updateAttributes(IProgressMonitor, boolean) 
     */
	private boolean hasAttributes() {
	    
	    return data.hasAttributes();
	}
	
	public void updateAttributes(IProgressMonitor monitor, boolean force) throws MantisException {
		if (!hasAttributes() || force) {
			updateAttributes(monitor);
			data.recordAttributesUpdated();
		}
	}
	
	public abstract void updateAttributes(IProgressMonitor monitor) throws MantisException;
	
	protected abstract void updateUsers(String projectId, IProgressMonitor monitor) throws MantisException;

	public void setData(MantisClientData data) {
		this.data = data;
	}
	
	protected AbstractWebLocation getLocation() {

        return location;
    }
	
	public RepositoryVersion getRepositoryVersion(IProgressMonitor monitor) throws MantisException {
	    
	    if (!hasAttributes()) {
	        updateAttributes(monitor);
	        data.recordAttributesUpdated();
	    }
	
	    return data.getRepositoryVersion();
	}
	
	public List<MantisCustomFieldType> getCustomFieldTypes(
			IProgressMonitor monitor) throws MantisException {
		
		if ( !hasAttributes()) {
			updateAttributes(monitor);
			data.recordAttributesUpdated();
		}
		
		return data.getCustomFieldTypes();
	}
	
	public List<MantisCustomField> getCustomFieldsForProject(String projectName, IProgressMonitor monitor)
	        throws MantisException {

	    if ( !hasAttributes()) {
            updateAttributes(monitor);
            data.recordAttributesUpdated();
        }

	    for ( MantisProject project : getProjects(monitor))
            if ( project.getName().equals(projectName))
	            return data.getCustomFields(project.getValue());
	    
	    throw new MantisException("No custom field project data found for project with name " + projectName + " .");
	    
	}

    public boolean isCompleted(TaskData taskData, IProgressMonitor progressMonitor) throws MantisException {

	    updateAttributes(progressMonitor, false);
	    
	    TaskAttribute status = taskData.getRoot().getAttribute(MantisAttributeMapper.Attribute.STATUS.getKey());
	    String statusName = status.getValue();
	    
	    int statusLevel = -1;
	    
	    for ( MantisTicketStatus mantisTicketStatus : getTicketStatus() ) {
	        if ( mantisTicketStatus.getName().equals(statusName)) {
	            statusLevel = mantisTicketStatus.getValue();
	            break;
	        }
	    }
	    
	    if ( statusLevel == -1) {
	        MantisCorePlugin.log(new Status(Status.WARNING, MantisCorePlugin.PLUGIN_ID, "Unable to find the level for the status named " + statusName + " ."));
	        return false;
	    }
	    

	    int resolvedStatusThreshold = data.getResolvedStatusThreshold();
	    
	    return statusLevel >= resolvedStatusThreshold;
	                
	}
}
