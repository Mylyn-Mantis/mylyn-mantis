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

import java.net.Proxy;
import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;

import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisETA;
import com.itsolut.mantis.core.model.MantisPriority;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectCategory;
import com.itsolut.mantis.core.model.MantisProjectFilter;
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

	protected String username;

	protected String password;

	protected URL repositoryUrl;

	protected Version version;

	protected MantisClientData data;
	
	protected MantisUserData userData;

	private AbstractWebLocation location;
	
	public AbstractMantisClient(URL repositoryUrl, Version version, String username, String password, AbstractWebLocation webLocation) {
		this.repositoryUrl = repositoryUrl;
		this.version = version;
		this.username = username;
		this.password = password;
		this.location = webLocation;
		
		this.data = new MantisClientData();
		this.userData = new MantisUserData();
		System.setProperty("java.protocol.handler.pkgs","com.sun.net.ssl.internal.www.protocol");
	}

	public Version getVersion() {
		return version;
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
	
	public String[] getUsers(String project) {
		if(!userData.usersPerProject.containsKey(project)) {
			try {
				updateUsers(project);
			} catch (MantisException e) {
				MantisCorePlugin.log(e);
				StatusHandler.log(MantisCorePlugin.toStatus(e));
			}
		}
			
		return userData.usersPerProject.get(project);
	}
	
	public String[] getDevelopers(String project) {

        if (!userData.developersPerProject.containsKey(project)) {
            try {
                updateUsers(project);
            } catch (MantisException e) {
                MantisCorePlugin.log(e);
                StatusHandler.log(MantisCorePlugin.toStatus(e));
            }
        }

        return userData.developersPerProject.get(project);

    }
	
	public abstract MantisProject[] getProjects() throws MantisException;

	public abstract MantisProjectCategory[] getProjectCategories(String projectName) throws MantisException;

	public abstract MantisProjectFilter[] getProjectFilters(String projectName) throws MantisException;

	public boolean hasAttributes() {
		return (data.lastUpdate != 0);
	}
	
	public void updateAttributes(IProgressMonitor monitor, boolean force) throws MantisException {
		if (!hasAttributes() || force) {
			updateAttributes(monitor);
			data.lastUpdate = System.currentTimeMillis();
		}
	}
	
	public abstract void updateAttributes(IProgressMonitor monitor) throws MantisException;
	protected abstract void updateUsers(String projectId) throws MantisException;

	public void setData(MantisClientData data) {
		this.data = data;
	}
	
	protected AbstractWebLocation getLocation() {

        return location;
    }
	
	
}
