/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.util;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncInfo;
import android.os.Build;
import com.nextgis.maplib.api.IGISApplication;


public class AccountUtil
{
    public static boolean isSyncActive(
            Account account,
            String authority)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return isSyncActiveHoneycomb(account, authority);
        } else {
            SyncInfo currentSync = ContentResolver.getCurrentSync();
            return currentSync != null && currentSync.account.equals(account) &&
                   currentSync.authority.equals(authority);
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static boolean isSyncActiveHoneycomb(
            Account account,
            String authority)
    {
        for (SyncInfo syncInfo : ContentResolver.getCurrentSyncs()) {
            if (syncInfo.account.equals(account) && syncInfo.authority.equals(authority)) {
                return true;
            }
        }
        return false;
    }


    public static AccountData getAccountData(
            Context context,
            String accountName)
    {
        IGISApplication app = (IGISApplication) context.getApplicationContext();
        Account account = app.getAccount(accountName);

        AccountData accountData = new AccountData();

        accountData.url = app.getAccountUrl(account);
        accountData.login = app.getAccountLogin(account);
        accountData.password = app.getAccountPassword(account);

        return accountData;
    }


    public static class AccountData
    {
        public String url;
        public String login;
        public String password;
    }
}
