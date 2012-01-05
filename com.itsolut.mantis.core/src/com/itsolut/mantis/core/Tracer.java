/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core;

import org.eclipse.osgi.util.NLS;

/**
 * @author Robert Munteanu
 *
 */
public interface Tracer {

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
    void trace(TraceLocation location, String message, Object... arguments);

}