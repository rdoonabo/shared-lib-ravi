package com.i27academy.k8s

class K8s {
    def jenkins
    K8s(jenkins) {
        this.jenkins = jenkins
    }

   def auth_login(gke_name, gke_zone, gke_project) {
    jenkins.sh """#!/bin/bash
    echo "Enterting the Authencation method for GKE cluster login"
    gcloud config set account jenkins@final-devops-project-445009.iam.gserviceaccount.com
    gcloud compute instances list
    echo "successfully "
    gcloud container clusters get-credentials $gke_name --zone $gke_zone --project $gke_project
    
    kubectl get nodes
    kubectl get pods
    echo "get kube config file successfully"
    """
   }


  def k8sdeploy(fileName,docker_image,namespace) {
    jenkins.sh """#!/bin/bash 
    echo "Excuitng the K8s Deploy Method"
    echo "Final tag is $docker_image"
    sed -i "s|DIT|$docker_image|g" ./.cicd/$fileName
    kubectl apply -f ./.cicd/$fileName -n $namespace
    echo "deploy k8s-dev done successfully"
    """
  }  
  }
