#!/usr/bin/perl
use strict;

my $argoFolder = $ARGV[0] || "/Volumes/CodeSource/Workspaces/Catanotes/argouml";
my $argoVersion = $ARGV[1] || "0.28";  
 
my %namesToGroups = (
  'argouml-mdr' => 'org.tigris.argouml',
  'argouml-model' => 'org.tigris.argouml',
  'argouml' => 'org.tigris.argouml',
  'argouml-diagrams-sequence' => 'org.tigris.argouml',
  'gef' => 'org.tigris.gef',
  'java-interfaces' => 'org.omg.uml',
  'jmi' => 'javax.jmi',
  'jmiutils' => 'javax.jmi',
  'mdrapi' => 'org.netbeans.mdr',
  'mof' => 'org.omg.mof',
  'nbmdr' => 'org.netbeans.mdr',
  'ocl-argo' => 'org.tigris.argouml',
  'openide-util' => 'org.netbeans.openide',
  'swidgets' => 'org.tigris',
  'toolbar' => 'org.tigris',
);

my @doNotDeploy = [
   'commons-logging',
   'log4j',
   'antlr'
];

main();

sub main
{
   my $i = 0;
   my @lines = `find $argoFolder/src/argouml-build/build -name *.jar`;
   my $installScript = "#!/bin/sh\n";
   my $mavenDeps = "";
   foreach my $line (@lines) {
       $line =~ /^.*\/(.*)\.jar$/;
       my $filename = $1;
       print "Found $filename\n";       
       $filename =~ /(.*?)-([0-9].*)$/;
       my $name = $1;
       my $group = %namesToGroups->{$name};
       if (!$group) {
          $group = $name;
       }       
       my $version = $2;
       if (!$version) {
          $name = $filename;
          $version = $name =~ /argo/ ? $argoVersion : "1.0";
       }
       print "Name: $name Version: $version Group:$group\n";  
       $installScript .= "mvn deploy:deploy-file -DgroupId=$group -DartifactId=$name -Dversion=$version -DrepositoryId=ubik-central -Durl=http://forge.ubik-products.com/archiva/repository/ubik-central -Dpackaging=jar -Dfile=$line\n";          
       $mavenDeps .= "<dependency>
  <groupId>$group</groupId>
  <artifactId>$name</artifactId>
  <version>$version</version>
</dependency>\n";
       $i++;
   }
   print "$i files\n";
   print "Install script:\n";
   print "$installScript";
   open(DEPLOY_FILE,">deploy.sh") || die "can't write: $!";
   print DEPLOY_FILE $installScript;
   close(DEPLOY_FILE);   
   open(DEPS_FILE,">deps.xml") || die "can't write: $!";
   print DEPS_FILE $mavenDeps;
   close(DEPS_FILE);   
   print "Maven dependencies:\n";
   print "$mavenDeps";
}
