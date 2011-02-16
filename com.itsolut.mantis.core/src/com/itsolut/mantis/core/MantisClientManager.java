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

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.internal.tasks.core.*;
import org.eclipse.mylyn.internal.tasks.core.TaskRepositoryDelta.Type;
import org.eclipse.mylyn.tasks.core.IRepositoryListener;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.osgi.util.NLS;

import com.itsolut.mantis.core.exception.MantisException;

/**
 * Caches {@link IMantisClient} objects.
 * 
 * @author Steffen Pingel
 */
public class MantisClientManager implements IRepositoryListener, IRepositoryChangeListener {

    private Map<String, IMantisClient> clientByUrl = new HashMap<String, IMantisClient>();
    private PersistedState state;

    public MantisClientManager(File cacheFile) {

        state = new PersistedState(cacheFile);
        state.read();
    }

    public synchronized void persistCache() {

        state.write();
    }

    public synchronized IMantisClient getRepository(TaskRepository taskRepository) throws MantisException {

        IMantisClient client = clientByUrl.get(taskRepository.getRepositoryUrl());
        if (client == null)
            client = newMantisClient(taskRepository);
        return client;
    }
    
    public synchronized IMantisClient getRepository(String url) throws MantisException {
        
        IMantisClient client = clientByUrl.get(url);
        if (client == null)
            throw new MantisException("No client with url " + url + " .");
        return client;
    }

    private IMantisClient newMantisClient(TaskRepository taskRepository) throws MantisException {

        AbstractWebLocation location = MantisClientFactory.getDefault().getTaskRepositoryLocationFactory()
                .createWebLocation(taskRepository);

        IMantisClient repository = MantisClientFactory.getDefault().createClient(location);

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
        
        MantisCorePlugin.debug(NLS.bind("repositoryChanged : {0} , {1} = {2} .",  new Object[] {repository.getUrl(), delta.getType(), delta.getKey()}), null);

        // do not refresh on sync time stamp updates, it's not relevant
        if (delta.getType() == Type.PROPERTY && delta.getKey().equals(IRepositoryConstants.PROPERTY_SYNCTIMESTAMP))
            return;

        clientByUrl.remove(repository.getRepositoryUrl());
        state.remove(repository.getRepositoryUrl());

    }

    public void repositorySettingsChanged(TaskRepository repository) {

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
            } catch ( FileNotFoundException cacheDoesNotExist) {
                // possible, deal with it
            } catch (Throwable e) {
                cleanCache(e);
            } finally {
                closeSilently(in);
            }

        }

        public void cleanCache(Throwable reason) {

            MantisCorePlugin.warn("Removing invalid cache file", reason);
            cacheFile.delete();
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
