package com.ubikproducts.maven.plugins.argo2modello;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.nio.file.Paths;

import org.argouml.configuration.Configuration;
import org.junit.Test;

import com.ubikproducts.maven.plugins.argo2modello.ArgoUMLDriver.ArgoUMLDriverBuilder;

public class ArgoUMLDriverTest {

   private File javaProfile = Paths.get("src/main/profiles/default-java.xmi").toFile();

   private String otherProfilsFolders = "src/test/resources/profils";

    @Test
    public void testReadCustomProfile() {
        ArgoUMLDriver argoUmlDriver = ArgoUMLDriverBuilder.newBuilder()
                .withJavaProfile(javaProfile)
                .withProfileFolder(otherProfilsFolders)
                .build();
        assertNotNull(argoUmlDriver);
        System.out.println("Searchpaths:");
        for ( String s : argoUmlDriver.getSearchPaths() ) {
            System.out.println(s);
        }
        System.out.println("Profiles:");
        for ( String s : argoUmlDriver.getProfilesList() ) {
            System.out.println(s);
        }
    }
}
