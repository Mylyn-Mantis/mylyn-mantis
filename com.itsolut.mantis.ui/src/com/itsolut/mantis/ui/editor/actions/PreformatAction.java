package com.itsolut.mantis.ui.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.mylyn.htmltext.HtmlComposer;
import org.eclipse.mylyn.htmltext.commands.Command;
import org.eclipse.mylyn.htmltext.commands.formatting.PreformatCommand;

import com.itsolut.mantis.ui.internal.MantisImages;

public class PreformatAction extends AbstractCommandWrapper {

    public PreformatAction(HtmlComposer composer) {

        super("Preformatted", IAction.AS_CHECK_BOX, composer); //$NON-NLS-1$
        setImageDescriptor(MantisImages.PRE);
    }

    @Override
    protected Command getWrappedCommand() {

        return new PreformatCommand();
    }

}
