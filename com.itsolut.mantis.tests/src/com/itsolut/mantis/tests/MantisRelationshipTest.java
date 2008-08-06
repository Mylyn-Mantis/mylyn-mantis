/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import java.math.BigInteger;

import com.itsolut.mantis.core.model.MantisRelationship;

import junit.framework.TestCase;

public class MantisRelationshipTest extends TestCase {
	
	public void testRelationTypeNotNull() {
		
		assertNotNull(MantisRelationship.RelationType.fromRelation("bogus"));
	}

}
