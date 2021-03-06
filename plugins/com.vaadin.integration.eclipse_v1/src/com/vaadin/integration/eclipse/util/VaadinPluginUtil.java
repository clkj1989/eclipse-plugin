package com.vaadin.integration.eclipse.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.osgi.framework.Bundle;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.DownloadUtils.Version;
import com.vaadin.integration.eclipse.variables.VaadinClasspathVariableInitializer;

public class VaadinPluginUtil {

    /**
     * Handle an exception in a background thread or other non-UI context. The
     * handling primarily consists of tracing the exception.
     *
     * @param ex
     */
    public static void handleBackgroundException(Exception ex) {
        handleBackgroundException(ex.getMessage(), ex);
    }

    /**
     * Handle an exception in a background thread or other non-UI context. The
     * handling primarily consists of tracing the exception.
     *
     * @param message
     * @param ex
     */
    public static void handleBackgroundException(String message, Exception ex) {
        handleBackgroundException(IStatus.ERROR, message, ex);
    }

    /**
     * Handle an exception in a background thread or other non-UI context. The
     * handling primarily consists of tracing the exception.
     *
     * @param severity
     *            IStatus.OK, IStatus.INFO, IStatus.WARNING or IStatus.ERROR
     * @param message
     * @param ex
     */
    public static void handleBackgroundException(int severity, String message,
            Exception ex) {
        // TODO trace the exception and do any other background exception
        // handling
        IStatus status = new Status(severity, VaadinPlugin.PLUGIN_ID,
                message, ex);
        VaadinPlugin.getInstance().getLog().log(status);
        // ex.printStackTrace();
    }

    /**
     * Display an error message to the user.
     *
     * @param message
     * @param ex
     */
    public static void displayError(String message, Throwable ex, Shell shell) {
        // TODO trace if needed and report to the user
        MessageDialog.openError(shell, "Error", message);
    }

    /**
     * Find a project that has the Vaadin project facet based on a selection.
     *
     * If the selection is an element in a suitable project, return that
     * project.
     *
     * Otherwise, return null.
     *
     * @param selection
     * @return an Vaadin project
     */
    public static IProject getProject(ISelection selection) {
        IProject project = null;
        if (selection != null && selection.isEmpty() == false
                && selection instanceof IStructuredSelection) {
            IStructuredSelection ssel = (IStructuredSelection) selection;
            if (ssel.size() == 1) {
                Object obj = ssel.getFirstElement();
                if (ssel instanceof TreeSelection) {
                    TreeSelection ts = (TreeSelection) ssel;
                    obj = ts.getPaths()[0].getFirstSegment();
                } else {
                    obj = ssel.getFirstElement();
                }
                if (obj instanceof IResource) {
                    IContainer container;
                    if (obj instanceof IContainer) {
                        container = (IContainer) obj;
                    } else {
                        container = ((IResource) obj).getParent();
                    }
                    if (container != null
                            && VaadinFacetUtils.isVaadinProject(container
                                    .getProject())) {
                        project = container.getProject();
                    }
                }
            }
        }
        return project;
    }

    public static IFolder getWebInfLibFolder(IProject project) {
        IVirtualComponent component = ComponentCore.createComponent(project);
        IVirtualFolder contentFolder = component.getRootFolder();
        return (IFolder) contentFolder.getFolder(WebArtifactEdit.WEBLIB)
                .getUnderlyingFolder();
    }

    public static IFolder getWebInfFolder(IProject project) {
        IVirtualComponent component = ComponentCore.createComponent(project);
        IVirtualFolder contentFolder = component.getRootFolder();
        return (IFolder) contentFolder.getFolder(WebArtifactEdit.WEB_INF)
                .getUnderlyingFolder();
    }

    public static IFolder getWebContentFolder(IProject project) {
        IVirtualComponent component = ComponentCore.createComponent(project);
        IVirtualFolder contentFolder = component.getRootFolder();
        return (IFolder) contentFolder.getUnderlyingFolder();
    }

