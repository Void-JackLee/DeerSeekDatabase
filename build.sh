#!/bin/sh 
rm *.jar
mvn clean package -Ppro -DskipTests
rm target/original*
mv target/*.jar .
