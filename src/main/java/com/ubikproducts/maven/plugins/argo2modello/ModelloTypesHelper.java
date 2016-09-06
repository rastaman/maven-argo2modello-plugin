package com.ubikproducts.maven.plugins.argo2modello;

import org.codehaus.modello.model.ModelDefault;

public class ModelloTypesHelper {

    public static final String BOOLEAN = "boolean";
    public static final String BYTE = "byte";
    public static final String CHAR = "char";
    public static final String DOUBLE = "double";
    public static final String FLOAT = "float";
    public static final String INT = "int";
    public static final String LONG = "long";
    public static final String SHORT = "short";

    public static boolean isBaseType(String type) {
        if (type == null)
            return false;
        if (isPrimitive(type))
            return true;
        if (type.indexOf('<') > -1)
            type = type.substring(0, type.indexOf('<'));
        if ("String".equals(type))
            return true;
        if (!type.startsWith("java.util."))
            type = "java.util." + type;
        if (ModelDefault.SET.equals(type) || ModelDefault.MAP.equals(type) || ModelDefault.LIST.equals(type)
                || ModelDefault.PROPERTIES.equals(type))
            return true;

        return false;
    }

    public static boolean isPrimitive(String type) {
        return ((BOOLEAN.equals(type)) || (BYTE.equals(type)) || (CHAR.equals(type)) || (DOUBLE.equals(type))
                || (FLOAT.equals(type)) || (INT.equals(type)) || (LONG.equals(type)) || (SHORT.equals(type)));
    } // -- isPrimitive
}
