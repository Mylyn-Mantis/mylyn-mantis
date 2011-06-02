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
import org.eclipse.mylyn.htmltext.configuration.Configuration;
import org.eclipse.mylyn.htmltext.configuration.EnterModeConfiguration;
import org.eclipse.mylyn.htmltext.configuration.EnterModeConfiguration.EnterMode;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AttributeEditorFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.services.IServiceLocator;

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
                
                Configuration configuration = new Configuration();
                configuration.addConfigurationNode(new EnterModeConfiguration(EnterMode.BR));

                composer = new HtmlComposer(parent, SWT.None, configuration);

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

                        if ( !newValue.equals(oldValue) )
                            attributeChanged();
                    }
                });

                control = composer.getBrowser();
            }
            
            setControl(control);
        }
        
        @Override
        protected void decorateIncoming(Color color) {
        
            if ( composer != null )
                composer.setBackground(color);
            else
                super.decorateIncoming(color);
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