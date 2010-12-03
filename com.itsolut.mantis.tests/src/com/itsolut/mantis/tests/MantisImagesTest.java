/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import java.net.MalformedURLException;

import junit.framework.TestCase;

import com.itsolut.mantis.ui.internal.MantisImages;

/**
 * @author Robert Munteanu
 */
@SuppressWarnings("restriction")
public class MantisImagesTest extends TestCase {

	public void testPrefixedIconUrl() throws MalformedURLException {

		assertTrue(MantisImages.makeIconFileURL("prefix", "name").toString().endsWith("/icons/prefix/name"));
	}

	public void testNonPrefixedIconUrl() throws MalformedURLException {

		assertTrue(MantisImages.makeIconFileURL("", "name").toString().endsWith("/icons/name"));
	}

}
