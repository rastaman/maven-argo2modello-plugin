package com.ubikproducts.maven.plugins.argo2modello;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.argouml.model.CoreHelper;
import org.argouml.model.Facade;

/*
 * Copyright (c) 2004, Codehaus.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.codehaus.modello.ModelloException;
import org.codehaus.modello.model.BaseElement;
import org.codehaus.modello.model.CodeSegment;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelAssociation;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelDefault;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.model.ModelInterface;
import org.codehaus.modello.model.VersionDefinition;
import org.codehaus.modello.model.VersionRange;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse</a>
 */
public class ModelReader extends AbstractModelloGenerator {
    private Map<String, String> modelAttributes = new HashMap<String, String>();

    private Map<String, Map<String, String>> classAttributes = new HashMap<String, Map<String, String>>();

    private Map<String, Map<String, String>> interfaceAttributes = new HashMap<String, Map<String, String>>();

    private Map<String, Map<String, String>> fieldAttributes = new HashMap<String, Map<String, String>>();

    private Map<String, Map<String, String>> associationAttributes = new HashMap<String, Map<String, String>>();

    private Map<String, String> modelDefaults = new HashMap<String, String>();

    private Logger log = Logger.getLogger(ModelReader.class);

    private Facade facade;

    private CoreHelper coreHelper;

    private ArgoUMLDriver driver;

    private TypesRepository typesRepository;

    private ExclusionsRepository exclusionsRepository;

    public ModelReader() {

    }

    public ModelReader withDriver(ArgoUMLDriver driver) {
        this.driver = driver;
        this.facade = driver.getFacade();
        this.coreHelper = driver.getCoreHelper();
        return this;
    }

    public ModelReader withTypesRepository(TypesRepository typesRepository) {
        this.typesRepository = typesRepository;
        return this;
    }

    public ModelReader withModelDefault(String key, String value) {
        this.modelDefaults.put(key, value);
        return this;
    }

    public ModelReader withExclusionsRepository(ExclusionsRepository exclusionsRepository) {
        this.exclusionsRepository = exclusionsRepository;
        return this;
    }

    public Map<String, String> getAttributesForModel() {
        return modelAttributes;
    }

    public Map<String, String> getAttributesForClass(ModelClass modelClass) {
        return classAttributes.get(modelClass.getName());
    }

    public Map<String, String> getAttributesForInterface(ModelInterface modelInterface) {
        return interfaceAttributes.get(modelInterface.getName());
    }

    public Map<String, String> getAttributesForField(ModelField modelField) {
        return fieldAttributes.get(
                modelField.getModelClass().getName() + ':' + modelField.getName() + ':' + modelField.getVersionRange());
    }

    public Map<String, String> getAttributesForAssociation(ModelAssociation modelAssociation) {
        return associationAttributes.get(modelAssociation.getModelClass().getName() + ':' + modelAssociation.getName()
                + ':' + modelAssociation.getVersionRange());
    }

    public Model loadModel(Object umlModel) throws ModelloException {
        try {
            Model model = new Model();

            parseModel(model, umlModel);

            return model;
        } catch (IOException ex) {
            throw new ModelloException("Error parsing the model.", ex);
        }
    }

    public void parseModel(Model model, Object umlModel) throws IOException {
        UmlExtractor ue = UmlExtractor.of(umlModel, typesRepository);
        if ( ue.getId() != null) {
            model.setId(ue.getId());
        }
        if (!modelDefaults.isEmpty()) {
            parseDefaults(model, umlModel);
        }
        parseInterfaces(model,umlModel);
        parseClasses( model, umlModel );
        //    modelAttributes = getAttributes( parser );
    }

    private void parseDefaults(Model model, Object umlModel) throws IOException {
        for ( String k : modelDefaults.keySet() ) {
            ModelDefault modelDefault = new ModelDefault();
            modelDefault.setKey(k);
            modelDefault.setValue(modelDefaults.get(k));
            model.addDefault(modelDefault);
        }
    }

//    private ModelDefault buildModelDefault(String key, String value) {
//        
//    }
    private void parseVersionDefinition(Model model, XmlPullParser parser) throws XmlPullParserException, IOException {
        if ("versionDefinition".equals(parser.getName())) {
            VersionDefinition versionDefinition = new VersionDefinition();

            while (parser.nextTag() == XmlPullParser.START_TAG) {
                if ("type".equals(parser.getName())) {
                    versionDefinition.setType(parser.nextText());
                } else if ("value".equals(parser.getName())) {
                    versionDefinition.setValue(parser.nextText());
                } else {
                    parser.nextText();
                }
            }

            model.setVersionDefinition(versionDefinition);
        }
    }

