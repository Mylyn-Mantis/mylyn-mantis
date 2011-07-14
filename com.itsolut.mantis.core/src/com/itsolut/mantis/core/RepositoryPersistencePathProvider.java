/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

import com.google.inject.Provider;


/**
 * @author Robert Munteanu
 *
 */
class RepositoryPersistencePathProvider implements Provider<IPath> {

    public IPath get() {

        return Platform.getStateLocation(MantisCorePlugin.getDefault().getBundle()).append("repositoryConfigurations");
    }
}
