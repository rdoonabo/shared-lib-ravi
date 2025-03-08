package com.i27academy.builds

class Docker {
    def jenkins
    Docker(jenkins) {
        this.jenkins = jenkins
    }

    def buildApp(appName){
        jenkins.sh """#!/bin/bash
        echo "building the $appName  application"
        mvn clean package -DskipTests=true
        """

    } 
}
