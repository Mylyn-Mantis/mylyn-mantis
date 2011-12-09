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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.itsolut.mantis.core.model.*;

/**
 * @author Robert Munteanu
 *
 */
public class MantisCacheData implements Serializable {

    // increment when structure changes
    private static final long serialVersionUID = 9L;
    
    private long lastUpdate = 0;

    private List<MantisProject> projects = new ArrayList<MantisProject>();

    private ArrayListMultimapHolder<Integer, MantisProjectFilter> projectFiltersById = ArrayListMultimapHolder.create();

    private ArrayListMultimapHolder<Integer,MantisCustomField> customFieldsByProjectId = ArrayListMultimapHolder.create();

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

    private ArrayListMultimapHolder<Integer, MantisVersion> versionsByProjectId = ArrayListMultimapHolder.create();

    private ArrayListMultimapHolder<Integer, MantisUser> reportersByProjectId = ArrayListMultimapHolder.create();
    
    private ArrayListMultimapHolder<Integer, MantisUser> developersByProjectId = ArrayListMultimapHolder.create();

    private int reporterThreshold;

    private int developerThreshold;
    
    int dueDateUpdateThreshold;

    int dueDateViewThreshold;

    public boolean timeTrackingEnabled;

    int bugSubmitStatus;
    
    int bugAssignedStatus;
    
    private Map<MantisTicket.Key, Integer> defaultValuesForAttributes = new EnumMap<MantisTicket.Key, Integer>(MantisTicket.Key.class);

    private Map<MantisTicket.Key, String> defaultStringValuesForAttributes = new EnumMap<MantisTicket.Key, String>(MantisTicket.Key.class);
    
    private Map<Integer, MantisTag> tagsById;  

	private int bugResolutionFixedThreshold;
	
	private boolean etaEnabled;
	
	private boolean projectionEnabled;
	
	Map<String, MantisUser> allUsers = new HashMap<String, MantisUser>();

    private boolean enableProfiles;
	
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

        if ( reportersByProjectId == null )
            reportersByProjectId = ArrayListMultimapHolder.create();
        
        return reportersByProjectId.get();
    }

    public ListMultimap<Integer, MantisUser> getDevelopersByProjectId() {

        if ( developersByProjectId == null )
            developersByProjectId = ArrayListMultimapHolder.create();
        
        return developersByProjectId.get();
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

        if ( versionsByProjectId == null )
            versionsByProjectId = ArrayListMultimapHolder.create();
        
        return versionsByProjectId.get();
    }
    
    public ListMultimap<Integer, MantisProjectFilter> getProjectFiltersById() {

        if ( projectFiltersById == null )
            projectFiltersById = ArrayListMultimapHolder.create();
        
        return projectFiltersById.get();
    }
    
    public ListMultimap<Integer, MantisCustomField> getCustomFieldsByProjectId() {

        if ( customFieldsByProjectId == null )
            customFieldsByProjectId = ArrayListMultimapHolder.create();
        
        return customFieldsByProjectId.get();
    }
    
    
    public Map<Integer, MantisTag> getTagsById() {

        return Collections.unmodifiableMap(tagsById);
    }
    
    public void setTagsById(Map<Integer, MantisTag> tagsById ) {
        
        if ( this.tagsById == null )
            this.tagsById = Maps.newHashMapWithExpectedSize(tagsById.size());
        
        this.tagsById.clear();
        this.tagsById.putAll(tagsById);
    }

    /**
     * This class allows safe serialisation of a {@link ArrayListMultimap} in an OSGI environment
     * 
     * <p>When reading a serialized version of a multimap, the classloaded does not have access to
     * the classes from the <tt>com.itsolut.mantis.core</tt> bundle, and it does not have have a
     * <tt>Eclipse-BuddyPolicy:Registered</tt> header , which means we can not use
     * <tt>RegisterBuddy</tt> to allow it to access our classes.</p>
     * 
     * <p>This class simply wraps a multimap instance and serializes it as a map of
     * {@literal K -> Collection<V> }</p< 
     *
     */
    private static class ArrayListMultimapHolder<K, V> implements Serializable {
        
        // copied from ArrayListMultimap.DEFAULT_VALUES_PER_KEY
        private static final int DEFAULT_VALUES_PER_KEY = 10;

        public static  <K,V> ArrayListMultimapHolder<K, V> create() {
            
            return new ArrayListMultimapHolder<K, V>();
        }
        
        private ArrayListMultimap<K, V> wrapped = ArrayListMultimap.create();
        
        public ArrayListMultimapHolder() {

        }
        
        public ArrayListMultimap<K, V> get() {
            
            return wrapped;
        }
        
        private void writeObject(ObjectOutputStream out) throws IOException {
            
            Map<K, Collection<V>> wrappedMap = wrapped.asMap();
            
            Map<K, Collection<V>> outMap = Maps.newHashMapWithExpectedSize(wrappedMap.size());
            for ( Map.Entry<K, Collection<V>> entry : wrappedMap.entrySet() )
                outMap.put(entry.getKey(), Lists.newArrayList(entry.getValue()));
            
            out.writeObject(outMap);
        }
        
        private void readObject(ObjectInputStream in) throws IOException {
            try {
                @SuppressWarnings("unchecked")
                Map<K, Collection<V>> map = (Map<K, Collection<V>>) in.readObject();
                wrapped = ArrayListMultimap.create(map.size(), DEFAULT_VALUES_PER_KEY);
                
                for ( Map.Entry<K, Collection<V>> entry : map.entrySet() )
                    wrapped.putAll(entry.getKey(), entry.getValue());
                        
            } catch (ClassNotFoundException e) {
                IOException e2 = new IOException();
                e2.initCause(e);
                throw e2;
            }
        }
    }


    public void setEnableProfiles(boolean enableProfiles) {

        this.enableProfiles = enableProfiles;
    }
    
    public boolean isEnableProfiles() {

        return enableProfiles;
    }
}
