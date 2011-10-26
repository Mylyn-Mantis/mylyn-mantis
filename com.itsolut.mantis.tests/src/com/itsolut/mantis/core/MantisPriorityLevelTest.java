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

/**
 * @author David Carver
 * @author Robert Munteanu
 */
public class MantisPriorityLevelTest {

	@Test
	public void testGetMylynPriority() {
		
		assertEquals("P1", MantisPriorityLevel.fromPriorityId(70).toString());
		assertEquals("P1", MantisPriorityLevel.fromPriorityId(60).toString());
		assertEquals("P1", MantisPriorityLevel.fromPriorityId(50).toString());
		assertEquals("P2", MantisPriorityLevel.fromPriorityId(40).toString());
		assertEquals("P3", MantisPriorityLevel.fromPriorityId(30).toString());
		assertEquals("P4", MantisPriorityLevel.fromPriorityId(20).toString());
		assertEquals("P5", MantisPriorityLevel.fromPriorityId(10).toString());
		assertEquals("P5", MantisPriorityLevel.fromPriorityId(0).toString());
	}
}
