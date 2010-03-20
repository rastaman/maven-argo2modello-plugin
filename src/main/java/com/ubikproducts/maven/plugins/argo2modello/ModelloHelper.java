package com.ubikproducts.maven.plugins.argo2modello;

import org.codehaus.modello.model.ModelDefault;

public class ModelloHelper {

    public static boolean isBaseType( String type )
    {
        if ( type == null )
            return false;
        if ( type.indexOf( '<' ) > -1 )
            type = type.substring( 0, type.indexOf( '<' ) );
        if ( !type.startsWith( "java.util." ) )
            type = "java.util." + type;
        if ( ModelDefault.SET.equals( type ) || ModelDefault.MAP.equals( type ) || ModelDefault.LIST.equals( type ) || ModelDefault.PROPERTIES.equals( type ) )
            return true;
        return false;
    }
}
