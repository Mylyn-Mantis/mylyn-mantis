/*******************************************************************************
 * Copyright (c) 2012 IT Solutions, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Torsten Stolpmann - Initial implementatin
 *******************************************************************************/
package com.itsolut.mantis.core;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * This connector is intended for usage outside of an Eclipse/OSGI environment.
 *
 * @author Torsten Stolpmann
 */
public class StandaloneMantisRepositoryConnector extends MantisRepositoryConnector {

    public StandaloneMantisRepositoryConnector() {

        super(null);
        Injector injector = Guice.createInjector(new StandaloneMantisCorePluginModule(this));
        injector.injectMembers(this);
    }
}