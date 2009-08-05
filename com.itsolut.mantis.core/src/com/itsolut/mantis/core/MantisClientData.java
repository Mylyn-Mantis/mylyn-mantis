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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.itsolut.mantis.core.model.MantisCustomFieldType;
import com.itsolut.mantis.core.model.MantisETA;
import com.itsolut.mantis.core.model.MantisPriority;
import com.itsolut.mantis.core.model.MantisProjection;
import com.itsolut.mantis.core.model.MantisReproducibility;
import com.itsolut.mantis.core.model.MantisResolution;
import com.itsolut.mantis.core.model.MantisSeverity;
import com.itsolut.mantis.core.model.MantisTicketStatus;
import com.itsolut.mantis.core.model.MantisViewState;

/**
 * 
 * @author Chris Hane
 */
public class MantisClientData implements Serializable {
    
    private static final long serialVersionUID = 2411077852012039140L;

	List<MantisPriority> priorities;
	
	List<MantisSeverity> severities;
	
	List<MantisResolution> resolutions;
	
	List<MantisTicketStatus> statuses;
	
	List<MantisReproducibility> reproducibilities;
	
	List<MantisETA> etas;
	
	List<MantisViewState> viewStates;
	
	List<MantisProjection> projections;
	
	private RepositoryVersion repositoryVersion;
	
	private List<MantisCustomFieldType> customFieldTypes = Collections.emptyList();

	long lastUpdate = 0;
	
	public MantisResolution getResolution(int value) {
		for (MantisResolution r : resolutions) {
			if(r.getValue() == value)
				return r;
		}
		
		return null;
	}
	
	public MantisTicketStatus getStatus(int value) {
		for (MantisTicketStatus status : statuses) {
			if(status.getValue() == value) {
				return status;
			}
		}
		
		return null;
	}
	
	public MantisSeverity getSeverity(int value) {
		for (MantisSeverity severity : severities) {
			if(severity.getValue() == value) {
				return severity;
			}
		}
		
		return null;
	}
	
	public MantisPriority getPriority(int value) {
		for (MantisPriority priority : priorities) {
			if(priority.getValue() == value) {
				return priority;
			}
		}
		
		return null;
	}
	
	
    public void setRepositoryVersion(RepositoryVersion repositoryVersion) {

        this.repositoryVersion = repositoryVersion;
    }
    
    
    public RepositoryVersion getRepositoryVersion() {

        return repositoryVersion;
    }
	
    public List<MantisCustomFieldType> getCustomFieldTypes() {
		return Collections.unmodifiableList(customFieldTypes);
	}
    
    public void setCustomFieldTypes(List<MantisCustomFieldType> customFieldTypes) {
		this.customFieldTypes = customFieldTypes;
	}
    

}
