package com.ubikproducts.maven.plugins.argo2modello;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.modello.core.ModelloCore;
import org.codehaus.modello.maven.ModelloJavaMojo;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Generator of Modello models from UML models applied to the Modello {@code org.codehaus.modello.model} 
 * package. 
 * <p>The reverse engineering and generation of the UML file has been done with ArgoUML 0.35-1 from the sources 
 * of Modello 1.8.4.<p>
 * @author lmaitre
 *
 */
public class ModelloConvertTest extends PlexusTestContainer {

    private Argo2ModelloMojo argo2ModelloPlugin;

    private ModelloJavaMojo modelloJavaMojo;

    @Before
    public void setUp() throws Exception {
        modelloJavaMojo = new ModelloJavaMojo();
        argo2ModelloPlugin = Argo2ModelloMojoBuilder.newBuilder().withTestModel("src/test/resources/models/modello-reverse2.uml.zargo")
                .withOtherProfiles("src/test/resources/profils")
                .withJavaProfil("src/main/profiles/default-java.xmi")
                .withDefaultImports("java.util.List,java.util.Map")
                .withExclusions(new String[] {
                        "Comparable",
                        "T",
                        "Class",
                        "Exception"
                })
                .withForcedGeneration()
                .build();
    }

    @Test
    @Ignore
    public void legacy_generate_modello() {
        argo2ModelloPlugin.setLegacyGeneration(true);
        File destinationModel = new File("target/modello-legacy.mdo");
        argo2ModelloPlugin.setDestinationModel(destinationModel);
        try {
            argo2ModelloPlugin.execute();
            assertTrue(destinationModel.exists());
            generateJava(destinationModel);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No error should have happened");
        }
    }

    @Test
    @Ignore
    public void generate_modello() {
        argo2ModelloPlugin.setLegacyGeneration(false);
        File destinationModel = new File("target/modello-new.mdo");
        argo2ModelloPlugin.setDestinationModel(destinationModel);
        try {
            argo2ModelloPlugin.execute();
            assertTrue(destinationModel.exists());
            //generateJava(destinationModel);
        } catch (MojoExecutionException e) {
            e.printStackTrace();
            fail("No error should have happenned");
        }
    }

    @Test
    public void generate_modello_with_model_reader() {
        argo2ModelloPlugin.setLegacyGeneration(false);
        argo2ModelloPlugin.setReaderGeneration(true);
        File destinationModel = new File("target/modello-reader.mdo");
        argo2ModelloPlugin.setDestinationModel(destinationModel);
        try {
            argo2ModelloPlugin.execute();
            assertTrue(destinationModel.exists());
            generateJava(destinationModel);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No error should have happenned");
        }
    }

    private void generateJava(File modelloModel) throws Exception {
/*
        ModelloCore modelloCore = (ModelloCore) lookup( ModelloCore.ROLE );
        BuildContext buildContext = (BuildContext) lookup( BuildContext.class );
        ModelloJavaMojo mojo = new ModelloJavaMojo();
        File outputDirectory = getTestFile( "target/java-test" );
        FileUtils.deleteDirectory( outputDirectory );
        mojo.setOutputDirectory( outputDirectory );
        String models[] = new String[1];
        models[0] = getTestPath( "src/test/resources/java-model.mdo" );
        mojo.setModels( models );
        mojo.setVersion( "1.0.0" );
        mojo.setPackageWithVersion( false );
        mojo.setPackagedVersions( Arrays.asList( new String[] { "0.9.0", "1.0.0" } ) );
        mojo.setModelloCore( modelloCore );
        mojo.setBuildContext( buildContext );
        mojo.execute();        
 */
        ModelloCore modelloCore = (ModelloCore) lookup( ModelloCore.ROLE );
        BuildContext buildContext = (BuildContext) lookup( BuildContext.class );

        File destinationFolder = new File("./target",modelloModel.getName().replaceAll("\\.mdo", ""));
        FileUtils.deleteDirectory(destinationFolder);
        modelloJavaMojo.setOutputDirectory(destinationFolder);
        modelloJavaMojo.setModels(new String[] {
                modelloModel.getAbsolutePath()
        });
        modelloJavaMojo.setVersion( "1.0.0" );
        modelloJavaMojo.setPackageWithVersion( false );
        modelloJavaMojo.setModelloCore( modelloCore );
        modelloJavaMojo.setBuildContext( buildContext );
        modelloJavaMojo.execute();
/*        
        modelloJavaMojo.setBasedir(destinationFolder.getAbsolutePath());
        BuildContext context = null;
        modelloJavaMojo.setBuildContext(context);
        Log log = null;
        modelloJavaMojo.setLog(log);
        ModelloCore modelloCore = null;
        modelloJavaMojo.setModelloCore(modelloCore);
        modelloJavaMojo.setModels(new String[] {
                modelloModel.getAbsolutePath()
        });
        modelloJavaMojo.setOutputDirectory(destinationFolder);
        List<String> packagedVersions = new ArrayList<String>();
        modelloJavaMojo.setPackagedVersions(packagedVersions);
        modelloJavaMojo.setPackageWithVersion(false);
        Map<Object,Object> pluginContext = new HashMap<Object,Object>();
        modelloJavaMojo.setPluginContext(pluginContext);
        MavenProject project = null;
        modelloJavaMojo.setProject(project);
        String version = null;
        modelloJavaMojo.setVersion(version);
*/
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

        public Argo2ModelloMojoBuilder withExclusions(String[] exclusions) {
            String excludedClasses = Arrays.asList(exclusions).stream()
                    .collect(Collectors.joining(","));
            argo2modelloMojo.setExcludedClasses(excludedClasses);
            return this;
        }

        public Argo2ModelloMojo build() {
            return argo2modelloMojo;
        }
    }
}
