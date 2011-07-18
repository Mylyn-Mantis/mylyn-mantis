/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.ui.internal;

import org.eclipse.mylyn.tasks.core.IRepositoryListener;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;
import org.eclipse.mylyn.tasks.ui.TaskRepositoryLocationUiFactory;
import org.eclipse.mylyn.tasks.ui.TasksUi;

import com.google.inject.AbstractModule;
import com.itsolut.mantis.core.IMantisClientManager;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.StatusFactory;


/**
 * This plugin configures the dependencies for beans belonging to the UI module.
 * 
 * <p>It needs to preserve the unicity of the 
 * 
 * @author Robert Munteanu
 *
 */
class MantisUiPluginModule extends AbstractModule {

    @Override
    protected void configure() {

        // we need to retrieve the single instance from the Core module
        MantisRepositoryConnector connector = (MantisRepositoryConnector) TasksUi.getRepositoryManager().getRepositoryConnector(MantisCorePlugin.REPOSITORY_KIND);
        
        bind(StatusFactory.class);
        bind(IMantisClientManager.class).toInstance(connector.getClientManager());
        bind(TaskRepositoryLocationFactory.class).to(TaskRepositoryLocationUiFactory.class);
        bind(IRepositoryListener.class).toInstance(connector.getRepositoryListener());
    }

}
