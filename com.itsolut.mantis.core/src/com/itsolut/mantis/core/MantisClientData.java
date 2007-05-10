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
import java.util.List;

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

	long lastUpdate = 0;

}
