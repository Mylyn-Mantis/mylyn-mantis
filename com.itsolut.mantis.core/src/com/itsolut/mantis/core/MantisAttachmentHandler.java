/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package com.itsolut.mantis.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.internal.tasks.core.RepositoryTaskHandleUtil;
import org.eclipse.mylyn.internal.tasks.core.deprecated.ITaskAttachment;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryAttachment;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryTaskAttribute;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractAttachmentHandler;

/**
 * 
 *  <p>Works around the fact that Mantis does not have a description column for attachments ( which
 * is required for storing contexts ) by setting a special filename, which contains the expected
 * description + a timestamp.</p>
 * 
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisAttachmentHandler extends AbstractAttachmentHandler {
	
	private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
	
	private MantisRepositoryConnector connector;

	public MantisAttachmentHandler(MantisRepositoryConnector connector) {
		this.connector = connector;
	}

	public void downloadAttachment(TaskRepository repository, RepositoryAttachment attachment, File file) throws CoreException {
		String id = attachment.getAttributeValue(RepositoryTaskAttribute.ATTACHMENT_ID);
		if (id == null) {
			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, IStatus.OK, "Attachment download from " + repository.getUrl() + " failed, missing attachment filename.", null));
		}
		
		try {
			IMantisClient client = connector.getClientManager().getRepository(repository);
			byte[] data = client.getAttachmentData(Integer.parseInt(id));
			writeData(file, data);
		} catch (Exception e) {
			MantisCorePlugin.log(e);
			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, 0, "Attachment download from " +repository.getUrl() + " failed, please see details.", e ));
		}
	}

	private void writeData(File file, byte[] data) throws IOException {
		OutputStream out = new FileOutputStream(file);
		try {
			out.write(data);
		} finally {
			out.close();
		}
	}

	public void uploadAttachment(TaskRepository repository, ITask task,
			ITaskAttachment attachment, String comment, IProgressMonitor monitor) throws CoreException {
		
		if (!MantisRepositoryConnector.hasAttachmentSupport(repository, task)) {
			throw new CoreException(new Status(IStatus.INFO, MantisCorePlugin.PLUGIN_ID, IStatus.OK, "Attachments are not supported by this repository access type.", null));
		}

		try {
			IMantisClient client = connector.getClientManager().getRepository(repository);
			int id = Integer.parseInt(RepositoryTaskHandleUtil.getTaskId(task.getHandleIdentifier()));
			byte[] data = readData(attachment);
			
			//hack since context methods are final in superclasses & Mantis does not have a description column
			String filename = attachment.getFilename();
//			if(MYLAR_CONTEXT_DESCRIPTION.equals(attachment.getDescription())){
//				filename = MYLAR_CONTEXT_DESCRIPTION + "-" + dateFormat.format(new Date()) ;
//			}
			
			
			client.putAttachmentData(id, filename, data);
		} catch (Exception e) {
			MantisCorePlugin.log(e);
			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, 0, "Attachment upload to " + task.getRepositoryUrl() + " failed, please see details.", e ));
		}
	}

	private byte[] readData(ITaskAttachment attachment) throws IOException {
		InputStream in = attachment.createInputStream();
		try {
			byte[] data = new byte[(int) attachment.getLength()];
			in.read(data, 0, (int) attachment.getLength());
			return data;
		} finally {
			in.close();
		}
	}

	public boolean canDownloadAttachment(TaskRepository repository, ITask task) {
		if (repository == null) {
			return false;
		}
		return MantisRepositoryConnector.hasAttachmentSupport(repository, task);
	}

	public boolean canUploadAttachment(TaskRepository repository, ITask task) {
		if (repository == null) {
			return false;
		}
		return MantisRepositoryConnector.hasAttachmentSupport(repository, task);
	}

	public boolean canDeprecate(TaskRepository repository, RepositoryAttachment attachment) {		
		return false;
	}

	public void updateAttachment(TaskRepository repository, RepositoryAttachment attachment) throws CoreException {
		// ignore
	}

	public byte[] getAttachmentData(TaskRepository repository, RepositoryAttachment attachment) throws CoreException {
		String id = attachment.getAttributeValue(RepositoryTaskAttribute.ATTACHMENT_ID);
		if (id == null) {
			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, IStatus.OK, "Attachment download from " + repository.getUrl() + " failed, missing attachment filename.", null));
		}
		
		try {
			IMantisClient client = connector.getClientManager().getRepository(repository);
			return client.getAttachmentData(Integer.parseInt(id));
		} catch (Exception e) {
			MantisCorePlugin.log(e);
			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, 0, "Attachment download from " +repository.getUrl() + " failed, please see details.", e ));
		}
	}

	public InputStream getAttachmentAsStream(TaskRepository repository,
			RepositoryAttachment attachment, IProgressMonitor monitor)
			throws CoreException {
		return new ByteArrayInputStream( getAttachmentData(repository, attachment) );
	}
	
}
