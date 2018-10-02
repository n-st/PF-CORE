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
 * $Id: OnlineStorageSubscription.java 13338 2010-08-11 13:48:33Z tot $
 */
package de.dal33t.powerfolder.security;

/**
 * Exception class for expired accounts
 *
 * @author Christoph Kappel <kappel@powerfolder.com>
 **/

public class AccountExpiredException extends AuthenticationFailedException {

    private static final long serialVersionUID = 100L;

    public AccountExpiredException() {
        super("Account expired");
    }

}