    private void parseInterfaces(Model model, Object umlModel) throws IOException {
        Iterator<Object> it = driver.getCoreHelper().getAllInterfaces(umlModel).iterator();
        while (it.hasNext()) {
            Object inf = it.next();
            ModelInterface modelInterface = new ModelInterface(model, null);
            parseBaseElement(modelInterface, inf);
            
            if (!exclusionsRepository.isExcluded(modelInterface.getName())) {
                model.addInterface(modelInterface);

                UmlExtractor ue = UmlExtractor.of(inf, typesRepository);
                if ( ue.getPackage()!=null) {
                    modelInterface.setPackageName(ue.getPackage());
                }
                if ( ue.getInheritance()!=null) {
                    modelInterface.setSuperInterface(ue.getInheritance());
                }
                Map<String, String> attributes = getAttributes(inf);
                interfaceAttributes.put(modelInterface.getName(), attributes);
                //TODO: parseCodeSegment(modelInterface, inf);
            }
        }
    }

    private void parseClasses(Model model, Object umlModel) throws IOException {
        Iterator<Object> it = driver.getCoreHelper().getAllClasses(umlModel).iterator();
        while(it.hasNext()) {
            Object umlClass = it.next();
            ModelClass modelClass = new ModelClass(model, null);
            parseBaseElement(modelClass, umlClass);
            if (!exclusionsRepository.isExcluded(modelClass.getName())
                    && exclusionsRepository.isIncluded(modelClass.getName())) {
                UmlExtractor ue = UmlExtractor.of(umlClass, typesRepository);
                if ( ue.getPackage()!=null) {
                    modelClass.setPackageName(ue.getPackage());
                }
                if ( ue.getInheritance()!=null) {
                    modelClass.setSuperClass(ue.getInheritance());
                }

                parseClassInterfaces(modelClass, umlClass);
                parseFields(modelClass, umlClass);
                //parseCodeSegment(modelClass, umlClass);
                //TODO: Operations
                model.addClass(modelClass);
                Map<String, String> attributes = getAttributes(umlClass);
                classAttributes.put(modelClass.getName(), attributes);
            }
        }
    }

    private void parseClassInterfaces(ModelClass modelClass, Object umlClass)
            throws IOException {
        Collection realizations = facade.getSpecifications(umlClass);
        if (!realizations.isEmpty()) {
            Iterator clsEnum = realizations.iterator();
            while (clsEnum.hasNext()) {
                Object inter = clsEnum.next();
                String modelInterface = UmlExtractor.getPackageName(inter);
                modelClass.addInterface(modelInterface);
            }
        }
    }

    private void parseFields(ModelClass modelClass, Object umlClass) throws IOException {
        String clazzName = UmlExtractor.getFullName(umlClass);
        log.info("Adding fields to " + clazzName);
        Collection fieldsUml = facade.getStructuralFeatures(umlClass);
        if (!fieldsUml.isEmpty()) {
            Iterator jt = fieldsUml.iterator();
            while (jt.hasNext()) {
                Object umlField = jt.next();
                ModelField modelField = new ModelField();
                ModelAssociation modelAssociation = null;
                Map<String, String> fAttributes = getAttributes(umlField);
                Map<String, String> aAttributes = new HashMap<String, String>();
                parseBaseElement(modelField, umlField);

                UmlExtractor ue = UmlExtractor.of(umlField, typesRepository);
                String type = ue.getType();
                if (type != null) {
                    aAttributes = getAttributes(umlField);
                    modelAssociation = parseAssociation(umlField);
                    log.info("Add field " + facade.getName(umlField) + " with type " + type);
                    //modelField.setType(type);
                    modelField.setAlias(ue.getAlias());
                    modelField.setDefaultValue(ue.getDefaultValue());
                    modelField.setTypeValidator(ue.getTypeValidator());
                    modelField.setRequired(ue.isRequired());
                    // if ( !allClasses.containsKey(type) ||
                    // ModelDefault.isBaseType(type))
                    
                    if (ModelloTypesHelper.isBaseType(type)) {
                        // simple field
                        modelField.setType(type);
                        //modelField.setModifiers(ue.getModifiers());
                        modelClass.addField(modelField);
                    } else if (modelAssociation != null) {
                        // association (and complex types :-( )

                        //modelAssociation.setModifiers(ue.getModifiers());

                        // Base element
                        modelAssociation.setName(modelField.getName());

                        modelAssociation.setDescription(modelField.getDescription());

                        modelAssociation.setVersionRange(modelField.getVersionRange());

                        modelAssociation.setComment(modelField.getComment());

                        modelAssociation.setAnnotations(modelField.getAnnotations());

                        // model field fields
                        modelAssociation.setType(modelField.getType());

                        modelAssociation.setAlias(modelField.getAlias());

                        modelAssociation.setDefaultValue(modelField.getDefaultValue());

                        modelAssociation.setTypeValidator(modelField.getTypeValidator());

                        modelAssociation.setRequired(modelField.isRequired());

                        modelAssociation.setIdentifier(modelField.isIdentifier());

                        if (modelAssociation.getName() != null) {
                            associationAttributes.put(modelClass.getName() + ":" + modelAssociation.getName() + ":"
                                    + modelAssociation.getVersionRange(), aAttributes);
                        }
                        modelClass.addField(modelAssociation);
                    } else {
                    }
                } else {
                    log.info("Cannot add type of attr " + modelField.getName() + " with no type for " + umlField);
                }
                if (modelField.getName() != null) {
                    fieldAttributes.put(
                            modelClass.getName() + ":" + modelField.getName() + ":" + modelField.getVersionRange(),
                            fAttributes);
                }
            }
        }
    }

