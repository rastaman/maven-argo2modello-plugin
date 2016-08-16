package com.ubikproducts.maven.plugins.argo2modello;

import org.junit.BeforeClass;

public class SubversionConvertTest extends ConvertProjectTest {

    @BeforeClass
    protected void setUp() throws Exception {
        this.setTestModel("subversion");
        this.initModelFiles();
    }

}
