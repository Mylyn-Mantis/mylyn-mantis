/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.core;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.itsolut.mantis.core.MantisClientManager.PersistedState;
import com.itsolut.mantis.core.model.MantisUser;

/**
 * @author Robert Munteanu
 */
public class PersistedStateTest {

	private static final String REPOSITORY_URL = "http://localhost";
	
	@Rule
	public TemporaryFolder scratchDir =new TemporaryFolder();
	
	@Test
	public void saveAndRead() throws IOException {
		
		File cacheFile = scratchDir.newFile("persistedState.ser");
		PersistedState writeState = new PersistedState(cacheFile);
		
		writeState.add(REPOSITORY_URL, createCacheData());
		writeState.write();
		
		PersistedState readState = new PersistedState(cacheFile);
		MantisCacheData cacheData = readState.get(REPOSITORY_URL);
		assertNotNull(cacheData);
	}

	private MantisCacheData createCacheData() {
		
		MantisCacheData cacheData = new MantisCacheData();
		cacheData.getReportersByProjectId().put(1, new MantisUser(5	, "username", null, null));
		return cacheData;
	}
}
