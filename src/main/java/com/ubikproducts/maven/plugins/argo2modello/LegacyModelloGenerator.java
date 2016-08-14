package com.ubikproducts.maven.plugins.argo2modello;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.argouml.model.Facade;
import org.argouml.model.Model;
import org.argouml.support.GeneratorJava2;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.Format.TextMode;

import com.ubikproducts.maven.plugins.argo2modello.ExclusionsRepository.ExclusionsRepositoryBuilder;

import org.jdom.output.XMLOutputter;

public class LegacyModelloGenerator {

    private Map<String, Object> interfacesMap = new HashMap<String, Object>();

    private Map<String, Object> classesMap = new HashMap<String, Object>();

    private GeneratorJava2 generator = new GeneratorJava2();

    private Map<String, Set<Object>> allClasses;

    private String packageName;

    private String defaultImports;

    private List<TaggedValueHandler> taggedValuesHandlers = new ArrayList<TaggedValueHandler>();

    private File sourceModel;

    private Logger log = Logger.getLogger(LegacyModelloGenerator.class);

    private final Facade facade;
    
    private ExclusionsRepository exclusionsRepository = ExclusionsRepositoryBuilder.newBuilder()
            .build();

    private LegacyModelloGenerator() {
        facade = Model.getFacade();
    }

    public void generate(Object m,File destination) throws IOException {
        Document doc = new Document();
        Element rootElement = new Element("model");
        rootElement.setAttribute("uml.lastModified", "" + sourceModel.lastModified());

        doc.setRootElement(rootElement);
        addTaggedValues(m, rootElement);
        addElement(rootElement, "name", facade.getName(m));
        Element defaults = addElement(rootElement, "defaults");
        if (defaultImports != null && !"".equals(defaultImports)) {
            Element defaultImportsElement = addElement(defaults, "default");
            addElement(defaultImportsElement, "key", "defaultImports");
            addElement(defaultImportsElement, "value", defaultImports);
        }
        Element pkgDef = addElement(defaults, "default");
        addElement(pkgDef, "key", "package");
        boolean seenPkg = false;
        Iterator<Object> it = Model.getCoreHelper().getAllInterfaces(m).iterator();
        // interfaces
        Element interfaces = addElement(rootElement, "interfaces");
        while (it.hasNext()) {
            Object inf = it.next();
            if (!isExcluded(facade, inf)) {
                if (!seenPkg) {
                    seenPkg = true;
                    packageName = facade.getName(facade.getNamespace(inf));
                    addElement(pkgDef, "value", packageName);
                }
                Element elemInf = addElement(interfaces, "interface");
                String infName = facade.getName(inf);
                addElement(elemInf, "name", infName);
                addPackage(inf, elemInf);
                addTaggedValues(inf, elemInf);
                addOperations(inf, elemInf);
                addInheritance(inf, elemInf);
            }
        }
        // classes
        // 1st pass - collection types names
        it = Model.getCoreHelper().getAllClasses(m).iterator();
        allClasses = new HashMap<String, Set<Object>>();
        while (it.hasNext()) {
            Object clazz = it.next();
            String name = facade.getName(clazz);
            if (!isExcluded(facade, clazz)) {
                addToStore(name, clazz);
            }
        }
        Element classes = addElement(rootElement, "classes");
        it = Model.getCoreHelper().getAllClasses(m).iterator();
        Map<String, List<Object>> classesCounter = new HashMap<String, List<Object>>();
        while (it.hasNext()) {
            Object clazz = it.next();
            String pkgName = getPackageName(facade.getNamespace(clazz));
            if (pkgName.startsWith("java.") || isExcluded(facade, clazz))
                continue;
            if (!seenPkg) {
                seenPkg = true;
                packageName = pkgName;
                addElement(pkgDef, "value", pkgName);
            }
            String clazzName = facade.getName(clazz);

            // don't do 2 times the same class
            String fqcn = pkgName + "." + clazzName;
            if (classesCounter.containsKey(fqcn)) {
                classesCounter.get(fqcn).add(clazz);
            } else {
                classesCounter.put(fqcn, new ArrayList<Object>());
                classesCounter.get(fqcn).add(clazz);
                Element elemClazz = addElement(classes, "class");
                addElement(elemClazz, "name", clazzName);
                addPackage(clazz, elemClazz);
                addTaggedValues(clazz, elemClazz);
                addInterfaces(clazz, elemClazz);
                addInheritance(clazz, elemClazz);
                addFields(clazz, elemClazz);
                addAssociations(clazz, elemClazz);
                addOperations(clazz, elemClazz);
            }
        }
        XMLOutputter outputter = new XMLOutputter();
        Format f = Format.getPrettyFormat();
        f.setIndent("  ");
        f.setEncoding("UTF-8");
        f.setLineSeparator("\n");
        f.setTextMode(TextMode.PRESERVE);
        outputter.setFormat(f);
        // outputter.getFormat().setLineSeparator( "" );
        try {
            outputter.output(doc, new FileWriter(destination));
        } catch (IOException e) {
            throw new IOException(
                    "Cannot write model '" + destination.getAbsolutePath() + "': " + e.getMessage(), e);
        }
        // Output report
        log.info("-- Argo2Modello generation report --");
        log.info("Generated " + classesCounter.size() + " classes.");
        for (String s : classesCounter.keySet()) {
            log.info(s + ":" + classesCounter.get(s).size());
        }
        log.info("-- End of Argo2Modello generation report --");

    }

