package com.i27academy.K8s

class K8s {
    def jenkins
    K8s(jenkins) {
        this.jenkins = jenkins
    }

   def auth_login() {
    jenkins.sh """#!/bin/bash
    echo "Enterting the Authencation method for GKE cluster login"
    gcloud config set account jenkins@final-devops-project-445009.iam.gserviceaccount.com
    gcloud compute instances list
    """
   }
    
  }
