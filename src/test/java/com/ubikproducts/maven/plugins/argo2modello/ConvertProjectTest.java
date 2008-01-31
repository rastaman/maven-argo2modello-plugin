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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.argouml.kernel.Project;
import org.argouml.model.Facade;
import org.argouml.model.Model;
import org.argouml.notation.InitNotation;
import org.argouml.notation.providers.java.InitNotationJava;
import org.argouml.notation.providers.uml.InitNotationUml;
import org.argouml.persistence.PersistenceManager;
import org.argouml.persistence.ProjectFilePersister;
import org.argouml.profile.ProfileFacade;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format.TextMode;

public class ConvertProjectTest
    extends TestCase
{
    private File sourceModel = new File("/Volumes/Datas/Workspaces/Effervens/paf/paf-model/src/main/uml/effervens-paf.zargo");
    
    private File destinationModel = new File("/Volumes/Datas/Workspaces/Effervens/paf/paf-model/src/main/mdo/effervens-paf.mdo");

    /**
     * The default implementation to start.
     */
    private static final String DEFAULT_MODEL_IMPLEMENTATION =
        "org.argouml.model.mdr.MDRModelImplementation";

    private Logger log = Logger.getLogger( ConvertProjectTest.class );
    
    public void testConvert() {
        DOMConfigurator.configure( "src/test/resources/log4j.xml" );
        //
        initModel();
        ProfileFacade.setManager(
                                 new org.argouml.profile.internal.ProfileManagerImpl());
        
        ProjectFilePersister persister =
            PersistenceManager.getInstance().getPersisterFromFileName( sourceModel.getAbsolutePath() );
        new InitNotation().init();
        new InitNotationUml().init();
        new InitNotationJava().init();
        Project p =null;
        try
        {
            p = persister.doLoad( sourceModel );
        }
        catch ( Exception e )
        {            
            //throw new MojoExecutionException("Cannot load model '"+sourceModel.getAbsolutePath()+"': "+e.getMessage(),e);
            e.printStackTrace();
            fail(e.getMessage());
        }       
        Object m = p.getUserDefinedModelList().iterator().next();
        Facade facade = Model.getFacade();
        assertNotNull( m);
        Document doc = new Document();
/*
<model xsd.namespace="http://www.ubik-products.com/xsd/paf-model/1.0"
    xsd.target-namespace="http://www.ubik-products.com/xsd/paf-model/1.0"
    jpox.table-prefix="paf_"
    jpox.mapping-in-package="true">
  <id>paf-model</id>
  <name>PafModel</name>
  <description>Effervens PAF Model</description>
  <version>1.0.0</version>
  <defaults>
    <default>
      <key>package</key>
      <value>com.effervens.paf.model</value>
    </default>
  </defaults>
  <classes>
 */
        Element rootElement = new Element("model");
        doc.setRootElement( rootElement );
        addTaggedValuesAsAttributes( rootElement, m );
        addElement( rootElement, "name", facade.getName( m ) );
        Element defaults = new Element("defaults");
        rootElement.addContent( defaults );
        Element pkgDef = new Element("default");
        addElement( pkgDef, "key", "package" );
        defaults.addContent( pkgDef );
        boolean seenPkg = false;
        Element classes = new Element("classes");
        rootElement.addContent( classes );
        Iterator it = Model.getCoreHelper().getAllClasses( m ).iterator();
        while (it.hasNext()) {
            Object clazz = it.next();
            if (!seenPkg) {
                seenPkg = true;
                addElement( pkgDef, "value", facade.getName( 
                                                            facade.getNamespace( clazz )));
            }
            String clazzName = facade.getName( clazz );
            Element elemClazz = new Element("class");
            addElement( elemClazz, "name", clazzName );
            addTaggedValuesAsAttributes( elemClazz, clazz );
            classes.addContent( elemClazz );
            if (!facade.getGeneralizations( clazz ).isEmpty()) {
                Object gen = facade.getGeneralizations( clazz ).iterator().next();         
                Object parent = facade.getGeneral( gen );
                String parentName = facade.getName( parent );
                log.info( "Set superclass to "+ parentName + " ("+parent+")");
                addElement( elemClazz, "extend", parentName );
            }
            Element fields = new Element("fields");
            elemClazz.addContent( fields );
            Iterator jt = facade.getStructuralFeatures( clazz ).iterator();
            while (jt.hasNext()) {
                Object attr = jt.next();
                Element elemField = new Element("field");
                addElement( elemField, "name", facade.getName( attr ) );
                log.info( "Add "+ facade.getName( attr )+" with "+facade.getName( facade.getType( attr )));
                if (facade.getType( attr )!=null)
                    addElement( elemField, "type", facade.getName( facade.getType( attr )) );
                addTaggedValuesAsAttributes( elemField, attr );
                fields.addContent( elemField );
            }
            // add attributes implementing associations
            Collection ends = facade.getAssociationEnds(clazz);
            if (!ends.isEmpty()) {
                for (Object associationEnd : ends) {
                    Object association = facade.getAssociation(associationEnd);
                    Object otherAssociationEnd = facade.getNextEnd( associationEnd );
                    if (!facade.isNavigable( otherAssociationEnd ))
                        continue;
                    String endName = facade.getName( otherAssociationEnd );
                    String typeName = facade.getName( facade.getType( otherAssociationEnd ) );
                    if ("".equals( endName ) || endName == null) {
                        endName = typeName;
                    }
                    String multiplicity = facade.getName( facade.getMultiplicity( otherAssociationEnd ));
                    Element elemField = new Element("field");
                    addElement( elemField, "name", endName );
                    addTaggedValuesAsAttributes( elemField, otherAssociationEnd );
                    Element assoc = new Element("association");
                    addElement( assoc, "type", typeName );
                    if (multiplicity.indexOf( "*" )>-1) {
                        addElement( assoc, "multiplicity", "*" );
                    } else {
                        addElement( assoc, "multiplicity", "1" );                            
                    }
                    elemField.addContent( assoc );
                    fields.addContent( elemField );
                }
            } else {
                log.info( "No association ends for '" + facade.getName( clazz )+"'");
            }
        }
        XMLOutputter outputter = new XMLOutputter();
        Format f = Format.getPrettyFormat();
        f.setIndent( "  " );
        f.setEncoding( "UTF-8" );
        outputter.setFormat( f );        
        //outputter.getFormat().setLineSeparator( "" );
       try
       {
           outputter.output( doc, new FileWriter(destinationModel) );
       }
           catch ( IOException e )
       {
               e.printStackTrace();
       }
    }
    
    private void addElement(Element e, String n, String s) {
        Facade facade = Model.getFacade();
        Element child = new Element(n);
        child.setText( s );
        e.addContent( child );
    }
    
    private void addTaggedValuesAsAttributes(Element e, Object o) {
        Facade facade = Model.getFacade();
        Object tag;
        String name;
        boolean seenVersion = false;
        Iterator it = facade.getTaggedValues( o );
        while (it.hasNext()) {
            tag = it.next();
            name = facade.getTag( tag );
            if ("documentation".equals( name )) {
                addElement( e, "description", facade.getValueOfTag( tag ) );
            } else if ("primaryKey".equals( name )) {
                addElement( e, "identifier", facade.getValueOfTag( tag ) );                
            } else if ("model.id".equals( name )) {
                addElement( e, "id", facade.getValueOfTag( tag ) );                                
            } else if ("version".equals( name )) {
                seenVersion = true;
                addElement( e, "version", facade.getValueOfTag( tag ) );
            } else {
                e.setAttribute( name, facade.getValueOfTag( tag ) );
            }
        }
        if (!seenVersion)
            addElement( e, "version", "1.0.0" );            
    }
    
    /**
    Gets the Java package name for a given namespace,
    ignoring the root namespace (which is the model).

    @param namespace the namespace
    @return the Java package name
 */
 public String getPackageName(Object namespace) {
     if (namespace == null
     || !Model.getFacade().isANamespace(namespace)
     || Model.getFacade().getNamespace(namespace) == null) {
         return "";
     }
     String packagePath = Model.getFacade().getName(namespace);
     if (packagePath == null) {
         return "";
     }
     while ((namespace = Model.getFacade().getNamespace(namespace))
             != null) {
         // ommit root package name; it's the model's root
         if (Model.getFacade().getNamespace(namespace) != null) {
             packagePath =
         Model.getFacade().getName(namespace) + '.' + packagePath;
         }
     }
     return packagePath;
 }    
    /**
     * Initialise the UMl model repository.
     */
    private void initModel() {
        String className = System.getProperty(
                "argouml.model.implementation",
                DEFAULT_MODEL_IMPLEMENTATION);
        Throwable ret = Model.initialise(className);
    }
}
