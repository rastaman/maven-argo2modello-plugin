package com.ubikproducts.maven.plugins.argo2modello;

import java.io.File;

public class TifConvertTest extends ConvertProjectTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.setTestModel( "tif-mini" );
		File destModel = new File( "/Users/ludo/Workspaces/JSONApps/jsonapps/sandbox/tif-model/src/main/model/tif.mdo" );
		this.setDestinationModel( destModel );
		this.initModelFiles();	
		this.setDefaultImports("fr.factory.tif.generic.utils.Biface,fr.factory.tif.generic.utils.BifaceI18n,fr.factory.tif.generic.utils.*,javax.persistence.*,javax.xml.bind.annotation.*,java.util.*,javax.persistence.Query,java.util.Locale,java.util.HashMap,java.util.Vector,java.lang.reflect.Method");
	}
	
}
