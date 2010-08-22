= How To =

In order to use the plugin with a profile you need to setup the ArgoUML search profiles path.
To do this you need to have a java property file call "argo.user.properties" in the ArgoUML preferences folder of your homedir
which indicate where the plugin (and ArgoUML) will find the profiles if you create an UML project which use user defined profile.
The java property to create or adjust in this file is called "argo.profiles.directories" and usually point to the src/main/profiles folder
of your project. 

Here is for instance the relevant properties on my home computer (i hopde their values are self-explanatory):
<pre>
snowquad-4:proambu ludo$ cat ~/.argouml/argo.user.properties | grep profile
argo.profiles.default=CUML 1.4*CJava*CGoodPractices*CCodeGeneration*Ufile\:/Users/ludo/Workspaces/JSONApps/webapps/proambu/src/main/profiles/jsonapps-profile.xmi*
argo.defaultModel=/org/argouml/model/mdr/profiles/default-uml14.xmi
argo.profiles.directories=/Users/ludo/UMLProfiles*/Users/ludo/Workspaces/JSONApps/webapps/proambu/src/main/profiles*
</pre>

