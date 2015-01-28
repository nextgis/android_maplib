
/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;


public class FileUtil
{

    public static boolean isIntegerParseInt(String str)
    {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }


    public static void writeToFile(
            File filePath,
            String sData)
            throws IOException
    {
        FileOutputStream os = new FileOutputStream(filePath, false);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(os);
        outputStreamWriter.write(sData);
        outputStreamWriter.close();
    }


    public static String readFromFile(File filePath)
            throws IOException
    {

        String ret = "";

        FileInputStream inputStream = new FileInputStream(filePath);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String receiveString;
        StringBuilder stringBuilder = new StringBuilder();

        while ((receiveString = bufferedReader.readLine()) != null) {
            stringBuilder.append(receiveString);
        }

        inputStream.close();
        ret = stringBuilder.toString();

        return ret;
    }


    public static synchronized void createDir(File dir)
    {
        if (dir.exists()) {
            return;
        }
        if (!dir.mkdirs()) {
            throw new RuntimeException("Can not create dir " + dir);
        }
    }


    public static boolean deleteRecursive(File fileOrDirectory)
    {
        boolean isOK = true;

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                isOK = deleteRecursive(child) && isOK;
            }
        }

        return fileOrDirectory.delete() && isOK;
    }


    public static void copyStream(
            InputStream is,
            OutputStream os,
            byte[] buffer,
            int bufferSize)
            throws IOException
    {
        for (; ; ) {
            int count = is.read(buffer, 0, bufferSize);
            if (count == -1) {
                break;
            }
            os.write(buffer, 0, count);
        }
    }

    public static String getFileNameByUri(final Context context, Uri uri, String defaultName)
    {
        String fileName = defaultName;
        try {
            if (uri.getScheme().compareTo("content") == 0) {
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor.moveToFirst()) {
                    int column_index = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    fileName = cursor.getString(column_index);
                }
            } else if (uri.getScheme().compareTo("file") == 0) {
                fileName = uri.getLastPathSegment();
            } else {
                fileName = fileName + "_" + uri.getLastPathSegment();
            }
        }
        catch (Exception e){
            //do nothing, only return default file name;
            Log.d(Constants.TAG, e.getLocalizedMessage());
        }
        return fileName;
    }
}
