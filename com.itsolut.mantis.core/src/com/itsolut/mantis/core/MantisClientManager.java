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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.internal.tasks.core.IRepositoryChangeListener;
import org.eclipse.mylyn.internal.tasks.core.IRepositoryConstants;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryChangeEvent;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryDelta;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryDelta.Type;
import org.eclipse.mylyn.tasks.core.IRepositoryListener;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;

import com.itsolut.mantis.core.exception.MantisException;

/**
 * Caches {@link IMantisClient} objects.
 * 
 * @author Steffen Pingel
 */
public class MantisClientManager implements IRepositoryListener, IRepositoryChangeListener {

    private Map<String, IMantisClient> clientByUrl = new HashMap<String, IMantisClient>();
    private PersistedState state;

    private TaskRepositoryLocationFactory taskRepositoryLocationFactory = new TaskRepositoryLocationFactory();
    
    public MantisClientManager(File cacheFile) {

        state = new PersistedState(cacheFile);
        state.read();
    }

    public void persistCache() {

        state.write();
    }

    public synchronized IMantisClient getRepository(TaskRepository taskRepository) throws MantisException {

        IMantisClient client = clientByUrl.get(taskRepository.getRepositoryUrl());
        if (client == null)
            client = newMantisClient(taskRepository);
        return client;
    }

    private IMantisClient newMantisClient(TaskRepository taskRepository) throws MantisException {

        AbstractWebLocation location = MantisClientFactory.getDefault().getTaskRepositoryLocationFactory()
                .createWebLocation(taskRepository);

        IMantisClient repository = MantisClientFactory.getDefault().createClient(location);

        MantisCorePlugin.debug("Creating new Mantis client for url " + taskRepository.getRepositoryUrl()
                + " . Currently cached entries : " + clientByUrl.keySet() + " ." + " . MantisClientManager identity : "
                + System.identityHashCode(this) + " .", new RuntimeException());

        MantisCacheData cacheData = state.get(location.getUrl());
        if (cacheData != null) {
            repository.setCacheData(cacheData);
        } else {
            state.add(location.getUrl(), repository.getCacheData());
        }

        clientByUrl.put(taskRepository.getRepositoryUrl(), repository);

        return repository;
    }

    public synchronized void repositoryAdded(TaskRepository repository) {

        if (!MantisCorePlugin.REPOSITORY_KIND.equals(repository.getConnectorKind()))
            return;

        MantisCorePlugin.debug("repositoryAdded : " + repository.getRepositoryUrl() + " .", new RuntimeException());

        // make sure there is no stale client still in the cache, bug #149939
        clientByUrl.remove(repository.getRepositoryUrl());
        state.remove(repository.getRepositoryUrl());
    }

    public synchronized void repositoryRemoved(TaskRepository repository) {

        if (!MantisCorePlugin.REPOSITORY_KIND.equals(repository.getConnectorKind()))
            return;

        MantisCorePlugin.debug("repositoryRemoved : " + repository.getRepositoryUrl() + " .", new RuntimeException());

        clientByUrl.remove(repository.getRepositoryUrl());
        state.remove(repository.getRepositoryUrl());
    }

    public void repositoryChanged(TaskRepositoryChangeEvent event) {

        TaskRepository repository = event.getRepository();
        TaskRepositoryDelta delta = event.getDelta();

        if (!MantisCorePlugin.REPOSITORY_KIND.equals(repository.getConnectorKind()))
            return;

        MantisCorePlugin.debug("repositoryChanged : " + repository.getUrl() + ", " + delta.getType() + " .",
                new RuntimeException());

        // do not refresh on sync time stamp updates, it's not relevant
        if (delta.getType() == Type.PROPERTY && delta.getKey().equals(IRepositoryConstants.PROPERTY_SYNCTIMESTAMP))
            return;

        clientByUrl.remove(repository.getRepositoryUrl());
        state.remove(repository.getRepositoryUrl());

    }

    public synchronized void repositorySettingsChanged(TaskRepository repository) {

        // handled in repositoryChanged

    }

    public void repositoryUrlChanged(TaskRepository repository, String oldUrl) {

    }

    private static class PersistedState implements Serializable {

        private static final long serialVersionUID = 1L;

        private Map<String, MantisCacheData> _cacheDataByUrl = new HashMap<String, MantisCacheData>();

        private File cacheFile;

        public PersistedState(File cacheFile) {

            this.cacheFile = cacheFile;
        }

        public void add(String url, MantisCacheData data) {

            _cacheDataByUrl.put(url, data);
        }

        public void remove(String url) {

            _cacheDataByUrl.remove(url);
        }

        public MantisCacheData get(String url) {

            return _cacheDataByUrl.get(url);
        }

        public void read() {

            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(new FileInputStream(cacheFile));
                int size = in.readInt();
                for (int i = 0; i < size; i++) {
                    String url = (String) in.readObject();
                    MantisCacheData data = (MantisCacheData) in.readObject();
                    add(url, data);
                }
            } catch (Throwable e) {
                cleanCache(e);
            } finally {
                closeSilently(in);
            }

        }

        public void cleanCache(Throwable reason) {

            cacheFile.delete();
            MantisCorePlugin.log(new Status(IStatus.WARNING, MantisCorePlugin.PLUGIN_ID, "Removing invalid cache file",
                    reason));
        }

        public void write() {

            ObjectOutputStream out = null;
            try {
                out = new ObjectOutputStream(new FileOutputStream(cacheFile));
                out.writeInt(_cacheDataByUrl.size());
                for (String url : _cacheDataByUrl.keySet()) {
                    out.writeObject(url);
                    out.writeObject(_cacheDataByUrl.get(url));
                }
            } catch (Throwable e) {
                MantisCorePlugin.log(e);
            } finally {
                closeSilently(out);
            }

        }

        private void closeSilently(Closeable closeable) {

            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }

}
