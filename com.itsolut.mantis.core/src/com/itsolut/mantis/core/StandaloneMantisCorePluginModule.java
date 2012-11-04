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

import org.eclipse.core.runtime.IPath;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * This plugin module is intended for usage outside of an Eclipse/OSGI environment.
 *
 * @author Torsten Stolpmann
 */
public class StandaloneMantisCorePluginModule extends AbstractModule {

    private final MantisRepositoryConnector mantisRepositoryConnector;

    public StandaloneMantisCorePluginModule(
            final StandaloneMantisRepositoryConnector connector) {

        this.mantisRepositoryConnector = connector;
    }

    @Override
    protected void configure() {

        bind(StatusFactory.class);
        bind(MantisAttachmentHandler.class);
        bind(MantisTaskDataHandler.class);
        bind(IMantisClientManager.class).to(MantisClientManager.class);
        bind(MantisCommentMapper.class);
        bind(IPath.class).annotatedWith(RepositoryPersistencePath.class).toProvider(StandaloneRepositoryPersistencePathProvider.class);
        bind(MantisRepositoryConnector.class).toInstance(mantisRepositoryConnector);
        // we cannot support a core plugin related tracer here
        // so we have to skip tracing with a no-op implementation
        bind(Tracer.class).to(NoOpTracer.class).in(Singleton.class);
    }
}
