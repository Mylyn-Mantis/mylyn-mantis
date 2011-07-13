package com.itsolut.mantis.core;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;


/**
 * @author Robert Munteanu
 *
 */
public class MantisPluginModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(StatusFactory.class).in(Singleton.class);
    }

}
