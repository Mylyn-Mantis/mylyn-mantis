package com.itsolut.mantis.ui.editor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.editors.EditorUtil;
import org.eclipse.mylyn.internal.tasks.ui.editors.PreviewAttributeEditor;
import org.eclipse.mylyn.internal.tasks.ui.editors.RichTextAttributeEditor;
import org.eclipse.mylyn.internal.tasks.ui.editors.TaskEditorRichTextPart;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractRenderingEngine;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.internal.EditorAreaHelper;
import org.eclipse.ui.internal.WorkbenchPage;

import com.itsolut.mantis.core.MantisAttributeMapper;
import com.itsolut.mantis.ui.MantisUIPlugin;

public class MantisStepsToReproducePart extends TaskEditorRichTextPart {
	protected static final String LABEL_SECTION_STEPS = "Steps To Reproduce";
	private RichTextAttributeEditor editor;

	private TaskAttribute attribute;	

	private int sectionStyle;
	
	private Composite composite;


	public MantisStepsToReproducePart() {
		super();
		setPartName(LABEL_SECTION_STEPS);
	}
	
	@Override
	public void createControl(Composite parent, FormToolkit toolkit) {
		attribute = getTaskData().getRoot().getAttribute(MantisAttributeMapper.Attribute.STEPS_TO_REPRODUCE.getKey());
		AbstractAttributeEditor attributEditor = createAttributeEditor(attribute);
		if (!(attributEditor instanceof RichTextAttributeEditor)) {
			String clazz;
			if (attributEditor != null) {
				clazz = attributEditor.getClass().getName();
			} else {
				clazz = "<null>";
			}
			StatusHandler.log(new Status(IStatus.WARNING, MantisUIPlugin.PLUGIN_ID,
					"Expected an instance of RichTextAttributeEditor, got \"" + clazz + "\""));
			return;
		}

		Section section = createSection(parent, toolkit, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR);

		composite = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);

		editor = (RichTextAttributeEditor) attributEditor;

		AbstractRenderingEngine renderingEngine = getTaskEditorPage().getAttributeEditorToolkit().getRenderingEngine(
				attribute);
		if (renderingEngine != null) {
			PreviewAttributeEditor previewEditor = new PreviewAttributeEditor(getModel(), attribute,
					getTaskEditorPage().getTaskRepository(), renderingEngine, editor);
			previewEditor.createControl(composite, toolkit);
			GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(
					previewEditor.getControl());
		} else {
			editor.createControl(composite, toolkit);
			if (editor.isReadOnly()) {
				GridDataFactory.fillDefaults().minSize(EditorUtil.MAXIMUM_WIDTH, 0).hint(EditorUtil.MAXIMUM_WIDTH,
						SWT.DEFAULT).applyTo(editor.getControl());
			} else {
				final GridData gd = new GridData();
				// wrap text at this margin, see comment below
				int width = getEditorWidth();
				// the goal is to make the text viewer as big as the text so it does not require scrolling when first drawn 
				// on screen
				Point size = editor.getViewer().getTextWidget().computeSize(width, SWT.DEFAULT, true);
				gd.widthHint = EditorUtil.MAXIMUM_WIDTH;
				gd.minimumWidth = EditorUtil.MAXIMUM_WIDTH;
				gd.horizontalAlignment = SWT.FILL;
				gd.grabExcessHorizontalSpace = true;
				// limit height to be avoid dynamic resizing of the text widget: 
				// MAXIMUM_HEIGHT < height < MAXIMUM_HEIGHT * 3
				//gd.minimumHeight = AbstractAttributeEditor.MAXIMUM_HEIGHT;
				gd.heightHint = Math.min(Math.max(EditorUtil.MAXIMUM_HEIGHT, size.y), EditorUtil.MAXIMUM_HEIGHT * 3);
				if (getExpandVertically()) {
					gd.verticalAlignment = SWT.FILL;
					gd.grabExcessVerticalSpace = true;
				}
				editor.getControl().setLayoutData(gd);
				editor.getControl().setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
			}
		}

		getTaskEditorPage().getAttributeEditorToolkit().adapt(editor);

		toolkit.paintBordersFor(composite);
		section.setClient(composite);
		setSection(toolkit, section);
	}
	
	private int getEditorWidth() {
		int widthHint = 0;
		if (getManagedForm() != null && getManagedForm().getForm() != null) {
			widthHint = getManagedForm().getForm().getClientArea().width - 90;
		}
		if (widthHint <= 0 && getTaskEditorPage().getEditor().getEditorSite() != null
				&& getTaskEditorPage().getEditor().getEditorSite().getPage() != null) {
			EditorAreaHelper editorManager = ((WorkbenchPage) getTaskEditorPage().getEditor().getEditorSite().getPage()).getEditorPresentation();
			if (editorManager != null && editorManager.getLayoutPart() != null) {
				widthHint = editorManager.getLayoutPart().getControl().getBounds().width - 90;
			}
		}
		if (widthHint <= 0) {
			widthHint = EditorUtil.MAXIMUM_WIDTH;
		}
		return widthHint;
	}
	

}
