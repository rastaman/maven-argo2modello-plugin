package com.ubikproducts.maven.plugins.argo2modello;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.jdom.Element;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse</a>
 */
public class ModelReader {
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
        //if ("versionDefinition".equals(parser.getName())) {
        //    parseVersionDefinition(model, parser);
        //}
        parseInterfaces(model,umlModel);
        //else if ( "classes".equals( parser.getName() ) )
        //{
        //    parseClasses( model, parser );
        //}
        parseClasses( model, umlModel );
        //else if ( "model".equals( parser.getName() ) )
        //{
        //    modelAttributes = getAttributes( parser );
        //}
        
    }

    private void parseDefaults(Model model, Object umlModel) throws IOException {
        for ( String k : modelDefaults.keySet() ) {
            ModelDefault modelDefault = new ModelDefault();
            modelDefault.setKey(k);
            modelDefault.setValue(modelDefaults.get(k));
            model.addDefault(modelDefault);
        }
    }

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
            ModelInterface modelInterface = new ModelInterface();
            model.addInterface(modelInterface);

            parseBaseElement(modelInterface, inf);

            UmlExtractor ue = UmlExtractor.of(inf, typesRepository);
            if ( ue.getPackage()!=null) {
                modelInterface.setPackageName(ue.getPackage());
            }
            if ( ue.getInheritance()!=null) {
                modelInterface.setSuperInterface(ue.getInheritance());
            }
            Map<String, String> attributes = getAttributes(inf);
            interfaceAttributes.put(modelInterface.getName(), attributes);
            parseCodeSegment(modelInterface, parser);
        }
    }

    private void parseClasses(Model model, Object umlModel) throws IOException {
        Iterator<Object> it = driver.getCoreHelper().getAllClasses(umlModel).iterator();
        while(it.hasNext()) {
            Object umlClass = it.next();
            ModelClass modelClass = new ModelClass();
            parseBaseElement(modelClass, umlClass);
            UmlExtractor ue = UmlExtractor.of(umlClass, typesRepository);
            if ( ue.getPackage()!=null) {
                modelClass.setPackageName(ue.getPackage());
            }
            if ( ue.getInheritance()!=null) {
                modelClass.setSuperClass(ue.getInheritance());
            }

            parseClassInterfaces(modelClass, umlClass);
            parseFields(modelClass, parser);
            parseCodeSegment(modelClass, parser);

            model.addClass(modelClass);
            Map<String, String> attributes = getAttributes(umlClass);
            classAttributes.put(modelClass.getName(), attributes);
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

    private void parseFields(ModelClass modelClass, Object umlClass) throws XmlPullParserException, IOException {
        String clazzName = UmlExtractor.getFullName(umlClass);
        Collection fieldsUml = facade.getStructuralFeatures(umlClass);
        if (!fieldsUml.isEmpty()) {
            Iterator jt = fieldsUml.iterator();
            while (jt.hasNext()) {
                Object attr = jt.next();
                ModelField modelField = new ModelField();
                ModelAssociation modelAssociation = null;
                Map<String, String> fAttributes = getAttributes(attr);
                Map<String, String> aAttributes = new HashMap<String, String>();
                parseBaseElement(modelField, attr);

                
                if (facade.getType(attr) != null) {
                    aAttributes = getAttributes(parser);
                    modelAssociation = parseAssociation(parser);
                    /*
                     * <association> <type>ContentTest</type>
                     * <multiplicity>1</multiplicity> </association>
                     */
                    String type = facade.getName(facade.getType(attr)).trim();
                    log.info("Add " + facade.getName(attr) + " with " + type);
                }
                if ("alias".equals(parser.getName())) {
                    modelField.setAlias(parser.nextText());
                }
                if ("type".equals(parser.getName())) {
                    modelField.setType(parser.nextText());
                }
                if ("defaultValue".equals(parser.getName())) {
                    modelField.setDefaultValue(parser.nextText());
                }
                if ("typeValidator".equals(parser.getName())) {
                    modelField.setTypeValidator(parser.nextText());
                }
                if ("required".equals(parser.getName())) {
                    modelField.setRequired(Boolean.valueOf(parser.nextText()));
                }
                if ("identifier".equals(parser.getName())) {
                    modelField.setIdentifier(Boolean.valueOf(parser.nextText()).booleanValue());
                }
                    // if ( !allClasses.containsKey(type) ||
                    // ModelDefault.isBaseType(type))
                    if (!allClasses.containsKey(type) || ModelloHelper.isBaseType(type))
                        addElement(elemField, "type", type);
                    else {
                        Element monoAssoc = addElement(elemField, "association");
                        addElement(monoAssoc, "type", type);
                        addElement(monoAssoc, "multiplicity", "1");
                    }
                } else {
                    log.info("Cannot add type of attr " + facade.getName(attr) + " with no type for " + attr);
                }
                // if not default case
                addVisibility(attr, elemField);
                addTaggedValues(attr, elemField);
            }
        }

        while (parser.nextTag() == XmlPullParser.START_TAG) {
            if ("field".equals(parser.getName())) {

                while (parser.nextTag() == XmlPullParser.START_TAG) {
                    } else if ("association".equals(parser.getName())) {
                    } else if ("alias".equals(parser.getName())) {
                        modelField.setAlias(parser.nextText());
                    } else if ("type".equals(parser.getName())) {
                        modelField.setType(parser.nextText());
                    } else if ("defaultValue".equals(parser.getName())) {
                        modelField.setDefaultValue(parser.nextText());
                    } else if ("typeValidator".equals(parser.getName())) {
                        modelField.setTypeValidator(parser.nextText());
                    } else if ("required".equals(parser.getName())) {
                        modelField.setRequired(Boolean.valueOf(parser.nextText()));
                    } else if ("identifier".equals(parser.getName())) {
                        modelField.setIdentifier(Boolean.valueOf(parser.nextText()).booleanValue());
                    } else {
                        parser.nextText();
                    }
                }

                if (modelField.getName() != null) {
                    fieldAttributes.put(
                            modelClass.getName() + ":" + modelField.getName() + ":" + modelField.getVersionRange(),
                            fAttributes);
                }

                if (modelAssociation != null) {
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
                    modelClass.addField(modelField);
                }
            } else {
                parser.next();
            }
        }
    }

    private ModelAssociation parseAssociation(XmlPullParser parser) throws XmlPullParserException, IOException {
        ModelAssociation modelAssociation = new ModelAssociation();

        while (parser.nextTag() == XmlPullParser.START_TAG) {
            if (parseBaseElement(modelAssociation, parser)) {
            } else if ("type".equals(parser.getName())) {
                modelAssociation.setTo(parser.nextText());
            } else if ("multiplicity".equals(parser.getName())) {
                modelAssociation.setMultiplicity(parser.nextText());
            } else {
                parser.nextText();
            }
        }

        return modelAssociation;
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
            throws XmlPullParserException, IOException {

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

    private Map<String, String> getAttributes(XmlPullParser parser) {
        Map<String, String> attributes = new HashMap<String, String>();

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String name = parser.getAttributeName(i);

            String value = parser.getAttributeValue(i);

            attributes.put(name, value);
        }

        return attributes;
    }
}
