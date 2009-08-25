package com.ubikproducts.maven.plugins.argo2modello;

public class SubversionConvertTest extends ConvertProjectTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.setTestModel("subversion");
		this.initModelFiles();
	}
	
}
