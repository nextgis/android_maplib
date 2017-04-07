/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
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

public class HttpResponse
{
    protected int    mResponseCode;
    protected String mResponseMessage;
    protected String mResponseBody;
    protected boolean mIsOk = false;

    public HttpResponse(int responseCode)
    {
        mResponseCode = responseCode;
    }

    public HttpResponse(
            int responseCode,
            String responseMessage)
    {
        mResponseCode = responseCode;
        mResponseMessage = responseMessage;
    }

    public HttpResponse(
            int responseCode,
            String responseMessage,
            String responseBody)
    {
        mResponseCode = responseCode;
        mResponseMessage = responseMessage;
        mResponseBody = responseBody;
    }

    public int getResponseCode()
    {
        return mResponseCode;
    }

    public void setResponseCode(int responseCode)
    {
        mResponseCode = responseCode;
    }

    public String getResponseMessage()
    {
        return mResponseMessage;
    }

    public void setResponseMessage(String responseMessage)
    {
        mResponseMessage = responseMessage;
    }

    public String getResponseBody()
    {
        return mResponseBody;
    }

    public void setResponseBody(String responseBody)
    {
        mResponseBody = responseBody;
    }

    public boolean isOk()
    {
        return mIsOk;
    }

    public void setOk(boolean ok)
    {
        mIsOk = ok;
    }
}
