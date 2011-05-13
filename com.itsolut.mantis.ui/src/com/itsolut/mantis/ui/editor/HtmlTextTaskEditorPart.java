/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.ui.editor;

import org.eclipse.mylyn.internal.tasks.ui.editors.EditorUtil;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class HtmlTextTaskEditorPart extends AbstractTaskEditorPart {
    
    private Composite composite;
    private String attributeName;
    private int style = ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED;
    
    public HtmlTextTaskEditorPart(String partName, String attributeName, boolean expandedByDefault) {

        setPartName(partName);
        this.attributeName = attributeName;
        if ( !expandedByDefault )
            collapse();
        
    }
    
    @Override
    public void createControl(Composite parent, FormToolkit toolkit) {
        
        TaskAttribute attribute = getTaskData().getRoot().getAttribute(attributeName);
        
        if ( getModel().hasIncomingChanges(attribute))
            expand();
        
        Section section = createSection(parent, toolkit, style);

        composite = toolkit.createComposite(section);
        composite.setLayout(EditorUtil.createSectionClientLayout());

        AbstractAttributeEditor attributeEditor = createAttributeEditor(attribute);
        
        attributeEditor.createControl(composite, toolkit);
        
        attributeEditor.getControl().setLayoutData(
                EditorUtil.getTextControlLayoutData(getTaskEditorPage(), composite, getExpandVertically()));
        attributeEditor.getControl().setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
        
        toolkit.paintBordersFor(composite);
        section.setClient(composite);
        setSection(toolkit, section);
        
        getTaskEditorPage().getAttributeEditorToolkit().adapt(attributeEditor);
     
    }
    
    private void collapse() {
        
        style = style & ~ExpandableComposite.EXPANDED;
    }
    
    private void expand() {
        
        style = style | ExpandableComposite.EXPANDED;
    }
}