    public static IFolder getSrcFolder(IProject project) throws CoreException {
        try {
            IJavaProject javaProject = JavaCore.create(project);
            for (IClasspathEntry classPathEntry : javaProject.getRawClasspath()) {
                if (classPathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IPath path = classPathEntry.getPath();
                    return project.getWorkspace().getRoot().getFolder(path);
                    // return project.getFolder(path);
                }
            }

            return null;
        } catch (JavaModelException e) {
            throw newCoreException("Unable to locate source folder", e);
        }
    }

    /**
     * Find the Vaadin Application type for a project or null if none found.
     *
     * @param jproject
     * @return
     * @throws JavaModelException
     */
    private static IType findVaadinApplicationType(IJavaProject jproject)
            throws JavaModelException {
        IType type = jproject.findType(VaadinPlugin.VAADIN_PACKAGE_PREFIX
                + VaadinPlugin.APPLICATION_CLASS_NAME);
        if (type == null) {
            type = jproject.findType(VaadinPlugin.TOOLKIT_PACKAGE_PREFIX
                    + VaadinPlugin.APPLICATION_CLASS_NAME);
        }
        return type;
    }

    /**
     * Returns either "com.vaadin." or "com.itmill.toolkit." depending on the
     * Vaadin version in the project. Defaults to "com.vaadin." if neither
     * found.
     *
     * @param project
     * @return
     */
    public static String getVaadinPackagePrefix(IProject project) {
        if (isVaadin6(project)) {
            return VaadinPlugin.VAADIN_PACKAGE_PREFIX;
        } else {
            return VaadinPlugin.TOOLKIT_PACKAGE_PREFIX;
        }
    }

    /**
     * Returns either "VAADIN" or "ITMILL" depending on the Vaadin version in
     * the project, returning a default value if neither is found.
     *
     * @param project
     * @return
     */
    public static String getVaadinResourceDirectory(IProject project) {
        if (isVaadin6(project)) {
            return "VAADIN";
        } else {
            return "ITMILL";
        }
    }

    /**
     * Checks the Vaadin version, returns true for Vaadin 6.0+, false for IT
     * Mill Toolkit.
     *
     * @param project
     * @return true if a Vaadin project (or unknown), false for IT Mill Toolkit
     *         project
     */
    public static boolean isVaadin6(IProject project) {
        IJavaProject jproject = JavaCore.create(project);
        if (jproject != null) {
            try {
                if (jproject.findType(VaadinPlugin.VAADIN_PACKAGE_PREFIX
                        + VaadinPlugin.APPLICATION_CLASS_NAME) != null) {
                    return true;
                } else if (jproject
                        .findType(VaadinPlugin.TOOLKIT_PACKAGE_PREFIX
                                + VaadinPlugin.APPLICATION_CLASS_NAME) != null) {
                    return false;
                }
            } catch (JavaModelException e) {
                handleBackgroundException(
                        IStatus.WARNING,
                        "Failed to check the Vaadin version used in the project, assuming 6.0+",
                        e);
                return true;
            }
        }
        // default value
        return true;
    }

    private static IPackageFragmentRoot getJavaFragmentRoot(IProject project)
            throws CoreException {
        try {
            IJavaProject javaProject = JavaCore.create(project);
            for (IClasspathEntry classPathEntry : javaProject.getRawClasspath()) {
                if (classPathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IPackageFragmentRoot fragmentRoot = javaProject
                            .findPackageFragmentRoot(classPathEntry.getPath());
                    return fragmentRoot;
                }
            }

            return null;
        } catch (JavaModelException e) {
            throw newCoreException("Unable to locate source folder", e);
        }
    }

    /**
     * Create a configuration file from a template if it does not exist.
     *
     * @param file
     *            the file to create from template
     * @param template
     * @throws CoreException
     */
    @SuppressWarnings("unchecked")
    public static IFile ensureFileFromTemplate(IFile file, String template)
            throws CoreException {

        try {
            if (file.exists()) {
                return file;
            }

            String stub = VaadinPluginUtil.readTextFromTemplate(template);

            ByteArrayInputStream stubstream = new ByteArrayInputStream(stub
                    .getBytes());

            file.create(stubstream, true, null);

            return file;

        } catch (JavaModelException e) {
            throw VaadinPluginUtil.newCoreException("Failed to create "
                    + file.getName() + " file", e);
        } catch (IOException e) {
            throw VaadinPluginUtil.newCoreException("Failed to create "
                    + file.getName() + " file", e);
        }
    }

