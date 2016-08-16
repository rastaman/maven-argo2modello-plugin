package com.ubikproducts.maven.plugins.argo2modello;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.argouml.model.Facade;
import org.argouml.support.GeneratorJava2;
import org.codehaus.modello.model.BaseElement;
import org.codehaus.modello.model.CodeSegment;
import org.codehaus.modello.model.ModelField;
import org.jdom.Element;

public class UmlExtractor {

    private final Facade facade;

    private final TypesRepository typesRepository;
    
    private final Object umlObject;

    private final Map<String, String> taggedValues;

    private final Map<String, String> attributes;

    private final Set<String> annotations;

    private Map<String, ModelField> fields = new LinkedHashMap<String, ModelField>();

    private GeneratorJava2 generator = new GeneratorJava2();

    private static Set<String> RESERVED_WORDS = new HashSet<String>(
            Arrays.asList(new String[] { "documentation", "model.id", "primaryKey", "version" }));

    public static String DEFAULT_VERSION = "1.0.0";

    private BaseElement modelElement;
    
    public UmlExtractor(BaseElement modelElement, Object umlObject, Facade facade, TypesRepository typesRepository) {
        this.taggedValues = new HashMap<String, String>();
        this.attributes = new HashMap<String, String>();
        this.typesRepository = typesRepository;
        this.facade = facade;
        this.umlObject = umlObject;
        this.annotations = new LinkedHashSet<String>();
    }

    public BaseElement build() {
        Iterator<Object> it = facade.getTaggedValues(umlObject);
        while (it.hasNext()) {
            Object tag = it.next().toString();
            String name = facade.getTag(tag).trim();
            // Object td = facade.getTagDefinition(tag);
            // Object type = facade.getType(tag);
            Object value = facade.getValueOfTag(tag).toString().trim();
            if (!name.startsWith("@")) {
                annotations.add(getJavaAnnotation(name, value.toString()));
            } else if (!RESERVED_WORDS.contains(name)) {
                attributes.put(name, value.toString());
            }
            taggedValues.put(name, value.toString());
        }
        return modelElement;
    }

    public String getName() {
        return facade.getName(umlObject);
    }

    private String getJavaAnnotation(String name, String value) {
        if ("".equals(value)) {
            return name;
        }
        return name + "(" + value + ")";
    }

