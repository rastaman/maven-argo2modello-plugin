package com.ubikproducts.maven.plugins.argo2modello;

import org.codehaus.modello.model.Model;

public class ModelloGenerator {

    private Model model;

    private Object nativeModel;

    private ModelloGenerator() {
        model = new Model();
    }

    public Model generate() {
        if (nativeModel != null) {
            
        }
        return model;
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

        public static ModelloGeneratorBuilder newBuilder() {
            return new ModelloGeneratorBuilder();
        }

        public ModelloGenerator build() {
            return modelloGenerator;
        }
    }
}
