/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.ubikproducts.maven.plugins.argo2modello;

import java.io.File;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.maven.plugin.MojoExecutionException;

public class ConvertProjectTest
    extends TestCase
{
    private String testModel = "users"; // users

    private File sourceModel = new File( "src/test/resources/uml/" + testModel + ".uml" );

    private File destinationModel = new File( "target/" + testModel + ".mdo" );

    private File javaProfile = new File( "src/main/profiles/default-java.xmi" );

    private String defaultImports = "javax.persistence.*,javax.xml.bind.annotation.*";

    /**
     * The default implementation to start.
     */
    private static final String DEFAULT_MODEL_IMPLEMENTATION = "org.argouml.model.mdr.MDRModelImplementation";

    private Logger log = Logger.getLogger( ConvertProjectTest.class );

    public void testConvert()
    {
        DOMConfigurator.configure( "src/test/resources/log4j.xml" );
        Argo2ModelloMojo plugin = new Argo2ModelloMojo();
        plugin.setDestinationModel( destinationModel );
        plugin.setSourceModel( sourceModel );
        plugin.setJavaProfile( javaProfile );
        plugin.setDefaultImports( defaultImports );
        try
        {
            plugin.execute();
        }
        catch ( MojoExecutionException e )
        {
            e.printStackTrace();
            fail( e.getMessage() );
        }
        assertTrue( destinationModel.exists() );
    }

}
