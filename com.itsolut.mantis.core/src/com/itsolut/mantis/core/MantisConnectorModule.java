package com.itsolut.mantis.core;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;


/**
 * @author Robert Munteanu
 *
 */
public class MantisConnectorModule extends AbstractModule {

    private final MantisRepositoryConnector repositoryConnector;

    public MantisConnectorModule(MantisRepositoryConnector repositoryConnector) {
        
        this.repositoryConnector = repositoryConnector;
    }
    
    @Override
    protected void configure() {
        
        bind(MantisClientManager.class).toInstance(repositoryConnector.getClientManager());
        bind(StatusFactory.class).in(Singleton.class);
        bind(MantisAttachmentHandler.class).in(Singleton.class);
        bind(MantisTaskDataHandler.class).in(Singleton.class);
    }

}
