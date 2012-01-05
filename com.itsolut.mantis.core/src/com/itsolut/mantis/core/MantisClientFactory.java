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

import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;

import com.google.inject.Inject;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.soap.MantisSoapClient;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisClientFactory {

    private final TaskRepositoryLocationFactory taskRepositoryLocationFactory;
    private final Tracer tracer;

    @Inject
    public MantisClientFactory(TaskRepositoryLocationFactory taskRepositoryLocationFactory, Tracer tracer) {
        
        this.taskRepositoryLocationFactory = taskRepositoryLocationFactory;
        this.tracer = tracer;
    }

    public IMantisClient createClient(AbstractWebLocation webLocation) throws MantisException {

        return new MantisSoapClient(webLocation, tracer);
    }
    
    public IMantisClient createClient(TaskRepository taskRepository) throws MantisException {
        
        return createClient(taskRepositoryLocationFactory.createWebLocation(taskRepository));
    }
}
