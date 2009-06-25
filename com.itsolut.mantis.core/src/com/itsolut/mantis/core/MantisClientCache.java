package com.itsolut.mantis.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisCustomFieldType;

/**
 * @author Robert Munteanu
 * 
 */
public class MantisClientCache {

	private IMantisClient client;

	private List<MantisCustomFieldType> customFieldTypes = Collections.emptyList();

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

	private void refreshIfNeeded(IProgressMonitor monitor) throws MantisException{

		if (lastUpdate == 0)
			refresh(monitor);

	}

	public void refresh(IProgressMonitor monitor) throws MantisException{

		refreshCustomFieldTypes(monitor);
	}

	private void refreshCustomFieldTypes(IProgressMonitor monitor) throws MantisException {

		customFieldTypes = Arrays.asList(client.getCustomFieldsTypes(monitor));
	}

}