    private ModelAssociation parseAssociation(Object umlObject) throws IOException {
        String type = UmlExtractor.getType(umlObject);
        if (!typesRepository.isKnown(type)) {
            return null;
        }
        ModelAssociation modelAssociation = new ModelAssociation();
        parseBaseElement(modelAssociation, umlObject);
        modelAssociation.setTo(type);
        String multiplicity = null;
        modelAssociation.setMultiplicity(multiplicity);
        return modelAssociation;
    }

    private void parseAssociations(ModelClass modelClass, Object umlClass) throws IOException {
        // add attributes implementing associations
        Collection ends = facade.getAssociationEnds(umlClass);
        if (!ends.isEmpty()) {
            for (Object associationEnd : ends) {
                Object association = facade.getAssociation(associationEnd);
                Object otherAssociationEnd = facade.getNextEnd(associationEnd);
                String otherEndName = facade.getName(associationEnd);
                String otherTypeName = facade.getName(facade.getType(associationEnd));
                String endName = facade.getName(otherAssociationEnd);
                String typeName = facade.getName(facade.getType(otherAssociationEnd));
                if (!facade.isNavigable(otherAssociationEnd))
                    continue;
                // TODO: Check strange code
                if ("".equals(endName) || endName == null) {
                    endName = StringUtils.uncapitalize(typeName);
                }
                ModelAssociation modelAssociation = new ModelAssociation();

                //addTaggedValues(otherAssociationEnd, elemField);
                parseBaseElement(modelAssociation, otherAssociationEnd);
                modelAssociation.setName(endName);
                modelAssociation.setType(typeName);

                String multiplicity = facade.getName(facade.getMultiplicity(otherAssociationEnd));
                if (multiplicity.indexOf("*") > -1) {
                    //addElement(assoc, "multiplicity", "*");
                    modelAssociation.setMultiplicity("*"/*multiplicity*/);
                } else {
                    //addElement(assoc, "multiplicity", "1");
                    modelAssociation.setMultiplicity("1"/*multiplicity*/);
                }
                // move the annotations to the association
                /*
                 * if ( elemField.getChild( "annotations" ) != null ) { Element
                 * annotations = (Element) elemField.getChild( "annotations"
                 * ).clone(); assoc.addContent( annotations );
                 * elemField.removeChild( "annotations" ); }
                 */
            }
        } else {
            log.info("No association ends for '" + facade.getName(umlClass) + "'");
        }
    }

    private void parseCodeSegment(ModelClass modelClass, XmlPullParser parser)
            throws XmlPullParserException, IOException {
        while (parser.nextTag() == XmlPullParser.START_TAG) {
            if ("codeSegment".equals(parser.getName())) {
                CodeSegment codeSegment = new CodeSegment();

                while (parser.nextTag() == XmlPullParser.START_TAG) {
                    if (parseBaseElement(codeSegment, parser)) {
                    } else if ("code".equals(parser.getName())) {
                        codeSegment.setCode(parser.nextText());
                    } else {
                        parser.nextText();
                    }
                }

                modelClass.addCodeSegment(codeSegment);
            } else {
                parser.next();
            }
        }
    }

    private void parseCodeSegment(ModelInterface modelInterface, XmlPullParser parser)
            throws XmlPullParserException, IOException {
        while (parser.nextTag() == XmlPullParser.START_TAG) {
            if ("codeSegment".equals(parser.getName())) {
                CodeSegment codeSegment = new CodeSegment();

                while (parser.nextTag() == XmlPullParser.START_TAG) {
                    if (parseBaseElement(codeSegment, parser)) {
                    } else if ("code".equals(parser.getName())) {
                        codeSegment.setCode(parser.nextText());
                    } else {
                        parser.nextText();
                    }
                }

                modelInterface.addCodeSegment(codeSegment);
            } else {
                parser.next();
            }
        }
    }

    private boolean parseBaseElement(BaseElement element, Object umlObject)
            throws IOException {

        UmlExtractor ue = UmlExtractor.of(umlObject, typesRepository);

        if (ue.getName()!=null) {
            element.setName(ue.getName());
        }
        if (ue.getDescription()!=null) {
            element.setDescription(ue.getDescription());
        }
        if (ue.getVersion()!=null) {
            element.setVersionRange(new VersionRange(ue.getVersion()));
        }
        if (ue.getComment()!=null) {
            element.setComment(ue.getComment());
        }
        if (!ue.getAnnotations().isEmpty()) {
            element.setAnnotations(new ArrayList<String>(ue.getAnnotations()));
        }
        return true;
    }

    private Map<String, String> getAttributes(Object umlObject) {
        Map<String, String> attributes = new HashMap<String, String>();
        /*
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String name = parser.getAttributeName(i);

            String value = parser.getAttributeValue(i);

            attributes.put(name, value);
        }
        */
        return attributes;
    }
}
