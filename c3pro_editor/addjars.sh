#!/bin/sh

mvn deploy:deploy-file -DgroupId=univie -DartifactId=c3pro   -Dversion=1.0.0   -Dpackaging=jar -Dfile=../c3pro.jar            -Durl=file:repo
mvn deploy:deploy-file -DgroupId=jbpt   -DartifactId=jbpt    -Dversion=0.2.348 -Dpackaging=jar -Dfile=../lib/jbpt-0.2.348.jar -Durl=file:repo
mvn deploy:deploy-file -DgroupId=jgraph -DartifactId=jgraphx -Dversion=2.1.0.0 -Dpackaging=jar -Dfile=../lib/jgraphx.jar      -Durl=file:repo
mvn deploy:deploy-file -DgroupId=log4j  -DartifactId=log4j   -Dversion=1.2.15  -Dpackaging=jar -Dfile=../lib/log4j-1.2.15.jar -Durl=file:repo
mvn deploy:deploy-file -DgroupId=c3pro  -DartifactId=jdom    -Dversion=1.0.0   -Dpackaging=jar -Dfile=../lib/jdom.jar -Durl=file:repo
mvn deploy:deploy-file -DgroupId=indygemma  -DartifactId=cbsbot    -Dversion=0.1.0   -Dpackaging=jar -Dfile=../lib/cbsbot-0.1.0-SNAPSHOT.jar -Durl=file:repo

#collections-generic-4.01.jar
#colt-1.2.0.jar
#jung-algorithms-2.0.1.jar
#jung-api-2.0.1.jar
#jung-graph-impl-2.0.1.jar
#junit_4_10.jar
#log4j-1.2.15.jar
#log4j.properties
#org.json.jar
