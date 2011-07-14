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


import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import com.google.inject.Inject;


/**
 * The headless Trac plug-in class.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisCorePlugin extends Plugin {

    static final String PLUGIN_ID = "com.itsolut.mantis.core";

    public static final String ENCODING_UTF_8 = "UTF-8";

    private static MantisCorePlugin plugin;

    public final static String REPOSITORY_KIND = "mantis";

    public static final boolean DEBUG = Boolean.getBoolean(MantisCorePlugin.class.getName().toLowerCase() + ".debug");
    
    private StatusFactory statusFactory;

    private MantisRepositoryConnector connector;

    public static MantisCorePlugin getDefault() {

        return plugin;
    }

    public static String getVersionString() {
        
        Version version = getDefault().getBundle().getVersion();

        return version.getMajor() + "." + version.getMinor() + "." + version.getMicro();
    }
    
    @Override
    public void start(BundleContext context) throws Exception {

        super.start(context);
        
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {

        if ( connector != null )
            connector.stop();
        
        plugin = null;
        super.stop(context);
    }
    
    @Inject
    public void setMantisRepositoryConnector(MantisRepositoryConnector connector) {
        
        this.connector = connector;
    }

    @Inject
    public void setStatusFactory(StatusFactory statusFactory) {

        this.statusFactory = statusFactory;
    }

    private StatusFactory getStatusFactory() {
        
        if ( statusFactory == null )
            throw new IllegalStateException();

        return statusFactory;
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
}
