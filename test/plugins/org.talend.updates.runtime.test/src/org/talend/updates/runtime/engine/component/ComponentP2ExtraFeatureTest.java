// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.updates.runtime.engine.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.m2e.core.MavenPlugin;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.talend.commons.runtime.service.ComponentsInstallComponent;
import org.talend.commons.utils.resource.BundleFileUtil;
import org.talend.librariesmanager.prefs.LibrariesManagerUtils;
import org.talend.updates.runtime.TestUtils;
import org.talend.updates.runtime.engine.P2InstallerTest;
import org.talend.updates.runtime.model.ExtraFeature;
import org.talend.updates.runtime.model.P2ExtraFeatureException;
import org.talend.updates.runtime.model.UpdateSiteLocationType;
import org.talend.utils.io.FilesUtils;

/**
 * created by ycbai on 2017年5月18日 Detailled comment
 *
 */
public class ComponentP2ExtraFeatureTest {

    private ComponentP2ExtraFeature feature;

    private File tmpFolder = null;

    public File tempP2Folder;

    private static final NullProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();

    private static File TEST_COMPONENTS_V1_UPDATE_SITE_FILE;

    private static File TEST_COMPONENTS_V2_UPDATE_SITE_FILE;

    @BeforeClass
    public static void beforeClass() throws URISyntaxException, IOException {
        TEST_COMPONENTS_V1_UPDATE_SITE_FILE = TestUtils
                .getResourceFile("/resources/components/org.talend.components.file-0.1.0.zip"); //$NON-NLS-1$
        TEST_COMPONENTS_V2_UPDATE_SITE_FILE = TestUtils
                .getResourceFile("/resources/components/org.talend.components.file-0.2.0.zip"); //$NON-NLS-1$
    }

