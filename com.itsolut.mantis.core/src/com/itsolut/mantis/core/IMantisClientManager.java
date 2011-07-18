/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core;

import org.eclipse.mylyn.tasks.core.TaskRepository;

import com.itsolut.mantis.core.exception.MantisException;

/**
 * The {@link IMantisClientManager} handles persistence and caching for {@link IMantisClient} instances.
 * 
 * @author Robert Munteanu
 *
 */
public interface IMantisClientManager extends IShutdown {

    /**
     * Returns a client for the specified taskRepository
     * 
     * <p>The client is created on the fly if it does not exist.</p>
     * 
     * @param taskRepository the repository
     * @return the mantis client, never null
     * @throws MantisException an error creating the repository
     */
    IMantisClient getRepository(TaskRepository taskRepository) throws MantisException;
}