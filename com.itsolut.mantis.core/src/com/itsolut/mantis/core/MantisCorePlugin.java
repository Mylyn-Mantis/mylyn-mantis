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


import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import com.google.inject.Inject;


/**
 * The headless Mantis plug-in class.
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisCorePlugin extends Plugin {

    static final String PLUGIN_ID = "com.itsolut.mantis.core";

    public static final String ENCODING_UTF_8 = "UTF-8";

    private static MantisCorePlugin plugin;

    public final static String REPOSITORY_KIND = "mantis";

    private DebugHelper debugHelper;
    
    private StatusFactory statusFactory;

    private IShutdown shutdown;

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
        
        debugHelper = new DebugHelper(context);
        
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        
        if ( shutdown != null ) {
            shutdown.onShutdown();
            shutdown = null;
        }

        plugin = null;
        debugHelper = null;
        super.stop(context);
    }
    
    @Inject
    public void setStatusFactory(StatusFactory statusFactory) {

        this.statusFactory = statusFactory;
    }
    
    @Inject
    public void setShutdown(IMantisClientManager shutdown) {

        this.shutdown = shutdown;
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
     * Records a trace if debugging is enabled
     * 
     * <p>
     * 
     * @param location the trace location to verify enablement against
     * @param message the message to log
     * @param the optional arguments. If present, the message is expected to be a valid argument for {@link NLS#bind(String, Object[])} 
     * 
     */
    public void trace(TraceLocation location, String message, Object... arguments) {

        if ( arguments.length > 0 )
            message = NLS.bind(message, arguments);
        
        debugHelper.trace(location, message);
        
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
    
    static class DebugHelper implements DebugOptionsListener {
        
        private boolean debugEnabled;
        private DebugTrace trace;

        public DebugHelper(BundleContext bundleContext) {

            Dictionary<String, String> properties = new Hashtable<String, String>(1);
            properties.put(DebugOptions.LISTENER_SYMBOLICNAME, PLUGIN_ID);
            
            bundleContext.registerService(DebugOptionsListener.class.getName(), this, properties );
        }

        public void optionsChanged(DebugOptions options) {

            debugEnabled = options.getBooleanOption(PLUGIN_ID + "/debug", false);
            trace = options.newDebugTrace(PLUGIN_ID);
        }
        
        public void trace(TraceLocation traceLocation, String message) {
            
            if ( !debugEnabled )
                return;
            
            trace.trace("/debug" + traceLocation.getPrefix(), message);
        }
    }
}
