/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.disk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Profiling;
import de.dal33t.powerfolder.util.ProfilingEntry;
import de.dal33t.powerfolder.util.test.ControllerTestCase;

public abstract class FileInfoDAOTestCase extends ControllerTestCase {

    protected void testFindAll(FileInfoDAO dao, int n) {
        Collection<FileInfo> fInfos = new ArrayList<FileInfo>();
        for (int i = 0; i < n; i++) {
            fInfos.add(createRandomFileInfo(i, IdGenerator.makeId()));
        }
        dao.store(null, fInfos);
        fInfos.clear();
        for (int i = 0; i < n; i++) {
            fInfos.add(createRandomFileInfo(i, IdGenerator.makeId()));
        }
        dao.store("XXX", fInfos);
        fInfos.clear();
        for (int i = 0; i < n; i++) {
            fInfos.add(createRandomFileInfo(i, IdGenerator.makeId()));
        }
        dao.store("WWW", fInfos);

        assertEquals(n, dao.count(null, true, true));
        assertEquals(n, dao.count("XXX", true, true));
        assertEquals(n, dao.count("WWW", true, true));
        assertEquals(0, dao.count("123", true, true));

        assertEquals(n, dao.findAllFiles(null).size());
        assertEquals(n, dao.findAllFiles("XXX").size());
        assertEquals(n, dao.findAllFiles("WWW").size());
        assertEquals(0, dao.findAllFiles("123").size());

        assertEquals(n, dao.findAllFiles(null).size());
        assertEquals(n, dao.findAllFiles("XXX").size());
        assertEquals(n, dao.findAllFiles("WWW").size());
        assertEquals(0, dao.findAllFiles("123").size());
    }

    protected void testFindNewestVersion(FileInfoDAO dao) {
        FileInfo expected = createRandomFileInfo(10, "MyExcelsheet.xls");
        expected = version(expected, 1);
        dao.store(null, expected);

        FileInfo remote = version(expected, 0);
        dao.store("REMOTE1", remote);

        FileInfo remote2 = version(expected, 2);
        dao.store("REMOTE2", remote2);

        assertNotNull(dao.findNewestVersion(expected, ""));
        assertEquals(1, dao.findNewestVersion(expected, "").getVersion());
        assertNotNull(dao.findNewestVersion(expected, "REMOTE1", "REMOTE2",
            null));
        assertEquals(2,
            dao.findNewestVersion(expected, "REMOTE1", "REMOTE2", null)
                .getVersion());
        assertNotNull(dao.findNewestVersion(expected, "REMOTE1", "REMOTE2"));
        assertEquals(2, dao.findNewestVersion(expected, "REMOTE1", "REMOTE2")
            .getVersion());
    }

    protected void testIndexFileInfo(FileInfoDAO dao) {
        FileInfo expected = createRandomFileInfo(10, "MyExcelsheet.xls");
        dao.store(null, expected);
        FileInfo retrieved = dao.find(
            FileInfoFactory.lookupInstance(expected.getFolderInfo(),
                expected.getRelativeName()), null);
        assertNotNull("Retrieved FileInfo is null", retrieved);
        assertEquals(expected, retrieved);

        // Should overwrite
        dao.store(null, retrieved);

        assertEquals(1, dao.findAllFiles(null).size());
        assertEquals(1, dao.count(null, true, true));
    }

    protected void testFindInDir(FileInfoDAO dao, int n) {
        // Some random garbage content
        for (int i = 0; i < n; i++) {
            dao.store(null, createRandomFileInfo(i, "MyExcelsheet.xls"));
            dao.store(null,
                createRandomFileInfo(i, "A-RandomDirectory", i, true));
        }

        FileInfo dirInfo = createRandomFileInfo(0, "TheDirectory", 0, true);
        FileInfo dirInfo2 = createRandomFileInfo(0, "x", 0, true);
        dao.store(null, dirInfo);
        dao.store(null, dirInfo2);
        for (int i = 0; i < n; i++) {
            FileInfo fInfo = createRandomFileInfo(i, dirInfo.getFilenameOnly()
                + "/MyExcelsheet.xls");
            dao.store(null, fInfo);
            FileInfo fInfo2 = createRandomFileInfo(i,
                dirInfo2.getFilenameOnly() + "/MyExcelsheet.xls");
            dao.store(null, fInfo2);
        }

        int nItems = n * 4 + 2;
        int nFiles = n * 3;
        assertEquals(nItems, dao.count(null, true, false));
        assertEquals(nFiles, dao.count(null, false, false));

        assertEquals(0, dao.findInDirectory(null, (DirectoryInfo) null, false)
            .size());
        assertEquals(nItems, dao.findInDirectory(null, (String) null, true)
            .size());
        assertEquals(nItems, dao
            .findInDirectory(null, "subdir1/SUBDIR2/", true).size());
        assertEquals(n * 2 + 2,
            dao.findInDirectory(null, "subdir1/SUBDIR2/", false).size());
        assertEquals(n,
            dao.findInDirectory(null, (DirectoryInfo) dirInfo, false).size());
        //
        // Alexandria/
        // Alexandria/Alexandria
        // Alexandria/Alexandria.General
        //
        FileInfo alexSubdir = createFileInfo("Alexandria", 1, true);
        dao.store("ALEX", alexSubdir);
        dao.store("ALEX", createFileInfo("Alexandria/Alexandria", 1, true));
        dao.store("ALEX",
            createFileInfo("Alexandria/Alexandria.General", 1, true));
        dao.store("ALEX",
            createFileInfo("Alexandria/Alexandria.General/file.txt", 1, false));
        FileInfoCriteria crit = new FileInfoCriteria();
        crit.addDomain("ALEX");
        crit.setPath("");
        assertEquals(1, dao.findFiles(crit).size());

        crit = new FileInfoCriteria();
        crit.addDomain("ALEX");
        crit.setPath("Alexandria");
        assertEquals(2, dao.findFiles(crit).size());

        crit = new FileInfoCriteria();
        crit.addDomain("ALEX");
        crit.setPath("Alexandria/Alexandria");
        assertEquals("Found: " + dao.findFiles(crit), 0, dao.findFiles(crit)
            .size());
    }

