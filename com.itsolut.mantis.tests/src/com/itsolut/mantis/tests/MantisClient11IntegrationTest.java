/*******************************************************************************
 * Copyright (C) 2010 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/


package com.itsolut.mantis.tests;

import static com.itsolut.mantis.tests.RepositoryConfiguration.MANTIS_11_BASIC;

public class MantisClient11IntegrationTest extends AbstractMantisClientIntegrationTest {

	@Override
	protected RepositoryConfiguration getRepositoryConfiguration() {

		return MANTIS_11_BASIC;
	}

}
