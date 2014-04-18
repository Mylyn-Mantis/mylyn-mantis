/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.ui.internal.wikitext;

import org.eclipse.mylyn.wikitext.tasks.ui.editor.MarkupTaskEditorExtension;

/**
 * @author Robert Munteanu
 * 
 */
public class MantisHtmlMarkupTaskEditorExtension extends MarkupTaskEditorExtension<MantisHtmlMarkupLanguage> {

    public MantisHtmlMarkupTaskEditorExtension() {

        setMarkupLanguage(new MantisHtmlMarkupLanguage());
    }
}
