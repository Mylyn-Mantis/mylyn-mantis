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

import com.itsolut.mantis.core.model.*;

/**
 * @author Robert Munteanu
 *
 */
public class MantisCacheData implements Serializable {

    // increment when structure changes
    private static final long serialVersionUID = 4L;
    
    long lastUpdate = 0;

    List<MantisProject> projects = new ArrayList<MantisProject>();

    Map<Integer, List<MantisProjectFilter>> projectFiltersById = new HashMap<Integer, List<MantisProjectFilter>>();

    Map<Integer, List<MantisCustomField>> customFieldsByProjectId = new HashMap<Integer, List<MantisCustomField>>();

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

    Map<Integer, List<MantisVersion>> versionsByProjectId = new HashMap<Integer, List<MantisVersion>>();

    Map<Integer, List<String>> reportersByProjectId = new HashMap<Integer, List<String>>();
    
    Map<Integer, List<String>> developersByProjectId = new HashMap<Integer, List<String>>();

    int reporterThreshold;

    int developerThreshold;
    
    int dueDateUpdateThreshold;

    int dueDateViewThreshold;

    boolean timeTrackingEnabled;

    int bugSubmitStatus;
    
    int bugAssignedStatus;
    
    Map<MantisTicket.Key, Integer> defaultValuesForAttributes = new EnumMap<MantisTicket.Key, Integer>(MantisTicket.Key.class);

    Map<MantisTicket.Key, String> defaultStringValuesForAttributes = new EnumMap<MantisTicket.Key, String>(MantisTicket.Key.class);

	int bugResolutionFixedThreshold;
}
