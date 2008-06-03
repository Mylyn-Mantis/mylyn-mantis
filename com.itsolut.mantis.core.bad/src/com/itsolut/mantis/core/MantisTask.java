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

import org.eclipse.mylyn.tasks.core.AbstractTask;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisTask extends AbstractTask {

	private String severity;
	
	public enum PriorityLevel {
		NONE, LOW, NORMAL, HIGH, URGENT, IMMEDIATE;
		

		@Override
		public String toString() {
			switch (this) {
			case IMMEDIATE:
				return "P1";
			case URGENT:
				return "P2";
			case HIGH:
				return "P2";
			case NORMAL:
				return "P3";
			case LOW:
				return "P4";
			case NONE:
				return "P3";
			default:
				return "P5";
			}
		}

		public static PriorityLevel fromPriority(String priority) {
			if (priority == null)
				return null;
			if (priority.equals("immediate"))
				return IMMEDIATE;
			if (priority.equals("urgent"))
				return URGENT;
			if (priority.equals("high"))
				return HIGH;
			if (priority.equals("normal"))
				return NORMAL;
			if (priority.equals("low"))
				return LOW;
			if (priority.equals("none"))
				return NONE;
			return null;
		}
	}
	
	public MantisTask(String repositoryUrl, String taskId, String label) {
		super(repositoryUrl, taskId, label);
		
		setUrl(repositoryUrl + IMantisClient.TICKET_URL + taskId);
	}
	
	
	@Override
	public String getConnectorKind() {
		return MantisCorePlugin.REPOSITORY_KIND;
	}
	
	//TODO use priority attributes from repository instead of hard coded enum
	public static String getMylynPriority(String mantisPriority) {
		if (mantisPriority != null) {
			PriorityLevel priority = PriorityLevel.fromPriority(mantisPriority);
			if (priority != null) {
				return priority.toString();
			}
		}
		
		return AbstractTask.PriorityLevel.P3.toString();
	}
	
	public static boolean isCompleted(String status) {
		return "closed".equals(status) || "resolved".equals(status);
	}
	
	public static String getRepositoryBaseUrl(String repositoryUrl) {
		String baseUrl = repositoryUrl;
		
		// get the base url of the installation (located in mc / as of version 1.1.0 its located in api/soap)
		if(repositoryUrl.toLowerCase().contains("mc/mantisconnect.php")) {
			baseUrl = repositoryUrl.substring(0, repositoryUrl.toLowerCase().indexOf("mc/mantisconnect.php"));			
		} else if (repositoryUrl.toLowerCase().contains("api/soap/mantisconnect.php")) {
			baseUrl = repositoryUrl.substring(0, repositoryUrl.toLowerCase().indexOf("api/soap/mantisconnect.php"));
		}
		
		return baseUrl;
	}

	@Override
	public boolean isLocal() {
		// TODO Auto-generated method stub
		return false;
	}


	public void setSeverity(String severity) {
		this.severity = severity;
	}


	public String getSeverity() {
		return severity;
	}

}
