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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.AbstractRepositoryTask;
import org.eclipse.mylar.tasks.core.IAttachmentHandler;
import org.eclipse.mylar.tasks.core.RepositoryAttachment;
import org.eclipse.mylar.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylar.tasks.core.TaskRepository;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisAttachmentHandler implements IAttachmentHandler {

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

	public void uploadAttachment(TaskRepository repository, AbstractRepositoryTask task, String comment, String description, File file, String contentType, boolean isPatch) throws CoreException {
		
		if (!MantisRepositoryConnector.hasAttachmentSupport(repository, task)) {
			throw new CoreException(new Status(IStatus.INFO, MantisCorePlugin.PLUGIN_ID, IStatus.OK, "Attachments are not supported by this repository access type.", null));
		}

		try {
			IMantisClient client = connector.getClientManager().getRepository(repository);
			int id = Integer.parseInt(AbstractRepositoryTask.getTaskId(task.getHandleIdentifier()));
			byte[] data = readData(file);
			
			//hack since context methods are final in superclasses & Mantis does not have a description column
			String filename = file.getName();
			if(AbstractRepositoryConnector.MYLAR_CONTEXT_DESCRIPTION.equals(description)){
				filename = AbstractRepositoryConnector.MYLAR_CONTEXT_DESCRIPTION;
			}
			
			
			client.putAttachmentData(id, filename, data);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, 0, "Attachment upload to " + task.getRepositoryUrl() + " failed, please see details.", e ));
		}
	}

	private byte[] readData(File file) throws IOException {
		if (file.length() > Integer.MAX_VALUE) {
			throw new IOException("Can not upload files larger than " + Integer.MAX_VALUE + " bytes");
		}

		InputStream in = new FileInputStream(file);
		try {
			byte[] data = new byte[(int) file.length()];
			in.read(data, 0, (int) file.length());
			return data;
		} finally {
			in.close();
		}
	}

	public boolean canDownloadAttachment(TaskRepository repository, AbstractRepositoryTask task) {
		if (repository == null) {
			return false;
		}
		return MantisRepositoryConnector.hasAttachmentSupport(repository, task);
	}

	public boolean canUploadAttachment(TaskRepository repository, AbstractRepositoryTask task) {
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
			throw new CoreException(new Status(IStatus.ERROR, MantisCorePlugin.PLUGIN_ID, 0, "Attachment download from " +repository.getUrl() + " failed, please see details.", e ));
		}
	}
	
}
