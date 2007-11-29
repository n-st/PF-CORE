/* $Id$
 * 
 * Copyright (c) 2007 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.util.test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * Provides basic testcase-setup with five controllers. Bart, Lisa, Homer, Marge
 * and Maggie.
 * <p>
 * After <code>#setUp()</code> is invoked it is ensured, that all controllers
 * are running. There are several utility methods to bring the test into a usual
 * state. To connect two controllers just call
 * <code>{@link #tryToConnect(Controller, Controller)}</code> in
 * <code>{@link #setUp()}</code>. After that all controllers are connected,
 * Lisa, Marge and Maggie run in normal node, Bart and Homer act as supernode.
 * <p>
 * You can access both controllers and do manupulating/testing stuff on them
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public abstract class FiveControllerTestCase extends MultipleControllerTestCase
{
    protected static final String BART_ID = "Bart";
    protected static final String HOMER_ID = "Homer";
    protected static final String MARGE_ID = "Marge";
    protected static final String LISA_ID = "Lisa";
    protected static final String MAGGIE_ID = "Maggie";

    // For the optional test folder.
    protected static final File TESTFOLDER_BASEDIR_BART = new File(TestHelper
        .getTestDir(), "ControllerBart/testFolder");
    protected static final File TESTFOLDER_BASEDIR_HOMER = new File(TestHelper
        .getTestDir(), "ControllerHomer/testFolder");
    protected static final File TESTFOLDER_BASEDIR_MARGE = new File(TestHelper
        .getTestDir(), "ControllerMarge/testFolder");
    protected static final File TESTFOLDER_BASEDIR_LISA = new File(TestHelper
        .getTestDir(), "ControllerLisa/testFolder");
    protected static final File TESTFOLDER_BASEDIR_MAGGIE = new File(TestHelper
        .getTestDir(), "ControllerMaggie/testFolder");

    /**
     * The test folder info.
     */
    private FolderInfo testFolder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Copy fresh configs
        startController(HOMER_ID);
        startController(BART_ID);
        startController(MARGE_ID);
        startController(LISA_ID);
        startController(MAGGIE_ID);
        System.out
            .println("-------------- Controllers started -----------------");
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        testFolder = null;
    }

    private void startController(String id) throws IOException {
        FileUtils.copyFile(new File("src/test-resources/Controller" + id
            + ".config"), new File("build/test/Controller" + id
            + "/PowerFolder.config"));
        startController(id, "build/test/Controller" + id + "/PowerFolder");
    }

    // For subtest ************************************************************

    /**
     * Connects and waits for connection of the simpsons controllers
     * 
     * @throws RuntimeException
     *             if not all simpsons are connected afterwards.
     */
    protected void connectSimpsons() {
        if (!tryToConnectSimpsons()) {
            throw new RuntimeException("Unable to connect simpsons controllers");
        }
    }

    /**
     * Tries completely to connect the simpsons under each other.
     * 
     * @return true if succeeded
     */
    protected boolean tryToConnectSimpsons() {
        return tryToConnect(getContollerHomer(), getContollerBart())
            && tryToConnect(getContollerHomer(), getContollerMarge())
            && tryToConnect(getContollerHomer(), getContollerLisa())
            && tryToConnect(getContollerHomer(), getContollerMaggie())
            && tryToConnect(getContollerBart(), getContollerMarge())
            && tryToConnect(getContollerBart(), getContollerLisa())
            && tryToConnect(getContollerBart(), getContollerMaggie())
            && tryToConnect(getContollerMarge(), getContollerLisa())
            && tryToConnect(getContollerMarge(), getContollerMaggie())
            && tryToConnect(getContollerLisa(), getContollerMaggie());
    }

    protected Controller getContollerBart() {
        return getContoller(BART_ID);
    }

    protected Controller getContollerHomer() {
        return getContoller(HOMER_ID);
    }

    protected Controller getContollerMarge() {
        return getContoller(MARGE_ID);
    }

    protected Controller getContollerLisa() {
        return getContoller(LISA_ID);
    }

    protected Controller getContollerMaggie() {
        return getContoller(MAGGIE_ID);
    }

    protected Folder getFolderAtBart() {
        return getContollerBart().getFolderRepository().getFolder(testFolder);
    }

    protected Folder getFolderAtHomer() {
        return getContollerHomer().getFolderRepository().getFolder(testFolder);
    }

    protected Folder getFolderAtMarge() {
        return getContollerMarge().getFolderRepository().getFolder(testFolder);
    }

    protected Folder getFolderAtLisa() {
        return getContollerLisa().getFolderRepository().getFolder(testFolder);
    }

    protected Folder getFolderAtMaggie() {
        return getContollerMaggie().getFolderRepository().getFolder(testFolder);
    }

    /**
     * Sets the syncprofile on the testfolder of all simpsons.
     * 
     * @param profile
     *            the profile to set.
     */
    protected void setSyncProfile(SyncProfile profile) {
        getFolderAtHomer().setSyncProfile(profile);
        getFolderAtBart().setSyncProfile(profile);
        getFolderAtMarge().setSyncProfile(profile);
        getFolderAtLisa().setSyncProfile(profile);
        getFolderAtMaggie().setSyncProfile(profile);
    }

    /**
     * Lets the simpsons join all the same folder.
     * 
     * @param profile
     *            the profile to use
     */
    protected void joinTestFolder(SyncProfile profile) {
        Reject.ifTrue(testFolder != null, "Reject already setup a testfolder!");
        testFolder = new FolderInfo("testFolder", UUID.randomUUID().toString(),
            true);
        joinFolder(testFolder, TESTFOLDER_BASEDIR_BART, getContollerBart(),
            profile);
        joinFolder(testFolder, TESTFOLDER_BASEDIR_HOMER, getContollerHomer(),
            profile);
        joinFolder(testFolder, TESTFOLDER_BASEDIR_MARGE, getContollerMarge(),
            profile);
        joinFolder(testFolder, TESTFOLDER_BASEDIR_LISA, getContollerLisa(),
            profile);
        joinFolder(testFolder, TESTFOLDER_BASEDIR_MAGGIE, getContollerMaggie(),
            profile);
        try {
            // Give them time to join
            TestHelper.waitForCondition(5, new Condition() {
                public boolean reached() {
                    return getFolderAtBart().getMembersCount() >= 5
                        && getFolderAtHomer().getMembersCount() >= 5
                        && getFolderAtMarge().getMembersCount() >= 5
                        && getFolderAtLisa().getMembersCount() >= 5
                        && getFolderAtMaggie().getMembersCount() >= 5;
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Homer: "
                + getFolderAtHomer().getMembersCount() + ", Bart: "
                + getFolderAtBart().getMembersCount() + ", Marge: "
                + getFolderAtMarge().getMembersCount() + ", Lisa: "
                + getFolderAtLisa().getMembersCount() + ", Maggie: "
                + getFolderAtMaggie().getMembersCount() + ". Folder: "
                + testFolder + " id: " + testFolder.id);
        }
    }
}
