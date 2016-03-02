/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.core.soap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;

import biz.futureware.mantis.rpc.soap.client.ObjectRef;
import biz.futureware.mantis.rpc.soap.client.ProjectData;
import biz.futureware.mantis.rpc.soap.client.ProjectVersionData;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisETA;
import com.itsolut.mantis.core.model.MantisProject;
import com.itsolut.mantis.core.model.MantisVersion;

/**
 * @author Robert Munteanu
 */
public class MantisConverterTest {

	@Test
	public void convertNullEtas() throws MantisException {
		
		assertEquals(0, MantisConverter.convert(null, MantisETA.class).size());
	}
	
	@Test
	public void convertEmptyEtas() throws MantisException {
		
		assertEquals(0, MantisConverter.convert(new ObjectRef[0], MantisETA.class).size());
	}
	
	@Test
	public void convertEtas() throws MantisException {
		
		ObjectRef[] refs = new ObjectRef[] {
				new ObjectRef(BigInteger.ONE, "One"),
				new ObjectRef(BigInteger.TEN, "Ten")
		};
		
		List<MantisETA> etas = MantisConverter.convert(refs, MantisETA.class);
		
		assertEquals(2, etas.size());
		
		MantisETA first = etas.get(0);
		
		assertEquals(1, first.getValue());
		assertEquals("One", first.getName());
	}
	
	@Test
	public void convertProjects() {
		
		final ProjectData child = new ProjectData(BigInteger.TEN, "First", null, null, null, null, null, null, null, null);
		ProjectData first = new ProjectData(BigInteger.ONE, "First", null, null, null, null, null, null, new ProjectData[] { child }, null);
		ProjectData second = new ProjectData(BigInteger.valueOf(2), "First", null, null, null, null, null, null, null, null);
		
		List<MantisProject> projects = MantisConverter.convert(new ProjectData[] { first, second } );
		
		assertEquals(3, projects.size());
		MantisProject project = Iterables.find(projects, new Predicate<MantisProject>() {
			public boolean apply(MantisProject input) {
				return input.getValue() == child.getId().intValue();
			}
		});

		assertEquals(Integer.valueOf(1), project.getParentProjectId());
	}
	
	@Test
	public void convertProjectVersion() {
		
		MantisVersion version = MantisConverter.convert(newVersion());
		
		assertEquals("Version 1", version.getName());
		assertEquals("Description", version.getDescription());
		assertFalse(version.isReleased());
		assertNotNull(version.getTime());
	}
	
	@Test
	public void convertProjectVersion_nullDate() {
		
		MantisVersion version = MantisConverter.convert(newVersion());
		version.setTime(null);
		
		assertNull(version.getTime());
	}

	private ProjectVersionData newVersion() {
		
		return new ProjectVersionData(BigInteger.ONE, "Version 1", BigInteger.TEN, Calendar.getInstance(), "Description", Boolean.FALSE, Boolean.FALSE);
	}
}
