package com.ubikproducts.maven.plugins.argo2modello;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.codehaus.modello.ModelloException;
import org.codehaus.modello.core.io.ModelWriter;
import org.codehaus.modello.model.BaseElement;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelDefault;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.model.ModelInterface;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.Format.TextMode;
import org.jdom.output.XMLOutputter;

public class JDOMModelWriter implements ModelWriter {

    public static enum JDOMModelWriterParameter {
        SOURCE_LAST_MODIFIED("sourceModel.lastModified"), DEFAULT_IMPORTS("defaultImports");

        private final String name;

        JDOMModelWriterParameter(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private Logger log = Logger.getLogger(JDOMModelWriter.class);

    private Map<String, Set<Object>> allClasses = new LinkedHashMap<String, Set<Object>>();

    private Map<String, Set<String>> fieldsForClasses = new HashMap<String, Set<String>>();

    private Map<String, List<Object>> classesCounter = new HashMap<String, List<Object>>();

    private Set<String> excludedClassesSet = new LinkedHashSet<String>();

    private Properties parameters;

    @Override
    public void saveModel(Model model, Properties parameters, Writer writer) throws ModelloException, IOException {
        this.parameters = parameters;
        checkParameters();
        Document modelDocument = generateModel(model);
        writeDOM(modelDocument, writer);
    }

    private void checkParameters() throws ModelloException {

    }

    private boolean isNullOrEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    public Element addElement(Element e, String n) {
        return addElement(e, n, null);
    }

    public Element addElement(Element e, String n, String s) {
        Element child = new Element(n);
        if (s != null)
            child.setText(s);
        e.addContent(child);
        return child;
    }

    private Document generateModel(Model model) {
        Document xmlDocument = new Document();
        Element rootElement = new Element("model");
        xmlDocument.setRootElement(rootElement);
        rootElement.setAttribute("uml.lastModified",
                "" + parameters.getProperty(JDOMModelWriterParameter.SOURCE_LAST_MODIFIED.name()));
        addTaggedValues(model, rootElement);
        // String packageName = model.getDefaultPackageName(false, version);
        addElement(rootElement, "name", model.getName());
        Element defaults = addElement(rootElement, "defaults");
        for ( ModelDefault md : model.getDefaults() ) {
            Element modelDefaultElement = addElement(defaults, "default");
            addElement(modelDefaultElement, "key", md.getKey());
            addElement(modelDefaultElement, "value", md.getValue());
        }
        //Element pkgDef = addElement(defaults, "default");
        //addElement(pkgDef, "key", "package");
        //boolean seenPkg = false;
        List<ModelInterface> modelInterfaces = model.getAllInterfaces();
        // interfaces
        Element interfaces = addElement(rootElement, "interfaces");
        for (ModelInterface inf : modelInterfaces) {
            addInterface(inf, interfaces);
        }
        // classes
        // 1st pass - collection types names
        List<ModelClass> clazzes = model.getAllClasses();
        allClasses = new HashMap<String, Set<Object>>();
        for (ModelClass clazz : clazzes) {
            String name = clazz.getName();
            if (!isExcluded(clazz)) {
                addToStore(name, clazz);
            }
        }
        Element classes = addElement(rootElement, "classes");
        Map<String, List<Object>> classesCounter = new HashMap<String, List<Object>>();
        for (ModelClass clazz : clazzes) {
            addClass(clazz, classes);
        }
        return xmlDocument;
    }

    private void addClass(ModelClass clazz, Element elemClazzes) {
        String pkgName = getPackageName(clazz);
        if (pkgName.startsWith("java.") || isExcluded(clazz))
            return;
        // if (!seenPkg) {
        // seenPkg = true;
        // packageName = pkgName;
        // addElement(pkgDef, "value", pkgName);
        // }
        String clazzName = clazz.getName();

        // don't do 2 times the same class
        String fqcn = pkgName + "." + clazzName;
        if (classesCounter.containsKey(fqcn)) {
            classesCounter.get(fqcn).add(clazz);
        } else {
            classesCounter.put(fqcn, new ArrayList<Object>());
            classesCounter.get(fqcn).add(clazz);
            Element elemClazz = addElement(elemClazzes, "class");
            addElement(elemClazz, "name", clazzName);
            addPackage(clazz.getPackageName(), elemClazz);
            addTaggedValues(clazz, elemClazz);
            addInterfaces(clazz, elemClazz);
            addInheritance(clazz, elemClazz);
            addFields(clazz, elemClazz);
            addAssociations(clazz, elemClazz);
            // addOperations(clazz, elemClazz);
        }
    }

    public String getPackageName(ModelClass clazz) {
        return clazz.getPackageName();
    }

    private void addInterface(ModelInterface inf, Element interfacesElement) {
        if (!isExcluded(inf)) {
            // if (!seenPkg) {
            // seenPkg = true;
            // packageName = inf.getPackageName();
            // addElement(pkgDef, "value", packageName);
            // }
            Element elemInf = addElement(interfacesElement, "interface");
            String infName = inf.getName();
            addElement(elemInf, "name", infName);
            addPackage(inf.getPackageName(), elemInf);
            addTaggedValues(inf, elemInf);
            // addOperations(inf, elemInf);
            addInheritance(inf, elemInf);
        }
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

    public boolean isExcluded(BaseElement o) {
        String fqcn = getFullName(o);
        boolean excluded = excludedClassesSet.contains(fqcn);
        // if (!excluded) {
        // excluded = excludedClassesSet.contains(packageName + "." + fqcn);
        // }
        // log.info(packageName + "." + fqcn + "(" + packageName + ") is" +
        // (excluded ? " " : " not ") + "excluded");
        return excluded;
    }

    public void addPackage(String cls, Element elemClazz) {
        addElement(elemClazz, "packageName", cls);
    }

    public void addInterfaces(ModelClass cls, Element elemClazz) {
        List<String> interfaces = cls.getInterfaces();
        if (!interfaces.isEmpty()) {
            Element infs = addElement(elemClazz, "interfaces");
            for (String inf : interfaces) {
                addElement(infs, "interface", inf);
            }
        }
    }

    public void addInheritance(Object clazz, Element elemClazz) {
        if (clazz instanceof ModelClass && ((ModelClass) clazz).getSuperClass() != null) {
            addElement(elemClazz, "superClass", ((ModelClass) clazz).getSuperClass());
        } else if (clazz instanceof ModelInterface && ((ModelInterface) clazz).getSuperInterface() != null) {
            addElement(elemClazz, "superInterface", ((ModelInterface) clazz).getSuperInterface());
        }
    }

    public void addTaggedValues(BaseElement baseElement, Element e) {
        Element annotations = new Element("annotations");
        for (String s : baseElement.getAnnotations()) {
            addElement(annotations, "annotation", s);
        }
        if (baseElement.getDescription() != null) {
            addElement(e, "description", baseElement.getDescription());
        }
        if (baseElement.getVersionRange() != null) {
            addElement(e, "version", baseElement.getVersionRange().getFromVersion().toString());
        } else {
            addElement(e, "version", "1.0.0");
        }
        if (baseElement instanceof ModelClass) {

        }
        // frack
        // while (it.hasNext()) {
        // if ("primaryKey".equals(name)) {
        // addElement(e, "identifier", facade.getValueOfTag(tag));
        // } else if ("model.id".equals(name)) {
        // addElement(e, "id", facade.getValueOfTag(tag));
        // } else {
        // try {
        // e.setAttribute(name.trim(), facade.getValueOfTag(tag));
        // } catch (Exception e1) {
        // log.warn("Cannot set name to " + name + ": " + e1.getMessage());
        // }
        // }
        // }

        if (annotations.getChildren().size() > 0) {
            e.addContent(annotations);
        }
    }

    private String getFullName(BaseElement cls) {
        String className = cls.getName();
        if (cls instanceof ModelClass) {
            className = ((ModelClass) cls).getPackageName() + "." + className;
        } else if (cls instanceof ModelInterface) {
            className = ((ModelInterface) cls).getPackageName() + "." + className;
        }
        return className;
    }

    public void addFields(ModelClass clazz, Element elemClazz) {
        String clazzName = getFullName(clazz);
        log.info("Generating fields for " + clazzName);
        List<ModelField> fieldsUml = clazz.getAllFields();
        if (!fieldsUml.isEmpty()) {
            Element fields = addElement(elemClazz, "fields");
            for (ModelField attr : fieldsUml) {
                Element elemField = addElement(fields, "field");
                addElement(elemField, "name", attr.getName());
                fieldsForClasses.put(clazzName, new HashSet<String>());
                fieldsForClasses.get(clazzName).add(attr.getName().toUpperCase());

                if (attr.getType() != null) {
                    /*
                     * <association> <type>ContentTest</type>
                     * <multiplicity>1</multiplicity> </association>
                     */
                    String type = attr.getType();
                    log.info("Generate field " + attr.getName() + " with type " + type);
                    // if ( !allClasses.containsKey(type) ||
                    // ModelDefault.isBaseType(type))
                    if (!allClasses.containsKey(type) || ModelloTypesHelper.isBaseType(type))
                        addElement(elemField, "type", type);
                    else {
                        Element monoAssoc = addElement(elemField, "association");
                        addElement(monoAssoc, "type", type);
                        addElement(monoAssoc, "multiplicity", "1");
                    }
                } else {
                    log.info("Cannot add type of attr " + attr.getName() + " with no type for " + attr);
                }
                // if not default case
                addVisibility(attr, elemField);
                addTaggedValues(attr, elemField);
            }
        }
    }

    public void addVisibility(ModelField modelField, Element elem) {
        // if (!facade.isPrivate(obj) || facade.isStatic(obj)) {
        // Element modifier = addElement(elem, "modifier");
        // if (facade.isProtected(obj)) {
        // modifier.addContent("protected");
        // } else if (facade.isPublic(obj)) {
        // modifier.addContent("public");
        // } else if (facade.isPackage(obj)) {
        // modifier.addContent("package");
        // }
        // if (facade.isStatic(obj)) {
        // if (!"".equals(modifier.getText()))
        // modifier.addContent(",");
        // modifier.addContent("static");
        // }
        // }
        // by default objects are private (but they are public in ArgoUML - take
        // care to fields)
    }

    public void addAssociations(ModelClass clazz, Element elemClazz) {
        // add attributes implementing associations
        // clazz.getAllFields().iterator().next().get
        // Collection ends = facade.getAssociationEnds(clazz);
        // if (!ends.isEmpty()) {
        // Element fields = elemClazz.getChild("fields");
        // if (fields == null)
        // fields = addElement(elemClazz, "fields");
        // for (Object associationEnd : ends) {
        // Object association = facade.getAssociation(associationEnd);
        // Object otherAssociationEnd = facade.getNextEnd(associationEnd);
        // String otherEndName = facade.getName(associationEnd);
        // String otherTypeName =
        // facade.getName(facade.getType(associationEnd));
        // String endName = facade.getName(otherAssociationEnd);
        // String typeName =
        // facade.getName(facade.getType(otherAssociationEnd));
        // if (!facade.isNavigable(otherAssociationEnd))
        // continue;
        // // TODO: Check strange code
        // if ("".equals(endName) || endName == null) {
        // endName = StringUtils.uncapitalize(typeName);
        // }
        // String multiplicity =
        // facade.getName(facade.getMultiplicity(otherAssociationEnd));
        // Element elemField = addElement(fields, "field");
        // addElement(elemField, "name", endName);
        // addTaggedValues(otherAssociationEnd, elemField);
        // Element assoc = addElement(elemField, "association");
        // addElement(assoc, "type", typeName);
        // if (multiplicity.indexOf("*") > -1) {
        // addElement(assoc, "multiplicity", "*");
        // } else {
        // addElement(assoc, "multiplicity", "1");
        // }
        // // move the annotations to the association
        // /*
        // * if ( elemField.getChild( "annotations" ) != null ) { Element
        // * annotations = (Element) elemField.getChild( "annotations"
        // * ).clone(); assoc.addContent( annotations );
        // * elemField.removeChild( "annotations" ); }
        // */
        // }
        // } else {
        // //log.info("No association ends for '" + clazz.getName()+ "'");
        // }
    }

    private void writeDOM(Document modelDocument, Writer writer) throws IOException {
        XMLOutputter outputter = new XMLOutputter();
        Format f = Format.getPrettyFormat();
        f.setIndent("  ");
        f.setEncoding("UTF-8");
        f.setLineSeparator("\n");
        f.setTextMode(TextMode.PRESERVE);
        outputter.setFormat(f);
        // outputter.getFormat().setLineSeparator( "" );
        try {
            outputter.output(modelDocument, writer);
        } catch (IOException e) {
            throw new IOException("Cannot write model : " + e.getMessage(), e);
        }

    }
}