    @Before
    public void before() throws IOException {
        feature = new ComponentP2ExtraFeatureForJUnit("FileInput", "0.1.0", "", "tos_di,tos_bd,tp_bd",
                "mvn:org.talend.components/components-file-definition/0.1.0/zip", "org.talend.components.file");
        tmpFolder = org.talend.utils.files.FileUtils.createTmpFolder("test", "comp"); //$NON-NLS-1$ //$NON-NLS-2$
        tempP2Folder = org.talend.utils.files.FileUtils.createTmpFolder("test", "p2"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @After
    public void after() {
        FilesUtils.deleteFolder(tmpFolder, true);
        FilesUtils.deleteFolder(tempP2Folder, true);
        FilesUtils.deleteFolder(feature.getTmpFolder(), true);
    }

    private final class ComponentP2ExtraFeatureForJUnit extends ComponentP2ExtraFeature {

        public ComponentP2ExtraFeatureForJUnit(String name, String version, String description, String product, String mvnURI,
                String p2IuId) {
            super(name, version, description, product, mvnURI, p2IuId);
        }

    }

    @Test
    public void testConstructor() {
        ComponentP2ExtraFeature extraFeature = new ComponentP2ExtraFeatureForJUnit("FileInput", "0.1.0", "File input components",
                "tos_di,tos_bd,tp_bd", "mvn:org.talend.components/components-file-definition/0.1.0/zip",
                "org.talend.components.file");
        assertEquals("FileInput", extraFeature.getName()); //$NON-NLS-1$
        assertEquals("0.1.0", extraFeature.getVersion()); //$NON-NLS-1$
        assertEquals("File input components", extraFeature.getDescription()); //$NON-NLS-1$
        assertEquals("tos_di,tos_bd,tp_bd", extraFeature.getProduct()); //$NON-NLS-1$
        assertEquals("mvn:org.talend.components/components-file-definition/0.1.0/zip", extraFeature.getMvnURI()); //$NON-NLS-1$
        assertEquals("org.talend.components.file", extraFeature.getP2IuId()); //$NON-NLS-1$
    }

    public void testUpdateSiteCompatibleTypes() {
        assertEquals(EnumSet.of(UpdateSiteLocationType.DEFAULT_REPO), feature.getUpdateSiteCompatibleTypes());
    }

    public void testExtraFeatureInstall() throws ProvisionException, P2ExtraFeatureException {
        assertFalse(feature.isInstalled(NULL_PROGRESS_MONITOR));
        List<URI> repoUris = new ArrayList<>(1);
        repoUris.add(URI.create("jar:" + TEST_COMPONENTS_V1_UPDATE_SITE_FILE.toURI().toString() + "!/")); //$NON-NLS-1$//$NON-NLS-2$
        feature.install(NULL_PROGRESS_MONITOR, repoUris);
        try {
            assertTrue(feature.isInstalled(NULL_PROGRESS_MONITOR));
        } finally {
            TestUtils.uninstallIU(feature.getP2AgentUri(), feature.getP2ProfileId(), feature.getP2IuId());
            assertFalse(feature.isInstalled(NULL_PROGRESS_MONITOR));
        }
    }

    @Test
    public void testExtraFeatureHasUpdateAndInstallIt() throws Exception {
        assertFalse(feature.isInstalled(NULL_PROGRESS_MONITOR));
        List<URI> repoUris = Collections
                .singletonList(URI.create("jar:" + TEST_COMPONENTS_V1_UPDATE_SITE_FILE.toURI().toString() + "!/")); //$NON-NLS-1$//$NON-NLS-2$
        feature.install(NULL_PROGRESS_MONITOR, repoUris);
        try {
            assertTrue(feature.isInstalled(NULL_PROGRESS_MONITOR));
            ExtraFeature updateFeature = feature.createFeatureIfUpdates(NULL_PROGRESS_MONITOR, repoUris);
            assertNull(updateFeature);
            // check for an update using another update site
            repoUris = Collections
                    .singletonList(URI.create("jar:" + TEST_COMPONENTS_V2_UPDATE_SITE_FILE.toURI().toString() + "!/")); //$NON-NLS-1$//$NON-NLS-2$
            updateFeature = feature.createFeatureIfUpdates(NULL_PROGRESS_MONITOR, repoUris);
            assertNotNull(updateFeature);
            updateFeature.install(NULL_PROGRESS_MONITOR, repoUris);
            Set<IInstallableUnit> installedIUs = feature.getInstalledIUs(feature.getP2IuId(), NULL_PROGRESS_MONITOR);
            assertFalse(installedIUs.isEmpty());
            assertEquals("0.2.0", installedIUs.iterator().next().getVersion().getOriginal()); //$NON-NLS-1$
        } finally {
            TestUtils.uninstallIU(feature.getP2AgentUri(), feature.getP2ProfileId(), feature.getP2IuId());
            assertFalse(feature.isInstalled(NULL_PROGRESS_MONITOR));
        }
    }

    @Test
    public void testExtraFeatureNoUpdateAvailable() throws ProvisionException, P2ExtraFeatureException {
        assertFalse(feature.isInstalled(NULL_PROGRESS_MONITOR));
        List<URI> repoUris = Collections
                .singletonList(URI.create("jar:" + TEST_COMPONENTS_V1_UPDATE_SITE_FILE.toURI().toString() + "!/")); //$NON-NLS-1$//$NON-NLS-2$
        feature.install(NULL_PROGRESS_MONITOR, repoUris);
        try {
            assertTrue(feature.isInstalled(NULL_PROGRESS_MONITOR));
            ExtraFeature updateFeature = feature.createFeatureIfUpdates(NULL_PROGRESS_MONITOR, repoUris);
            assertNull(updateFeature);
        } finally {
            TestUtils.uninstallIU(feature.getP2AgentUri(), feature.getP2ProfileId(), feature.getP2IuId());
            assertFalse(feature.isInstalled(NULL_PROGRESS_MONITOR));
        }
    }

    @Test
    public void test_syncLibraries_nonExisted() throws IOException {
        final String oldLibPath = System.getProperty(LibrariesManagerUtils.TALEND_LIBRARY_PATH);
        try {
            // set prod lib
            final File testProdLibFolder = new File(tmpFolder, "prod/" + LibrariesManagerUtils.LIB_JAVA_SUB_FOLDER);
            System.setProperty(LibrariesManagerUtils.TALEND_LIBRARY_PATH, testProdLibFolder.getAbsolutePath());
            Assert.assertFalse(testProdLibFolder.exists());

            final File testFile = new File(tmpFolder, "test");
            feature.syncLibraries(testFile);
            Assert.assertFalse(testProdLibFolder.exists());
        } finally {
            if (oldLibPath != null) {
                System.setProperty(LibrariesManagerUtils.TALEND_LIBRARY_PATH, oldLibPath);
            } else {
                System.clearProperty(LibrariesManagerUtils.TALEND_LIBRARY_PATH);
            }

        }
    }

    @Test
    public void test_syncLibraries_nonFolder() throws IOException {
        final String oldLibPath = System.getProperty(LibrariesManagerUtils.TALEND_LIBRARY_PATH);
        try {
            // set prod lib
            final File testProdLibFolder = new File(tmpFolder, "prod/" + LibrariesManagerUtils.LIB_JAVA_SUB_FOLDER);
            System.setProperty(LibrariesManagerUtils.TALEND_LIBRARY_PATH, testProdLibFolder.getAbsolutePath());
            Assert.assertFalse(testProdLibFolder.exists());

            final File testFile = new File(tmpFolder, "test.txt");
            testFile.createNewFile();

            feature.syncLibraries(testFile);
            Assert.assertFalse(testProdLibFolder.exists());
        } finally {
            if (oldLibPath != null) {
                System.setProperty(LibrariesManagerUtils.TALEND_LIBRARY_PATH, oldLibPath);
            } else {
                System.clearProperty(LibrariesManagerUtils.TALEND_LIBRARY_PATH);
            }

        }
    }

    @Test
    public void test_syncLibraries() throws IOException {
        final File testDataFile = BundleFileUtil.getBundleFile(this.getClass(), P2InstallerTest.TEST_COMP_MYJIRA);
        Assert.assertNotNull(testDataFile);
        Assert.assertTrue(testDataFile.exists());
        final String oldLibPath = System.getProperty(LibrariesManagerUtils.TALEND_LIBRARY_PATH);
        try {
            // set prod lib
            final File testProdLibFolder = new File(tmpFolder, "prod/" + LibrariesManagerUtils.LIB_JAVA_SUB_FOLDER);
            System.setProperty(LibrariesManagerUtils.TALEND_LIBRARY_PATH, testProdLibFolder.getAbsolutePath());

            final File testFolder = new File(tmpFolder, "test");
            final File testLibFolder = new File(testFolder, LibrariesManagerUtils.LIB_JAVA_SUB_FOLDER);
            FilesUtils.copyFile(testDataFile, new File(testLibFolder, testDataFile.getName()));

            Assert.assertFalse(testProdLibFolder.exists());

            feature.syncLibraries(testFolder);

            Assert.assertTrue(testProdLibFolder.exists());
            final String[] list = testProdLibFolder.list();
            Assert.assertEquals(1, list.length);
            Assert.assertEquals(testDataFile.getName(), list[0]);
        } finally {
            if (oldLibPath != null) {
                System.setProperty(LibrariesManagerUtils.TALEND_LIBRARY_PATH, oldLibPath);
            } else {
                System.clearProperty(LibrariesManagerUtils.TALEND_LIBRARY_PATH);
            }

        }
    }

    @Test
    public void test_syncM2Repository() throws Exception {
        final File testDataFile = BundleFileUtil.getBundleFile(this.getClass(), P2InstallerTest.TEST_COMP_MYJIRA);
        Assert.assertNotNull(testDataFile);
        Assert.assertTrue(testDataFile.exists());

        FilesUtils.unzip(testDataFile.getAbsolutePath(), tmpFolder.getAbsolutePath());

        // check from local
        final String localRepositoryPath = MavenPlugin.getMaven().getLocalRepositoryPath();
        Assert.assertNotNull(localRepositoryPath);
        File localRepoFolder = new File(localRepositoryPath);
        Assert.assertTrue(localRepoFolder.exists());

        File libFile = new File(localRepoFolder, "org/talend/libraries/mytest/6.0.0/mytest-6.0.0.jar");
        if (libFile.exists()) {
            libFile.delete();
        }
        File pomFile = new File(localRepoFolder, "org/talend/libraries/mytest/6.0.0/mytest-6.0.0.pom");
        if (pomFile.exists()) {
            pomFile.delete();
        }

        feature.syncM2Repository(tmpFolder);

        Assert.assertTrue("sync lib for M2 repository failure", libFile.exists());
        Assert.assertTrue(pomFile.exists());
    }

    @Test
    public void test_syncM2Repository_login() throws Exception {
        final File testDataFile = BundleFileUtil.getBundleFile(this.getClass(), P2InstallerTest.TEST_COMP_MYJIRA);
        Assert.assertNotNull(testDataFile);
        Assert.assertTrue(testDataFile.exists());

        FilesUtils.unzip(testDataFile.getAbsolutePath(), tmpFolder.getAbsolutePath());

        // check from local
        final String localRepositoryPath = MavenPlugin.getMaven().getLocalRepositoryPath();
        Assert.assertNotNull(localRepositoryPath);
        File localRepoFolder = new File(localRepositoryPath);
        Assert.assertTrue(localRepoFolder.exists());

        final String libPath = "org/talend/libraries/mytest/6.0.0/mytest-6.0.0.jar";
        File libFile = new File(localRepoFolder, libPath);
        if (libFile.exists()) {
            libFile.delete();
        }
        File pomFile = new File(localRepoFolder, "org/talend/libraries/mytest/6.0.0/mytest-6.0.0.pom");
        if (pomFile.exists()) {
            pomFile.delete();
        }

        feature.setLogin(true);
        feature.syncM2Repository(tmpFolder);

        Assert.assertFalse(libFile.exists());
        Assert.assertFalse(pomFile.exists());

        final File tempM2RepoFolder = feature.getTempM2RepoFolder();
        Assert.assertNotNull(tempM2RepoFolder);
        Assert.assertTrue(tempM2RepoFolder.exists());

        File tmpLibFile = new File(tempM2RepoFolder, ComponentsInstallComponent.FOLDER_M2_REPOSITORY + '/' + libPath);
        Assert.assertTrue(tmpLibFile.exists()); // in temp
        Assert.assertFalse(libFile.exists());// no install yet
    }

}