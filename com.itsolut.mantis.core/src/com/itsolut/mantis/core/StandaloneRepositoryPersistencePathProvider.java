/*******************************************************************************
 * Copyright (c) 2012 IT Solutions, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Torsten Stolpmann - Initial implementatin
 *******************************************************************************/
package com.itsolut.mantis.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.google.inject.Provider;


public class StandaloneRepositoryPersistencePathProvider  implements Provider<IPath> {

    public IPath get() {

        return new Path("repositoryConfigurations");
    }
}
