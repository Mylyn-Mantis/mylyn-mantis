/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.ui.wizard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.itsolut.mantis.core.model.MantisProject;

/**
 * @author Robert Munteanu
 */
public class MantisProjectITreeContentProviderTest {

	@Test
	public void emptyContent() {
		
		MantisProject[] projects = new MantisProject[0];
		
		MantisProjectITreeContentProvider contentProvider = new MantisProjectITreeContentProvider();
		contentProvider.setInput(projects);
		
		assertEquals(0, contentProvider.getChildren(projects).length);
	}
	
	@Test
	public void twoProjectsAndOneChild() {
		
		MantisProject firstProject = new MantisProject("First", 1);
		MantisProject secondProject = new MantisProject("Second", 2);
		MantisProject childProject = new MantisProject("Child", 3, firstProject.getValue());
		
		MantisProject[] projects = new MantisProject[] { firstProject, secondProject, childProject };
		
		MantisProjectITreeContentProvider contentProvider = new MantisProjectITreeContentProvider();
		contentProvider.setInput(projects);
		
		// children
		assertEquals(2, contentProvider.getChildren(projects).length);
		assertEquals(1, contentProvider.getChildren(firstProject).length);
		assertEquals(0, contentProvider.getChildren(secondProject).length);
		assertEquals(0, contentProvider.getChildren(childProject).length);
		
		// projects
		assertNull(contentProvider.getParent(firstProject));
		assertNull(contentProvider.getParent(secondProject));
		assertEquals(firstProject, contentProvider.getParent(childProject));
	}
}
