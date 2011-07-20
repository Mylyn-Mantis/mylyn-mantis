/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core;

import org.eclipse.core.runtime.IPath;

import com.google.inject.AbstractModule;

/**
 * @author Robert Munteanu
 *
 */
class MantisCorePluginModule extends AbstractModule {
    
    private final MantisRepositoryConnector mantisRepositoryConnector;

    public MantisCorePluginModule(MantisRepositoryConnector mantisRepositoryConnector) {
        
        this.mantisRepositoryConnector = mantisRepositoryConnector;
    }

    @Override
    protected void configure() {
        
        bind(StatusFactory.class);
        bind(MantisAttachmentHandler.class);
        bind(MantisTaskDataHandler.class);
        bind(IMantisClientManager.class).to(MantisClientManager.class);
        bind(MantisCommentMapper.class);
        bind(IPath.class).annotatedWith(RepositoryPersistencePath.class).toProvider(RepositoryPersistencePathProvider.class);
        bind(MantisRepositoryConnector.class).toInstance(mantisRepositoryConnector);
    }

}
