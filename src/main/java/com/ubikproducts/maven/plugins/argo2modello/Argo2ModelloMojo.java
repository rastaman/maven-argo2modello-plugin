package com.ubikproducts.maven.plugins.argo2modello;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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

/**
 * Goal which touches a timestamp file.
 * 
 * @goal generate-modello
 * @phase process-sources
 */
public class Argo2ModelloMojo
    extends AbstractMojo
{
    /**
     * Location of the file.
     * 
     * @parameter expression="src/main/mdo/model.mdo"
     * @required
     */
    private File destinationModel;

    /**
     * Location of the file.
     * 
     * @parameter expression="src/main/uml/model.xmi"
     * @required
     */
    private File sourceModel;

    /**
     * The default implementation to start.
     */
    private static final String DEFAULT_MODEL_IMPLEMENTATION = "org.argouml.model.mdr.MDRModelImplementation";

    private Logger log = Logger.getLogger( Argo2ModelloMojo.class );

    public void execute()
        throws MojoExecutionException
    {
        //
        initModel();
        ProfileFacade.setManager( new org.argouml.profile.internal.ProfileManagerImpl() );

        ProjectFilePersister persister =
            PersistenceManager.getInstance().getPersisterFromFileName( sourceModel.getAbsolutePath() );
        new InitNotation().init();
        new InitNotationUml().init();
        new InitNotationJava().init();
        Project p = null;
        try
        {
            p = persister.doLoad( sourceModel );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Cannot load model '" + sourceModel.getAbsolutePath() + "': " +
                e.getMessage(), e );
        }
        Object m = p.getUserDefinedModelList().iterator().next();
        Facade facade = Model.getFacade();
        Document doc = new Document();
        Element rootElement = new Element( "model" );
        doc.setRootElement( rootElement );
        addTaggedValuesAsAttributes( rootElement, m );
        addElement( rootElement, "name", facade.getName( m ) );
        Element defaults = new Element( "defaults" );
        rootElement.addContent( defaults );
        Element pkgDef = new Element( "default" );
        addElement( pkgDef, "key", "package" );
        defaults.addContent( pkgDef );
        boolean seenPkg = false;
        Element classes = new Element( "classes" );
        rootElement.addContent( classes );
        Iterator it = Model.getCoreHelper().getAllClasses( m ).iterator();
        while ( it.hasNext() )
        {
            Object clazz = it.next();
            if ( !seenPkg )
            {
                seenPkg = true;
                addElement( pkgDef, "value", facade.getName( facade.getNamespace( clazz ) ) );
            }
            String clazzName = facade.getName( clazz );
            Element elemClazz = new Element( "class" );
            addElement( elemClazz, "name", clazzName );
            addTaggedValuesAsAttributes( elemClazz, clazz );
            classes.addContent( elemClazz );
            if ( !facade.getGeneralizations( clazz ).isEmpty() )
            {
                Object gen = facade.getGeneralizations( clazz ).iterator().next();
                Object parent = facade.getGeneral( gen );
                String parentName = facade.getName( parent );
                log.info( "Set superclass to " + parentName + " (" + parent + ")" );
/*
      <superClass>MavenBaseObject</superClass>
      <isInternalSuperClass>true</isInternalSuperClass>      
 */
                addElement( elemClazz, "superClass", parentName );
                if (facade.getName( facade.getNamespace( clazz ) ).equals( 
                                                                          facade.getName( facade.getNamespace( parent ) )))
                    addElement( elemClazz, "isInternalSuperClass", "true" );
                else
                    addElement( elemClazz, "isInternalSuperClass", "false" );                    
            }
            Element fields = new Element( "fields" );
            elemClazz.addContent( fields );
            Iterator jt = facade.getStructuralFeatures( clazz ).iterator();
            while ( jt.hasNext() )
            {
                Object attr = jt.next();
                Element elemField = new Element( "field" );
                addElement( elemField, "name", facade.getName( attr ) );
                log.info( "Add " + facade.getName( attr ) + " with " + facade.getName( facade.getType( attr ) ) );
                if ( facade.getType( attr ) != null )
                    addElement( elemField, "type", facade.getName( facade.getType( attr ) ) );
                addTaggedValuesAsAttributes( elemField, attr );
                fields.addContent( elemField );
            }
            // add attributes implementing associations
            Collection ends = facade.getAssociationEnds( clazz );
            if ( !ends.isEmpty() )
            {
                for ( Object associationEnd : ends )
                {
                    Object association = facade.getAssociation( associationEnd );
                    Object otherAssociationEnd = facade.getNextEnd( associationEnd );
                    if ( !facade.isNavigable( otherAssociationEnd ) )
                        continue;
                    String endName = facade.getName( otherAssociationEnd );
                    String typeName = facade.getName( facade.getType( otherAssociationEnd ) );
                    if ( "".equals( endName ) || endName == null )
                    {
                        endName = typeName;
                    }
                    String multiplicity = facade.getName( facade.getMultiplicity( otherAssociationEnd ) );
                    Element elemField = new Element( "field" );
                    addElement( elemField, "name", endName );
                    addTaggedValuesAsAttributes( elemField, otherAssociationEnd );
                    Element assoc = new Element( "association" );
                    addTaggedValuesAsAttributes( assoc, otherAssociationEnd );
                    addElement( assoc, "type", typeName );
                    if ( multiplicity.indexOf( "*" ) > -1 )
                    {
                        addElement( assoc, "multiplicity", "*" );
                    }
                    else
                    {
                        addElement( assoc, "multiplicity", "1" );
                    }
                    elemField.addContent( assoc );
                    fields.addContent( elemField );
                }
            }
            else
            {
                log.info( "No association ends for '" + facade.getName( clazz ) + "'" );
            }
        }
        XMLOutputter outputter = new XMLOutputter();
        Format f = Format.getPrettyFormat();
        f.setIndent( "  " );
        f.setEncoding( "UTF-8" );
        outputter.setFormat( f );
        try
        {
            outputter.output( doc, new FileWriter( destinationModel ) );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    private void addElement( Element e, String n, String s )
    {
        Facade facade = Model.getFacade();
        Element child = new Element( n );
        child.setText( s );
        e.addContent( child );
    }

    private void addTaggedValuesAsAttributes( Element e, Object o )
    {
        Facade facade = Model.getFacade();
        Object tag;
        String name;
        boolean seenVersion = false;
        Iterator it = facade.getTaggedValues( o );
        while ( it.hasNext() )
        {
            tag = it.next();
            name = facade.getTag( tag );
            if ( "documentation".equals( name ) )
            {
                addElement( e, "description", facade.getValueOfTag( tag ) );
            }
            else if ( "primaryKey".equals( name ) )
            {
                addElement( e, "identifier", facade.getValueOfTag( tag ) );
            }
            else if ( "model.id".equals( name ) )
            {
                addElement( e, "id", facade.getValueOfTag( tag ) );
            }
            else if ( "version".equals( name ) )
            {
                seenVersion = true;
                addElement( e, "version", facade.getValueOfTag( tag ) );
            }
            else
            {
                e.setAttribute( name, facade.getValueOfTag( tag ) );
            }
        }
        if ( !seenVersion )
            addElement( e, "version", "1.0.0" );
    }

    /**
     * Initialise the UMl model repository.
     */
    private void initModel()
    {
        String className = System.getProperty( "argouml.model.implementation", DEFAULT_MODEL_IMPLEMENTATION );
        Throwable ret = Model.initialise( className );
    }
}