    private void addToStore(String clazzName, Object clazz) {
        if (!allClasses.containsKey(clazzName)) {
            Set<Object> l = new HashSet<Object>();
            allClasses.put(clazzName, l);
            l.add(clazz);
        } else if (!allClasses.get(clazzName).contains(clazz)) {
            Set<Object> l = allClasses.get(clazzName);
            l.add(clazz);
        }
    }

    public boolean isExcluded(Facade f, Object o) {
        String fqcn = getFullName(f, o);
        /*boolean excluded = excludedClassesSet.contains(fqcn);
        if (!excluded) {
            excluded = excludedClassesSet.contains(packageName + "." + fqcn);
        }
        log.info(packageName + "." + fqcn + "(" + packageName + ") is" + (excluded ? " " : " not ") + "excluded");
        return excluded;*/
        return exclusionsRepository.isExcluded(fqcn);
    }

    public String getFullName(Facade f, Object o) {
        String ns = getNamespaceName(f, o);
        return (ns != null ? ns + "." : "") + f.getName(o);
    }

    public String getNamespaceName(Facade f, Object o) {
        String ns = null;
        if (f.getNamespace(o) != null)
            ns = getNamespaceName(f, f.getNamespace(o));
        return ns;
    }
    // XML Content generation

    public Element addElement(Element e, String n) {
        return addElement(e, n, null);
    }

    public Element addElement(Element e, String n, String s) {
        Facade facade = Model.getFacade();
        Element child = new Element(n);
        if (s != null)
            child.setText(s);
        if (e == null)
            System.out.println("oups");
        e.addContent(child);
        return child;
    }

    public void addPackage(Object cls, Element elemClazz) {
        Facade f = Model.getFacade();
        Object ns = f.getNamespace(cls);
        String pkg = getPackageName(ns);
        addElement(elemClazz, "packageName", pkg);
    }

    public void addInheritance(Object clazz, Element elemClazz) {
        Facade facade = Model.getFacade();
        if (!facade.getGeneralizations(clazz).isEmpty()) {
            Object gen = facade.getGeneralizations(clazz).iterator().next();
            Object parent = facade.getGeneral(gen);
            String parentName = facade.getName(parent);
            if (facade.isAInterface(clazz)) {
                log.info("Set superinterface to " + parentName + " (" + parent + ")");
                addElement(elemClazz, "superInterface", parentName);
            } else if (facade.isAClass(clazz)) {
                log.info("Set superclass to " + parentName + " (" + parent + ")");
                addElement(elemClazz, "superClass", parentName);
            }
        }
    }

    private Map<String, Set<String>> fieldsForClasses = new HashMap<String, Set<String>>();

