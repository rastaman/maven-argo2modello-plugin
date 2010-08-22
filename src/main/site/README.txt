= How To =

In order to use the plugin with a user defined profile you need to setup the ArgoUML search profiles path.
To do this you need to have a java property file call "argo.user.properties" in the ArgoUML preferences folder of your homedir
which indicate where the plugin (and ArgoUML) will find the profiles if you create an UML project which use user defined profile.
The java property to create or adjust in this file is called "argo.profiles.directories" and usually point to the src/main/profiles folder
of your project. 

Here is for instance the relevant properties on my home computer (i hope their values are self-explanatory):
<pre>
snowquad-4:proambu ludo$ cat ~/.argouml/argo.user.properties | grep profile
argo.profiles.default=CUML 1.4*CJava*CGoodPractices*CCodeGeneration*Ufile\:/Users/ludo/Workspaces/JSONApps/webapps/proambu/src/main/profiles/jsonapps-profile.xmi*
argo.defaultModel=/org/argouml/model/mdr/profiles/default-uml14.xmi
argo.profiles.directories=/Users/ludo/UMLProfiles*/Users/ludo/Workspaces/JSONApps/webapps/proambu/src/main/profiles*
</pre>

= Building =

In order to build the maven plugin/project you need to have deployed the ArgoUML JARs to a Maven repository. To do this i provide a perl script called "mkdeps.pl"
which should generate the appropriate lines of shell to deploy the ArgoUML and dependencies JARs. Sorry it didn't generate proper files now so you have to pipe the output to files.

If i remember well my Maven repository ( http://forge.ubik-products.com/archiva/repository/ubik-central/ ) is available for reading and have the latest JARs deployed, you can use it.

= Contact =

To report bugs use the issues manager of the project.
If you need more information about the plugin feel free to ask in the mailing lists or to contact me directly at: rastaman@tigris<DOT>org
I also have contributed an (experimental) modello-to-uml plugin for Modello, it still didn't do full round-trip 
and so is not ready for general use but if you're interested in testing it, let me know!