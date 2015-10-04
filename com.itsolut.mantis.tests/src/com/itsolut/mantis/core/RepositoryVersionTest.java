/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.itsolut.mantis.core.exception.MantisException;

public class RepositoryVersionTest {

	@Test
	public void testVersion116Recognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_1_6_OR_LOWER, RepositoryVersion.fromVersionString("1.1.6"));
	}

	@Test
	public void testVersion117Recognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_1_7_OR_HIGHER, RepositoryVersion.fromVersionString("1.1.7"));
	}

	@Test
	public void testVersion110rc4Recognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_1_6_OR_LOWER, RepositoryVersion.fromVersionString("1.1.0rc4"));
	}

	@Test
	public void testVersion112SVNRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_1_6_OR_LOWER, RepositoryVersion.fromVersionString("1.1.2-SVN"));
	}

	@Test
	public void testVersion1112Recognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_1_7_OR_HIGHER, RepositoryVersion.fromVersionString("1.1.12"));
	}

	@Test
	public void testVersion120IsRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_2_OR_HIGHER, RepositoryVersion.fromVersionString("1.2.0"));
	}

	@Test
	public void testVersion121IsRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_2_OR_HIGHER, RepositoryVersion.fromVersionString("1.2.1"));
	}
	
	@Test
	public void testVersion13DevisRecognized() throws MantisException {
		
		assertEquals(RepositoryVersion.VERSION_1_3_DEV, RepositoryVersion.fromVersionString("1.3dev"));
	}
	
	@Test
	public void testVersion122IsRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_2_2_OR_HIGHER, RepositoryVersion.fromVersionString("1.2.2"));
	}
	
	@Test
	public void testVersion1210IsRecognized() throws MantisException {
		
		assertEquals(RepositoryVersion.VERSION_1_2_9_OR_HIGHER, RepositoryVersion.fromVersionString("1.2.10"));
	}

	@Test
	public void testVersion1216IsRecognized() throws MantisException {
		
		assertEquals(RepositoryVersion.VERSION_1_2_16_OR_HIGHER, RepositoryVersion.fromVersionString("1.2.16"));
	}

}
