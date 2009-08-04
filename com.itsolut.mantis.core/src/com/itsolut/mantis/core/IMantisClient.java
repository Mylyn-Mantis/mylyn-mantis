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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisETA;
import com.itsolut.mantis.core.model.MantisPriority;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisProjectCategory;
import com.itsolut.mantis.core.model.MantisProjectFilter;
import com.itsolut.mantis.core.model.MantisProjection;
import com.itsolut.mantis.core.model.MantisReproducibility;
import com.itsolut.mantis.core.model.MantisResolution;
import com.itsolut.mantis.core.model.MantisSearch;
import com.itsolut.mantis.core.model.MantisSeverity;
import com.itsolut.mantis.core.model.MantisTicket;
import com.itsolut.mantis.core.model.MantisTicketStatus;
import com.itsolut.mantis.core.model.MantisVersion;
import com.itsolut.mantis.core.model.MantisViewState;

/**
 * Defines the requirements for classes that provide remote access to 
 * Mantis repositories.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public interface IMantisClient {

    /**
     * @author Robert Munteanu
     *
     * @deprecated The client-side declared version is not used anymore as we 
     * will detect it from the server version
     */
    @Deprecated
	public enum Version {
		MC_1_0a5;

		public static Version fromVersion(String version) {
			try {
				return Version.valueOf(version);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}

		@Override
		public String toString() {
			switch (this) {
			case MC_1_0a5:
				return "Mantis Connect 1.0a5";
			default:
				return null;
			}
		}

	}

	public static final String CHARSET = "UTF-8";

	public static final String TIME_ZONE = "UTC";

//	public static final String LOGIN_URL = "/login";

	public static final String QUERY_URL = "/query?format=tab";

	public static final String TICKET_URL = "/mc_issue_get";

	public static final String NEW_TICKET_URL = "/mc_issue_add";

	public static final String TICKET_ATTACHMENT_URL = "file_download.php?type=bug&file_id=";

	public static final String DEFAULT_USERNAME = "anonymous";
	
	public static final String URL_SHOW_BUG = "view.php?id=";

    public static final String SEARCH_LIMIT = "searchLimit";

	/**
	 * Gets ticket with <code>id</code> from repository.
	 * 
	 * @param id
	 *            the id of the ticket to get
	 * @return the ticket
	 * @throws MantisException
	 *             thrown in case of a connection error
	 */
	MantisTicket getTicket(int id) throws MantisException;

	/**
	 * Queries tickets from repository. All found tickets are added to
	 * <code>result</code>.
	 * 
	 * @param query
	 *            the search criteria
	 * @param result
	 *            the list of found tickets
	 * @throws MantisException
	 *             thrown in case of a connection error
	 */
	void search(MantisSearch query, List<MantisTicket> result) throws MantisException;

	/**
	 * Validates the repository connection.
	 * 
	 * @throws MantisException
	 *             thrown in case of a connection error
	 */
	void validate() throws MantisException;

	/**
	 * Updates cached repository details: milestones, versions etc.
	 * 
	 * @throws MantisException
	 *             thrown in case of a connection error
	 */
	void updateAttributes(IProgressMonitor monitor, boolean force) throws MantisException;

	MantisPriority[] getPriorities();

	MantisSeverity[] getSeverities();

	MantisResolution[] getTicketResolutions();

	MantisTicketStatus[] getTicketStatus();

	MantisReproducibility[] getReproducibility();

	MantisETA[] getETA();

	MantisViewState[] getViewState();

	MantisProjection[] getProjection();
	
	/**
	 * @param project
	 * @return all users which are allocated to this project
	 */
	String[] getUsers(String project);
	
	/**
	 * @param project
	 * @return all users which are allowed to handle tasks in this project
	 */
	String[] getDevelopers(String project);

	public MantisProject[] getProjects() throws MantisException;

	public MantisProjectCategory[] getProjectCategories(String projectName) throws MantisException;

	public MantisProjectFilter[] getProjectFilters(String projectName) throws MantisException;

//	MantisTicketType[] getTicketTypes();

	/**
	 * @return The versions for a specific project. 
	 */
	MantisVersion[] getVersions(String projectName) throws MantisException;

	byte[] getAttachmentData(int id) throws MantisException;

	void putAttachmentData(int id, String name, byte[] data) throws MantisException;

	/**
	 * @param monitor 
	 * @return the id of the created ticket
	 */
	int createTicket(MantisTicket ticket, IProgressMonitor monitor) throws MantisException;

	void updateTicket(MantisTicket ticket, String comment, IProgressMonitor monitor) throws MantisException;

	/**
	 * Sets a reference to the cached repository attributes.
	 * 
	 * @param data
	 *            cached repository attributes
	 */
	void setData(MantisClientData data);
	
	RepositoryVersion getRepositoryVersion(IProgressMonitor monitor) throws MantisException;
}
