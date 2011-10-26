/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.core.model;


import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;

import org.junit.Test;

public class MantisRelationshipTest {
	
	@Test
	public void testRelationTypeNotNull() {
		
		assertNotNull(MantisRelationship.RelationType.fromRelationId(BigInteger.valueOf(99)));
		assertNotNull(MantisRelationship.RelationType.fromRelation("bogus"));
	}

}
