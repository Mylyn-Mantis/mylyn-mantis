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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.mylyn.internal.tasks.core.IRepositoryChangeListener;
import org.eclipse.mylyn.internal.tasks.core.IRepositoryConstants;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryChangeEvent;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryDelta;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryDelta.Type;
import org.eclipse.mylyn.tasks.core.IRepositoryListener;
import org.eclipse.mylyn.tasks.core.TaskRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.itsolut.mantis.core.exception.MantisException;

/**
 * Caches {@link IMantisClient} objects.
 * 
 * @author Robert Munteanu
 */
@Singleton
public class MantisClientManager implements IRepositoryListener, IRepositoryChangeListener, IMantisClientManager, IShutdown {

    private Map<String, IMantisClient> clientByUrl = new HashMap<String, IMantisClient>();
    private final PersistedState state;
    private final MantisClientFactory clientFactory;
    private final Tracer tracer;

    @Inject
    public MantisClientManager(@RepositoryPersistencePath IPath repositoryPersistencePath, MantisClientFactory clientFactory, Tracer tracer) {

        this.clientFactory = clientFactory;
        state = new PersistedState(repositoryPersistencePath.toFile());
        this.tracer = tracer;
    }

    public synchronized void onShutdown() {

        state.write();
    }

    public synchronized IMantisClient getRepository(TaskRepository taskRepository) throws MantisException {

        IMantisClient client = clientByUrl.get(taskRepository.getRepositoryUrl());
        if (client == null)
            client = newMantisClient(taskRepository);
        return client;
    }

    private IMantisClient newMantisClient(TaskRepository taskRepository) throws MantisException {

        IMantisClient repository = clientFactory.createClient(taskRepository);

        String repositoryUrl = taskRepository.getUrl();
        MantisCacheData cacheData = state.get(repositoryUrl);
        if (cacheData != null) {
            repository.setCacheData(cacheData);
        } else {
            state.add(repositoryUrl, repository.getCacheData());
        }

        clientByUrl.put(taskRepository.getRepositoryUrl(), repository);

        return repository;
    }

    public synchronized void repositoryAdded(TaskRepository repository) {

        if (!MantisCorePlugin.REPOSITORY_KIND.equals(repository.getConnectorKind()))
            return;

        // make sure there is no stale client still in the cache, bug #149939
        clientByUrl.remove(repository.getRepositoryUrl());
        state.remove(repository.getRepositoryUrl());
    }

    public synchronized void repositoryRemoved(TaskRepository repository) {

        if (!MantisCorePlugin.REPOSITORY_KIND.equals(repository.getConnectorKind()))
            return;
        
        clientByUrl.remove(repository.getRepositoryUrl());
        state.remove(repository.getRepositoryUrl());
    }

    public synchronized void repositoryChanged(TaskRepositoryChangeEvent event) {

        TaskRepository repository = event.getRepository();
        TaskRepositoryDelta delta = event.getDelta();
        
        if (!MantisCorePlugin.REPOSITORY_KIND.equals(repository.getConnectorKind()))
            return;
        
        tracer.trace(TraceLocation.CLIENT_MANAGER, "repositoryChanged : {0} , {1} = {2} .",  repository.getUrl(), delta.getType(), delta.getKey());

        boolean credentialsChanged = delta.getType() == Type.CREDENTIALS;
        boolean urlChanged = delta.getType() == Type.PROPERTY && delta.getKey().equals(IRepositoryConstants.PROPERTY_URL);

        if ( !credentialsChanged && !urlChanged )
            return;
        
        tracer.trace(TraceLocation.CLIENT_MANAGER, "Clearing repository state; credentialsChanged: {0}, urlChanged: {1}", credentialsChanged, urlChanged);;
        
        clientByUrl.remove(repository.getRepositoryUrl());
        state.remove(repository.getRepositoryUrl());

    }

    public void repositorySettingsChanged(TaskRepository repository) {

        // handled in repositoryChanged

    }

    public void repositoryUrlChanged(TaskRepository repository, String oldUrl) {

        // handled in repositoryChanged
    }

    static class PersistedState implements Serializable {

        private static final long serialVersionUID = 1L;

        private Map<String, MantisCacheData> _cacheDataByUrl = new HashMap<String, MantisCacheData>();

        private File cacheFile;
        private boolean read;

        public PersistedState(File cacheFile) {

            this.cacheFile = cacheFile;
        }

        public void add(String url, MantisCacheData data) {
            
            ensureRead();

            add0(url, data);
        }
        
        private void ensureRead() {

            if ( !read )
                read();
        }
        
        private void add0(String url, MantisCacheData data) {

            _cacheDataByUrl.put(url, data);
        }

        public void remove(String url) {

            ensureRead();
            
            _cacheDataByUrl.remove(url);
        }

        public MantisCacheData get(String url) {

            ensureRead();
            
            return _cacheDataByUrl.get(url);
        }

        private void read() {

            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(new FileInputStream(cacheFile));
                int size = in.readInt();
                for (int i = 0; i < size; i++) {
                    String url = (String) in.readObject();
                    MantisCacheData data = (MantisCacheData) in.readObject();
                    add0(url, data);
                }
            } catch ( FileNotFoundException cacheDoesNotExist) {
                // possible, deal with it
            } catch (Throwable e) {
                cleanCache(e);
            } finally {
                read = true;
                closeSilently(in);
            }

        }

        public void cleanCache(Throwable reason) {

            if ( MantisCorePlugin.getDefault() != null )
                MantisCorePlugin.warn("Removing invalid cache file", reason);
            cacheFile.delete();
        }

        public void write() {
            
            ensureRead();

            ObjectOutputStream out = null;
            try {
                out = new ObjectOutputStream(new FileOutputStream(cacheFile));
                out.writeInt(_cacheDataByUrl.size());
                for (String url : _cacheDataByUrl.keySet()) {
                    out.writeObject(url);
                    out.writeObject(_cacheDataByUrl.get(url));
                }
            } catch (Throwable e) {
                MantisCorePlugin.error("Failed writing persistent state.", e);
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
