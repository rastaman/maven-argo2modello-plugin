package com.ubikproducts.maven.plugins.argo2modello;

import java.io.File;

public class TifConvertTest extends ConvertProjectTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.setTestModel( "tif" );
		//this.setDestinationModel( new File( "/Users/ludo/Workspaces/JSONApps/jsonapps/webapps/weblive/src/main/model/tif.mdo" ) );
		this.initModelFiles();
	}
	
}
