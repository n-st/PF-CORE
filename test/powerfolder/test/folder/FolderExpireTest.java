/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: FolderExpireTest.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.test.folder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

/**
 * This test checks that a FileInfo expires after a period if deleted.
 */
public class FolderExpireTest extends ControllerTestCase {

    @Override
    public void setUp() throws Exception {

        super.setUp();

        // Setup a test folder.
        setupTestFolder(SyncProfile.HOST_FILES);

        Path localBase = getFolder().getLocalBase();

        // This file should not be affected because it is not deleted.
        Path baseLineFile = localBase.resolve("baseLine.txt");
        if (Files.exists(baseLineFile)) {
            Files.delete(baseLineFile);
        }

        Files.createFile(baseLineFile);

        // This file should get removed from known files after expiry period.
        Path deletedFile = localBase.resolve("deleted.txt");
        if (Files.exists(deletedFile)) {
            Files.delete(deletedFile);
        }

        Files.createFile(deletedFile);

        // Speed things up!
        ConfigurationEntry.DB_MAINTENANCE_SECONDS
            .setValue(getController(), "5");

        scanFolder(getFolder());

    }

    /**
     * Test the file info gets deleted after expiry time.
     */
    public void testFolderExpire() throws IOException {

        // Start with two files...
        assertEquals(2, getFolder().getKnownItemCount());

        Path localBase = getFolder().getLocalBase();

        Path deletedFile = localBase.resolve("deleted.txt");
        Files.delete(deletedFile);

        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        scanFolder(getFolder());

        // Expiry time is default 3 months. Nothing changed.
        assertEquals(getFolder().getKnownItemCount(), 2);

        // Change expiry from 3 monthes to 10 seconds.
        ConfigurationEntry.MAX_FILEINFO_DELETED_AGE_SECONDS.setValue(
            getController(), 5);

        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        scanFolder(getFolder());

        // deleted.txt should be removed from known files.
        assertEquals(1, getFolder().getKnownItemCount());

        // Check: Look for:-
        // 'Successfully wrote folder database file (1 files)'
        // at end of output.

    }

}