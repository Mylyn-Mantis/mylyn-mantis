package com.itsolut.mantis.ui.editor;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.mylyn.internal.tasks.ui.editors.TaskEditorAttributePart;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import com.itsolut.mantis.core.MantisAttributeMapper;

public class MantisAttributeEditorPart extends TaskEditorAttributePart {

	
	
	public MantisAttributeEditorPart() {
		super();
		setPartName("Mantis Attributes");
	}

	@Override
	public void createControl(Composite parent, FormToolkit toolkit) {
		super.createControl(parent, toolkit);
		
//		Composite section = toolkit.createComposite(parent);
//		Composite comp1 = toolkit.createComposite(section);
//		comp1.setLayout(new GridLayout(2, false));
//
//		addAttribute(comp1, toolkit,
//				getAttribute(MantisAttributeMapper.Attribute.PROJECT));
//		addAttribute(comp1, toolkit,
//				getAttribute(MantisAttributeMapper.Attribute.CATEGORY));
//		addAttribute(comp1, toolkit,
//				getAttribute(MantisAttributeMapper.Attribute.VERSION));
//		addAttribute(comp1, toolkit,
//				getAttribute(MantisAttributeMapper.Attribute.VIEW_STATE));
//
//		toolkit.paintBordersFor(comp1);
//
//		Composite comp2 = toolkit.createComposite(section);
//		comp2.setLayout(new GridLayout(2, false));
//
//		addAttribute(comp2, toolkit,
//				getAttribute(MantisAttributeMapper.Attribute.REPRODUCIBILITY));
//		addAttribute(comp2, toolkit,
//				getAttribute(MantisAttributeMapper.Attribute.SEVERITY));
//		addAttribute(comp2, toolkit,
//				getAttribute(MantisAttributeMapper.Attribute.PROJECTION));
//		addAttribute(comp2, toolkit, getAttribute(MantisAttributeMapper.Attribute.ETA));
//
//		toolkit.paintBordersFor(comp2);
//
//		Composite comp3 = toolkit.createComposite(section);
//		comp3.setLayout(new GridLayout(2, false));
//
//		addAttribute(comp3, toolkit,
//				getAttribute(MantisAttributeMapper.Attribute.RESOLUTION));
//		addAttribute(comp3, toolkit,
//				getAttribute(MantisAttributeMapper.Attribute.STATUS));
//		addAttribute(comp3, toolkit,
//				getAttribute(MantisAttributeMapper.Attribute.PRIORITY));
//		addAttribute(comp3, toolkit,
//				getAttribute(MantisAttributeMapper.Attribute.FIXED_IN));
//		toolkit.paintBordersFor(comp3);
	}

	@Override
	public Control getControl() {
		// TODO Auto-generated method stub
		return super.getControl();
	}
	
	private TaskAttribute getAttribute(MantisAttributeMapper.Attribute attribute) {
		return getTaskData().getRoot().getAttribute(attribute.getKey());
	}

	private static final int COLUMN_MARGIN = 5;
	
	private void addAttribute(Composite attributesComposite,
			FormToolkit toolkit, final TaskAttribute attribute) {

		/* Allow layout to work for cached tasks that don't contain the attr */
		AbstractAttributeEditor editor = createAttributeEditor(attribute);
		if (editor == null)
			return;
		
		editor.createLabelControl(attributesComposite, toolkit);
		GridDataFactory.defaultsFor(editor.getLabelControl()).indent(COLUMN_MARGIN, 0).applyTo(
				editor.getLabelControl());
		editor.createControl(attributesComposite, toolkit);
		getTaskEditorPage().getAttributeEditorToolkit().adapt(editor);
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(editor.getControl());
	}
}
