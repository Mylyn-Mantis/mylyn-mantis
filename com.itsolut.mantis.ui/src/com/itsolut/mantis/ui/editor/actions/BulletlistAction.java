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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylyn.htmltext.HtmlComposer;
import org.eclipse.mylyn.htmltext.commands.Command;
import org.eclipse.mylyn.htmltext.commands.list.BulletlistCommand;

/**
 * @author Tom Seidel <tom.seidel@remus-software.org>
 */
public class BulletlistAction extends AbstractCommandWrapper {

	public BulletlistAction(HtmlComposer composer) {
		super("Bulletted list", IAction.AS_CHECK_BOX, composer); //$NON-NLS-1$
		setImageDescriptor(ImageDescriptor.createFromImage(images.get(25)));

	}

	@Override
	protected Command getWrappedCommand() {
		return new BulletlistCommand();
	}

}
