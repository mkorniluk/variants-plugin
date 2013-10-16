package variants.actions;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;

/**
 * Our sample action implements workbench action delegate. The action proxy will
 * be created by the workbench and shown in the UI. When the user tries to use
 * the action, this delegate will be created and execution will be delegated to
 * it.
 * 
 * @see IWorkbenchWindowActionDelegate
 */
public class PickVariant implements IWorkbenchWindowActionDelegate, IStartup {
    private IWorkbenchWindow window;
    List<MenuItem> variantItems = new ArrayList<MenuItem>();
    String currentVariant = null;

    /**
     * The constructor.
     */
    public PickVariant() {
    }

    /**
     * The action has been activated. The argument of the method represents the
     * 'real' action sitting in the workbench UI.
     * 
     * @see IWorkbenchWindowActionDelegate#run
     */
    public void run(IAction action) {
    }

    public String readCurrentVariant() {
        IProject project = getCurrentSelectedProject();
        if (project == null)
            return null;

        IFile ifile = project.getFile("variant");
        try {
            DataInputStream stream = new DataInputStream(ifile.getContents());
            String variant = stream.readUTF();
            stream.close();
            return variant;
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private void switchVariant(String variant) {
        IProject project = getCurrentSelectedProject();
        if (project == null)
            return;

        currentVariant = variant;
        IFile file = project.getFile("variant");
        try {
            file.delete(true, null);
        } catch (CoreException e1) {
            e1.printStackTrace();
        }
        InputStream source = new ByteArrayInputStream(variant.getBytes());
        try {
            file.create(source, false, null);
            source.close();
        } catch (CoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }

        clearFolders(project);

        linkVariant(project);

        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void linkVariant(final IProject project) {
        ProgressMonitorDialog dialog = new ProgressMonitorDialog(new Shell());
        try {
            dialog.run(true, false, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor arg0) throws InvocationTargetException, InterruptedException {
                    linkVariant(project, "src/");
                    linkVariant(project, "res/");
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void linkVariant(IProject project, String subfolder) {
        IFolder mainFolder = project.getFolder("variants/common/" + subfolder);
        IFolder targetFolder = project.getFolder(subfolder);
        try {
            Runtime.getRuntime().exec("mkdir " + targetFolder.getLocation().toString());
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        if (mainFolder.exists())
            try {
                IResource[] members = mainFolder.members();
                for (IResource member : members) {
                    if (member instanceof IFile) {
                        IFile fileLink = project.getFile(subfolder + member.getName());
                        Process ps = Runtime.getRuntime().exec(
                                "fsutil hardlink create " + fileLink.getLocation().toString() + " "
                                        + member.getLocation().toString());
                        DataInputStream inputStream = new DataInputStream(ps.getInputStream());
                        String output = inputStream.readLine();
                        inputStream.close();
                        // InputStream stream = ((IFile) member).getContents();
                        // fileLink.create(stream, IResource.REPLACE, null);
                    }
                    if (member instanceof IFolder) {
                        linkVariant(project, subfolder + member.getName() + "/");
                    }
                }
            } catch (CoreException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        IFolder folder = project.getFolder("variants/" + currentVariant + "/" + subfolder);
        if (folder.exists())
            try {
                IResource[] members = folder.members();
                for (IResource member : members) {
                    if (member instanceof IFile) {
                        IFile fileLink = project.getFile(subfolder + member.getName());
                        Runtime.getRuntime().exec(
                                "fsutil hardlink create " + fileLink.getLocation().toString() + " "
                                        + member.getLocation().toString());
                        // fileLink.copy(member.getLocation(),
                        // IResource.REPLACE,
                        // null);
                    }
                    if (member instanceof IFolder) {
                        linkVariant(project, subfolder + member.getName() + "/");
                    }
                }
            } catch (CoreException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

    private void clearFolders(IProject project) {
        IFolder srcFolder = project.getFolder("src");
        try {
            IResource[] members = srcFolder.members();
            for (IResource member : members) {
                member.delete(false, null);
            }
        } catch (CoreException e1) {
            e1.printStackTrace();
        }

        IFolder resFolder = project.getFolder("res");
        try {
            IResource[] members = resFolder.members();
            for (IResource member : members) {
                member.delete(false, null);
            }
        } catch (CoreException e1) {
            e1.printStackTrace();
        }
    }

    public static IProject getCurrentSelectedProject() {
        IProject project = null;
        ISelectionService selectionService = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();

        ISelection selection = selectionService.getSelection();

        if (selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection) selection).getFirstElement();

            if (element instanceof IResource) {
                project = ((IResource) element).getProject();
            } else if (element instanceof PackageFragmentRoot) {
                IJavaProject jProject = ((PackageFragmentRoot) element).getJavaProject();
                project = jProject.getProject();
            } else if (element instanceof IJavaElement) {
                IJavaProject jProject = ((IJavaElement) element).getJavaProject();
                project = jProject.getProject();
            }
        }
        return project;
    }

    public void updateVariantsMenu(List<String> folders) {
        MenuManager menuManager = ((WorkbenchWindow) window).getMenuManager();

        Menu menu = menuManager.getMenu();
        MenuItem[] items = menu.getItems();
        for (MenuItem item : items) {
            if (item.getText().equals("Variants")) {
                Menu variantsMenu = item.getMenu();
                for (MenuItem itemToRemove : variantItems) {
                    itemToRemove.dispose();
                }
                variantItems.clear();
                for (final String variant : folders) {
                    final MenuItem variantItem = new MenuItem(variantsMenu, SWT.CHECK);
                    variantItems.add(variantItem);
                    variantItem.setText(variant);
                    if (variant.equals(currentVariant))
                        variantItem.setSelection(true);
                    variantItem.addSelectionListener(new SelectionListener() {

                        @Override
                        public void widgetSelected(SelectionEvent arg0) {
                            switchVariant(variant);
                            for (MenuItem itemToUncheck : variantItems) {
                                if (itemToUncheck != variantItem)
                                    itemToUncheck.setSelection(false);
                            }
                            variantItem.setSelection(true);
                            switchVariant(variant);
                        }

                        @Override
                        public void widgetDefaultSelected(SelectionEvent arg0) {
                        }
                    });
                }
                menuManager.update();
                break;
            }
        }
    }

    /**
     * Selection in the workbench has been changed. We can change the state of
     * the 'real' action here if we want, but this can only happen after the
     * delegate has been created.
     * 
     * @see IWorkbenchWindowActionDelegate#selectionChanged
     */
    public void selectionChanged(IAction action, ISelection selection) {
        updateVariants();
    }

    private void updateVariants() {
        IProject project = getCurrentSelectedProject();
        if (project == null)
            return;
        if (currentVariant == null)
            currentVariant = readCurrentVariant();
        List<String> folders = new ArrayList<String>();
        try {
            IFolder folder = project.getFolder("variants");
            IResource[] members = folder.members();
            for (int i = 0; i < members.length; i++) {
                if (members[i] instanceof IFolder) {
                    String name = ((IFolder) members[i]).getName();
                    if (!name.equals("common"))
                        folders.add(name);
                }
            }
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        updateVariantsMenu(folders);
    }

    /**
     * We can use this method to dispose of any system resources we previously
     * allocated.
     * 
     * @see IWorkbenchWindowActionDelegate#dispose
     */
    public void dispose() {
    }

    /**
     * We will cache window object in order to be able to provide parent shell
     * for the message dialog.
     * 
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow window) {
        this.window = window;
        currentVariant = readCurrentVariant();
        updateVariants();
    }

    @Override
    public void earlyStartup() {
    }
}