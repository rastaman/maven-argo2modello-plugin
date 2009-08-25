package com.ubikproducts.maven.plugins.argo2modello;

public interface TaggedValueHandler {

	public boolean accept( Object taggedValue );
	
	public String getName( Object taggedVaue );
	
	public String getValue( Object taggedValue );
	
	public void handle( Object taggedValue, Object classElement );

}
