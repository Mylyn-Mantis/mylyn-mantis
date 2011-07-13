/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.core;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Locale;

import org.apache.axis.AxisFault;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.internal.provisional.commons.soap.AxisHttpFault;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;

import com.itsolut.mantis.core.exception.MantisLocalException;
import com.itsolut.mantis.core.exception.MantisLoginException;
import com.itsolut.mantis.core.exception.MantisRemoteException;
import com.itsolut.mantis.core.exception.TicketNotFoundException;

public class StatusFactory {

    public Status toStatus( String message, Throwable t, TaskRepository repository) {
        
        String actualMessage = message == null ? t.getMessage() : message;
        
        if ( t instanceof TicketNotFoundException )
            return new Status(IStatus.WARNING, MantisCorePlugin.PLUGIN_ID, actualMessage, t);
        
        if ( repository == null)
            return new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, actualMessage, t);
        
        if ( t instanceof MantisLoginException || ( actualMessage != null && actualMessage.toLowerCase(Locale.ENGLISH).contains("access denied")) )
            return new RepositoryStatus(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, RepositoryStatus.ERROR_PERMISSION_DENIED, actualMessage);
        if ( t instanceof MantisRemoteException) {
            if ( t.getCause() instanceof AxisHttpFault )  {
            	
            	AxisHttpFault httpFault = (AxisHttpFault) t.getCause();
            	
            	switch ( httpFault.getReturnCode() ) {
            	
            		case 404:
            			return RepositoryStatus.createNotFoundError(repository.getUrl(), MantisCorePlugin.PLUGIN_ID);
            		case 403:
            			return new RepositoryStatus(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, RepositoryStatus.ERROR_PERMISSION_DENIED, "Access denied by server configuration. Please contact your server administrator.");
            		case 401:
            			return new RepositoryStatus(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, RepositoryStatus.ERROR_PERMISSION_DENIED, "Server requested authentication, but none was given. Please provide HTTP credentials.");
            		case 302:
            		case 301:
            			return RepositoryStatus.createStatus(repository, IStatus.WARNING, MantisCorePlugin.PLUGIN_ID, "Repository moved to " + MantisRepositoryLocations.create(httpFault.getLocation()).getBaseRepositoryLocation() + ", please update the server location.");
            	}
            }
            if ( t.getCause() instanceof AxisFault ) {
                AxisFault fault = (AxisFault) t.getCause();
                if ( fault.detail instanceof IOException )
                    return new RepositoryStatus(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, RepositoryStatus.ERROR_PERMISSION_DENIED, "IO Error : " + fault.detail.getMessage() + " .");
            }
            
            if ( ((MantisRemoteException) t).isUnexpected() )
                return RepositoryStatus.createInternalError(MantisCorePlugin.PLUGIN_ID, actualMessage, t);
            
            return new RepositoryStatus(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, RepositoryStatus.ERROR_REPOSITORY, actualMessage);
            
        }
        if ( t instanceof MalformedURLException || t.getCause() instanceof MalformedURLException )
            return RepositoryStatus.createStatus(repository, RepositoryStatus.ERROR_REPOSITORY_NOT_FOUND, MantisCorePlugin.PLUGIN_ID, t.getMessage());
        if ( t instanceof MantisLocalException)
            return RepositoryStatus.createInternalError(MantisCorePlugin.PLUGIN_ID, actualMessage, t);
        if ( t instanceof IOException || t.getCause() instanceof IOException )
            return new RepositoryStatus(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, RepositoryStatus.ERROR_IO, actualMessage);
        
        return RepositoryStatus.createInternalError(MantisCorePlugin.PLUGIN_ID, actualMessage, t);
        
    }
}