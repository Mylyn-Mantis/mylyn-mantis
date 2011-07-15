/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Robert Munteanu
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.io.Serializable;
import java.util.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.itsolut.mantis.core.model.*;

/**
 * @author Robert Munteanu
 *
 */
public class MantisCacheData implements Serializable {

    // increment when structure changes
    private static final long serialVersionUID = 8L;
    
    private long lastUpdate = 0;

    private List<MantisProject> projects = new ArrayList<MantisProject>();

    private ListMultimap<Integer, MantisProjectFilter> projectFiltersById = ArrayListMultimap.create();

    private ListMultimap<Integer,MantisCustomField> customFieldsByProjectId = ArrayListMultimap.create();

    RepositoryVersion repositoryVersion;

    int resolvedStatus;

    List<MantisPriority> priorities;

    List<MantisTicketStatus> statuses;

    List<MantisSeverity> severities;

    List<MantisResolution> resolutions;

    List<MantisReproducibility> reproducibilities;

    List<MantisProjection> projections;

    List<MantisETA> etas;

    List<MantisViewState> viewStates;

    Map<Integer, List<MantisProjectCategory>> categoriesByProjectId = new HashMap<Integer, List<MantisProjectCategory>>();

    private ListMultimap<Integer, MantisVersion> versionsByProjectId = ArrayListMultimap.create();

    private ListMultimap<Integer, MantisUser> reportersByProjectId = ArrayListMultimap.create();
    
    private ListMultimap<Integer, MantisUser> developersByProjectId = ArrayListMultimap.create();

    private int reporterThreshold;

    private int developerThreshold;
    
    int dueDateUpdateThreshold;

    int dueDateViewThreshold;

    public boolean timeTrackingEnabled;

    int bugSubmitStatus;
    
    int bugAssignedStatus;
    
    private Map<MantisTicket.Key, Integer> defaultValuesForAttributes = new EnumMap<MantisTicket.Key, Integer>(MantisTicket.Key.class);

    private Map<MantisTicket.Key, String> defaultStringValuesForAttributes = new EnumMap<MantisTicket.Key, String>(MantisTicket.Key.class);

	private int bugResolutionFixedThreshold;
	
	private boolean etaEnabled;
	
	private boolean projectionEnabled;
	
	Map<String, MantisUser> allUsers = new HashMap<String, MantisUser>();
	
	public boolean hasBeenRefreshed() {
	    
	    return lastUpdate != 0;
	}
    
    public void setLastUpdate(long lastUpdate) {

        this.lastUpdate = lastUpdate;
    }
    
    
    public List<MantisProject> getProjects() {

        return projects;
    }
    
    public void setProjects(List<MantisProject> projects) {

        this.projects = projects;
    }

    public int getDeveloperThreshold() {

        return developerThreshold;
    }

    public void setDeveloperThreshold(int developerThreshold) {

        this.developerThreshold = developerThreshold;
    }

    public int getReporterThreshold() {

        return reporterThreshold;
    }

    public void setReporterThreshold(int reporterThreshold) {

        this.reporterThreshold = reporterThreshold;
    }

    public ListMultimap<Integer, MantisUser> getReportersByProjectId() {

        return reportersByProjectId;
    }

    public void setReportersByProjectId(ListMultimap<Integer, MantisUser> reportersByProjectId) {

        this.reportersByProjectId = reportersByProjectId;
    }
    
    
    public ListMultimap<Integer, MantisUser> getDevelopersByProjectId() {

        return developersByProjectId;
    }
    
    
    public void setDevelopersByProjectId(ListMultimap<Integer, MantisUser> developersByProjectId) {

        this.developersByProjectId = developersByProjectId;
    }
    
    public void putDefaultValueForAttribute(MantisTicket.Key key, Integer value) {
        
        defaultValuesForAttributes.put(key, value);
    }
    
    public Integer getDefaultValueForAttribute(MantisTicket.Key key ) {
        
        return defaultValuesForAttributes.get(key);
    }

    public void putDefaultValueForStringAttribute(MantisTicket.Key key, String value) {
        
        defaultStringValuesForAttributes.put(key, value);
    }

    public String getDefaultValueForStringAttribute(MantisTicket.Key key ) {
        
        return defaultStringValuesForAttributes.get(key);
    }

    public int getBugResolutionFixedThreshold() {

        return bugResolutionFixedThreshold;
    }

    public void setBugResolutionFixedThreshold(int bugResolutionFixedThreshold) {

        this.bugResolutionFixedThreshold = bugResolutionFixedThreshold;
    }

    public boolean isEtaEnabled() {

        return etaEnabled;
    }

    public void setEtaEnabled(boolean etaEnabled) {

        this.etaEnabled = etaEnabled;
    }

    public boolean isProjectionEnabled() {

        return projectionEnabled;
    }

    public void setProjectionEnabled(boolean projectionEnabled) {

        this.projectionEnabled = projectionEnabled;
    }

    public ListMultimap<Integer, MantisVersion> getVersionsByProjectId() {

        return versionsByProjectId;
    }

    public void setVersionsByProjectId(ListMultimap<Integer, MantisVersion> versionsByProjectId) {

        this.versionsByProjectId = versionsByProjectId;
    }
    
    
    public ListMultimap<Integer, MantisProjectFilter> getProjectFiltersById() {

        return projectFiltersById;
    }
    
    
    public ListMultimap<Integer, MantisCustomField> getCustomFieldsByProjectId() {

        return customFieldsByProjectId;
    }
}