    protected static FileInfo createFileInfo(String name, int version,
        boolean directory)
    {
        FolderInfo foInfo = createRandomFolderInfo();
        MemberInfo mInfo = new MemberInfo(IdGenerator.makeId(),
            IdGenerator.makeId(), IdGenerator.makeId());
        AccountInfo aInfo = new AccountInfo(IdGenerator.makeId(),
            IdGenerator.makeId());
        return FileInfoFactory.unmarshallExistingFile(foInfo, name, null, 100,
            mInfo, aInfo, new Date(), version, null, directory, null);
    }

    protected static FileInfo createRandomFileInfo(int n, String name,
        int version)
    {
        return createRandomFileInfo(n, name, version, false);
    }

    protected static FileInfo createRandomFileInfo(int n, String name,
        int version, boolean directory)
    {
        FolderInfo foInfo = createRandomFolderInfo();
        String fn = "subdir1/SUBDIR2/" + name + "-" + n;
        MemberInfo mInfo = new MemberInfo(IdGenerator.makeId(),
            IdGenerator.makeId(), IdGenerator.makeId());
        AccountInfo aInfo = new AccountInfo(IdGenerator.makeId(),
            IdGenerator.makeId());
        return FileInfoFactory.unmarshallExistingFile(foInfo, fn, null, n,
            mInfo, aInfo, new Date(), version, null, directory, null);
    }

    protected static FileInfo createRandomFileInfo(int n, String name) {
        return createRandomFileInfo(n, name, 0);
    }

    protected static FolderInfo createRandomFolderInfo() {
        FolderInfo foInfo = new FolderInfo("TestFolder / " + UUID.randomUUID(),
            "FOLDERID").intern();
        return foInfo;
    }

    protected static FileInfo version(FileInfo fInfo, int version) {
        return FileInfoFactory.unmarshallExistingFile(fInfo.getFolderInfo(),
            fInfo.getRelativeName(), fInfo.getOID(), fInfo.getSize(),
            fInfo.getModifiedBy(), fInfo.getModifiedByAccount(),
            fInfo.getModifiedDate(), version, fInfo.getHashes(),
            fInfo.isDiretory(), fInfo.getTags());
    }

    protected static FileInfo version(FileInfo fInfo, int version, Date modDate)
    {
        return FileInfoFactory.unmarshallExistingFile(fInfo.getFolderInfo(),
            fInfo.getRelativeName(), fInfo.getOID(), fInfo.getSize(),
            fInfo.getModifiedBy(), fInfo.getModifiedByAccount(), modDate,
            version, fInfo.getHashes(), fInfo.isDiretory(), fInfo.getTags());
    }
    
    protected void testAssertEquals(FileInfo fInfo, FileInfo copy) {
        // Test
        assertEquals(fInfo, copy);
        assertEquals(fInfo.getFolderInfo(), copy.getFolderInfo());
        assertEquals(fInfo.getRelativeName(), copy.getRelativeName());
        assertEquals(fInfo.getFilenameOnly(), copy.getFilenameOnly());
        assertEquals(fInfo.getExtension(), copy.getExtension());
        assertEquals(fInfo.getSize(), copy.getSize());
        assertEquals(fInfo.getModifiedBy(), copy.getModifiedBy());
        assertEquals(fInfo.getModifiedDate(), copy.getModifiedDate());
        assertEquals(fInfo.getVersion(), copy.getVersion());

        assertEquals(fInfo.getOID(), copy.getOID());
        assertEquals(fInfo.getHashes(), copy.getHashes());
        assertEquals(fInfo.getTags(), copy.getTags());
    }
}