    public void addFields(Object clazz, Element elemClazz) {
        Facade facade = Model.getFacade();
        String clazzName = getFullName(clazz);
        Collection fieldsUml = facade.getStructuralFeatures(clazz);
        if (!fieldsUml.isEmpty()) {
            Element fields = addElement(elemClazz, "fields");
            Iterator jt = fieldsUml.iterator();
            while (jt.hasNext()) {
                Object attr = jt.next();
                Element elemField = addElement(fields, "field");
                addElement(elemField, "name", facade.getName(attr));
                if (fieldsForClasses.get(clazzName) == null)
                    fieldsForClasses.put(clazzName, new HashSet<String>());
                fieldsForClasses.get(clazzName).add(facade.getName(attr).toUpperCase());

                if (facade.getType(attr) != null) {
                    /*
                     * <association> <type>ContentTest</type>
                     * <multiplicity>1</multiplicity> </association>
                     */
                    String type = facade.getName(facade.getType(attr)).trim();
                    log.info("Add " + facade.getName(attr) + " with " + type);
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
    }

    public void addVisibility(Object obj, Element elem) {
        Facade facade = Model.getFacade();
        if (!facade.isPrivate(obj) || facade.isStatic(obj)) {
            Element modifier = addElement(elem, "modifier");
            if (facade.isProtected(obj)) {
                modifier.addContent("protected");
            } else if (facade.isPublic(obj)) {
                modifier.addContent("public");
            } else if (facade.isPackage(obj)) {
                modifier.addContent("package");
            }
            if (facade.isStatic(obj)) {
                if (!"".equals(modifier.getText()))
                    modifier.addContent(",");
                modifier.addContent("static");
            }
        }
        // by default objects are private (but they are public in ArgoUML - take
        // care to fields)
    }

    public void addInterfaces(Object cls, Element elemClazz) {
        Collection realizations = Model.getFacade().getSpecifications(cls);
        if (!realizations.isEmpty()) {
            Element interfaces = addElement(elemClazz, "interfaces");
            Iterator clsEnum = realizations.iterator();
            while (clsEnum.hasNext()) {
                Object inter = clsEnum.next();
                addElement(interfaces, "interface", getPackageName(inter));
            }
        }
    }

    public void addOperations(Object cls, Element elemClazz) {
        Facade facade = Model.getFacade();
        String clazzName = facade.getName(cls);
        // add operations
        // TODO: constructors
        Collection bFeatures = facade.getOperations(cls);

        if (!bFeatures.isEmpty()) {
            String tv;
            Element operations = addElement(elemClazz, "codeSegments");
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
                    Set<String> fields = fieldsForClasses.get(getFullName(cls));
                    if (fields != null && fields.contains(name.toUpperCase())) {
                        List<Element> fieldsList = elemClazz.getChild("fields").getChildren();
                        for (Element e : fieldsList) {
                            if (name.toUpperCase().equals(e.getChild("name").getText().toUpperCase())) {
                                if (setter)
                                    e.setAttribute("java.setter", "false");
                                else
                                    e.setAttribute("java.getter", "false");
                            }
                        }
                    }
                    // continue;
                }
                StringBuffer sb = new StringBuffer();
                sb.append(GeneratorJava2.INDENT);
                sb.append(generator.generateOperation(behavioralFeature, false));

                tv = generator.generateTaggedValues(behavioralFeature);

                if ((facade.isAClass(cls)) && (facade.isAOperation(behavioralFeature))
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
                Element operation = addElement(operations, "codeSegment");
                addTaggedValues(behavioralFeature, operation);
                Element methodBody = addElement(operation, "code");
                methodBody.setText(sb.toString());
            }
        }
    }

    public void addAssociations(Object clazz, Element elemClazz) {
        Facade facade = Model.getFacade();
        // add attributes implementing associations
        Collection ends = facade.getAssociationEnds(clazz);
        if (!ends.isEmpty()) {
            Element fields = elemClazz.getChild("fields");
            if (fields == null)
                fields = addElement(elemClazz, "fields");
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
                String multiplicity = facade.getName(facade.getMultiplicity(otherAssociationEnd));
                Element elemField = addElement(fields, "field");
                addElement(elemField, "name", endName);
                addTaggedValues(otherAssociationEnd, elemField);
                Element assoc = addElement(elemField, "association");
                addElement(assoc, "type", typeName);
                if (multiplicity.indexOf("*") > -1) {
                    addElement(assoc, "multiplicity", "*");
                } else {
                    addElement(assoc, "multiplicity", "1");
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
            log.info("No association ends for '" + facade.getName(clazz) + "'");
        }
    }

    /**
     * Add tagged values from the UML model object o to the Modello class
     * element for this object.
     * 
     * @param o
     *            The UML model object
     * @param e
     *            The Modello class element
     */
    public void addTaggedValues(Object o, Element e) {
        Facade facade = Model.getFacade();
        Object tag;
        String name;
        boolean seenVersion = false;
        Iterator it = facade.getTaggedValues(o);
        Element annotations = new Element("annotations");
        while (it.hasNext()) {
            tag = it.next();
            name = facade.getTag(tag);
            Object td = facade.getTagDefinition(tag);
            Object type = facade.getType(tag);
            if (name == null) {
                log.debug("No name for tagged value '" + tag + "' with value '" + facade.getValue(tag) + "' and class "
                        + (tag != null ? tag.getClass().getName() : "N/A") + " and TD " + td + " and type " + type);
            } else if ("documentation".equals(name)) {
                addElement(e, "description", facade.getValueOfTag(tag));
            } else if ("primaryKey".equals(name)) {
                addElement(e, "identifier", facade.getValueOfTag(tag));
            } else if ("model.id".equals(name)) {
                addElement(e, "id", facade.getValueOfTag(tag));
            } else if ("version".equals(name)) {
                String version = null;
                try {
                    version = StringUtils.strip(facade.getValueOfTag(tag));
                    String[] splittedVersion = StringUtils.split(version, ".");
                    if (splittedVersion.length > 3) {
                        throw new Exception();
                    }

                    String majorString = splittedVersion[0];
                    String minorString = "0";
                    String microString = "0";
                    if (splittedVersion.length > 1) {
                        minorString = splittedVersion[1];
                        if (splittedVersion.length > 2) {
                            microString = splittedVersion[2];
                        }
                    }
                    try {
                        short major = Short.parseShort(majorString);
                        short minor = Short.parseShort(minorString);
                        short micro = Short.parseShort(microString);
                    } catch (NumberFormatException e1) {
                        throw new Exception();
                    }
                    addElement(e, "version", version);
                } catch (Exception e1) {
                    log.warn("Discarding non-standard version " + version);
                }
            } else if ("@".equals(name.substring(0, 1))) {
                String content = "".equals(facade.getValueOfTag(tag)) ? name
                        : name + "(" + facade.getValueOfTag(tag) + ")";
                addElement(annotations, "annotation", content);
                // log.info( "Added "+content );
            } else {
                try {
                    e.setAttribute(name.trim(), facade.getValueOfTag(tag));
                } catch (Exception e1) {
                    log.warn("Cannot set name to " + name + ": " + e1.getMessage());
                }
            }
        }
        if (annotations.getChildren().size() > 0)
            e.addContent(annotations);
        if (!seenVersion)
            addElement(e, "version", "1.0.0");
    }

    protected boolean isTaggedValueHandled(Object taggedValue) {
        for (TaggedValueHandler tvh : taggedValuesHandlers) {
            if (tvh.accept(taggedValue))
                return true;
        }
        return false;
    }

    protected void handleTaggedValue(Object taggedValue, Element classElement) {
        for (TaggedValueHandler tvh : taggedValuesHandlers) {
            if (tvh.accept(taggedValue))
                tvh.handle(taggedValue, classElement);
        }
    }

    // ArgoUML initialization

    /**
     * Gets the Java package name for a given namespace, ignoring the root
     * namespace (which is the model).
     * 
     * @param namespace
     *            the namespace
     * @return the Java package name
     */
    public String getPackageName(Object namespace) {
        if (namespace == null || !Model.getFacade().isANamespace(namespace)
                || Model.getFacade().getNamespace(namespace) == null) {
            return "";
        }
        String packagePath = Model.getFacade().getName(namespace);
        if (packagePath == null) {
            return "";
        }
        while ((namespace = Model.getFacade().getNamespace(namespace)) != null) {
            // ommit root package name; it's the model's root
            if (Model.getFacade().getNamespace(namespace) != null) {
                packagePath = Model.getFacade().getName(namespace) + '.' + packagePath;
            }
        }
        return packagePath;
    }

    public String getFullName(Object cls) {
        Facade f = Model.getFacade();
        return getPackageName(f.getNamespace(cls)) + '.' + f.getName(cls);
    }

    public static class LegacyModelloGeneratorBuilder {

        private final LegacyModelloGenerator legacyModelloGenerator;

        private LegacyModelloGeneratorBuilder() {
            this.legacyModelloGenerator = new LegacyModelloGenerator();
        }

        public static LegacyModelloGeneratorBuilder newBuilder() {
            return new LegacyModelloGeneratorBuilder();
        }

        public LegacyModelloGeneratorBuilder withDefaultImports(String defaultImports) {
            legacyModelloGenerator.defaultImports = defaultImports;
            return this;
        }

        public LegacyModelloGeneratorBuilder withSourceModel(File sourceModel) {
            legacyModelloGenerator.sourceModel = sourceModel;
            return this;
        }

        public LegacyModelloGeneratorBuilder withExclusionsRepository(ExclusionsRepository exclusionsRepository) {
            legacyModelloGenerator.exclusionsRepository = exclusionsRepository;
            return this;
        }

        public LegacyModelloGenerator build() {
            return this.legacyModelloGenerator;
        }
    }
}
