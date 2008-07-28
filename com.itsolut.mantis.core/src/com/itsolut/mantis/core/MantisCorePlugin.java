/*******************************************************************************
 * Copyright (c) 2003 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *******************************************************************************/
package com.itsolut.mantis.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.exception.MantisLoginException;

/**
 * The headless Trac plug-in class.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisCorePlugin extends Plugin {

	public static final String PLUGIN_ID = "com.itsolut.mantis.core";

	public static final String ENCODING_UTF_8 = "UTF-8";

	private static MantisCorePlugin plugin;

	public final static String REPOSITORY_KIND = "mantis";

	private MantisRepositoryConnector connector;
	
	public MantisCorePlugin() {
	}

	public static MantisCorePlugin getDefault() {
		return plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (connector != null) {
			connector.stop();
			connector = null;
		}
		
		plugin = null;
		super.stop(context);
	}

	public MantisRepositoryConnector getConnector() {
		return connector;
	}
	
	void setConnector(MantisRepositoryConnector connector) {
		this.connector = connector;
	}

	/**
	 * Returns the path to the file caching repository attributes.
	 */
	protected IPath getRepostioryAttributeCachePath() {
		IPath stateLocation = Platform.getStateLocation(MantisCorePlugin.getDefault().getBundle());
		IPath cacheFile = stateLocation.append("repositoryConfigurations");
		return cacheFile;
	}

	public static IStatus toStatus(Throwable e) {
		if (e instanceof MantisLoginException || "Access Denied".equals(e.getMessage())) {
			return new Status(IStatus.ERROR, PLUGIN_ID, IStatus.INFO, 
					"Your login name or password is incorrect. Ensure proper repository configuration in Task Repositories View.", null);
		} else if (e instanceof MantisException) {
			return new Status(IStatus.ERROR, PLUGIN_ID, IStatus.INFO, "Connection Error: " + e.getMessage(), e);
		} else if (e instanceof ClassCastException) {
			return new Status(IStatus.ERROR, PLUGIN_ID, IStatus.INFO, "Error parsing server response", e);
		} else {
			return new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, "Unexpected error", e);
		}
	}

	/**
	 * Convenience method for logging statuses to the plug-in log
	 * 
	 * @param status
	 *            the status to log
	 */
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	/**
	 * Convenience method for logging exceptions to the plug-in log
	 * 
	 * @param e
	 *            the exception to log
	 */
	public static void log(Throwable e) {
		String message = e.getMessage();
		if (e.getMessage() == null) {
			message = e.getClass().toString();
		}
		log(new Status(Status.ERROR, MantisCorePlugin.PLUGIN_ID, 0, message, e));
	}

	public static void log(String string) {
		log(new Status(Status.INFO, MantisCorePlugin.PLUGIN_ID, 0, string, null));
	}

	public static void log(String string, Exception ex) {
		log(new Status(Status.INFO, MantisCorePlugin.PLUGIN_ID, 0, string, ex));
	}

}


