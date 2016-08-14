package com.ubikproducts.maven.plugins.argo2modello;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.argouml.kernel.Project;
import org.codehaus.modello.ModelloException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.ubikproducts.maven.plugins.argo2modello.ArgoUMLDriver.ArgoUMLDriverBuilder;
import com.ubikproducts.maven.plugins.argo2modello.ExclusionsRepository.ExclusionsRepositoryBuilder;
import com.ubikproducts.maven.plugins.argo2modello.LegacyModelloGenerator.LegacyModelloGeneratorBuilder;
import com.ubikproducts.maven.plugins.argo2modello.ModelloDriver.ModelloDriverBuilder;
import com.ubikproducts.maven.plugins.argo2modello.ModelloGenerator.ModelloGeneratorBuilder;

/**
 * @goal generate
 * @phase process-sources
 */
public class Argo2ModelloMojo extends AbstractMojo {
    /**
     * Location of the file.
     * 
     * @parameter alias="destinationModel"
     *            property="argo2modello.destinationModel"
     *            default-value="src/main/mdo/model.mdo"
     * @required
     */
    private File destinationModel;

    /**
     * Location of the file.
     * 
     * @parameter alias="sourceModel" property="argo2modello.sourceModel"
     *            default-value="src/main/uml/model.uml"
     * @required
     */
    private File sourceModel;

    /**
     * Location of the Java Profile for ArgoUML, if not shipped in the ArgoUML
     * main JAR.
     * 
     * @parameter alias="javaProfile" property="argo2modello.javaProfile"
     *            default-value="src/main/profiles/default-java.xmi"
     */
    private File javaProfile;

    /**
     * List of other profiles path separated by commas.
     */
    private String otherProfilsFolders;

    /**
     * Force the generation of the Modello model. Else the modello model is
     * regenerated only when the UML file has changed (last modification time
     * has changed).
     * 
     * @parameter alias="force" property="argo2modello.force"
     *            default-value="false"
     */
    private boolean force;

    /**
     * Default imports to set in Modello model. Used for instance to add the
     * packages for annotations.
     * 
     * @parameter alias="defaultImports" property="argo2modello.defaultImports"
     *            default-value=""
     */
    private String defaultImports;

    /**
     * Classes to not generate, separated by commas.
     * 
     * @parameter alias="excludedClasses"
     *            property="argo2modello.excludedClasses" default-value=""
     * @since 1.0.3
     */
    private String excludedClasses;

    /**
     * Use the old legacy translation : generate directly XML elements from the
     * model. Hopefully you can have the same results or better with the new
     * generation system which rely on modello classes to generate the model
     * file (mdo).
     */
    private boolean legacyGeneration = true;

    //private Logger log = Logger.getLogger(Argo2ModelloMojo.class);

    private boolean validateSettings() throws MojoExecutionException {
        if (!sourceModel.exists())
            throw new MojoExecutionException("Source model '" + sourceModel.getAbsolutePath() + "' doesn't exist!");

        if (destinationModel.exists() && !force) {
            try {
                Document doc = new SAXBuilder().build(destinationModel);
                String oldLastModified = doc.getRootElement().getAttributeValue("uml.lastModified");
                String newLastModified = "" + sourceModel.lastModified();
                if (oldLastModified != null && oldLastModified.equals(newLastModified))
                    return false;
            } catch (JDOMException e) {
                throw new MojoExecutionException("Error reading current destination model '"
                        + destinationModel.getAbsolutePath() + "': " + e.getMessage());
            } catch (IOException e) {
                throw new MojoExecutionException("Error reading current destination model '"
                        + destinationModel.getAbsolutePath() + "': " + e.getMessage());
            }
        }
        return true;
    }

    public void execute() throws MojoExecutionException {

        if (!validateSettings()) {
            return;
        }

        // setup
        ExclusionsRepository exclusionsRepository = ExclusionsRepositoryBuilder.newBuilder()
                .withExclusions(excludedClasses)
                .build();
        //registerExclusions();

        ArgoUMLDriver driver = ArgoUMLDriverBuilder.newBuilder().withProfilesFolders(otherProfilsFolders)
                .withProfileFolder(javaProfile.getParentFile().getAbsolutePath()).build();

        Project p = driver.loadProject(sourceModel);
        Object m = driver.getFirstModel(p);

        if (!legacyGeneration) {
            ModelloGenerator generator = ModelloGeneratorBuilder.newBuilder().withNativeModel(m).build();

            org.codehaus.modello.model.Model modelloModel = generator.generate();

            ModelloDriver modelloDriver = ModelloDriverBuilder.newBuilder().build();

            try {
                FileWriter writer = new FileWriter(destinationModel);
                modelloDriver.saveModel(modelloModel, writer);
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot write to "+destinationModel.getAbsolutePath()+":"+e.getMessage());
            } catch (ModelloException e) {
                throw new MojoExecutionException("Modello error with "+modelloModel+":"+e.getMessage());
            }
        } else {
            LegacyModelloGenerator generator = LegacyModelloGeneratorBuilder.newBuilder()
                    .withExclusionsRepository(exclusionsRepository)
                    .withDefaultImports(defaultImports)
                    .withSourceModel(sourceModel)
                    .build();
            try {
                generator.generate(m, destinationModel);
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot write to "+destinationModel.getAbsolutePath()+":"+e.getMessage());
            }
        }
    }

    /**
     * @return the destinationModel
     */
    public File getDestinationModel() {
        return destinationModel;
    }

    /**
     * @param destinationModel
     *            the destinationModel to set
     */
    public void setDestinationModel(File destinationModel) {
        this.destinationModel = destinationModel;
    }

    /**
     * @return the sourceModel
     */
    public File getSourceModel() {
        return sourceModel;
    }

    /**
     * @param sourceModel
     *            the sourceModel to set
     */
    public void setSourceModel(File sourceModel) {
        this.sourceModel = sourceModel;
    }

    /**
     * @return the javaProfile
     */
    public File getJavaProfile() {
        return javaProfile;
    }

    /**
     * @param javaProfile
     *            the javaProfile to set
     */
    public void setJavaProfile(File javaProfile) {
        this.javaProfile = javaProfile;
    }

    /**
     * @return the defaultImports
     */
    public String getDefaultImports() {
        return defaultImports;
    }

    /**
     * @param defaultImports
     *            the defaultImports to set
     */
    public void setDefaultImports(String defaultImports) {
        this.defaultImports = defaultImports;
    }

    /**
     * @return the force
     */
    public boolean isForce() {
        return force;
    }

    /**
     * @param force
     *            the force to set
     */
    public void setForce(boolean force) {
        this.force = force;
    }

    public String getExcludedClasses() {
        return excludedClasses;
    }

    public void setExcludedClasses(String excludedClasses) {
        this.excludedClasses = excludedClasses;
    }

    public String getOtherProfilsFolder() {
        return otherProfilsFolders;
    }

    public void setOtherProfilsFolder(String otherProfilsFolders) {
        this.otherProfilsFolders = otherProfilsFolders;
    }

    public String getOtherProfilsFolders() {
        return otherProfilsFolders;
    }

    public void setOtherProfilsFolders(String otherProfilsFolders) {
        this.otherProfilsFolders = otherProfilsFolders;
    }

    public boolean isLegacyGeneration() {
        return legacyGeneration;
    }

    public void setLegacyGeneration(boolean legacyGeneration) {
        this.legacyGeneration = legacyGeneration;
    }
}
