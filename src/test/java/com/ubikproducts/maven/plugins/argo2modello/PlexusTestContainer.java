package com.ubikproducts.maven.plugins.argo2modello;

/*
 * Copyright 2001-2006 Codehaus Foundation.
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
import java.io.InputStream;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.composition.CycleDetectedInComponentGraphException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.DefaultContext;
import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Before;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

/**
 * @author Jason van Zyl
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:michal@codehaus.org">Michal Maczka</a>
 * @version $Id: PlexusTestCase.java 8420 2009-09-18 22:56:56Z hboutemy $
 */
public class PlexusTestContainer {

    private PlexusContainer container;

    private BuildContext buildContext;

    private Logger logger = new ConsoleLogger(AbstractLogger.LEVEL_DEBUG,"PlexusTestContainer");

    private static String basedir;

    @Before
    public void setUp() throws Exception {
        basedir = getBasedir();
        this.setupContainer();
    }

    protected void setupContainer() {
        // ----------------------------------------------------------------------------
        // Context Setup
        // ----------------------------------------------------------------------------

        DefaultContext context = new DefaultContext();

        context.put("basedir", getBasedir());

        customizeContext(context);

        boolean hasPlexusHome = context.contains("plexus.home");

        if (!hasPlexusHome) {
            File f = getTestFile("target/plexus-home");

            if (!f.isDirectory()) {
                f.mkdir();
            }

            context.put("plexus.home", f.getAbsolutePath());
        }

        // ----------------------------------------------------------------------------
        // Configuration
        // ----------------------------------------------------------------------------

        String config = getCustomConfigurationName();

        ContainerConfiguration containerConfiguration = new DefaultContainerConfiguration().setName("test")
                .setContext(context.getContextData());

        if (config != null) {
            containerConfiguration.setContainerConfiguration(config);
        } else {
            String resource = getConfigurationName(null);

            containerConfiguration.setContainerConfiguration(resource);
        }

        customizeContainerConfiguration(containerConfiguration);

        try {
            container = new DefaultPlexusContainer(containerConfiguration);
        } catch (PlexusContainerException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create plexus container.");
        }

        buildContext = getBuildContext();

        try {
            container.addComponent(buildContext, BuildContext.class.getName());
        } catch (CycleDetectedInComponentGraphException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to add buildContext.");
        }
    }

    /**
     * Allow custom test case implementations do augment the default container
     * configuration before executing tests.
     *
     * @param containerConfiguration
     */
    protected void customizeContainerConfiguration(ContainerConfiguration containerConfiguration) {
    }

    protected void customizeContext(Context context) {
    }

    protected PlexusConfiguration customizeComponentConfiguration() {
        return null;
    }

    protected void tearDown() throws Exception {
        if (container != null) {
            container.dispose();

            container = null;
        }
    }

    protected PlexusContainer getContainer() {
        if (container == null) {
            setupContainer();
        }

        return container;
    }

    protected InputStream getConfiguration() throws Exception {
        return getConfiguration(null);
    }

    protected InputStream getConfiguration(String subname) throws Exception {
        return getResourceAsStream(getConfigurationName(subname));
    }

    protected String getCustomConfigurationName() {
        return null;
    }

    /**
     * Allow the retrieval of a container configuration that is based on the
     * name of the test class being run. So if you have a test class called
     * org.foo.FunTest, then this will produce a resource name of
     * org/foo/FunTest.xml which would be used to configure the Plexus container
     * before running your test.
     *
     * @param subname
     * @return
     */
    protected String getConfigurationName(String subname) {
        return getClass().getName().replace('.', '/') + ".xml";
    }

    protected InputStream getResourceAsStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    protected ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    // ----------------------------------------------------------------------
    // Container access
    // ----------------------------------------------------------------------

    protected Object lookup(String componentKey) throws Exception {
        return getContainer().lookup(componentKey);
    }

    protected Object lookup(String role, String roleHint) throws Exception {
        return getContainer().lookup(role, roleHint);
    }

    protected <T> T lookup(Class<T> componentClass) throws Exception {
        return getContainer().lookup(componentClass);
    }

    protected <T> T lookup(Class<T> componentClass, String roleHint) throws Exception {
        return getContainer().lookup(componentClass, roleHint);
    }

    protected void release(Object component) throws Exception {
        getContainer().release(component);
    }

    protected BuildContext getBuildContext() {
        if (buildContext == null) {
            DefaultBuildContext context = new DefaultBuildContext();
            context.enableLogging(logger);
            buildContext = context;
        }
        return buildContext;
    }

    // ----------------------------------------------------------------------
    // Helper methods for sub classes
    // ----------------------------------------------------------------------

    public static File getTestFile(String path) {
        return new File(getBasedir(), path);
    }

    public static File getTestFile(String basedir, String path) {
        File basedirFile = new File(basedir);

        if (!basedirFile.isAbsolute()) {
            basedirFile = getTestFile(basedir);
        }

        return new File(basedirFile, path);
    }

    public static String getTestPath(String path) {
        return getTestFile(path).getAbsolutePath();
    }

    public static String getTestPath(String basedir, String path) {
        return getTestFile(basedir, path).getAbsolutePath();
    }

    public static String getBasedir() {
        if (basedir != null) {
            return basedir;
        }

        basedir = System.getProperty("basedir");

        if (basedir == null) {
            basedir = new File("").getAbsolutePath();
        }

        return basedir;
    }

    public String getTestConfiguration() {
        return getTestConfiguration(getClass());
    }

    public static String getTestConfiguration(Class<?> clazz) {
        String s = clazz.getName().replace('.', '/');

        return s.substring(0, s.indexOf("$")) + ".xml";
    }
}
