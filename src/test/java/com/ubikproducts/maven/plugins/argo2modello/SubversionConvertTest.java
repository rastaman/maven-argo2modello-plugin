package com.ubikproducts.maven.plugins.argo2modello;

import org.junit.Before;

public class SubversionConvertTest extends ConvertProjectTest {

    @Before
    public void setUp() throws Exception {
        this.setTestModel("subversion");
        this.initModelFiles();
    }

}
