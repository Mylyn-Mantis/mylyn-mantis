package com.itsolut.mantis.ui.editor;

import org.eclipse.mylyn.internal.tasks.ui.editors.TaskEditorRichTextPart;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import com.itsolut.mantis.core.MantisAttributeMapper;
import com.itsolut.mantis.core.util.MantisUtils;

/**
 * @author Robert Munteanu
 * 
 */
@SuppressWarnings("restriction")
public class AbstractRichTextPart extends TaskEditorRichTextPart {

    private String _key;

    public AbstractRichTextPart(String label, MantisAttributeMapper.Attribute attribute,
            boolean expandedByDefault) {

        setPartName(label);

        if (!expandedByDefault)
            collapse();

        _key = attribute.getKey();
    }

    @Override
    public void initialize(AbstractTaskEditorPage taskEditorPage) {

        super.initialize(taskEditorPage);

        TaskAttribute attribute = getTaskData().getRoot().getAttribute(_key);
        setAttribute(attribute);

        if ( MantisUtils.hasValue (attribute) )
            expand();

    }

    private void collapse() {

        setSectionStyle(getSectionStyle() & ~ExpandableComposite.EXPANDED);
    }
    
    private void expand() {
    	
    	setSectionStyle(getSectionStyle() & ExpandableComposite.EXPANDED);
    }

}
