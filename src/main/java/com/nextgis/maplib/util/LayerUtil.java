package com.nextgis.maplib.util;

import android.content.Context;

import com.nextgis.maplib.R;

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

        char [] forbiddenChars = {':', '@', '#', '%', '^', '&', '*', '!', '$', '(', ')'};

        String result = fieldName;
        for(char testChar : forbiddenChars) {
            result = result.replace(testChar, '_');
        }

        if(result.equals(Constants.FIELD_ID))
            return "_fixed_id";

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
}
