/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.itsolut.mantis.core.RepositoryVersion;
import com.itsolut.mantis.core.exception.MantisException;

public class RepositoryVersionTest {

	@Test
	public void testVersion005Recognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_1_6_OR_LOWER, RepositoryVersion.fromVersionString("0.0.5"));
	}

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
	public void testVersion120CVSIsRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_2_A3_OR_LOWER, RepositoryVersion.fromVersionString("1.2.0-CVS"));
	}

	@Test
	public void testVersion120a2IsRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_2_A3_OR_LOWER, RepositoryVersion.fromVersionString("1.2.0a2"));
	}

	@Test
	public void testVersion120a3IsRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_2_A3_OR_LOWER, RepositoryVersion.fromVersionString("1.2.0a3"));
	}

	@Test
	public void testVersion120rc1IsRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_2_RC1_OR_HIGHER, RepositoryVersion.fromVersionString("1.2.0rc1"));
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

}
