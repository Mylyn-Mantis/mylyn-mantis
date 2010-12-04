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

import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.osgi.framework.BundleContext;

import com.itsolut.mantis.core.exception.MantisLocalException;
import com.itsolut.mantis.core.exception.MantisLoginException;
import com.itsolut.mantis.core.exception.MantisRemoteException;

/**
 * The headless Trac plug-in class.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisCorePlugin extends Plugin {

    private static final String PLUGIN_ID = "com.itsolut.mantis.core";

    public static final String ENCODING_UTF_8 = "UTF-8";

    private static MantisCorePlugin plugin;

    public final static String REPOSITORY_KIND = "mantis";

    public static final boolean DEBUG = Boolean.getBoolean(MantisCorePlugin.class.getName().toLowerCase() + ".debug");

    private MantisRepositoryConnector connector;
    
    private StatusFactory statusFactory;

    public static MantisCorePlugin getDefault() {

        return plugin;
    }

    @Override
    public void start(BundleContext context) throws Exception {

        super.start(context);
        plugin = this;
        statusFactory = new StatusFactory();
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

    public StatusFactory getStatusFactory() {

        return statusFactory;
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
    protected IPath getRepositoryAttributeCachePath() {

        IPath stateLocation = Platform.getStateLocation(MantisCorePlugin.getDefault().getBundle());
        IPath cacheFile = stateLocation.append("repositoryConfigurations");
        return cacheFile;
    }

    private static void log(IStatus status) {

        getDefault().getLog().log(status);
    }

    /**
     * Logs debug information into the eclipse error log.
     * 
     * <p>
     * Enabled only if the system property <tt>com.itsolut.mantis.core.mantiscoreplugin.debug</tt>
     * is set to true
     * </p>
     * 
     * @param information
     *            the string to log
     * @param t
     *            a throwable, for context information
     */
    public static void debug(String information, Throwable t) {

        if (DEBUG)
            getDefault().getLog().log(new Status(IStatus.INFO, PLUGIN_ID, information, t));
    }
    
    public static void error(String message, Throwable t) {
        
        log(getDefault().getStatusFactory().toStatus(message, t, null));
    }
    
    public static void warn(String message) {
        
        log(new Status(Status.WARNING, MantisCorePlugin.PLUGIN_ID, message));
    }
    
    public static void warn(String message, Throwable e) {
        
        log(new Status(Status.WARNING, MantisCorePlugin.PLUGIN_ID, message, e));
    }
    
    public static class StatusFactory {

        public Status toStatus( String message, Throwable t, TaskRepository repository) {
            
            String actualMessage = message == null ? t.getMessage() : message;
            
            if ( repository == null)
                return new Status(IStatus.ERROR, PLUGIN_ID, actualMessage, t);
            
            if ( t instanceof MantisLoginException || "Access Denied".equals(actualMessage))
                return RepositoryStatus.createLoginError(repository.getRepositoryUrl(), actualMessage);
            if ( t instanceof MantisRemoteException)
                return new RepositoryStatus(IStatus.ERROR, PLUGIN_ID, RepositoryStatus.ERROR_REPOSITORY, actualMessage);
            if ( t instanceof MantisLocalException)
                return RepositoryStatus.createInternalError(PLUGIN_ID, actualMessage, t);
            if ( t instanceof IOException)
                return new RepositoryStatus(IStatus.ERROR, PLUGIN_ID, RepositoryStatus.ERROR_IO, actualMessage);
            
            return RepositoryStatus.createInternalError(PLUGIN_ID, actualMessage, t);
            
        }
        
    }
}