    public static void copyPluginFileToProject(IPath src, IFile dest)
            throws CoreException {
        try {
            // Bundle bundle = VaadinPlugin.getInstance().getBundle();
            // InputStream input = FileLocator.openStream(bundle, src, false);

            File file = src.toFile();
            FileInputStream input = new FileInputStream(file);
            dest.create(input, true, null);
            input.close();
        } catch (Exception e) {
            throw newCoreException("Failed to copy file to project", e);
        }
    }

    public static void copySourceFileToProject(IProject project,
            Path sourceFile, String destinationPackage, String destinationFile)
            throws CoreException {
        try {
            // JavaCore.createCompilationUnitFrom();
            Bundle bundle = VaadinPlugin.getInstance().getBundle();
            InputStream input = FileLocator.openStream(bundle, sourceFile,
                    false);
            StringWriter writer = new StringWriter();
            IOUtils.copy(input, writer);
            input.close();

            String contents = writer.toString();

            IPackageFragmentRoot fragmentRoot = VaadinPluginUtil
                    .getJavaFragmentRoot(project);
            IPackageFragment packageFragment = fragmentRoot
                    .createPackageFragment(destinationPackage, true, null);
            // ICompilationUnit compilationUnit =
            packageFragment.createCompilationUnit(destinationFile, contents,
                    true, null);

            // JavaCore.create(project.getWorkspace().getRoot().getFolder(sourceFile))
        } catch (Exception e) {
            throw newCoreException("Failed to copy source file to project", e);
        }

    }

    public static String createApplicationClassSource(String packageName,
            String applicationName, String applicationClass,
            String vaadinPackagePrefix) {
        String template = "package " + packageName + ";\n\n" + "import "
                + vaadinPackagePrefix + "Application;\n" + "import "
                + vaadinPackagePrefix + "ui.*;\n\n" + "public class "
                + applicationClass + " extends Application {\n"
                + "\t@Override\n" + "\tpublic void init() {\n"
                + "\t\tWindow mainWindow = new Window(\"" + applicationName
                + "\");\n"
                + "\t\tLabel label = new Label(\"Hello Vaadin user\");\n"
                + "\t\tmainWindow.addComponent(label);\n"
                + "\t\tsetMainWindow(mainWindow);\n" + "\t}\n" + "\n" + "}\n";

        return template;

    }

    /**
     * Ensure that some Vaadin jar file can be found in the project. If none can
     * be found, adds the specified version from the local repository.
     *
     * No launch configurations are updated. Use updateVaadinLibraries if such
     * updates are needed.
     *
     * @param project
     * @param vaadinJarVersion
     * @param monitor
     * @throws CoreException
     */
    public static void ensureVaadinLibraries(IProject project,
            Version vaadinJarVersion, IProgressMonitor monitor)
            throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask(
                    "Ensuring the project includes the Vaadin library", 1);

