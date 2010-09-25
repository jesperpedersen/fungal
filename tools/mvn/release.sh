#!/bin/sh
PWD=`pwd`
VERSION=0.9.0.Beta1
mvn deploy:deploy-file -Dfile=../../target/fungal.jar -DpomFile=core.xml -Durl=file:$PWD/../../../www/fungal/maven2/
mvn deploy:deploy-file -Dfile=../../target/fungal-sources.jar -DgeneratePom=false -DgroupId=com.github.fungal -DartifactId=fungal -Dversion=$VERSION -Dclassifier=sources -Dpackaging=jar -Durl=file:$PWD/../../../www/fungal/maven2/
mvn deploy:deploy-file -Dfile=../../target/fungal-javadoc.jar -DgeneratePom=false -DgroupId=com.github.fungal -DartifactId=fungal -Dversion=$VERSION -Dclassifier=javadoc -Dpackaging=jar -Durl=file:$PWD/../../../www/fungal/maven2/
mvn deploy:deploy-file -Dfile=../../target/fungal-cli.jar -DpomFile=cli.xml -Durl=file:$PWD/../../../www/fungal/maven2/
mvn deploy:deploy-file -Dfile=../../target/fungal-cli-sources.jar -DgeneratePom=false -Dclassifier=sources -Dpackaging=jar -DgroupId=com.github.fungal -DartifactId=fungal-cli -Dversion=$VERSION -Durl=file:$PWD/../../../www/fungal/maven2/
