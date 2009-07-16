package org.argouml.support;

import org.argouml.model.Model;
import org.argouml.model.ModelImplementation;

public class ArgoUMLStarter {

    
    /**
     * The default implementation to start.
     */
    private static final String DEFAULT_MODEL_IMPLEMENTATION = "org.argouml.model.mdr.MDRModelImplementation";

	 /**
     * Initialise the UML model repository.
     */
    private void initModel() {
        String className = System.getProperty(
                "argouml.model.implementation",
                DEFAULT_MODEL_IMPLEMENTATION);
        Throwable ret = Model.initialise(className);
    }
    
    /**
     * Initialize the Model subsystem with the default ModelImplementation.
     */
    public static void initializeDefault() {
    	if (Model.isInitiated()) {
    	    return;
    	}
        String className =
            System.getProperty(
                    "argouml.model.implementation",
                    DEFAULT_MODEL_IMPLEMENTATION);
        initializeModelImplementation(className);
    }

    /**
     * Initialize the Model subsystem with the MDR ModelImplementation.
     */
    public static void initializeMDR() {
        initializeModelImplementation(
                "org.argouml.model.mdr.MDRModelImplementation");
    }

    private static ModelImplementation initializeModelImplementation(
            String name) {
        ModelImplementation impl = null;

        Class implType = null;
        try {
            implType =
                Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            impl = (ModelImplementation) implType.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Model.setImplementation(impl);
        return impl;
    }
    
}
