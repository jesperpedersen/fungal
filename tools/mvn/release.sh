#!/bin/sh
PWD=`pwd`
mvn deploy:deploy-file -Dfile=../../target/fungal.jar -DpomFile=core.xml -Durl=file:$PWD/../../../www/fungal/maven2/
mvn deploy:deploy-file -Dfile=../../target/fungal-cli.jar -DpomFile=cli.xml -Durl=file:$PWD/../../../www/fungal/maven2/