            IJavaProject jproject = JavaCore.create(project);
            try {
                IType findType = findVaadinApplicationType(jproject);
                if (findType == null) {
                    addVaadinLibrary(jproject, vaadinJarVersion,
                            new SubProgressMonitor(monitor, 1));

                    // refresh library folder to recompile parts of project
                    IFolder lib = getWebInfLibFolder(project);
                    lib.refreshLocal(IResource.DEPTH_ONE, null);
                }
            } catch (JavaModelException e) {
                throw newCoreException(
                        "Failed to ensure that a Vaadin jar is included in project",
                        e);
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Ensure that an Vaadin jar file can be found in the project and is of the
     * correct version. If none can be found or the version does not match,
     * replaces any old Vaadin JAR with the specified version from the local
     * repository.
     *
     * Update widgetset compilation launch configurations in the project to
     * refer to the new Vaadin and GWT versions (only when changing Vaadin
     * version, not when adding the JAR).
     *
     * @param project
     * @param vaadinJarVersion
     *            or null to remove current Vaadin library
     * @throws CoreException
     */
    public static void updateVaadinLibraries(IProject project,
            Version vaadinJarVersion, IProgressMonitor monitor)
            throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("Updating Vaadin libraries in the project", 11);

            // do nothing if correct version is already in the project
            Version currentVersion = getVaadinLibraryVersion(project);
            if ((vaadinJarVersion == currentVersion)
                    || (vaadinJarVersion != null && vaadinJarVersion
                            .equals(currentVersion))) {
                return;
            }
            IJavaProject jproject = JavaCore.create(project);
            try {
                // replace the Vaadin JAR (currentVersion) with the new one
                if (currentVersion != null) {
                    removeVaadinLibrary(jproject, currentVersion);
                }
                monitor.worked(1);
                if (vaadinJarVersion != null) {
                    addVaadinLibrary(jproject, vaadinJarVersion,
                            new SubProgressMonitor(monitor, 9));
                }
                // refresh library folder to recompile parts of project
                IFolder lib = getWebInfLibFolder(project);
                lib.refreshLocal(IResource.DEPTH_ONE, null);

                // TODO also handle adding Vaadin JAR to a project if the user
                // has removed it and adds a different version?
                if (currentVersion != null
                        && vaadinJarVersion != null) {
                    // update launches
                    String oldVaadinJarName = currentVersion.getJarFileName();
                    IPath vaadinJarPath = VaadinPluginUtil
                            .findProjectVaadinJarPath(jproject);
                    updateLaunchClassPath(project, oldVaadinJarName,
                            vaadinJarPath);
                    monitor.worked(1);
                }
            } catch (JavaModelException e) {
                throw newCoreException(
                        "Failed to update Vaadin jar in project", e);
            }
        } finally {
            monitor.done();
        }
    }

    public static Version getVaadinLibraryVersion(IProject project)
            throws CoreException {
        IFolder lib = getWebInfLibFolder(project);
        IResource[] files = lib.members();

        for (IResource resource : files) {
            // is it a Vaadin JAR?
            if (resource instanceof IFile) {
                Version version = DownloadUtils.getVaadinJarVersion(resource
                        .getName());
                if (version != null) {
                    return version;
                }
            }
        }

        return null;
    }

    public static CoreException newCoreException(String message, Throwable e) {
        return new CoreException(new Status(Status.ERROR,
                VaadinPlugin.PLUGIN_ID, message, e));
    }

    /**
     * Adds the specified Vaadin jar version from the local store to the
     * project. The specified version must be found from the local store or an
     * exception is thrown.
     *
     * @param jproject
     * @param vaadinJarVersion
     * @param monitor
     * @throws CoreException
     */
    private static void addVaadinLibrary(IJavaProject jproject,
            Version vaadinJarVersion, IProgressMonitor monitor)
            throws CoreException {

        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask(
                    "Adding Vaadin required libraries to the project", 5);

            IProject project = jproject.getProject();
            IFile targetFile = getWebInfLibFolder(project).getFile(
                    vaadinJarVersion.getJarFileName());
            DownloadUtils.ensureVaadinJarExists(vaadinJarVersion,
                    new SubProgressMonitor(monitor, 1));
            IPath sourceFile = DownloadUtils
                    .getLocalVaadinJar(vaadinJarVersion);

            VaadinPluginUtil.copyPluginFileToProject(sourceFile, targetFile);

            // refresh project
            getWebInfLibFolder(project).refreshLocal(IResource.DEPTH_ONE, null);

            // make sure the GWT library versions match the Vaadin JAR
            // requirements
            updateGWTLibraries(jproject, new SubProgressMonitor(monitor, 4));
        } catch (Exception e) {
            throw newCoreException("Failed to add Vaadin jar to project", e);
        } finally {
            monitor.done();
        }
    }

    /**
     * Removes the specified Vaadin jar version from the project (if it exists).
     *
     * @param jproject
     * @param vaadinJarVersion
     * @throws CoreException
     */
    private static void removeVaadinLibrary(IJavaProject jproject,
            Version vaadinJarVersion) throws CoreException {
        try {
            IProject project = jproject.getProject();
            IFile targetFile = getWebInfLibFolder(project).getFile(
                    vaadinJarVersion.getJarFileName());
            targetFile.delete(true, null);

            // refresh project
            getWebInfLibFolder(project).refreshLocal(IResource.DEPTH_ONE, null);
        } catch (Exception e) {
            throw newCoreException("Failed to remove Vaadin jar from project",
                    e);
        }
    }

    /**
     * Ensure that the project classpath contains the GWT libraries, adding them
     * if necessary.
     *
     * Also update widgetset compilation launch configuration paths as needed.
     *
     * @param project
     * @param monitor
     * @throws CoreException
     */
    public static void ensureGWTLibraries(IProject project,
            IProgressMonitor monitor)
            throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor
                    .beginTask(
                            "Ensuring that the project classpath contains GWT libraries",
                            1);

            IJavaProject jproject = JavaCore.create(project);
            try {
                IType findType = jproject
                        .findType("com.google.gwt.core.client.EntryPoint");

                if (findType == null) {
                    updateGWTLibraries(jproject, new SubProgressMonitor(
                            monitor, 1));
                }
            } catch (JavaModelException e) {
                throw newCoreException(
                        "Failed to ensure GWT libraries are present in the project",
                        e);
            }
        } finally {
            monitor.done();
        }
    }

    // add or update GWT libraries in a project based on the Vaadin version in
    // the project (if any)
    private static void updateGWTLibraries(IJavaProject jproject,
            IProgressMonitor monitor)
            throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("Updating GWT libraries", 14);

            String gwtVersion = getRequiredGWTVersionForProject(jproject);
            monitor.worked(1);

            DownloadUtils.ensureGwtUserJarExists(gwtVersion,
                    new SubProgressMonitor(monitor, 5));
            DownloadUtils.ensureGwtDevJarExists(gwtVersion,
                    new SubProgressMonitor(monitor, 5));

            try {
                IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
                List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
                for (IClasspathEntry entry : rawClasspath) {
                    entries.add(entry);
                }

                // use the VAADIN_DOWNLOAD_VARIABLE variable and variable
                // classpath entries where feasible

                IPath devJarPath = getGWTDevJarPath(jproject);
                IClasspathEntry gwtDev = makeVariableClasspathEntry(
                        VaadinClasspathVariableInitializer.VAADIN_DOWNLOAD_VARIABLE,
                        devJarPath);

                IPath userJarPath = getGWTUserJarPath(jproject);
                IClasspathEntry gwtUser = makeVariableClasspathEntry(
                        VaadinClasspathVariableInitializer.VAADIN_DOWNLOAD_VARIABLE,
                        userJarPath);

                // replace gwt-dev-[platform].jar if found, otherwise append new
                // entry
                String devJarName = "gwt-dev-" + getPlatform() + ".jar";
                replaceClassPathEntry(entries, gwtDev, devJarName);

                // replace gwt-user.jar if found, otherwise append new entry
                replaceClassPathEntry(entries, gwtUser, "gwt-user.jar");

                IClasspathEntry[] entryArray = entries
                        .toArray(new IClasspathEntry[entries.size()]);
                jproject.setRawClasspath(entryArray, null);

                monitor.worked(1);

                IProject project = jproject.getProject();

                // update classpaths also in launches
                updateLaunchClassPath(project, devJarName, devJarPath);
                monitor.worked(1);

                updateLaunchClassPath(project, "gwt-user.jar", userJarPath);
                monitor.worked(1);
            } catch (JavaModelException e) {
                throw newCoreException("addGWTLibraries failed", e);
            }
        } finally {
            monitor.done();
        }

    }

    /**
     * Create a variable-based classpath entry if the given path is under the
     * target of the variable, an absolute one otherwise.
     *
     * @param variableName
     * @param jarPath
     * @return
     */
    private static IClasspathEntry makeVariableClasspathEntry(
            String variableName, IPath jarPath) {
        IPath variablePath = JavaCore.getClasspathVariable(variableName);
        if (variablePath.isPrefixOf(jarPath)) {
            // path starting with the variable name => relative to its content
            IPath jarVariablePath = new Path(variableName).append(jarPath
                    .removeFirstSegments(variablePath.segmentCount()));
            return JavaCore.newVariableEntry(jarVariablePath, null, null);
        } else {
            return JavaCore.newLibraryEntry(jarPath, null, null);
        }
    }

    // replace an existing class path entry (identified by last segment name)
    // with a new one or append the new entry if not found
    private static void replaceClassPathEntry(List<IClasspathEntry> entries,
            IClasspathEntry newEntry, String entryName) {
        boolean found = false;
        for (int i = 0; i < entries.size(); ++i) {
            if (entryName.equals(entries.get(i).getPath().lastSegment())) {
                entries.set(i, newEntry);
                found = true;
                break;
            }
        }
        if (!found) {
            entries.add(newEntry);
        }
    }

    /**
     * Update the class path for program execution launch configurations
     * referring to the given JAR file in their arguments (not the class path of
     * the launch itself!). This is called when a JAR is replaced by a different
     * version which may have a different name or location.
     *
     * The old JAR is identified by its file name without path. The JAR path is
     * extracted by back-tracking from the JAR file name to the previous path
     * separator and that full path is replaced with the given new path to a JAR
     * file.
     *
     * This is primarily meant for updating the generated widgetset compilation
     * launches, but will also modify certain other kinds of launches.
     *
     * @throws CoreException
     */
    private static void updateLaunchClassPath(IProject project, String jarName,
            IPath jarPath) throws CoreException {
        // list all launches
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType type = manager
                .getLaunchConfigurationType(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE);
        // it seems it is not possible to get the project for an external launch
        // with the official APIs
        // ILaunchConfiguration[] launchConfigurations = manager
        // .getLaunchConfigurations(type);

        // limit to launches that are top-level resources in the project of
        // interest
        for (IResource resource : project.members()) {
            // identify the external launches referring to the JAR
            if (resource instanceof IFile
                    && "launch".equals(resource.getFileExtension())) {
                ILaunchConfiguration launchConfiguration = manager
                        .getLaunchConfiguration((IFile) resource);
                if (launchConfiguration != null && launchConfiguration.exists()
                        && type.equals(launchConfiguration.getType())) {
                    String arguments = launchConfiguration.getAttribute(
                            IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "");
                    if (arguments.contains(jarName)) {
                        // update the classpath of a single launch
                        updateLaunchClassPath(launchConfiguration, jarName,
                                jarPath);
                    }
                }
            }
        }
    }

    private static void updateLaunchClassPath(
            ILaunchConfiguration launchConfiguration, String jarName,
            IPath jarPath) throws CoreException {
        // update a launch - careful about separators etc.
        ILaunchConfigurationWorkingCopy workingCopy = launchConfiguration
                .getWorkingCopy();

        String arguments = launchConfiguration.getAttribute(
                IExternalToolConstants.ATTR_TOOL_ARGUMENTS, "");

        // find the JAR reference (from previous separator to the next) and
        // replace it with the new path
        // on all platforms, also need to handle &quot;
        String separators;
        if ("windows".equals(VaadinPluginUtil.getPlatform())) {
            separators = ";&\"";
        } else {
            separators = ":&;\"";
        }

        // look for the JAR name potentially preceded with a path etc.
        Pattern pattern = Pattern.compile("[" + separators + "]([^"
                + separators + "]*"
                + jarName + ")[" + separators + "]");
        Matcher matcher = pattern.matcher(arguments);

        String newPath = JavaRuntime.newArchiveRuntimeClasspathEntry(jarPath)
                .getLocation();

        // replace path from previous separator to the next
        String result = arguments;
        matcher.find();
        for (int group = 1; group <= matcher.groupCount(); ++group) {
            result = result.replace(matcher.group(group), newPath);
            matcher.find();
        }

        workingCopy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS,
                result);

        // save the launch
        workingCopy.doSave();
    }


    /**
     * TODO should first check if user has defined custom version of GWT to
     * project. If not then return the newest available by plugin.
     *
     * ATM just return the one in plugin.
     *
     * @param gwtVersion
     * @throws CoreException
     */
    public static IPath getGWTDevJarPath(IJavaProject jproject)
            throws CoreException {
        // TODO should check if the user has changed the GWT version by hand
        String gwtVersion = getRequiredGWTVersionForProject(jproject);
        return DownloadUtils.getLocalGwtDevJar(gwtVersion);
    }

    public static String getPlatform() {
        String osname = System.getProperty("os.name");
        if (osname.toLowerCase().contains("mac")) {
            return "mac";
        } else if (osname.toLowerCase().contains("linux")) {
            return "linux";
        } else {
            return "windows";
        }

    }

    /**
     * TODO should first check if user has defined custom version of GWT to
     * project. If not then return the newest available by plugin.
     *
     * ATM just return the one in plugin.
     *
     * @param gwtVersion
     * @throws CoreException
     */
    public static IPath getGWTUserJarPath(IJavaProject project)
            throws CoreException {
        // TODO should check if the user has changed the GWT version by hand
        String gwtVersion = getRequiredGWTVersionForProject(project);
        return DownloadUtils.getLocalGwtUserJar(gwtVersion);
    }

    public static Path getPathToTemplateFile(String path) throws IOException {
        Bundle bundle = VaadinPlugin.getInstance().getBundle();
        URL fileURL = FileLocator.toFileURL(bundle.getResource("template/"
                + path));
        return new Path(fileURL.getPath());
    }

    public static String readTextFromStream(InputStream resourceAsStream) {
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(resourceAsStream, writer);
            resourceAsStream.close();
        } catch (IOException e) {
            // TODO this error message might not be ideal
            handleBackgroundException(IStatus.ERROR,
                    "Failed to read template file from the Vaadin plugin", e);
        }

        return writer.toString();

    }

    public static IPath getConfigurationPath() throws CoreException {
        URL userLocation = Platform.getUserLocation().getURL();
        URL configurationLocation = Platform.getConfigurationLocation()
                .getURL();

        if (configurationLocation != null) {
            try {
                return new Path(FileLocator.toFileURL(configurationLocation)
                        .getPath()).append(IPath.SEPARATOR
                        + VaadinPlugin.PLUGIN_ID);
            } catch (IOException e) {
                throw newCoreException("getConfigurationPath failed", e);
            }
        }

        if (userLocation != null) {
            try {
                return new Path(FileLocator.toFileURL(userLocation).getPath())
                        .append(IPath.SEPARATOR + VaadinPlugin.PLUGIN_ID);
            } catch (IOException e) {
                throw newCoreException("getConfigurationPath failed", e);
            }
        }

        IPath stateLocation = VaadinPlugin.getInstance().getStateLocation();
        if (stateLocation != null) {
            return stateLocation;
        }

        throw newCoreException(
                "getConfigurationPath found nowhere to store files", null);
    }

    public static IPath getDownloadDirectory() throws CoreException {
        IPath path = getConfigurationPath()
                .append(IPath.SEPARATOR + "download");

        // Create the directory if it does not exist
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }

        return path;
    }

    public static IPath getDownloadDirectory(String identifier)
            throws CoreException {
        IPath path = getDownloadDirectory()
                .append(IPath.SEPARATOR + identifier);

        // Create the directory if it does not exist
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }

        return path;
    }

    public static IPath getVersionedDownloadDirectory(String identifier,
            String version) throws CoreException {
        IPath path = getDownloadDirectory(identifier).append(
                IPath.SEPARATOR + version);

        return path;
    }

    public static String getRequiredGWTVersionForProject(IJavaProject jproject) {
        // if no information exists, default to 1.5.3
        String gwtVersion = "1.5.3";

        try {
            IFolder lib = getWebInfLibFolder(jproject.getProject());
            IResource[] files = lib.members();

            // Check gwt version from included Vaadin jar
            for (IResource resource : files) {
                // is it a Vaadin JAR?
                if (resource instanceof IFile) {
                    Version version = DownloadUtils
                            .getVaadinJarVersion(resource.getName());
                    if (version == null) {
                        continue;
                    }

                    IPath jarLocation = resource.getLocation();
                    if (jarLocation == null) {
                        throw newCoreException("Could not access Vaadin JAR",
                                null);
                    }

                    JarFile jarFile = new JarFile(jarLocation.toFile()
                            .getAbsolutePath());
                    ZipEntry entry = jarFile.getEntry("META-INF/GWT-VERSION");
                    if (entry == null) {
                        // found JAR but not GWT version information in it, use
                        // default
                        break;
                    }

                    // extract GWT version from the JAR
                    InputStream gwtVersionStream = jarFile
                            .getInputStream(entry);
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(gwtVersionStream));

                    gwtVersion = reader.readLine();

                    // do not continue with other JARs
                    break;
                }
            }
        } catch (IOException ex) {
            handleBackgroundException(IStatus.WARNING,
                    "Failed to determine the GWT library version to use, defaulting to "
                            + gwtVersion, ex);
        } catch (CoreException ex) {
            handleBackgroundException(IStatus.WARNING,
                    "Failed to determine the GWT library version to use, defaulting to "
                            + gwtVersion, ex);
        }

        return gwtVersion;
    }

    public static IPath findProjectVaadinJarPath(IJavaProject javaProject)
            throws CoreException {
        IJavaElement type;
        type = findVaadinApplicationType(javaProject);
        if (type == null) {
            return null;
        }
        while (type.getParent() != null) {
            if (type instanceof IPackageFragmentRoot) {
                return ((IPackageFragmentRoot) type).getPath();
            }

            type = type.getParent();
        }

        // For some reason we were not able to locate the jar which contains
        // Vaadin jar
        return null;
    }

    /**
     * Create the folder if it does not exist. If the parent folder does not
     * exist, it is created first.
     *
     * @param folder
     * @param monitor
     * @throws CoreException
     */
    public static void createFolders(IFolder folder, IProgressMonitor monitor)
            throws CoreException {
        if (folder.exists()) {
            return;
        }
        if (!folder.getParent().exists()) {
            createFolders((IFolder) folder.getParent(), monitor);
        }

        folder.create(true, false, monitor);

    }

    public static boolean typeExtendsClass(IType type, String className)
            throws JavaModelException {
        if (type.isStructureKnown() && type.isClass()) {
            ITypeHierarchy h = type.newSupertypeHierarchy(null);
            IType spr = h.getSuperclass(type);
            while (spr != null) {
                if (!spr.isClass()) {
                    break;
                }
                if (spr.getFullyQualifiedName().equals(className)) {

                    return true;
                }
                spr = h.getSuperclass(spr);
            }
        }
        return false;
    }

    private static InputStream openPluginFileAsStream(String templateName)
            throws IOException {
        Bundle bundle = VaadinPlugin.getInstance().getBundle();
        InputStream input = FileLocator.openStream(bundle, new Path(
                templateName), false);
        return input;
    }

    public static String readTextFromTemplate(String templateName)
            throws IOException {
        return readTextFromStream(openPluginFileAsStream("template/"
                + templateName));
    }

    public static IType[] getSubClasses(IProject project, String superClass,
            boolean allSubTypes, IProgressMonitor monitor)
            throws JavaModelException {
        // find all non-binary subclasses of a named class in the project
        IJavaProject javaProject = JavaCore.create(project);
        if (javaProject == null) {
            return new IType[0];
        }
        IType superType = javaProject.findType(superClass);
        ITypeHierarchy newTypeHierarchy = superType.newTypeHierarchy(monitor);

        IType[] subTypes;
        if (allSubTypes) {
            subTypes = newTypeHierarchy.getAllSubtypes(superType);
        } else {
            subTypes = newTypeHierarchy.getSubclasses(superType);
        }
        List<IType> types = new ArrayList<IType>();
        for (IType subclass : subTypes) {
            // #3441 for some reason, getAllSubtypes fetches types that are in
            // the project AND those open in editors => filter for project
            if (subclass.isResolved() && !subclass.isBinary()
                    && javaProject.equals(subclass.getJavaProject())) {
                types.add(subclass);
            }
        }
        return types.toArray(new IType[0]);
    }

    public static IType[] getApplicationClasses(IProject project,
            IProgressMonitor monitor) throws JavaModelException {
        // find all non-binary subclasses of Application in the project
        return getSubClasses(project, getVaadinPackagePrefix(project)
                + VaadinPlugin.APPLICATION_CLASS_NAME, false, monitor);
    }

    public static IType[] getWidgetSetClasses(IProject project,
            IProgressMonitor monitor) throws JavaModelException {
        // find all non-binary subclasses of WidgetSet in the project
        return getSubClasses(project, getVaadinPackagePrefix(project)
                + "terminal.gwt.client.WidgetSet", true, monitor);
    }
}
