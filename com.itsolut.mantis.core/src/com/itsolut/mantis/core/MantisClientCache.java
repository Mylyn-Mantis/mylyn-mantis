package com.itsolut.mantis.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.net.Policy;

import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisCustomFieldType;
import com.itsolut.mantis.core.model.MantisProject;

/**
 * @author Robert Munteanu
 * 
 */
public class MantisClientCache {

	private IMantisClient client;

	private List<MantisCustomFieldType> customFieldTypes = Collections.emptyList();
	
	private List<MantisProject> projects = Collections.emptyList();

	private long lastUpdate;

	public MantisClientCache(IMantisClient client) {

		this.client = client;
	}

	public List<MantisCustomFieldType> getCustomFieldTypes(IProgressMonitor monitor) {

		try {
			refreshIfNeeded(monitor);
		} catch (MantisException e) {
			
			MantisCorePlugin.log(new Status(Status.ERROR,
                    MantisCorePlugin.PLUGIN_ID, 0, "Failed getting custom fields", e));
		}
		
		return customFieldTypes;
	}
	
	public List<MantisProject> getProjects(IProgressMonitor monitor) {

		try {
			refreshIfNeeded(monitor);
		} catch (MantisException e) {
			
			MantisCorePlugin.log(new Status(Status.ERROR,
                    MantisCorePlugin.PLUGIN_ID, 0, "Failed getting projects", e));
		}
		
		return projects;
	}

	public void refreshIfNeeded(IProgressMonitor monitor) throws MantisException{

		if (lastUpdate == 0)
			refresh(monitor);

	}

	public void refresh(IProgressMonitor monitor) throws MantisException{
		
		IProgressMonitor subMonitor = Policy.subMonitorFor(monitor, 2);

		refreshCustomFieldTypes(subMonitor);
		refreshProjects(subMonitor);
		
		lastUpdate = System.currentTimeMillis();
	}

	private void refreshProjects(IProgressMonitor monitor) throws MantisException {
		
		projects = Arrays.asList(client.getProjects(monitor));
		
	}

	private void refreshCustomFieldTypes(IProgressMonitor monitor) throws MantisException {

		customFieldTypes = Arrays.asList(client.getCustomFieldsTypes(monitor));
	}

}
