package com.ubikproducts.maven.plugins.argo2modello;

import static org.junit.Assert.fail;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;

public class ModelloConvertTest {

    private Argo2ModelloMojo plugin;

    @Before
    public void setUp() throws Exception {
        plugin = Argo2ModelloMojoBuilder.newBuilder().withTestModel("src/test/resources/models/modello-reverse.uml")
                .withOtherProfiles("src/test/resources/profils")
                .withJavaProfil("src/main/profiles/default-java.xmi")
                .withDefaultImports("javax.persistence.*,javax.xml.bind.annotation.*")
                .withForcedGeneration()
                .build();
    }

    @Test
    public void legacy_generate_modello() {
        plugin.setLegacyGeneration(true);
        plugin.setDestinationModel(new File("target/modello-legacy.mdo"));
        try {
            plugin.execute();
        } catch (MojoExecutionException e) {
            e.printStackTrace();
            fail("No error should have happened");
        }
    }

    @Test
    public void generate_modello() {
        plugin.setLegacyGeneration(false);
        plugin.setDestinationModel(new File("target/modello-new.mdo"));
        try {
            plugin.execute();
        } catch (MojoExecutionException e) {
            e.printStackTrace();
            fail("No error should have happenned");
        }
    }

    @Test
    public void generate_modello_with_model_reader() {
        plugin.setLegacyGeneration(false);
        plugin.setReaderGeneration(true);
        plugin.setDestinationModel(new File("target/modello-reader.mdo"));
        try {
            plugin.execute();
        } catch (MojoExecutionException e) {
            e.printStackTrace();
            fail("No error should have happenned");
        }
    }

    public static class Argo2ModelloMojoBuilder {

        private Argo2ModelloMojo argo2modelloMojo;

        private Argo2ModelloMojoBuilder() {
            argo2modelloMojo = new Argo2ModelloMojo();
        }

        public static Argo2ModelloMojoBuilder newBuilder() {
            return new Argo2ModelloMojoBuilder();
        }

        public Argo2ModelloMojoBuilder withOtherProfiles(String otherProfiles) {
            argo2modelloMojo.setOtherProfilsFolders(otherProfiles);
            return this;
        }

        public Argo2ModelloMojoBuilder withDestinationModel(File destinationModel) {
            argo2modelloMojo.setDestinationModel(destinationModel);
            return this;
        }

        public Argo2ModelloMojoBuilder withTestModel(String testModel) {
            argo2modelloMojo.setSourceModel(new File(testModel));
            return this;
        }

        public Argo2ModelloMojoBuilder withForcedGeneration() {
            argo2modelloMojo.setForce(true);
            return this;
        }

        public Argo2ModelloMojoBuilder withDefaultImports(String defaultImports) {
            argo2modelloMojo.setDefaultImports(defaultImports);
            return this;
        }

        public Argo2ModelloMojoBuilder withJavaProfil(String javaProfil) {
            argo2modelloMojo.setJavaProfile(new File("src/main/profiles/default-java.xmi"));
            return this;
        }

        public Argo2ModelloMojo build() {
            return argo2modelloMojo;
        }
    }
}
