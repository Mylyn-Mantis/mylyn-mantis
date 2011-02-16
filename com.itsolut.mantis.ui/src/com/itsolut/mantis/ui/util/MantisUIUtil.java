
package com.itsolut.mantis.ui.util;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.mylyn.internal.provisional.commons.ui.EnhancedFilteredTree;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;

import com.itsolut.mantis.core.IMantisClient;
import com.itsolut.mantis.core.MantisCorePlugin;
import com.itsolut.mantis.core.MantisRepositoryConnector;
import com.itsolut.mantis.core.exception.MantisException;
import com.itsolut.mantis.ui.MantisUIPlugin;

/**
 * @author Robert Munteanu
 * 
 */
@SuppressWarnings("restriction")
public class MantisUIUtil {

    public static void updateRepositoryConfiguration(IRunnableContext container, TaskRepository repository) {

        MantisRepositoryConnector connector = (MantisRepositoryConnector) TasksUi.getRepositoryManager().getRepositoryConnector(
                MantisCorePlugin.REPOSITORY_KIND);
        final IMantisClient client;
        try {
            client = connector.getClientManager().getRepository(repository);
        } catch (MantisException e) {
            MantisUIPlugin.handleError(e, "Failed getting the repository.", true);
            return;
        }

        try {
            IRunnableWithProgress runnable = new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

                    try {
                        client.updateAttributes(monitor);
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
            MantisUIPlugin.handleError(e, "Failed updating the repository attributes.",  true);
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

    }
    
    /**
     * Creates a new {@link EnhancedFilteredTree} with the default look and feel settings for this connector
     * 
     * @param control the parent of the tree
     * @return the tree instance
     */
    public static EnhancedFilteredTree newEnhancedFilteredTree(Composite control) {
        
        EnhancedFilteredTree tree = new EnhancedFilteredTree(control, SWT.SINGLE | SWT.BORDER, new PatternFilter());
        
        GridDataFactory.defaultsFor(tree).align(SWT.FILL, SWT.FILL).grab(true, true).hint( SWT.DEFAULT, 130).applyTo(tree);
        
        return tree;
    }

}
