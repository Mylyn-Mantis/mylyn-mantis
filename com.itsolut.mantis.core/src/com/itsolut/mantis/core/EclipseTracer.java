/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;


/**
 * @author Robert Munteanu
 *
 */
public class EclipseTracer implements Tracer, DebugOptionsListener {
    
    private boolean debugEnabled;
    private DebugTrace trace;
    
    public void configure(BundleContext bundleContext) {
        
        Dictionary<String, String> properties = new Hashtable<String, String>(1);
        properties.put(DebugOptions.LISTENER_SYMBOLICNAME, MantisCorePlugin.PLUGIN_ID);
        
        bundleContext.registerService(DebugOptionsListener.class.getName(), this, properties );
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
        
        // if we are unconfigured or not enabled just skip silently
        if ( trace == null || !debugEnabled)
            return;

        if ( arguments.length > 0 )
            message = NLS.bind(message, arguments);
        
        trace(location, message);
        
    }
    
    public void optionsChanged(DebugOptions options) {

        debugEnabled = options.getBooleanOption(MantisCorePlugin.PLUGIN_ID + "/debug", false);
        trace = options.newDebugTrace(MantisCorePlugin.PLUGIN_ID, getClass());
    }
    
    private void trace(TraceLocation traceLocation, String message) {
        
        trace.trace("/debug" + traceLocation.getPrefix(), message);
    }
}
