/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2007, 2008 - 2007 IT Solutions, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *     David Carver - STAR - Migrated to Mylyn 3.0
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.tasks.core.IRepositoryListener;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;
import org.eclipse.mylyn.tasks.ui.TaskRepositoryLocationUiFactory;

/**
 * Caches {@link IMantisClient} objects.
 * 
 * @author Steffen Pingel
 */
public class MantisClientManager implements IRepositoryListener {
    
    private Map<String, IMantisClient> clientByUrl = new HashMap<String, IMantisClient>();
    
    private Map<String, MantisClientData> clientDataByUrl = new HashMap<String, MantisClientData>();
    
    private File cacheFile;
    
    private TaskRepositoryLocationFactory taskRepositoryLocationFactory = new TaskRepositoryLocationUiFactory();
    
    public MantisClientManager(File cacheFile) {

        this.cacheFile = cacheFile;
        
        readCache();
    }
    
    public synchronized IMantisClient getRepository(TaskRepository taskRepository) throws MalformedURLException {

        IMantisClient repository = clientByUrl.get(taskRepository.getRepositoryUrl());
        if (repository == null) {
            
            AuthenticationCredentials repositoryCredentials = taskRepository.getCredentials(AuthenticationType.REPOSITORY);
            AuthenticationCredentials repositoryHttpCredentials = taskRepository.getCredentials(AuthenticationType.HTTP);
            
            String repositoryUserName = "";
            String repositoryPassword = "";
            
            if (repositoryCredentials != null) {
            	repositoryUserName = repositoryCredentials.getUserName();
            	repositoryPassword = repositoryCredentials.getPassword();
            }
            
            String httpUserName = "";
            String httpPassword = "";
            
            if (repositoryHttpCredentials != null) {
                httpUserName = repositoryHttpCredentials.getUserName();
                httpPassword = repositoryHttpCredentials.getPassword();
            }
            
            AbstractWebLocation location = taskRepositoryLocationFactory.createWebLocation(taskRepository);
            
            repository = MantisClientFactory.createClient(taskRepository.getRepositoryUrl(), repositoryUserName, repositoryPassword, httpUserName, httpPassword, location);
            
            clientByUrl.put(taskRepository.getRepositoryUrl(), repository);
            
            MantisClientData data = clientDataByUrl.get(taskRepository.getRepositoryUrl());
            if (data == null) {
                data = new MantisClientData();
                clientDataByUrl.put(taskRepository.getRepositoryUrl(), data);
            }
            repository.setData(data);
        }
        return repository;
    }
    
    public synchronized void repositoryAdded(TaskRepository repository) {

        // make sure there is no stale client still in the cache, bug #149939
        clientByUrl.remove(repository.getRepositoryUrl());
        clientDataByUrl.remove(repository.getRepositoryUrl());
    }
    
    public synchronized void repositoryRemoved(TaskRepository repository) {

        clientByUrl.remove(repository.getRepositoryUrl());
        clientDataByUrl.remove(repository.getRepositoryUrl());
    }
    
    public synchronized void repositorySettingsChanged(TaskRepository repository) {

        clientByUrl.remove(repository.getRepositoryUrl());
    }
    
    public void readCache() {

        if (cacheFile == null || !cacheFile.exists()) {
            return;
        }
        
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(cacheFile));
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                String url = (String) in.readObject();
                MantisClientData data = (MantisClientData) in.readObject();
                if (url != null && data != null) {
                    clientDataByUrl.put(url, data);
                }
            }
        } catch (Throwable e) {
            MantisCorePlugin.log(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        
    }
    
    public void writeCache() {

        if (cacheFile == null) {
            return;
        }
        
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(cacheFile));
            out.writeInt(clientDataByUrl.size());
            for (String url : clientDataByUrl.keySet()) {
                out.writeObject(url);
                out.writeObject(clientDataByUrl.get(url));
            }
        } catch (IOException e) {
            MantisCorePlugin.log(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
    
    public void repositoryUrlChanged(TaskRepository repository, String oldUrl) {

    }
    
}
