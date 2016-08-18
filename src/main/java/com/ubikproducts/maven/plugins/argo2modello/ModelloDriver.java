package com.ubikproducts.maven.plugins.argo2modello;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import org.codehaus.modello.ModelloException;
import org.codehaus.modello.core.DefaultModelloCore;
import org.codehaus.modello.core.MetadataPluginManager;
import org.codehaus.modello.core.ModelloCore;
import org.codehaus.modello.core.io.ModelWriter;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelValidationException;

public class ModelloDriver {

    private final ModelloCore modelloCore;

    private final ModelWriter modelWriter;

    private Properties parameters = new Properties();

    private ModelloDriver() {
        this.modelloCore = new DefaultModelloCore();
        this.modelWriter = new JDOMModelWriter();
    }

    public static class ModelloDriverBuilder {

        private final ModelloDriver modelloDriver;

        private ModelloDriverBuilder() {
            this.modelloDriver = new ModelloDriver();
        }

        public static ModelloDriverBuilder newBuilder() {
            return new ModelloDriverBuilder();
        }

        public ModelloDriverBuilder withParameter(String name, String value) {
            modelloDriver.parameters.setProperty(name, value);
            return this;
        }

        public ModelloDriver build() {
            return modelloDriver;
        }
    }

    public MetadataPluginManager getMetadataPluginManager() {
        return modelloCore.getMetadataPluginManager();
    }

    public Model loadModel(File file) throws IOException, ModelloException, ModelValidationException {
        return modelloCore.loadModel(file);
    }

    public Model loadModel(Reader reader) throws ModelloException, ModelValidationException {
        return modelloCore.loadModel(reader);
    }

    public void saveModel(Model model, Writer writer) throws ModelloException, IOException {
        modelWriter.saveModel(model, parameters, writer);
    }

    public Model translate(Reader reader, String inputType, Properties parameters)
            throws ModelloException, ModelValidationException {
        return modelloCore.translate(reader, inputType, parameters);
    }

    public void generate(Model model, String outputType, Properties parameters) throws ModelloException {
        modelloCore.generate(model, outputType, parameters);
    }
}
