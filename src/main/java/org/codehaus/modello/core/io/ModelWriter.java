package org.codehaus.modello.core.io;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import org.codehaus.modello.ModelloException;
import org.codehaus.modello.model.Model;

public interface ModelWriter {

    public void saveModel(Model model, Properties parameters, Writer writer) throws ModelloException,IOException;

}
