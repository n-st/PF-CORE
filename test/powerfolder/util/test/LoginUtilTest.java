/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
 * $Id: Constants.java 11478 2010-02-01 15:25:42Z tot $
 */
package de.dal33t.powerfolder.util.test;

import java.util.Arrays;

import de.dal33t.powerfolder.util.Util;
import junit.framework.TestCase;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.LoginUtil;

public class LoginUtilTest extends TestCase {
    public void testObfuscate() {
        String password = "xC33öcn$k3444o$$44";
        String obf = LoginUtil.obfuscate(password.toCharArray());
        assertEquals(password.length(), LoginUtil.deobfuscate(obf).length);
        assertEquals(password, Util.toString(LoginUtil.deobfuscate(obf)));
        for (int i = 0; i < 200; i++) {
            password = IdGenerator.makeId();
            obf = LoginUtil.obfuscate(password.toCharArray());
            String deObf = Util.toString(LoginUtil.deobfuscate(obf));
            assertEquals(deObf, password.length(), deObf.length());
            assertEquals(password, deObf);
        }
        assertNull(LoginUtil.obfuscate(null));
        assertNull(LoginUtil.deobfuscate(null));
        assertTrue(Arrays.equals("".toCharArray(),
            LoginUtil.deobfuscate(LoginUtil.obfuscate("".toCharArray()))));;
        assertTrue(Arrays.equals("  ".toCharArray(),
            LoginUtil.deobfuscate(LoginUtil.obfuscate("  ".toCharArray()))));;

        password = "%$§\"&/(09€";
        obf = LoginUtil.obfuscate(password.toCharArray());
        assertEquals(password.length(), LoginUtil.deobfuscate(obf).length);
        assertEquals(password, Util.toString(LoginUtil.deobfuscate(obf)));

        password = "EsJs3XngawbCkMurIibtzQD23+OVPFjh2+uB4A8LaEA=";
        obf = LoginUtil.obfuscate(password.toCharArray());
        assertEquals(password.length(), LoginUtil.deobfuscate(obf).length);
        assertEquals(password, Util.toString(LoginUtil.deobfuscate(obf)));
    }

    public void testHash() {
        String password = IdGenerator.makeId();
        String hasedSalted = LoginUtil.hashAndSalt(password);
        assertTrue(LoginUtil.matches(password.toCharArray(), hasedSalted));
        assertFalse(LoginUtil.matches("test".toCharArray(), hasedSalted));
        assertFalse(LoginUtil.matches(null, hasedSalted));
        // Legacy support.
        assertTrue(LoginUtil.matches("XXX".toCharArray(), "XXX"));
    }

    public void testOTP() {
        // Valid
        for (int i = 0; i < 10000; i++) {
            String otp = LoginUtil.generateOTP(1000L);
            // 11BrLcYZedRqKqHhdy2sWhT2WCrNrxDEdSvDGgYDzCsFs58BRxYWG
            assertTrue(otp.length() >= 53);
            assertTrue(LoginUtil.isOTPValid(otp));
        }

        // Expired
        String otp = LoginUtil.generateOTP(500L);
        assertTrue(LoginUtil.isOTPValid(otp));
        TestHelper.waitMilliSeconds(600);
        assertFalse(LoginUtil.isOTPValid(otp));

        // Illegal stuff
        assertFalse(LoginUtil.isOTPValid(null));
        assertFalse(LoginUtil.isOTPValid("HACK"));
        assertFalse(LoginUtil
            .isOTPValid("30957s0cuxpcfeärl43#r3ä2ö43täö4eäföedäfgsdägösdägösäfdglsd08g7sa0g7w098470387"));
    }

    public void testPasswordPolicy() {
        assertFalse(LoginUtil.satisfiesUnixPolicy("12"));
        assertFalse(LoginUtil.satisfiesUnixPolicy("12345678"));
        assertFalse(LoginUtil.satisfiesUnixPolicy("ksjfdfgdgkjsrägklöjwerägjrägö100%&sdfsjföklsdj"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("aaa$56AAAA"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("aaa$56AA"));
        
        assertTrue(LoginUtil.satisfiesUnixPolicy("aaZZa44@"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("!2e4567B"));
        
        assertFalse(LoginUtil.satisfiesUnixPolicy("@!xxxx332445"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("@!xxXx332445"));
        
        assertFalse(LoginUtil.satisfiesUnixPolicy("abc123"));
        assertTrue(LoginUtil.satisfiesUnixPolicy("ABC123abc!"));
    }
}
