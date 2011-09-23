/*******************************************************************************
 * Copyright (c) 2007,2008 Itsolut, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Shane - Initial API and implementation.
 *     David Carver - STAR - Mylyn 3.0 implementation.
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentSource;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

import com.google.inject.Inject;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.core.model.MantisTicketComment;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * 
 *  <p>Works around the fact that Mantis does not have a description column for attachments ( which
 * is required for storing contexts ) by setting a special filename, which contains the expected
 * description + a timestamp.</p>
 * 
 * @see MantisAttributeMapper#updateTaskAttachment(org.eclipse.mylyn.tasks.core.ITaskAttachment, TaskAttribute)
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisAttachmentHandler extends AbstractTaskAttachmentHandler {

    static final String CONTEXT_DESCRIPTION = "mylyn/context/zip";;

    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss");

    private final IMantisClientManager clientManager;

    private final StatusFactory statusFactory;

    @Inject
    public MantisAttachmentHandler(IMantisClientManager clientManager, StatusFactory statusFactory) {
        this.clientManager = clientManager;
        this.statusFactory = statusFactory;
    }

    private byte[] getAttachmentData(TaskRepository repository, TaskAttachmentMapper attachment, IProgressMonitor monitor) throws CoreException {
        String id = attachment.getAttachmentId();
        if (id == null || id.length() == 0) {
            throw new CoreException(statusFactory.toStatus("Attachment download from " + repository.getRepositoryUrl() + " failed, missing attachment filename.", null, repository));
        }

        try {
            IMantisClient client = clientManager.getRepository(repository);
            return client.getAttachmentData(Integer.parseInt(id), monitor);
        } catch (MantisException e) {
            throw new CoreException(statusFactory.toStatus("Attachment download from " +repository.getRepositoryUrl() + " failed : " + e.getMessage(), e , repository));
        }
    }

    private InputStream getAttachmentAsStream(TaskRepository repository,
            TaskAttachmentMapper attachment, IProgressMonitor monitor)
    throws CoreException {
        return new ByteArrayInputStream( getAttachmentData(repository, attachment, monitor) );
    }

    @Override
    public boolean canGetContent(TaskRepository repository, ITask task) {
        
        return repository != null;
    }

    @Override
    public boolean canPostContent(TaskRepository repository, ITask task) {
        
        return repository != null;
    }

    @Override
    public InputStream getContent(TaskRepository repository, ITask task,
            TaskAttribute attachmentAttribute, IProgressMonitor monitor)
    throws CoreException {
        try {
            monitor.beginTask("Getting attachment", IProgressMonitor.UNKNOWN);
            TaskAttachmentMapper attachment = TaskAttachmentMapper.createFrom(attachmentAttribute);
            return getAttachmentAsStream(repository, attachment, monitor);
        } finally {
            monitor.done();
        }
    }

    @Override
    public void postContent(TaskRepository repository, ITask task,
            AbstractTaskAttachmentSource source, String comment,
            TaskAttribute attachmentAttribute, IProgressMonitor monitor)
    throws CoreException {
        
        monitor.beginTask("Uploading attachment", 2);        

        try {
            IMantisClient client = clientManager.getRepository(repository);
            int id = Integer.parseInt(task.getTaskId());
            byte[] data = readData(source, monitor);

            //hack since context methods are final in superclasses & Mantis does not have a description column
            String filename = source.getName();

            if (CONTEXT_DESCRIPTION.equals(source.getDescription()))
                filename = CONTEXT_DESCRIPTION + "-" + dateFormat.format(new Date()) + ".zip"; // add zip extension
            else if ( attachmentAttribute != null){
                TaskAttachmentMapper mapper = TaskAttachmentMapper.createFrom(attachmentAttribute);
                if ( mapper.getFileName() != null)
                    filename = mapper.getFileName();
            }
        
            client.putAttachmentData(id, filename, data, monitor);
            Policy.advance(monitor, 1);
            
            if  ( !MantisUtils.isEmpty(comment) )
                client.addIssueComment(id, new MantisTicketComment(comment, 0), monitor);
            Policy.advance(monitor, 1);
            
        } catch (MantisException e) {
            throw new CoreException(statusFactory.toStatus("Attachment upload to " + task.getRepositoryUrl() + " failed, please see details.", e , repository));
        } catch (IOException e) {
            throw new CoreException(statusFactory.toStatus("Attachment upload to " + task.getRepositoryUrl() + " failed, please see details.", e , repository));
        } finally {
            monitor.done();
        }
    }

    private byte[] readData(AbstractTaskAttachmentSource attachment, IProgressMonitor monitor) throws IOException, CoreException {
        InputStream in = attachment.createInputStream(monitor);
        try {
            byte[] data = new byte[(int) attachment.getLength()];
            in.read(data, 0, (int) attachment.getLength());
            return data;
        } finally {
            in.close();
        }
    }
}
