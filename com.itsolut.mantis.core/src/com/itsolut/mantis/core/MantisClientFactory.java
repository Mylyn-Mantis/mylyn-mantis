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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.mylyn.commons.net.AbstractWebLocation;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisClientFactory {
    
    public static IMantisClient createClient(String location, String username, String password, String httpUsername, String httpPassword, AbstractWebLocation webLocation) throws MalformedURLException {

        URL url = new URL(location);
        
        return new MantisAxis1SOAPClient(url, username, password, httpUsername, httpPassword, webLocation);
        
    }
}
