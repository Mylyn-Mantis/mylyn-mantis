
package com.itsolut.mantis.ui.util;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.ui.PlatformUI;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.ui.MantisUIPlugin;

/**
 * @author Robert Munteanu
 * 
 */
public class MantisUIUtil {

    public static void updateRepositoryConfiguration(IRunnableContext container, TaskRepository repository, final boolean force) {

        MantisRepositoryConnector connector = (MantisRepositoryConnector) TasksUi.getRepositoryManager().getRepositoryConnector(
                MantisCorePlugin.REPOSITORY_KIND);
        final IMantisClient client;
        try {
            client = connector.getClientManager().getRepository(repository);
        } catch (MalformedURLException e) {
            MantisUIPlugin.handleMantisException(e);
            return;
        }

        try {
            IRunnableWithProgress runnable = new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

                    try {
                        client.updateAttributes(monitor, force);
                    } catch (MantisException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            };
            
            if ( container != null)
                container.run(true, true, runnable);
            else
                PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
        } catch (InvocationTargetException e) {
            MantisUIPlugin.handleMantisException(e.getCause());
            return;
        } catch (InterruptedException e) {
            return;
        }

    }

}
