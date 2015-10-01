package com.nextgis.maplib.util;

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
        return result;
    }
}
