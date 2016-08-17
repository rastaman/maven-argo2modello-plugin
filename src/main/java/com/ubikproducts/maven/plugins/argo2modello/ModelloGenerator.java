package com.ubikproducts.maven.plugins.argo2modello;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.argouml.model.Facade;
import org.argouml.support.GeneratorJava2;
import org.codehaus.modello.model.BaseElement;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelAssociation;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelDefault;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.model.ModelInterface;

public class ModelloGenerator {

    private Map<String, String> modelAttributes = new HashMap<String, String>();

    private Map<String, Map<String, String>> classAttributes = new HashMap<String, Map<String, String>>();

    private Map<String, Map<String, String>> interfaceAttributes = new HashMap<String, Map<String, String>>();

    private Map<String, Map<String, String>> fieldAttributes = new HashMap<String, Map<String, String>>();

    private Map<String, Map<String, String>> associationAttributes = new HashMap<String, Map<String, String>>();

    private Map<String, String> modelDefaults = new HashMap<String, String>();

    private Logger log = Logger.getLogger(ModelloGenerator.class);

    private ArgoUMLDriver driver;

    private TypesRepository typesRepository;

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

    private Model model;

    private Object nativeModel;

    private ModelloGenerator() {
        model = new Model();
    }

    public Model generate() {
        if (nativeModel != null) {
            processModel(nativeModel, model);
        }
        return model;
    }

    private void processModel(Object umlModel, Model model) {
        UmlExtractor u = parseBaseElement(model, umlModel);
        // else if ( "interfaces".equals( parser.getName() ) )
        // parseInterfaces( model, parser );
        // else if ( "classes".equals( parser.getName() ) )
        // parseClasses( model, parser );

        // do it at the end when we have all the informations
        // top package name, default imports,...
        addModelDefaults(model);
    }

    private void parseInterfaces(Model m, Object umlModel) {
        Iterator<Object> it = driver.getCoreHelper().getAllInterfaces(m).iterator();
        while (it.hasNext()) {
            Object umlObject = it.next();
            ModelInterface modelInterface = new ModelInterface();
            UmlExtractor tve = parseBaseElement(modelInterface, umlObject);
            /*
             * if (!isExcluded(facade, inf)) { if (!seenPkg) { seenPkg = true;
             * packageName = facade.getName(facade.getNamespace(inf));
             * addElement(pkgDef, "value", packageName); } Element elemInf =
             * addElement(interfaces, "interface"); String infName =
             * facade.getName(inf); addElement(elemInf, "name", infName);
             * addPackage(inf, elemInf); addTaggedValues(inf, elemInf);
             * addOperations(inf, elemInf); addInheritance(inf, elemInf); }
             */
            if (tve.getInheritance() != null) {
                modelInterface.setSuperInterface(tve.getInheritance());
            }
            if (tve.getPackage() != null) {
                modelInterface.setPackageName(tve.getPackage());
            }
            // parseCodeSegment(modelInterface, parser);
            Map<String, String> attributes = tve.getAttributes();
            /*
             * while (parser.nextTag() == XmlPullParser.START_TAG) { } else if
             * ("codeSegments".equals(parser.getName())) { }
             */
            model.addInterface(modelInterface);
            interfaceAttributes.put(modelInterface.getName(), attributes);
        }
    }

    private void parseClasses(Model m, Object umlModel) {

    }

    private void addModelDefaults(Model m) {
        for (String s : modelDefaults.keySet()) {
            ModelDefault md = new ModelDefault();
            md.setKey(s);
            md.setValue(modelDefaults.get(s));
            m.addDefault(md);
        }
    }

    private UmlExtractor parseBaseElement(BaseElement element, Object umlObject) {
        Facade facade = driver.getFacade();
        String name = facade.getName(umlObject);
        if (name != null) {
            element.setName(name);
        }
        UmlExtractor tve = UmlExtractor.of(umlObject,typesRepository);
        tve.build();
        element.setAnnotations(new ArrayList<String>(tve.getAnnotations()));
        element.setDescription(tve.getDescription());
        element.setVersionRange(new org.codehaus.modello.model.VersionRange(tve.getVersion()));
        /*
         * else if ( "comment".equals( parser.getName() ) ) {
         * element.setComment( parser.nextText() ); }
         */
        return tve;
    }

    public static class ModelloGeneratorBuilder {

        private final ModelloGenerator modelloGenerator;

        private ModelloGeneratorBuilder() {
            this.modelloGenerator = new ModelloGenerator();
        }

        public ModelloGeneratorBuilder withNativeModel(Object nativeModel) {
            modelloGenerator.nativeModel = nativeModel;
            return this;
        }

        public ModelloGeneratorBuilder withModelDefaults(Map<String, String> modelDefaults) {
            modelloGenerator.modelDefaults = modelDefaults;
            return this;
        }

        public ModelloGeneratorBuilder withModelDefault(String key, String value) {
            modelloGenerator.modelDefaults.put(key, value);
            return this;
        }

        public ModelloGeneratorBuilder withArgoUMLDriver(ArgoUMLDriver driver) {
            modelloGenerator.driver = driver;
            return this;
        }

        public ModelloGeneratorBuilder withTypesRepository(TypesRepository typesRepository) {
            modelloGenerator.typesRepository = typesRepository;
            return this;
        }

        public static ModelloGeneratorBuilder newBuilder() {
            return new ModelloGeneratorBuilder();
        }

        public ModelloGenerator build() {
            return modelloGenerator;
        }
    }

    public Map<String, String> getModelDefaults() {
        return modelDefaults;
    }

    public void setModelDefaults(Map<String, String> modelDefaults) {
        this.modelDefaults = modelDefaults;
    }

}
