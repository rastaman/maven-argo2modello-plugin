package com.ubikproducts.maven.plugins.argo2modello;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import org.codehaus.modello.ModelloException;
import org.codehaus.modello.core.io.ModelWriter;
import org.codehaus.modello.model.Model;

public class UMLModelWriter implements ModelWriter {

    @Override
    public void saveModel(Model model, Properties parameters, Writer writer) throws ModelloException, IOException {
        throw new IllegalArgumentException("Would be a quite convenient writer for round-trips");
    }

}
