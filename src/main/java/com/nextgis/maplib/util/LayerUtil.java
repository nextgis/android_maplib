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

import android.content.Context;
import com.nextgis.maplib.R;

import java.util.List;


/**
 * Raster and vector utilities
 */
public class LayerUtil {
    public static boolean containsCaseInsensitive(
            String strToCompare,
            String[] list)
    {
        for (String str : list) {
            if (str.equalsIgnoreCase(strToCompare)) {
                return (true);
            }
        }
        return (false);
    }

    public static boolean isFieldNameValid(String fieldName){
        return !containsCaseInsensitive(fieldName, Constants.VECTOR_FORBIDDEN_FIELDS);
    }

    public static String normalizeFieldName(String fieldName) {
        char [] forbiddenChars = {':', '@', '#', '%', '^', '&', '*', '!', '$', '(', ')', '+', '-', '?', '=', '/', '\\', '"', '\'', '[', ']', ','};
        String result = fieldName;

        if (Character.isDigit(result.charAt(0)))
            result = "_" + result;

        for(char testChar : forbiddenChars)
            result = result.replace(testChar, '_');

        if(result.equals(Constants.FIELD_ID))
            return "_fixed_id";

        return result;
    }

    public static String normalizeLayerName(String layerName) {
        char [] forbiddenChars = {':', '<', '>', '*', '?', '/', '\\', '"', '|'};
        String result = layerName;

        if (result.charAt(layerName.length() - 1) == '.')
            result += "_";

        for (char testChar : forbiddenChars)
            result = result.replace(testChar, '_');

        return result;
    }

    public static int stringToType(String type)
    {
        switch (type) {
            case "STRING":
                return GeoConstants.FTString;
            case "INTEGER":
                return GeoConstants.FTInteger;
            case "REAL":
                return GeoConstants.FTReal;
            case "DATETIME":
                return GeoConstants.FTDateTime;
            case "DATE":
                return GeoConstants.FTDate;
            case "TIME":
                return GeoConstants.FTTime;
            default:
                return Constants.NOT_FOUND;
        }
    }

    public static String typeToString(int type) {
        switch (type) {
            case GeoConstants.FTString:
                return "STRING";
            case GeoConstants.FTInteger:
                return "INTEGER";
            case GeoConstants.FTReal:
                return "REAL";
            case GeoConstants.FTDateTime:
                return "DATETIME";
            case GeoConstants.FTDate:
                return "DATE";
            case GeoConstants.FTTime:
                return "TIME";
            default:
                return "";
        }
    }

    public static String typeToString(Context context, int type) {
        switch (type) {
            case GeoConstants.FTString:
                return context.getString(R.string.field_type_string);
            case GeoConstants.FTInteger:
                return context.getString(R.string.field_type_int);
            case GeoConstants.FTReal:
                return context.getString(R.string.field_type_real);
            case GeoConstants.FTDateTime:
                return context.getString(R.string.field_type_datetime);
            case GeoConstants.FTDate:
                return context.getString(R.string.field_type_date);
            case GeoConstants.FTTime:
                return context.getString(R.string.field_type_time);
            default:
                return "n/a";
        }
    }


    public static String getSelectionForIds(List<Long> ids)
    {
        if (null == ids) {
            return null;
        }

        StringBuilder sb = new StringBuilder(1024);

        for (Long id : ids) {
            if (sb.length() == 0) {
                sb.append(com.nextgis.maplib.util.Constants.FIELD_ID);
                sb.append(" IN (");
            } else {
                sb.append(",");
            }
            sb.append(id);
        }

        if (sb.length() > 0) {
            sb.append(")");
        }

        return sb.toString();
    }
}
