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
