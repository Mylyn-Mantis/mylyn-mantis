/*******************************************************************************
 * Copyright (c) 2004 - 2006 Mylar committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2007 - 2007 IT Solutions, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Hane - adapted Trac implementation for Mantis
 *******************************************************************************/

package com.itsolut.mantis.ui.editor;

import java.util.Set;

import org.eclipse.jface.text.TextViewer;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorPartDescriptor;

/**
 * @author Steffen Pingel
 * @author Chris Hane
 */
public class MantisTaskEditor extends AbstractTaskEditorPage {
	
	protected static final String LABEL_SECTION_STEPS = "Steps To Reproduce";
	protected static final int STEPS_TO_REPRODUCE_HEIGHT = 150;
	protected TextViewer stepsToReproduceViewer;

	protected static final String LABEL_SECTION_ADDITIONAL = "Additional Information";
	protected static final int ADDITIONAL_INFO_HEIGHT = 150;
	protected TextViewer additionalViewer;
	
	protected static final int DESCRIPTION_HEIGHT = 150;
	private static final int DESCRIPTION_WIDTH = 79 * 7; // 500;
	
	private TaskData taskData;
	
	public MantisTaskEditor(TaskEditor editor, String connectorKind) {
		super(editor, connectorKind);
	}
	
	@Override
	protected Set<TaskEditorPartDescriptor> createPartDescriptors() {
		Set<TaskEditorPartDescriptor> descriptors = super.createPartDescriptors();

		// remove unnecessary default editor parts
		for (TaskEditorPartDescriptor taskEditorPartDescriptor : descriptors) {
			if (taskEditorPartDescriptor.getId().equals(ID_PART_PEOPLE)) {
				descriptors.remove(taskEditorPartDescriptor);
				break;
			}
		}

		return descriptors;
	}
	
	
	
// 	@Override
//	protected void createAttributeLayout(Composite attributesComposite) {
//		
//		FormToolkit toolkit = getManagedForm().getToolkit();
//		
//		Composite comp1 = toolkit.createComposite(attributesComposite);
//		comp1.setLayout(new GridLayout(2,false));
//		
//		createTextAttribute(comp1, toolkit, getAttribute(MantisAttribute.PROJECT), true);
//		createComboAttribute(comp1, toolkit, getAttribute(MantisAttribute.CATEGORY) );
//		createComboAttribute(comp1, toolkit, getAttribute(MantisAttribute.VERSION));
//		createComboAttribute(comp1, toolkit, getAttribute(MantisAttribute.VIEW_STATE));
//		
//		toolkit.paintBordersFor(comp1);
//		
//		Composite comp2 = toolkit.createComposite(attributesComposite);
//		comp2.setLayout(new GridLayout(2,false));
//		
//		createComboAttribute(comp2, toolkit, getAttribute(MantisAttribute.REPRODUCIBILITY));
//		createComboAttribute(comp2, toolkit, getAttribute(MantisAttribute.SEVERITY));
//		createComboAttribute(comp2, toolkit, getAttribute(MantisAttribute.PROJECTION));
//		createComboAttribute(comp2, toolkit, getAttribute(MantisAttribute.ETA));
//		
//		toolkit.paintBordersFor(comp2);
//		
//		Composite comp3 = toolkit.createComposite(attributesComposite);
//		comp3.setLayout(new GridLayout(2,false));
//
//		createComboAttribute(comp3, toolkit, getAttribute(MantisAttribute.RESOLUTION) );
//		createComboAttribute(comp3, toolkit, getAttribute(MantisAttribute.STATUS) );
//		createComboAttribute(comp3, toolkit, getAttribute(MantisAttribute.PRIORITY) );
//		createComboAttribute(comp3, toolkit, getAttribute(MantisAttribute.FIXED_IN) );
//		
//		toolkit.paintBordersFor(comp3);
//
//		toolkit.paintBordersFor(attributesComposite);
//	}

//	private TaskAttribute getAttribute(MantisAttributeMapper.Attribute attribute) {
//		return taskData.getAttribute(attribute.getMantisKey());
//	}
//
//	private void createTextAttribute(Composite attributesComposite,
//			FormToolkit toolkit, final RepositoryTaskAttribute attribute, boolean readonly) {
//		
//		/* Allow layout to work for cached tasks that don't contain the attr */
//		if( attribute == null )
//			return;
//		
//		Label label = createLabel(attributesComposite, attribute);
//		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(label);
//		Composite textFieldComposite = toolkit.createComposite(attributesComposite);
//		GridLayout textLayout = new GridLayout();
//		textLayout.marginWidth = 1;
//		textLayout.marginHeight = 2;
//		textFieldComposite.setLayout(textLayout);
//		GridData textData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
//		textData.horizontalSpan = 1;
//		textData.widthHint = 135;
//
//		if (attribute.isReadOnly() || readonly ) {
//			final Text text = createTextField(textFieldComposite, attribute, SWT.FLAT | SWT.READ_ONLY);
//			text.setLayoutData(textData);
//		} else {
//			final Text text = createTextField(textFieldComposite, attribute, SWT.FLAT);
//			// text.setFont(COMMENT_FONT);
//			text.setLayoutData(textData);
//			toolkit.paintBordersFor(textFieldComposite);
//			text.setData(attribute);
//
//			if (hasContentAssist(attribute)) {
//				ContentAssistCommandAdapter adapter = applyContentAssist(text,
//						createContentProposalProvider(attribute));
//
//				ILabelProvider propsalLabelProvider = createProposalLabelProvider(attribute);
//				if (propsalLabelProvider != null) {
//					adapter.setLabelProvider(propsalLabelProvider);
//				}
//				adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
//			}
//		}
//	}
//
//	private void createComboAttribute(Composite attributesComposite,
//			FormToolkit toolkit, final RepositoryTaskAttribute attribute) {
//		
//		/* Allow layout to work for cached tasks that don't contain the attr */
//		if( attribute == null )
//			return;
//		
//		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
//		data.horizontalSpan = 1;
//
//		
//		Label label = createLabel(attributesComposite, attribute);
//		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(label);
//		final CCombo attributeCombo = new CCombo(attributesComposite, SWT.FLAT | SWT.READ_ONLY);
//		toolkit.adapt(attributeCombo, true, true);
//		attributeCombo.setFont(TEXT_FONT);
//		attributeCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
//		if (hasChanged(attribute)) {
//			attributeCombo.setBackground(getColorIncoming());
//		}
//		attributeCombo.setLayoutData(data);
//
//		java.util.List<String> values = attribute.getOptions();
//		if (values != null) {
//			for (String val : values) {
//				if (val != null) {
//					attributeCombo.add(val);
//				}
//			}
//		}
//
//		String value = attribute.getValue();
//		if (value == null) {
//			value = "";
//		}
//		if (attributeCombo.indexOf(value) != -1) {
//			attributeCombo.select(attributeCombo.indexOf(value));
//		}
//		attributeCombo.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent event) {
//				if (attributeCombo.getSelectionIndex() > -1) {
//					String sel = attributeCombo.getItem(attributeCombo.getSelectionIndex());
//					attribute.setValue(sel);
//					attributeChanged(attribute);
//				}
//			}
//		});
//	}
// 	
//	@Override
//	protected void createDescriptionLayout(Composite composite) {
//		super.createDescriptionLayout(composite);
//		
//		// make description textbox bigger if it's an input field
//		if(!taskData.getDescriptionAttribute().isReadOnly()) {
//			GridData descriptionTextData = new GridData(GridData.FILL_BOTH);
//			descriptionTextData.minimumHeight = DESCRIPTION_HEIGHT;
//			descriptionTextData.widthHint = DESCRIPTION_WIDTH;
//			
//			descriptionTextViewer.getControl().setLayoutData(descriptionTextData);
//		}
//	}	
//
//	
//	@Override
//	protected void createAttachmentLayout(Composite composite) {
//		FormToolkit toolkit = this.getManagedForm().getToolkit();
//		Section section = createSection(composite, LABEL_SECTION_STEPS);
//		section.setExpanded(false);
//
//		Composite stepsToReproduceComposite = toolkit.createComposite(section);
//		GridLayout descriptionLayout = new GridLayout();
//
//		stepsToReproduceComposite.setLayout(descriptionLayout);
//		GridData stepsToReproduceData = new GridData(GridData.FILL_BOTH);
//		stepsToReproduceData.grabExcessVerticalSpace = true;
//		stepsToReproduceComposite.setLayoutData(stepsToReproduceData);
//		section.setClient(stepsToReproduceComposite);
//		
//		stepsToReproduceViewer = addTextEditor(repository, stepsToReproduceComposite,
//				taskData.getAttributeValue(MantisAttribute.STEPS_TO_REPRODUCE.getMantisKey()), true, SWT.FLAT | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
//		stepsToReproduceViewer.setEditable(true);
//
//		GridData descriptionTextData = new GridData(GridData.FILL_BOTH);
//		descriptionTextData.minimumHeight = STEPS_TO_REPRODUCE_HEIGHT;
//		descriptionTextData.widthHint = DESCRIPTION_WIDTH;
//		stepsToReproduceViewer.getControl().setLayoutData(descriptionTextData);
//		stepsToReproduceViewer.getControl().setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
//		stepsToReproduceViewer.getTextWidget().addModifyListener(new ModifyListener() {
//			public void modifyText(ModifyEvent e) {
//				String changed = stepsToReproduceViewer.getTextWidget().getText();
//				String original = taskData.getAttributeValue(MantisAttribute.STEPS_TO_REPRODUCE.getMantisKey());
//				if (original==null || !(original.equals(changed))) {
//					taskData.getAttribute(MantisAttribute.STEPS_TO_REPRODUCE.getMantisKey()).setValue(changed);
//					markDirty(true);
//				}
//				validateInput();
//			}
//		});
//
//		toolkit.paintBordersFor(stepsToReproduceComposite);
//		
//		////////////////
//		Section sectionAddtional = createSection(composite, LABEL_SECTION_ADDITIONAL);
//		sectionAddtional.setExpanded(false);
//
//		Composite additionalComposite = toolkit.createComposite(sectionAddtional);
//		GridLayout additionalLayout = new GridLayout();
//
//		additionalComposite.setLayout(additionalLayout);
//		GridData additionalData = new GridData(GridData.FILL_BOTH);
//		additionalData.grabExcessVerticalSpace = true;
//		additionalComposite.setLayoutData(additionalData);
//		sectionAddtional.setClient(additionalComposite);
//
//		additionalViewer = addTextEditor(repository, additionalComposite,
//				taskData.getAttributeValue(MantisAttribute.ADDITIONAL_INFO.getMantisKey()), true, SWT.FLAT | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
//		additionalViewer.setEditable(true);
//		
//		GridData additionalTextData = new GridData(GridData.FILL_BOTH);
//		additionalTextData.minimumHeight = ADDITIONAL_INFO_HEIGHT;
//		descriptionTextData.widthHint = DESCRIPTION_WIDTH;
//
//		additionalViewer.getControl().setLayoutData(additionalTextData);
//		additionalViewer.getControl().setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
//		additionalViewer.getTextWidget().addModifyListener(new ModifyListener() {
//			public void modifyText(ModifyEvent e) {
//				String changed = additionalViewer.getTextWidget().getText();
//				String original = taskData.getAttributeValue(MantisAttribute.ADDITIONAL_INFO.getMantisKey());
//				if (original==null || !(original.equals(changed))) {
//					taskData.getAttribute(MantisAttribute.ADDITIONAL_INFO.getMantisKey()).setValue(changed);
//					markDirty(true);
//				}
//				validateInput();
//			}
//		});
//
//		toolkit.paintBordersFor(additionalComposite);
//		///////////////////
//		
//		super.createAttachmentLayout(composite);
//	}

//	@Override
//	protected void addCCList(Composite attributesComposite) {
//		// suppress this function to hide the CC list
//		//super.addCCList(attributesComposite);
//	}
//	
//	@Override
//	protected void addSelfToCC(Composite composite) {
//		// suppress this function to hide the self to CC checkbox
//		// TODO: Implement self to CC as "monitor this bug" (not sure if Mantisconnect supports this yet)
//		//super.addSelfToCC(composite);
//	}
}
