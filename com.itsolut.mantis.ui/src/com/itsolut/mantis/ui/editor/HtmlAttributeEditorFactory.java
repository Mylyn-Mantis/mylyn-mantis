/*******************************************************************************
 * Copyright (C) 2011 Robert Munteanu <robert.munteanu@gmail.com>
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itsolut.mantis.ui.editor;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.mylyn.htmltext.HtmlComposer;
import org.eclipse.mylyn.htmltext.commands.GetHtmlCommand;
import org.eclipse.mylyn.htmltext.commands.SetHtmlCommand;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AttributeEditorFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.services.IServiceLocator;

import com.itsolut.mantis.core.MantisAttributeMapper;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.util.HtmlFormatter;
import com.itsolut.mantis.ui.editor.actions.BoldAction;
import com.itsolut.mantis.ui.editor.actions.BulletlistAction;
import com.itsolut.mantis.ui.editor.actions.ItalicAction;
import com.itsolut.mantis.ui.editor.actions.NumlistAction;
import com.itsolut.mantis.ui.editor.actions.PreformatAction;
import com.itsolut.mantis.ui.editor.actions.UnderlineAction;

/**
 * @author Robert Munteanu
 */
public class HtmlAttributeEditorFactory extends AttributeEditorFactory {
    
    public static final class HtmlAttributeEditor extends AbstractAttributeEditor {
        

        private HtmlComposer composer;

        public HtmlAttributeEditor(TaskDataModel manager, TaskAttribute taskAttribute) {

            super(manager, taskAttribute);
            setDecorationEnabled(true);
        }

        @Override
        public void createControl(final Composite parent, FormToolkit toolkit) {
            
            Control control;
            String value = HtmlFormatter.convertToDisplayHtml(getTaskAttribute().getValue());

            if (isReadOnly()) {
                
                Browser browser = new Browser(parent, SWT.None);
                GridDataFactory.fillDefaults().applyTo(browser);
                browser.setText(value);
                
                control = browser;

            } else {
                
                CoolBar coolbar = new CoolBar(parent, SWT.NONE);
                GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
                coolbar.setLayoutData(gd);

                ToolBar menu = new ToolBar(coolbar, SWT.HORIZONTAL | SWT.FLAT);
                ToolBarManager manager = new ToolBarManager(menu);
                CoolItem item = new CoolItem(coolbar, SWT.NONE);
                item.setControl(menu);

                composer = new HtmlComposer(parent, SWT.None);

                manager.add(new BoldAction(composer));
                manager.add(new ItalicAction(composer));
                manager.add(new PreformatAction(composer));
                manager.add(new UnderlineAction(composer));
                manager.add(new Separator());
                manager.add(new BulletlistAction(composer));
                manager.add(new NumlistAction(composer));

                manager.update(true);

                composer.setHtml(value);
                GridDataFactory.fillDefaults().applyTo(composer.getBrowser());

                composer.addModifyListener(new ModifyListener() {

                    public void modifyText(ModifyEvent e) {

                        String oldValue = getAttributeMapper().getValue(getTaskAttribute());

                        String newValue = HtmlFormatter.convertFromDisplayHtml(composer.getHtml());

                        getAttributeMapper().setValue(getTaskAttribute(), newValue);

                        boolean attributeChanged = !newValue.equals(oldValue);

                        MantisCorePlugin.debug(NLS.bind("Attribute {0} changed from {1} to {2}. Change detected : {3}.", new Object[] { getTaskAttribute().getId(), oldValue, newValue, attributeChanged }), new RuntimeException());

                        // HtmlText 0.7.0 does not properly fire change events
                        // 340938: Spurious change events fired by the HtmlComposer
                        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=340938
                        if (attributeChanged)
                            attributeChanged();
                    }
                });

                control = composer.getBrowser();
            }
            
            setControl(control);
        }
        
        public void appendRawText(String rawText) {
            
            if ( composer == null )
                return;
            
            String value = (String) composer.executeWithReturn(new GetHtmlCommand());
            String newValue = value + rawText;
            SetHtmlCommand command = new SetHtmlCommand();
            command.setHtml(newValue);
            composer.execute(command);
        }
    }

    private final boolean _useRichTextEditor;
    private final TaskDataModel _model;

    public HtmlAttributeEditorFactory(TaskDataModel model, TaskRepository taskRepository,
            IServiceLocator serviceLocator, boolean useRichTextEditor) {

        super(model, taskRepository, serviceLocator);
        
        _model = model;
        _useRichTextEditor = useRichTextEditor;
    }

    @Override
    public AbstractAttributeEditor createEditor(String type, final TaskAttribute taskAttribute) {
        
        if ( _useRichTextEditor && TaskAttribute.TYPE_LONG_RICH_TEXT.equals(type) ) 
            return new HtmlAttributeEditor(_model, taskAttribute);
        
        return super.createEditor(type, taskAttribute);
    }
}