    public Set<String> getAnnotations() {
        return annotations;
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getDescription() {
        return taggedValues.get("documentation");
    }

    public String getId() {
        return taggedValues.get("model.id");
    }

    public String getIdentifier() {
        return taggedValues.get("primaryKey");
    }

    public String getVersion() {
        if (!taggedValues.containsKey("version")) {
            return DEFAULT_VERSION;
        }
        String version = taggedValues.get("version");
        return VersionExtractor.getNormalizedVersion(StringUtils.strip(version));
    }

    public static class VersionExtractor {

        public static String getNormalizedVersion(String version) {
            String[] splittedVersion = StringUtils.split(version, ".");
            String majorString = splittedVersion[0];
            String minorString = "0";
            String microString = "0";
            if (splittedVersion.length > 1) {
                minorString = splittedVersion[1];
                if (splittedVersion.length > 2) {
                    microString = splittedVersion[2];
                }
            }
            short major = Short.parseShort(majorString);
            short minor = Short.parseShort(minorString);
            short micro = Short.parseShort(microString);
            return major + "." + minor + "." + micro;
        }
    }

    public String getPackage() {
        Object ns = facade.getNamespace(umlObject);
        return getPackageName(ns);
    }

    public ModelField getModelField(String name, Object attr) {
        ModelField modelField = new ModelField();
        modelField.setName(name);
/*
        if (facade.getType(attr) != null) {
            
             // <association> <type>ContentTest</type>
            // <multiplicity>1</multiplicity> </association>
            String type = facade.getName(facade.getType(attr)).trim();
            if (!typesRepository.isKnown(type) ) {
                modelField.setType(type);
            }
            else {
                modelField.
                modelField.Element monoAssoc = addElement(elemField, "association");
                addElement(monoAssoc, "type", type);
                addElement(monoAssoc, "multiplicity", "1");
            }
        } else {
            log.info("Cannot add type of attr " + facade.getName(attr) + " with no type for " + attr);
        }
        // if not default case
        addVisibility(attr, elemField);
        addTaggedValues(attr, elemField);
*/
        return modelField;
    }

    public Set<String> getFields() {
        fields.clear();
        Iterator jt = facade.getStructuralFeatures(umlObject).iterator();
        while (jt.hasNext()) {
            Object attr = jt.next();
            String name = facade.getName(attr);
            ModelField modelField = getModelField(name,attr);
            fields.put(name.toUpperCase(), modelField);
        }
        return fields.keySet();
    }

    private Set<CodeSegment> getCodeSegments() {
        String clazzName = facade.getName(umlObject);
        Collection bFeatures = facade.getOperations(umlObject);
        Set<CodeSegment> codeSegments = new LinkedHashSet<CodeSegment>();
        if (!bFeatures.isEmpty()) {
            for (Object behavioralFeature : bFeatures) {
                String name = facade.getName(behavioralFeature);
                boolean getter = false;
                boolean setter = false;
                if (name.startsWith("get")) {
                    name = name.substring("get".length());
                    getter = true;
                } else if (name.startsWith("set")) {
                    name = name.substring("set".length());
                    setter = true;
                } else if (name.startsWith("is")) {
                    name = name.substring("is".length());
                    getter = true;
                }
                if (getter || setter) {
                    if (fields != null && getFields().contains(name.toUpperCase())) {
                       /* List<Element> fieldsList = elemClazz.getChild("fields").getChildren();
                        for (Element e : fieldsList) {
                            if (name.toUpperCase().equals(e.getChild("name").getText().toUpperCase())) {
                                if (setter)
                                    e.setAttribute("java.setter", "false");
                                else
                                    e.setAttribute("java.getter", "false");
                            }
                        }*/
                    }
                    // continue;
                }
                StringBuffer sb = new StringBuffer();
                sb.append(GeneratorJava2.INDENT);
                sb.append(generator.generateOperation(behavioralFeature, false));

                String tv = generator.generateTaggedValues(behavioralFeature);

                if ((facade.isAClass(umlObject)) && (facade.isAOperation(behavioralFeature))
                        && (!facade.isAbstract(behavioralFeature))) {
                    sb.append(' ');
                    sb.append('{');

                    if (tv.length() > 0) {
                        sb.append(GeneratorJava2.LINE_SEPARATOR).append(GeneratorJava2.INDENT).append(tv);
                    }

                    // there is no ReturnType in behavioral feature (UML)
                    sb.append(GeneratorJava2.LINE_SEPARATOR);
                    sb.append(generator.generateMethodBody(behavioralFeature));
                    sb.append(GeneratorJava2.INDENT);
                    sb.append("}").append(GeneratorJava2.LINE_SEPARATOR);
                } else {
                    sb.append(";").append(GeneratorJava2.LINE_SEPARATOR);
                    if (tv.length() > 0) {
                        sb.append(GeneratorJava2.INDENT).append(tv).append(GeneratorJava2.LINE_SEPARATOR);
                    }
                }
//                Element operation = addElement(operations, "codeSegment");
                //addTaggedValues(behavioralFeature, operation);
                Element methodBody = null;//addElement(operation, "code");
                methodBody.setText(sb.toString());
            }
        }

        /*
         * Facade facade = Model.getFacade(); String clazzName =
         * facade.getName(cls); // add operations // TODO: constructors
         * Collection bFeatures = facade.getOperations(cls);
         * 
         * if (!bFeatures.isEmpty()) { String tv; Element operations =
         * addElement(elemClazz, "codeSegments"); for (Object behavioralFeature
         * : bFeatures) { String name = facade.getName(behavioralFeature);
         * boolean getter = false; boolean setter = false; if
         * (name.startsWith("get")) { name = name.substring("get".length());
         * getter = true; } else if (name.startsWith("set")) { name =
         * name.substring("set".length()); setter = true; } else if
         * (name.startsWith("is")) { name = name.substring("is".length());
         * getter = true; } if (getter || setter) { Set<String> fields =
         * fieldsForClasses.get(getFullName(cls)); if (fields != null &&
         * fields.contains(name.toUpperCase())) { List<Element> fieldsList =
         * elemClazz.getChild("fields").getChildren(); for (Element e :
         * fieldsList) { if
         * (name.toUpperCase().equals(e.getChild("name").getText().toUpperCase()
         * )) { if (setter) e.setAttribute("java.setter", "false"); else
         * e.setAttribute("java.getter", "false"); } } } // continue; }
         * StringBuffer sb = new StringBuffer();
         * sb.append(GeneratorJava2.INDENT);
         * sb.append(generator.generateOperation(behavioralFeature, false));
         * 
         * tv = generator.generateTaggedValues(behavioralFeature);
         * 
         * if ((facade.isAClass(cls)) &&
         * (facade.isAOperation(behavioralFeature)) &&
         * (!facade.isAbstract(behavioralFeature))) { sb.append(' ');
         * sb.append('{');
         * 
         * if (tv.length() > 0) {
         * sb.append(GeneratorJava2.LINE_SEPARATOR).append(GeneratorJava2.INDENT
         * ).append(tv); }
         * 
         * // there is no ReturnType in behavioral feature (UML)
         * sb.append(GeneratorJava2.LINE_SEPARATOR);
         * sb.append(generator.generateMethodBody(behavioralFeature));
         * sb.append(GeneratorJava2.INDENT);
         * sb.append("}").append(GeneratorJava2.LINE_SEPARATOR); } else {
         * sb.append(";").append(GeneratorJava2.LINE_SEPARATOR); if (tv.length()
         * > 0) {
         * sb.append(GeneratorJava2.INDENT).append(tv).append(GeneratorJava2.
         * LINE_SEPARATOR); } } Element operation = addElement(operations,
         * "codeSegment"); addTaggedValues(behavioralFeature, operation);
         * Element methodBody = addElement(operation, "code");
         * methodBody.setText(sb.toString()); } }
         */
        CodeSegment codeSegment = new CodeSegment();

        /*
         * while ( parser.nextTag() == XmlPullParser.START_TAG ) { if (
         * parseBaseElement( codeSegment, parser ) ) { } else if (
         * "code".equals( parser.getName() ) ) { codeSegment.setCode(
         * parser.nextText() ); } else { parser.nextText(); } }
         */

        //modelInterface.addCodeSegment(codeSegment);
        return null;
    }

    /**
     * Gets the Java package name for a given namespace, ignoring the root
     * namespace (which is the model).
     * 
     * @param namespace
     *            the namespace
     * @return the Java package name
     */
    private String getPackageName(Object namespace) {
        if (namespace == null || !facade.isANamespace(namespace) || facade.getNamespace(namespace) == null) {
            return "";
        }
        String packagePath = facade.getName(namespace);
        if (packagePath == null) {
            return "";
        }
        while ((namespace = facade.getNamespace(namespace)) != null) {
            // ommit root package name; it's the model's root
            if (facade.getNamespace(namespace) != null) {
                packagePath = facade.getName(namespace) + '.' + packagePath;
            }
        }
        return packagePath;
    }

    private String getFullName(Object cls) {
        return getPackageName(facade.getNamespace(cls)) + '.' + facade.getName(cls);
    }

    public String getInheritance() {
        if (!facade.getGeneralizations(umlObject).isEmpty()) {
            Object gen = facade.getGeneralizations(umlObject).iterator().next();
            Object parent = facade.getGeneral(gen);
            String parentName = facade.getName(parent);
            if (facade.isAInterface(umlObject)) {
                // log.info("Set superinterface to " + parentName + " (" +
                // parent + ")");
                return parentName;
            } else if (facade.isAClass(umlObject)) {
                // log.info("Set superclass to " + parentName + " (" + parent +
                // ")");
                return parentName;
            }
        }
        return null;
    }
}