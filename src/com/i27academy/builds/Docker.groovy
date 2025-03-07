package com.i27academy.builds

class Docker {
    def jenkins
    Docker(jenkins) {
        this.jenkins = jenkins
    }

    def buildApp(){
        jenkins.sh """#!/bin/bash
        echo "building the Eureka application"
        sh mvn clean package -DskipTests=true
        """

    } 
}
