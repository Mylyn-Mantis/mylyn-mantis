package com.itsolut.mantis.ui.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.mylyn.internal.provisional.commons.ui.WorkbenchUtil;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

import com.itsolut.mantis.ui.MantisUIPlugin;

/**
 * @author Robert Munteanu
 * 
 */
public class WikiLinkedErrorDialog extends ErrorDialog {

    private FormToolkit toolkit;

    public WikiLinkedErrorDialog(Shell parentShell, String dialogTitle, String message, IStatus status) {

        super(parentShell, dialogTitle, message, status, IStatus.OK | IStatus.INFO | IStatus.WARNING | IStatus.ERROR);
    }

    @Override
    protected Control createDialogArea(Composite parent) {

        Control control = super.createDialogArea(parent);
        
        toolkit = new FormToolkit(MantisUIPlugin.getDefault().getFormColors(parent.getDisplay()));

        Hyperlink link = toolkit.createHyperlink((Composite) control, "Open the troubleshooting page on the wiki", SWT.NONE);
        link.setBackground(control.getBackground());
        link.addHyperlinkListener(new HyperlinkAdapter() {

            public void linkActivated(HyperlinkEvent e) {

                WorkbenchUtil.openUrl(
                        "https://sourceforge.net/apps/mediawiki/mylyn-mantis/index.php?title=Troubleshooting",
                        IWorkbenchBrowserSupport.AS_EXTERNAL);

            }
        });
        
        GridDataFactory.fillDefaults().grab(false, false).span(2, 1).applyTo(link);

        return control;
    }
    
    @Override
    public boolean close() {

        if ( toolkit != null )
            toolkit.dispose();
        
        return super.close();
    }

}
