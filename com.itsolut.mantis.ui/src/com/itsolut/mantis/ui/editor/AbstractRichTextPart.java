
package com.itsolut.mantis.ui.editor;

import org.eclipse.mylyn.internal.tasks.ui.editors.TaskEditorRichTextPart;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;

import com.itsolut.mantis.core.MantisAttributeMapper;

/**
 * @author Robert Munteanu
 * 
 */
public class AbstractRichTextPart extends TaskEditorRichTextPart {

    private String _key;

    public AbstractRichTextPart(String label, MantisAttributeMapper.Attribute attribute) {

        setPartName(label);

        _key = attribute.getKey();

    }

    @Override
    public void initialize(AbstractTaskEditorPage taskEditorPage) {

        super.initialize(taskEditorPage);
        setAttribute(getTaskData().getRoot().getAttribute(_key));
    }

}
