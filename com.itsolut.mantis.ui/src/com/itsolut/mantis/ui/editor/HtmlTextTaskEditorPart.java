package com.itsolut.mantis.ui.editor;

import org.eclipse.mylyn.internal.tasks.ui.editors.EditorUtil;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class HtmlTextTaskEditorPart extends AbstractTaskEditorPart {
    
    private Composite composite;
    private String attributeName;
    
    public HtmlTextTaskEditorPart(String partName, String attributeName) {

        setPartName(partName);
        this.attributeName = attributeName; 
    }
    
    @Override
    public void createControl(Composite parent, FormToolkit toolkit) {

        Section section = createSection(parent, toolkit, ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);

        composite = toolkit.createComposite(section);
        composite.setLayout(EditorUtil.createSectionClientLayout());

        AbstractAttributeEditor attributeEditor = createAttributeEditor(getTaskData().getRoot().getAttribute(attributeName));
        
        attributeEditor.createControl(composite, toolkit);
        
        attributeEditor.getControl().setLayoutData(
                EditorUtil.getTextControlLayoutData(getTaskEditorPage(), composite, getExpandVertically()));
        attributeEditor.getControl().setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
        
        toolkit.paintBordersFor(composite);
        section.setClient(composite);
        setSection(toolkit, section);
    }
}
