/*******************************************************************************
 * Copyright (c) 2010 Robert Munteanu
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *
 * Contributors:
 *     Robert Munteanu - initial API and implementation
 *******************************************************************************/

package com.itsolut.mantis.ui.editor.actions;

import org.eclipse.mylyn.htmltext.commands.Command;

/**
 * @author Robert Munteanu <robert.munteanu@gmail.com>
 */
public class PreformatCommand extends Command {

	@Override
	public String getCommandIdentifier() {
		return "pre";
	}

}
