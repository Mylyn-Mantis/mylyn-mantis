/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.core.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * @author Robert Munteanu
 */
public class MantisUtilsTest {
	
	public static final long TEST_TIMESTAMP_MILLIS = 1320616800000l;
	public static final long TEST_TIMESTAMP_SECONDS = TEST_TIMESTAMP_MILLIS / 1000l;

	@Test
	public void convertFromEmptyCustomFieldValue() {
		
		assertThat(MantisUtils.convertFromCustomFieldDate(""), is(""));
	}

	@Test
	public void convertFromCustomFieldValue() {
		
		assertThat(MantisUtils.convertFromCustomFieldDate(String.valueOf(TEST_TIMESTAMP_SECONDS)), is(String.valueOf(TEST_TIMESTAMP_MILLIS)));
	}

	@Test
	public void convertToEmptyCustomFieldValue() {
		
		assertThat(MantisUtils.convertToCustomFieldDate(""), is(""));
	}
	
	@Test
	public void convertToCustomFieldValue() {
		
		assertThat(MantisUtils.convertToCustomFieldDate(String.valueOf(TEST_TIMESTAMP_MILLIS)), is(String.valueOf(TEST_TIMESTAMP_SECONDS)));
	}
}
