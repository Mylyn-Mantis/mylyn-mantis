package com.itsolut.mantis.ui.editor;

import org.eclipse.mylyn.htmltext.HtmlComposer;
import org.eclipse.mylyn.internal.tasks.ui.editors.EditorUtil;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class HtmlTextTaskEditorPart extends AbstractTaskEditorPart {
    
    private Composite composite;
    private String attributeName;
    private HtmlComposer composer;
    
    public HtmlTextTaskEditorPart(String partName, String attributeName) {

        setPartName(partName);
        this.attributeName = attributeName; 
    }
    
    @Override
    public void createControl(Composite parent, FormToolkit toolkit) {

        Section section = createSection(parent, toolkit, ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);

        composite = toolkit.createComposite(section);
        composite.setLayout(EditorUtil.createSectionClientLayout());

        String attributeValue = getTaskData().getRoot().getAttribute(attributeName).getValue();
        
        composer = new HtmlComposer(composite, SWT.None);
        composer.setHtml(attributeValue);
        
        composer.getBrowser().setLayoutData(
                EditorUtil.getTextControlLayoutData(getTaskEditorPage(), composer.getBrowser(), getExpandVertically()));
        composer.getBrowser().setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
        
        toolkit.paintBordersFor(composite);
        section.setClient(composite);
        setSection(toolkit, section);
    }
    
    @Override
    public void dispose() {
    
        super.dispose();
        
        composer.dispose();
    }
}
