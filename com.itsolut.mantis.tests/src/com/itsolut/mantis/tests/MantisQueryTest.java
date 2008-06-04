/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.itsolut.mantis.tests;

import junit.framework.TestCase;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisRepositoryQuery;
import com.itsolut.mantis.core.model.MantisSearch;

/**
 * @author Steffen Pingel
 */
public class MantisQueryTest extends TestCase {

	private MantisRepositoryQuery createQuery(String parameter) {
		// FIXME: remove this external depencency
		String url = "http://mylyn-mantis.sourceforge.net/MantisTest";
		return new MantisRepositoryQuery(url, url + IMantisClient.QUERY_URL + parameter, "description");
	}

	public void testMantisSearch() {
		//FIXME: Update the queryParameter as needed to test Mantis Search
		String queryParameter = "&order=priority&status=new&status=assigned&status=reopened&milestone=M1&owner=%7E%C3%A4%C3%B6%C3%BC";
		MantisRepositoryQuery query = createQuery(queryParameter);
		MantisSearch search = query.getMantisSearch();
		assertEquals(queryParameter, search.toUrl());
	}

}
