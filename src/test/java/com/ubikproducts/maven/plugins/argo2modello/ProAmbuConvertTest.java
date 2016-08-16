package com.ubikproducts.maven.plugins.argo2modello;

import static org.junit.Assert.fail;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;

public class ProAmbuConvertTest {

    private Argo2ModelloMojo plugin;

    @Before
    public void setUp() throws Exception {
        plugin = Argo2ModelloMojoBuilder.newBuilder().withTestModel("src/test/resources/uml/proambu.uml")
                .withOtherProfiles("src/test/resources/profils")
                .withJavaProfil("src/main/profiles/default-java.xmi")
                .withDefaultImports("javax.persistence.*,javax.xml.bind.annotation.*").build();
        /*
         * this.setTestModel("proambu"); File destModel = new
         * File("target/proambu.mdo"); this.setDestinationModel(destModel);
         * this.setOtherProfils("src/test/resources/profils");
         * this.initModelFiles();
         */
        // this.setDefaultImports("fr.factory.tif.generic.utils.Biface,fr.factory.tif.generic.utils.BifaceI18n,fr.factory.tif.generic.utils.*,javax.persistence.*,javax.xml.bind.annotation.*,java.util.*,javax.persistence.Query,java.util.Locale,java.util.HashMap,java.util.Vector,java.lang.reflect.Method");
    }

    @Test
    public void legacy_generate_modello() {
        plugin.setLegacyGeneration(true);
        plugin.setDestinationModel(new File("target/proambu-legacy.mdo"));
        try {
            plugin.execute();
        } catch (MojoExecutionException e) {
            e.printStackTrace();
            fail("No error should have happenned");
        }
    }

    @Test
    public void generate_modello() {
        plugin.setLegacyGeneration(false);
        plugin.setDestinationModel(new File("target/proambu-new.mdo"));
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
