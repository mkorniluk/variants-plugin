package variants.actions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

import variants.actions.VariantsProperties.Variant;

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
    VariantsProperties properties;
    Variant currentVariant;

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

    /*
     * public Variant readCurrentVariant() { IProject project =
     * getCurrentSelectedProject(); if (project == null) return null;
     * 
     * IFile ifile = project.getFile("variant"); try { DataInputStream stream =
     * new DataInputStream(ifile.getContents()); String variantName =
     * stream.readUTF(); stream.close(); for (Variant variant :
     * properties.variants) { if (variant.name.equals(variantName)) return
     * variant; } return null; } catch (CoreException e) { } catch (IOException
     * e) { } return null; }
     */

    private void switchVariant(Variant variant) {
        IProject project = getCurrentSelectedProject();
        if (project == null)
            return;

        currentVariant = variant;
        /*
         * IFile file = project.getFile("variant"); try { file.delete(true,
         * null); } catch (CoreException e1) { e1.printStackTrace(); }
         * InputStream source = new
         * ByteArrayInputStream(variant.name.getBytes()); try {
         * file.create(source, false, null); source.close(); } catch
         * (CoreException e) { e.printStackTrace(); } catch (IOException e) {
         * e.printStackTrace(); }
         */
        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }

        try {
            clearFolders(project);
        } catch (CoreException e1) {
            e1.printStackTrace();
        }

        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }

        linkVariant(project);

        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    private void linkVariant(final IProject project) {
        ProgressMonitorDialog dialog = new ProgressMonitorDialog(new Shell());
        try {
            dialog.run(true, false, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor arg0) throws InvocationTargetException, InterruptedException {
                    for (String s : currentVariant.srcFolders) {
                        linkVariant(project, "src", s);
                    }
                    for (String s : currentVariant.resFolders) {
                        linkVariant(project, "res", s);
                    }
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void linkVariant(IProject project, String destination, String subfolder) {
        IFolder targetFolder = project.getFolder(destination);
        try {
            Runtime.getRuntime().exec("mkdir " + targetFolder.getLocation().toString());
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        IFolder folder = project.getFolder("variants/" + subfolder);
        try {
            IResource[] members = folder.members();
            for (IResource member : members) {
                if (member instanceof IFile) {
                    IFile fileLink = project.getFile(destination + "/" + member.getName());
                    if (fileLink.exists()) {
                        System.out.println("file " + fileLink.getLocation().toString() + " exists");
                    } else {
                        Process exec;
                        if (System.getProperty("os.name").startsWith("Windows")) {
                            exec = Runtime.getRuntime().exec(
                                    "fsutil hardlink create \"" + fileLink.getLocation().toString() + "\" \""
                                            + member.getLocation().toString() + "\"");
                        } else {
                            exec = Runtime.getRuntime().exec(
                                    "ln \"" + member.getLocation().toString() + "\" \""
                                            + fileLink.getLocation().toString() + "\"");
                        }
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(exec.getInputStream()));
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            System.out.println(line);
                        }
                        BufferedReader bufferedReader2 = new BufferedReader(
                                new InputStreamReader(exec.getErrorStream()));
                        String line2;
                        while ((line2 = bufferedReader2.readLine()) != null) {
                            System.out.println(line2);
                        }
                    }
                    // fileLink.copy(member.getLocation(),
                    // IResource.REPLACE,
                    // null);
                }
                if (member instanceof IFolder) {
                    linkVariant(project, destination + "/" + member.getName(), subfolder + "/" + member.getName());
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearFolders(IProject project) throws CoreException {
        IFolder srcFolder = project.getFolder("src");
        if (srcFolder.exists()) {
            IResource[] members = srcFolder.members();
            for (IResource member : members) {
                member.delete(false, null);
            }
        }

        IFolder resFolder = project.getFolder("res");
        if (resFolder.exists()) {
            IResource[] members = resFolder.members();
            for (IResource member : members) {
                member.delete(false, null);
            }
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

    public void updateVariantsMenu() {
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
                for (final Variant variant : properties.variants) {
                    final MenuItem variantItem = new MenuItem(variantsMenu, SWT.CHECK);
                    variantItems.add(variantItem);
                    variantItem.setText(variant.name);
                    if (variant.equals(currentVariant))
                        variantItem.setSelection(true);
                    variantItem.addSelectionListener(new SelectionListener() {

                        @Override
                        public void widgetSelected(SelectionEvent arg0) {
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

        IFile file = project.getFile("variants.properties");
        if (file.exists()) {
            properties = new VariantsProperties();
            properties.read(file);
        }

        /*
         * if (currentVariant == null) currentVariant = readCurrentVariant();
         */

        /*
         * } else { try { IFolder folder = project.getFolder("variants");
         * IResource[] members = folder.members(); for (int i = 0; i <
         * members.length; i++) { if (members[i] instanceof IFolder) { String
         * name = ((IFolder) members[i]).getName(); if (!name.equals("common"))
         * folders.add(name); } } } catch (CoreException e) {
         * e.printStackTrace(); }
         */

        updateVariantsMenu();
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
        updateVariants();
    }

    @Override
    public void earlyStartup() {
    }
}