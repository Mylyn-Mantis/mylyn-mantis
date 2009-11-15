/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;

import junit.framework.TestCase;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisClientFactory;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.exception.MantisException;

/**
 * Provides a base implementation for test cases that access trac repositories.
 * 
 * @author Steffen Pingel
 * @author David Carver
 */
public abstract class AbstractMantisClientTest extends TestCase {

	protected IMantisClient newMantisClient(String repositoryUrl, String username, String password) throws MantisException {

		TaskRepository repository = new TaskRepository(MantisCorePlugin.REPOSITORY_KIND, repositoryUrl);
		repository.setCredentials(AuthenticationType.REPOSITORY, new AuthenticationCredentials(username, password),
				false);
		AbstractWebLocation location = new TaskRepositoryLocationFactory().createWebLocation(repository);

		return MantisClientFactory.getDefault().createClient(location);

	}

}
