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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import org.argouml.profile.internal.ProfileManagerImpl;
import org.argouml.support.ArgoUMLStarter;
import org.argouml.support.GeneratorJava2;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format.TextMode;

/**
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
     * @parameter expression="src/main/uml/model.uml"
     * @required
     */
    private File sourceModel;

    /**
     * Location of the Java Profile for ArgoUML, if not shipped in the ArgoUML main JAR.
     * 
     * @parameter expression="src/main/profiles/default-java.xmi"
     */
    private File javaProfile;

    /**
     * Force the generation of the Modello model. Else the modello model is regenerated only when the UML file has
     * changed (last modification time has changed).
     * 
     * @parameter expression=false
     */
    private boolean force;

    /**
     * Default imports to set in Modello model. Used for instance to add the packages for annotations.
     * 
     * @parameter
     */
    private String defaultImports;

    // temp caches
    private Map interfacesMap = new HashMap();

    private Map classesMap = new HashMap();

    private GeneratorJava2 generator = new GeneratorJava2();

    private Logger log = Logger.getLogger( Argo2ModelloMojo.class );

    public void execute()
        throws MojoExecutionException
    {

        if ( !sourceModel.exists() )
            throw new MojoExecutionException( "Source model '" + sourceModel.getAbsolutePath() + "' doesn't exist!" );

        if ( destinationModel.exists() && !force)
        {
            try
            {
                Document doc = new SAXBuilder().build( destinationModel );
                String oldLastModified = doc.getRootElement().getAttributeValue( "uml.lastModified" );
                String newLastModified = "" + sourceModel.lastModified();
                if ( oldLastModified != null && oldLastModified.equals( newLastModified ) )
                    return;
            }
            catch ( JDOMException e )
            {
                throw new MojoExecutionException( "Error reading current destination model '"
                    + destinationModel.getAbsolutePath() + "': " + e.getMessage() );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error reading current destination model '"
                    + destinationModel.getAbsolutePath() + "': " + e.getMessage() );
            }
        }

        if ( !Model.isInitiated() )
        {
            ArgoUMLStarter.initializeMDR();
        }

        // Always force reinitialization of Profile subsystem
        ProfileManagerImpl profileManagerImpl = new org.argouml.profile.internal.ProfileManagerImpl();
        if ( javaProfile != null )
        {
            String profileDir = javaProfile.getAbsolutePath();
            profileManagerImpl.addSearchPathDirectory( profileDir );
            profileManagerImpl.refreshRegisteredProfiles();
        }
        ProfileFacade.setManager( profileManagerImpl );

        new InitNotation().init();
        new InitNotationUml().init();
        new InitNotationJava().init();

        ProjectFilePersister persister =
            PersistenceManager.getInstance().getPersisterFromFileName( sourceModel.getAbsolutePath() );

        Project p = null;
        try
        {
            p = persister.doLoad( sourceModel );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Cannot load model '" + sourceModel.getAbsolutePath() + "': "
                + e.getMessage(), e );
        }
        Object m = p.getUserDefinedModelList().iterator().next();
        Facade facade = Model.getFacade();
        Document doc = new Document();
        Element rootElement = new Element( "model" );
        rootElement.setAttribute( "uml.lastModified", "" + sourceModel.lastModified() );

        doc.setRootElement( rootElement );
        addTaggedValues( m, rootElement );
        addElement( rootElement, "name", facade.getName( m ) );
        Element defaults = addElement( rootElement, "defaults" );
        if ( defaultImports != null && !"".equals( defaultImports ) )
        {
            Element defaultImportsElement = addElement( defaults, "default" );
            addElement( defaultImportsElement, "key", "defaultImports" );
            addElement( defaultImportsElement, "value", defaultImports );
        }
        Element pkgDef = addElement( defaults, "default" );
        addElement( pkgDef, "key", "package" );
        boolean seenPkg = false;
        Iterator it = Model.getCoreHelper().getAllInterfaces( m ).iterator();
        // interfaces
        Element interfaces = addElement( rootElement, "interfaces" );
        while ( it.hasNext() )
        {
            Object inf = it.next();
            if ( !seenPkg )
            {
                seenPkg = true;
                addElement( pkgDef, "value", facade.getName( facade.getNamespace( inf ) ) );
            }
            Element elemInf = addElement( interfaces, "interface" );
            String infName = facade.getName( inf );
            addElement( elemInf, "name", infName );
            addPackage( inf, elemInf );
            addTaggedValues( inf, elemInf );
            addOperations( inf, elemInf );
            addInheritance( inf, elemInf );
        }
        // classes
        Element classes = addElement( rootElement, "classes" );
        it = Model.getCoreHelper().getAllClasses( m ).iterator();
        while ( it.hasNext() )
        {
            Object clazz = it.next();
            if ( !seenPkg )
            {
                seenPkg = true;
                addElement( pkgDef, "value", facade.getName( facade.getNamespace( clazz ) ) );
            }
            String clazzName = facade.getName( clazz );
            Element elemClazz = addElement( classes, "class" );
            addElement( elemClazz, "name", clazzName );
            addPackage( clazz, elemClazz );
            addTaggedValues( clazz, elemClazz );
            addInterfaces( clazz, elemClazz );
            addInheritance( clazz, elemClazz );
            addFields( clazz, elemClazz );
            addAssociations( clazz, elemClazz );
            addOperations( clazz, elemClazz );
        }
        XMLOutputter outputter = new XMLOutputter();
        Format f = Format.getPrettyFormat();
        f.setIndent( "  " );
        f.setEncoding( "UTF-8" );
        f.setLineSeparator( "\n" );
        f.setTextMode( TextMode.PRESERVE );
        outputter.setFormat( f );
        // outputter.getFormat().setLineSeparator( "" );
        try
        {
            outputter.output( doc, new FileWriter( destinationModel ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot write model '" + destinationModel.getAbsolutePath() + "': "
                + e.getMessage(), e );
        }
    }

    // XML Content generation

    private Element addElement( Element e, String n )
    {
        return addElement( e, n, null );
    }

    private Element addElement( Element e, String n, String s )
    {
        Facade facade = Model.getFacade();
        Element child = new Element( n );
        if ( s != null )
            child.setText( s );
        if ( e == null )
            System.out.println( "oups" );
        e.addContent( child );
        return child;
    }

    public void addPackage( Object cls, Element elemClazz )
    {
        Facade f = Model.getFacade();
        Object ns = f.getNamespace( cls );
        String pkg = getPackageName( ns );
        addElement( elemClazz, "packageName", pkg );
    }

    public void addInheritance( Object clazz, Element elemClazz )
    {
        Facade facade = Model.getFacade();
        if ( !facade.getGeneralizations( clazz ).isEmpty() )
        {
            Object gen = facade.getGeneralizations( clazz ).iterator().next();
            Object parent = facade.getGeneral( gen );
            String parentName = facade.getName( parent );
            if ( facade.isAInterface( clazz ) )
            {
                log.info( "Set superinterface to " + parentName + " (" + parent + ")" );
                addElement( elemClazz, "superInterface", parentName );
            }
            else if ( facade.isAClass( clazz ) )
            {
                log.info( "Set superclass to " + parentName + " (" + parent + ")" );
                addElement( elemClazz, "superClass", parentName );
            }
        }
    }

    public void addFields( Object clazz, Element elemClazz )
    {
        Facade facade = Model.getFacade();
        Collection fieldsUml = facade.getStructuralFeatures( clazz );
        if ( !fieldsUml.isEmpty() )
        {
            Element fields = addElement( elemClazz, "fields" );
            Iterator jt = fieldsUml.iterator();
            while ( jt.hasNext() )
            {
                Object attr = jt.next();
                Element elemField = addElement( fields, "field" );
                addElement( elemField, "name", facade.getName( attr ) );
                if ( facade.getType( attr ) != null ) {
                    log.info( "Add " + facade.getName( attr ) + " with " + facade.getName( facade.getType( attr ) ) );
                    addElement( elemField, "type", facade.getName( facade.getType( attr ) ) );
                } else {
                    log.info( "Cannot add attr " + facade.getName( attr ) + " with no type for " + attr );                	
                }
                // if not default case
                addVisibility( attr, elemField );
                addTaggedValues( attr, elemField );
            }
        }
    }

    public void addVisibility( Object obj, Element elem )
    {
        Facade facade = Model.getFacade();
        if ( !facade.isPrivate( obj ) )
        {
            if ( facade.isProtected( obj ) )
            {
                addElement( elem, "modifier", "protected" );
            }
            else if ( facade.isPublic( obj ) )
            {
                addElement( elem, "modifier", "public" );
            }
            else if ( facade.isPackage( obj ) )
            {
                addElement( elem, "modifier", "package" );
            }
        }
        // by default objects are private (but they are public in ArgoUML - take care to fields)
    }

    public void addInterfaces( Object cls, Element elemClazz )
    {
        Collection realizations = Model.getFacade().getSpecifications( cls );
        if ( !realizations.isEmpty() )
        {
            Element interfaces = addElement( elemClazz, "interfaces" );
            Iterator clsEnum = realizations.iterator();
            while ( clsEnum.hasNext() )
            {
                Object inter = clsEnum.next();
                addElement( interfaces, "interface", getPackageName( inter ) );
            }
        }
    }

    public void addOperations( Object cls, Element elemClazz )
    {

        // add operations
        // TODO: constructors
        Collection bFeatures = Model.getFacade().getOperations( cls );

        if ( !bFeatures.isEmpty() )
        {
            String tv;
            Element operations = addElement( elemClazz, "codeSegments" );
            for ( Object behavioralFeature : bFeatures )
            {
                StringBuffer sb = new StringBuffer();
                sb.append( GeneratorJava2.INDENT );
                sb.append( generator.generateOperation( behavioralFeature, false ) );

                tv = generator.generateTaggedValues( behavioralFeature );

                if ( ( Model.getFacade().isAClass( cls ) ) && ( Model.getFacade().isAOperation( behavioralFeature ) )
                    && ( !Model.getFacade().isAbstract( behavioralFeature ) ) )
                {
                    sb.append( ' ' );
                    sb.append( '{' );

                    if ( tv.length() > 0 )
                    {
                        sb.append( GeneratorJava2.LINE_SEPARATOR ).append( GeneratorJava2.INDENT ).append( tv );
                    }

                    // there is no ReturnType in behavioral feature (UML)
                    sb.append( GeneratorJava2.LINE_SEPARATOR );
                    sb.append( generator.generateMethodBody( behavioralFeature ) );
                    sb.append( GeneratorJava2.INDENT );
                    sb.append( "}" ).append( GeneratorJava2.LINE_SEPARATOR );
                }
                else
                {
                    sb.append( ";" ).append( GeneratorJava2.LINE_SEPARATOR );
                    if ( tv.length() > 0 )
                    {
                        sb.append( GeneratorJava2.INDENT ).append( tv ).append( GeneratorJava2.LINE_SEPARATOR );
                    }
                }
                Element operation = addElement( operations, "codeSegment" );
                addTaggedValues( behavioralFeature, operation );
                Element methodBody = addElement( operation, "code" );
                methodBody.setText( sb.toString() );
            }
        }
    }

    public void addAssociations( Object clazz, Element elemClazz )
    {
        Facade facade = Model.getFacade();
        // add attributes implementing associations
        Collection ends = facade.getAssociationEnds( clazz );
        if ( !ends.isEmpty() )
        {
            Element fields = elemClazz.getChild( "fields" );
            if ( fields == null )
                fields = addElement( elemClazz, "fields" );
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
                Element elemField = addElement( fields, "field" );
                addElement( elemField, "name", endName );
                addTaggedValues( otherAssociationEnd, elemField );
                Element assoc = addElement( elemField, "association" );
                addElement( assoc, "type", typeName );
                if ( multiplicity.indexOf( "*" ) > -1 )
                {
                    addElement( assoc, "multiplicity", "*" );
                }
                else
                {
                    addElement( assoc, "multiplicity", "1" );
                }
                // move the annotations to the association
                if ( elemField.getChild( "annotations" ) != null )
                {
                    Element annotations = (Element) elemField.getChild( "annotations" ).clone();
                    assoc.addContent( annotations );
                    elemField.removeChild( "annotations" );
                }
            }
        }
        else
        {
            log.info( "No association ends for '" + facade.getName( clazz ) + "'" );
        }
    }

    private void addTaggedValues( Object o, Element e )
    {
        Facade facade = Model.getFacade();
        Object tag;
        String name;
        boolean seenVersion = false;
        Iterator it = facade.getTaggedValues( o );
        Element annotations = new Element( "annotations" );
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
            else if ( "@".equals( name.substring( 0, 1 ) ) )
            {
                String content =
                    "".equals( facade.getValueOfTag( tag ) ) ? name : name + "(" + facade.getValueOfTag( tag ) + ")";
                addElement( annotations, "annotation", content );
            }
            else
            {
                e.setAttribute( name, facade.getValueOfTag( tag ) );
            }
        }
        if ( annotations.getChildren().size() > 0 )
            e.addContent( annotations );
        if ( !seenVersion )
            addElement( e, "version", "1.0.0" );
    }

    // ArgoUML initialization

    /**
     * Gets the Java package name for a given namespace, ignoring the root namespace (which is the model).
     * 
     * @param namespace the namespace
     * @return the Java package name
     */
    public String getPackageName( Object namespace )
    {
        if ( namespace == null || !Model.getFacade().isANamespace( namespace )
            || Model.getFacade().getNamespace( namespace ) == null )
        {
            return "";
        }
        String packagePath = Model.getFacade().getName( namespace );
        if ( packagePath == null )
        {
            return "";
        }
        while ( ( namespace = Model.getFacade().getNamespace( namespace ) ) != null )
        {
            // ommit root package name; it's the model's root
            if ( Model.getFacade().getNamespace( namespace ) != null )
            {
                packagePath = Model.getFacade().getName( namespace ) + '.' + packagePath;
            }
        }
        return packagePath;
    }

    /**
     * @return the destinationModel
     */
    public File getDestinationModel()
    {
        return destinationModel;
    }

    /**
     * @param destinationModel the destinationModel to set
     */
    public void setDestinationModel( File destinationModel )
    {
        this.destinationModel = destinationModel;
    }

    /**
     * @return the sourceModel
     */
    public File getSourceModel()
    {
        return sourceModel;
    }

    /**
     * @param sourceModel the sourceModel to set
     */
    public void setSourceModel( File sourceModel )
    {
        this.sourceModel = sourceModel;
    }

    /**
     * @return the javaProfile
     */
    public File getJavaProfile()
    {
        return javaProfile;
    }

    /**
     * @param javaProfile the javaProfile to set
     */
    public void setJavaProfile( File javaProfile )
    {
        this.javaProfile = javaProfile;
    }

    /**
     * @return the defaultImports
     */
    public String getDefaultImports()
    {
        return defaultImports;
    }

    /**
     * @param defaultImports the defaultImports to set
     */
    public void setDefaultImports( String defaultImports )
    {
        this.defaultImports = defaultImports;
    }

    /**
     * @return the force
     */
    public boolean isForce()
    {
        return force;
    }

    /**
     * @param force the force to set
     */
    public void setForce( boolean force )
    {
        this.force = force;
    }
}
