/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.ui.internal;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;

import org.junit.Test;

/**
 * @author Robert Munteanu
 */
@SuppressWarnings("restriction")
public class MantisImagesTest {

	@Test
	public void testPrefixedIconUrl() throws MalformedURLException {

		assertTrue(MantisImages.makeIconFileURL("prefix", "name").toString().endsWith("/icons/prefix/name"));
	}

	@Test
	public void testNonPrefixedIconUrl() throws MalformedURLException {

		assertTrue(MantisImages.makeIconFileURL("", "name").toString().endsWith("/icons/name"));
	}

}
