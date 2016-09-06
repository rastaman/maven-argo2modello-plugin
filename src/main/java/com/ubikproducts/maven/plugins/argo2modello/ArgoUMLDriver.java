package com.ubikproducts.maven.plugins.argo2modello;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.argouml.kernel.Project;
import org.argouml.model.CoreHelper;
import org.argouml.model.Facade;
import org.argouml.model.Model;
import org.argouml.model.ModelImplementation;
import org.argouml.notation.InitNotation;
import org.argouml.notation.providers.java.InitNotationJava;
import org.argouml.notation.providers.uml.InitNotationUml;
import org.argouml.persistence.PersistenceManager;
import org.argouml.persistence.ProjectFilePersister;
import org.argouml.profile.Profile;
import org.argouml.profile.ProfileFacade;
import org.argouml.profile.internal.ProfileManagerImpl;
import org.argouml.support.GeneratorJava2;

public class ArgoUMLDriver {

    private ProfileManagerImpl profileManagerImpl;

    private final GeneratorJava2 generator;

    private Set<String> profilsFolders;

    private File javaProfile;

    private ArgoUMLDriver() {
        generator = new GeneratorJava2();
    }
    /**
     * The default implementation to start.
     */
    private static final String DEFAULT_MODEL_IMPLEMENTATION = "org.argouml.model.mdr.MDRModelImplementation";

    private String modelImplementationClassname = DEFAULT_MODEL_IMPLEMENTATION;

    private void initializeModelImplementation() {
        initializeModelImplementation(modelImplementationClassname);
    }

    private void initializeModelImplementation(
            String className) {
        try {
            ModelImplementation modelImplementation = (ModelImplementation) Class.forName(className).newInstance();
            Model.setImplementation(modelImplementation);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
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

    private void clearProfils() {
        for ( String searchPath : profileManagerImpl.getSearchPathDirectories() ) {
            profileManagerImpl.removeSearchPathDirectory(searchPath);
        }
    }

    /**
     * Load all the profils registered.
     * <p>This always force reinitialization of the Profil subsystem
     * by clearing the search path folders list.
     */
    private void loadProfils() {
        clearProfils();
        if (javaProfile != null) {
            loadProfil(javaProfile);
        }
        if (profilsFolders != null) {
            profilsFolders.stream().forEachOrdered(s -> {
                File profilFile = getFileForProfil(s);
                loadProfil(profilFile);
            });
        }
        profileManagerImpl.refreshRegisteredProfiles();
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

    public GeneratorJava2 getJavaGenerator() {
        return generator;
    }

    public Facade getFacade() {
        return Model.getFacade();
    }

    public CoreHelper getCoreHelper() {
        return Model.getCoreHelper();
    }

    public Object getFirstModel(Project p) {
        return p.getUserDefinedModelList().iterator().next();
    }

    public List<String> getProfilesList() {
        List<String> profilesList = new ArrayList<String>();
        for (Profile p : profileManagerImpl.getRegisteredProfiles()) {
            profilesList.add(String.format("%s -> %s", p.getDisplayName(), p.getProfileIdentifier()));
        }
        return profilesList;
    }

    public List<String> getSearchPaths() {
        return profileManagerImpl.getSearchPathDirectories();
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

        public ArgoUMLDriverBuilder withJavaProfile(File javaProfile) {
            argoUMLDriver.javaProfile = javaProfile;
            return this;
        }

        public ArgoUMLDriverBuilder withModelImplementation(String modelImplementationClassname) {
            argoUMLDriver.modelImplementationClassname = modelImplementationClassname;
            return this;
        }

        public ArgoUMLDriver build() {
            argoUMLDriver.initializeModelImplementation();
            argoUMLDriver.initializeProfilsManager();
            argoUMLDriver.loadProfils();
            argoUMLDriver.initializeSubsystems();
            return argoUMLDriver;
        }
    }
}
