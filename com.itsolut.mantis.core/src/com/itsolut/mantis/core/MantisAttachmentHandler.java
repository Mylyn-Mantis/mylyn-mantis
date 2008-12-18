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

import org.apache.commons.httpclient.methods.multipart.PartSource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.tasks.core.RepositoryTaskHandleUtil;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentSource;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

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

    private final MantisRepositoryConnector connector;

    public MantisAttachmentHandler(MantisRepositoryConnector connector) {
        this.connector = connector;
    }

    private byte[] getAttachmentData(TaskRepository repository, TaskAttachmentMapper attachment) throws CoreException {
        String id = attachment.getAttachmentId();
        if (id == null) {
            throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, IStatus.OK, "Attachment download from " + repository.getRepositoryUrl() + " failed, missing attachment filename.", null));
        }

        try {
            IMantisClient client = connector.getClientManager().getRepository(repository);
            return client.getAttachmentData(Integer.parseInt(id));
        } catch (Exception e) {
            MantisCorePlugin.log(e);
            throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, 0, "Attachment download from " +repository.getRepositoryUrl() + " failed, please see details.", e ));
        }
    }

    private InputStream getAttachmentAsStream(TaskRepository repository,
            TaskAttachmentMapper attachment, IProgressMonitor monitor)
    throws CoreException {
        return new ByteArrayInputStream( getAttachmentData(repository, attachment) );
    }

    @Override
    public boolean canGetContent(TaskRepository repository, ITask task) {
        if (repository == null) {
            return false;
        }
        return MantisRepositoryConnector.hasAttachmentSupport(repository, task);
    }

    @Override
    public boolean canPostContent(TaskRepository repository, ITask task) {
        if (repository == null) {
            return false;
        }
        return MantisRepositoryConnector.hasAttachmentSupport(repository, task);
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
        if (!MantisRepositoryConnector.hasAttachmentSupport(repository, task)) {
            throw new CoreException(new Status(IStatus.INFO, MantisCorePlugin.PLUGIN_ID, IStatus.OK, "Attachments are not supported by this repository access type.", null));
        }

        try {
            IMantisClient client = connector.getClientManager().getRepository(repository);
            int id = Integer.parseInt(RepositoryTaskHandleUtil.getTaskId(task.getHandleIdentifier()));
            byte[] data = readData(source, monitor);

            //hack since context methods are final in superclasses & Mantis does not have a description column
            String filename = source.getName();

            if (CONTEXT_DESCRIPTION.equals(source.getDescription()))
                filename = CONTEXT_DESCRIPTION + "-" + dateFormat.format(new Date());

            client.putAttachmentData(id, filename, data);
        } catch (Exception e) {
            MantisCorePlugin.log(e);
            throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, 0, "Attachment upload to " + task.getRepositoryUrl() + " failed, please see details.", e ));
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


    public static class AttachmentPartSource implements PartSource {

        private final AbstractTaskAttachmentSource attachment;

        public AttachmentPartSource(AbstractTaskAttachmentSource attachment) {
            this.attachment = attachment;
        }

        public InputStream createInputStream() throws IOException {
            try {
                return attachment.createInputStream(null);
            } catch (CoreException e) {
                StatusHandler.log(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID,
                        "Error submitting attachment", e));
                throw new IOException("Failed to create source stream");
            }
        }

        public String getFileName() {
            return attachment.getName();
        }

        public long getLength() {
            return attachment.getLength();
        }

    }


}
