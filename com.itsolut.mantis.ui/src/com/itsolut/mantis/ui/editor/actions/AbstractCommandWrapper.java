/*******************************************************************************
 * Copyright (c) 2010 Tom Seidel, Remus Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *
 * Contributors:
 *     Tom Seidel - initial API and implementation
 *******************************************************************************/

package com.itsolut.mantis.ui.editor.actions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylyn.htmltext.HtmlComposer;
import org.eclipse.mylyn.htmltext.HtmlTextActivator;
import org.eclipse.mylyn.htmltext.commands.Command;
import org.eclipse.mylyn.htmltext.model.TriState;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

/**
 * @author Tom Seidel <tom.seidel@remus-software.org>
 */
public abstract class AbstractCommandWrapper extends Action implements
		PropertyChangeListener {

	protected static List<Image> images = new ArrayList<Image>();

	static {

		ImageDescriptor createFromURL = ImageDescriptor
				.createFromURL(FileLocator.find(
						Platform.getBundle(HtmlTextActivator.PLUGIN_ID),
						new Path("ckeditor/skins/office2003/icons.png"),
						Collections.EMPTY_MAP));
		Image image = createFromURL.createImage();

		for (int i = 1, n = 75; i < n; i++) {
			Image img = new Image(null, 16, 16);
			GC gc = new GC(img);

			gc.drawImage(image, 0, i * 16, 16, 16, 0, 0, 16, 16);
			gc.dispose();
			images.add(img);
		}
		image.dispose();

	}

	protected final Command wrappedCommand;
	protected final HtmlComposer composer;

	public AbstractCommandWrapper(String text, int style, HtmlComposer composer) {
		super(text, style);
		this.composer = composer;
		wrappedCommand = getWrappedCommand();
		assert (wrappedCommand != null);
		wrappedCommand.setComposer(composer);
		wrappedCommand.addPropertyChangeListener(this);
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if ("state".equals(evt.getPropertyName())) {
			switch ((TriState) evt.getNewValue()) {
			case OFF:
				setChecked(false);
				setEnabled(true);
				break;
			case ON:
				setChecked(true);
				setEnabled(true);
				break;
			case DISABLED:
				setEnabled(false);
				setChecked(false);
			default:
				break;
			}
		}

	}

	@Override
	public void run() {
		wrappedCommand.execute();
	}

	protected abstract Command getWrappedCommand();

}
