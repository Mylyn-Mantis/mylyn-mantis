/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core;


/**
 * @author Robert Munteanu
 *
 */
public class NoOpTracer implements Tracer {

    public void trace(TraceLocation location, String message, Object... arguments) {

        // does nothing
    }
}
