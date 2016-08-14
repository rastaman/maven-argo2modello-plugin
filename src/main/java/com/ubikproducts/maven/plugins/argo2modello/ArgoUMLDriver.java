package com.ubikproducts.maven.plugins.argo2modello;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.argouml.kernel.Project;
import org.argouml.model.Facade;
import org.argouml.model.Model;
import org.argouml.notation.InitNotation;
import org.argouml.notation.providers.java.InitNotationJava;
import org.argouml.notation.providers.uml.InitNotationUml;
import org.argouml.persistence.PersistenceManager;
import org.argouml.persistence.ProjectFilePersister;
import org.argouml.profile.ProfileFacade;
import org.argouml.profile.internal.ProfileManagerImpl;
import org.argouml.support.ArgoUMLStarter;

public class ArgoUMLDriver {

    private ProfileManagerImpl profileManagerImpl;

    private Set<String> profilsFolders;

    private File javaProfile;

    private ArgoUMLDriver() {

    }

    private void initializeMDR() {
        if (!Model.isInitiated()) {
            ArgoUMLStarter.initializeMDR();
        }
    }

    private void initializeSubsystems() {
        new InitNotation().init();
        new InitNotationUml().init();
        new InitNotationJava().init();
    }

    private void initializeProfilsManager() {
        profileManagerImpl = new org.argouml.profile.internal.ProfileManagerImpl();
    }

    private void loadProfils() {
        // Always force reinitialization of Profile subsystem
        if (javaProfile != null) {
            // log.info("Loading Java profile from " +
            // javaProfile.getAbsolutePath());
            loadProfil(javaProfile);
        }
        if (profilsFolders != null) {
            for (String s : profilsFolders) {
                File profilFile = getFileForProfil(s);
                loadProfil(profilFile);
            }
        }
        ProfileFacade.setManager(profileManagerImpl);
    }

    private File getFileForProfil(String filename) {
        if (filename.startsWith("/")) {
            return new File(filename);
        } else {
            return new File(".", filename);
        }
    }

    private void loadProfil(File profil) {
        String profileDir = profil.isFile() ? profil.getParentFile().getAbsolutePath() : profil.getAbsolutePath();
        profileManagerImpl.addSearchPathDirectory(profileDir);
        profileManagerImpl.refreshRegisteredProfiles();
    }

    private ProjectFilePersister getPersisterForFile(File file) {
        return PersistenceManager.getInstance().getPersisterFromFileName(file.getAbsolutePath());
    }

    public Project loadProject(File file) throws MojoExecutionException {
        ProjectFilePersister persister = getPersisterForFile(file);
        try {
            return persister.doLoad(file);
        } catch (Exception e) {
            throw new MojoExecutionException("Cannot load model '" + file.getAbsolutePath() + "': " + e.getMessage(),
                    e);
        }
    }

    public Facade getFacade() {
        return Model.getFacade();
    }

    public Object getFirstModel(Project p) {
        return p.getUserDefinedModelList().iterator().next();
    }

    public static class ArgoUMLDriverBuilder {

        private final ArgoUMLDriver argoUMLDriver;

        private ArgoUMLDriverBuilder() {
            argoUMLDriver = new ArgoUMLDriver();
            argoUMLDriver.profilsFolders = new HashSet<String>();
        }

        public static ArgoUMLDriverBuilder newBuilder() {
            return new ArgoUMLDriverBuilder();
        }

        public ArgoUMLDriverBuilder withProfilesFolders(String profilesFolders) {
            argoUMLDriver.profilsFolders.addAll(Arrays.asList(profilesFolders.split(",")));
            return this;
        }

        public ArgoUMLDriverBuilder withProfileFolder(String profileFolder) {
            argoUMLDriver.profilsFolders.add(profileFolder);
            return this;
        }

        public ArgoUMLDriver build() {
            argoUMLDriver.initializeMDR();
            argoUMLDriver.initializeProfilsManager();
            argoUMLDriver.loadProfils();
            argoUMLDriver.initializeSubsystems();
            return argoUMLDriver;
        }
    }
}
