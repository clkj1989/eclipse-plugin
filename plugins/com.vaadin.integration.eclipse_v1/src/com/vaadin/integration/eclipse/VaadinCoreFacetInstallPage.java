package com.vaadin.integration.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jst.j2ee.internal.wizard.J2EEModuleFacetInstallPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetDataModelProperties;
import org.eclipse.wst.common.project.facet.core.internal.FacetedProjectWorkingCopy;

import com.vaadin.integration.eclipse.properties.VaadinVersionComposite;

/**
 * The install page that is used to configure the Vaadin facet when adding it to
 * a project, whether at project creation through the "Dynamic Web Project"
 * wizard or by adding a facet to an existing project.
 *
 * In the latter case, opening this page is optional.
 */
public class VaadinCoreFacetInstallPage extends J2EEModuleFacetInstallPage
        implements IVaadinFacetInstallDataModelProperties {

    private Button applicationCreateArtifacts;
    private Text applicationNameField;
    private Text applicationClassField;
    private Text applicationPackageField;
    private Button applicationCreatePortlet;
    private Text portletTitleField;

    public VaadinCoreFacetInstallPage() {
        super("com.vaadin.core.facet.install.page");
        setTitle("Vaadin project");
        setDescription("Configure Vaadin specific project details");
    }

    @Override
    protected String[] getValidationPropertyNames() {
        return new String[] { APPLICATION_NAME, APPLICATION_PACKAGE,
                APPLICATION_CLASS };
    }

    @Override
    protected Composite createTopLevelComposite(final Composite parent) {

        // when entering via the dialog, by default do create artifacts
        model.setProperty(CREATE_ARTIFACTS, Boolean.TRUE);

        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        createApplicationGroup(composite);

        createPortletGroup(composite);

        createVersionGroup(composite);

        Dialog.applyDialogFont(parent);

        return composite;
    }

    private void createApplicationGroup(final Composite composite) {
        final Group applicationGroup = new Group(composite, SWT.NONE);
        applicationGroup.setLayoutData(gdhfill());
        applicationGroup.setText("Application");
        applicationGroup.setLayout(new GridLayout(1, false));

        applicationCreateArtifacts = new Button(applicationGroup, SWT.CHECK);
        applicationCreateArtifacts.setText("Create project template");
        applicationCreateArtifacts.setLayoutData(gdhfill());
        // TODO handle with enablement like portlet creation
        applicationCreateArtifacts
                .addSelectionListener(new SelectionListener() {
                    public void widgetDefaultSelected(SelectionEvent e) {
                        enableFields(applicationCreateArtifacts.getSelection());
                    }

                    public void widgetSelected(SelectionEvent e) {
                        enableFields(applicationCreateArtifacts.getSelection());
                    }
                });
        synchHelper.synchCheckbox(applicationCreateArtifacts, CREATE_ARTIFACTS,
                null);

        Label label = new Label(applicationGroup, SWT.NONE);
        label.setLayoutData(gdhfill());
        label.setText("Application name:");

        applicationNameField = new Text(applicationGroup, SWT.BORDER);
        applicationNameField.setLayoutData(gdhfill());
        synchHelper.synchText(applicationNameField, APPLICATION_NAME,
                new Control[] { label });

        label = new Label(applicationGroup, SWT.NONE);
        label.setLayoutData(gdhfill());
        label.setText("Base package name:");

        applicationPackageField = new Text(applicationGroup, SWT.BORDER);
        applicationPackageField.setLayoutData(gdhfill());
        synchHelper.synchText(applicationPackageField, APPLICATION_PACKAGE,
                new Control[] { label });

        label = new Label(applicationGroup, SWT.NONE);
        label.setLayoutData(gdhfill());
        label.setText("Application class name:");

        applicationClassField = new Text(applicationGroup, SWT.BORDER);
        applicationClassField.setLayoutData(gdhfill());
        synchHelper.synchText(applicationClassField, APPLICATION_CLASS,
                new Control[] { label });
    }

    private void createPortletGroup(final Composite composite) {
        Label label;
        final Group portletGroup = new Group(composite, SWT.NONE);
        portletGroup.setLayoutData(gdhfill());
        portletGroup.setText("Portlet");
        portletGroup.setLayout(new GridLayout(1, false));

        // portlet creation
        applicationCreatePortlet = new Button(portletGroup, SWT.CHECK);
        applicationCreatePortlet.setText("Create portlet configuration");
        applicationCreatePortlet.setLayoutData(gdhfill());
        synchHelper.synchCheckbox(applicationCreatePortlet, CREATE_PORTLET,
                null);

        label = new Label(portletGroup, SWT.NONE);
        label.setLayoutData(gdhfill());
        label.setText("Portlet title:");

        portletTitleField = new Text(portletGroup, SWT.BORDER);
        portletTitleField.setLayoutData(gdhfill());
        synchHelper.synchText(portletTitleField, PORTLET_TITLE,
                new Control[] { label });

        portletTitleField.setEnabled(false);
    }

    private void createVersionGroup(final Composite composite) {
        final Group versionGroup = new Group(composite, SWT.NONE);
        versionGroup.setLayoutData(gdhfill());
        versionGroup.setText("Vaadin Version");
        versionGroup.setLayout(new GridLayout(1, false));

        // Vaadin version selection
        VaadinVersionComposite versionComposite = new VaadinVersionComposite(
                versionGroup, SWT.NULL);
        versionComposite.createContents();

        // this is used both in project creation and in adding a facet
        // afterwards, so e.g. the master DM property may not be set or may not
        // have the project property - using FPWC.getProject() instead
        IProject project = null;
        // need the actual implementation for getProject()
        FacetedProjectWorkingCopy fpwc = (FacetedProjectWorkingCopy) model
                .getProperty(IFacetDataModelProperties.FACETED_PROJECT_WORKING_COPY);
        // this test should be unnecessary, doing it just in case
        if (fpwc != null) {
            project = fpwc.getProject();
        }
        versionComposite.setProject(project);
        if (project == null) {
            versionComposite.selectLatestLocalVersion();
        }

        // synch version string to model
        synchHelper.synchCombo(versionComposite.getVersionCombo(),
                VAADIN_VERSION, new Control[] {});
    }

    protected void enableFields(boolean enabled) {
        applicationNameField.setEnabled(enabled);
        applicationPackageField.setEnabled(enabled);
        applicationClassField.setEnabled(enabled);

        if (!enabled) {
            applicationCreatePortlet.setSelection(false);
        }
        applicationCreatePortlet.setEnabled(enabled);
        // when re-enabling, the create portlet checkbox is still unchecked
        portletTitleField.setEnabled(false);
    }

}
