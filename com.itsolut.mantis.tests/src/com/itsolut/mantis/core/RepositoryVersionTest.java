/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.core;

import com.itsolut.mantis.core.exception.MantisException;

import junit.framework.TestCase;

public class RepositoryVersionTest extends TestCase {

	public void testVersion005Recognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_1_6_OR_LOWER, RepositoryVersion.fromVersionString("0.0.5"));
	}

	public void testVersion116Recognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_1_6_OR_LOWER, RepositoryVersion.fromVersionString("1.1.6"));
	}

	public void testVersion117Recognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_1_7_OR_HIGHER, RepositoryVersion.fromVersionString("1.1.7"));

	}

	public void testVersion110rc4Recognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_1_6_OR_LOWER, RepositoryVersion.fromVersionString("1.1.0rc4"));
	}

	public void testVersion112SVNRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_1_6_OR_LOWER, RepositoryVersion.fromVersionString("1.1.2-SVN"));
	}

	public void testVersion1112Recognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_1_7_OR_HIGHER, RepositoryVersion.fromVersionString("1.1.12"));
	}

	public void testVersion120CVSIsRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_2_A3_OR_LOWER, RepositoryVersion.fromVersionString("1.2.0-CVS"));
	}

	public void testVersion120a2IsRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_2_A3_OR_LOWER, RepositoryVersion.fromVersionString("1.2.0a2"));
	}

	public void testVersion120a3IsRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_2_A3_OR_LOWER, RepositoryVersion.fromVersionString("1.2.0a3"));
	}

	public void testVersion120rc1IsRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_2_RC1_OR_HIGHER, RepositoryVersion.fromVersionString("1.2.0rc1"));
	}

	public void testVersion120IsRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_2_RC1_OR_HIGHER, RepositoryVersion.fromVersionString("1.2.0"));
	}

	public void testVersion121IsRecognized() throws MantisException {

		assertEquals(RepositoryVersion.VERSION_1_2_RC1_OR_HIGHER, RepositoryVersion.fromVersionString("1.2.1"));
	}

}
