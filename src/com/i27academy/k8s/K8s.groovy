// K8s.groovy (in a shared library or script)
package com.i27academy.k8s

class K8s {
    def jenkins
    
    K8s(jenkins) {
        this.jenkins = jenkins
    }

    def auth_login(gke_name, gke_zone, gke_project) {
        jenkins.sh """#!/bin/bash
        echo "Enterting the Authentication method for GKE cluster login"
        gcloud config set account jenkins@final-devops-project-445009.iam.gserviceaccount.com
        gcloud compute instances list
        echo "Successfully authenticated"
        gcloud container clusters get-credentials $gke_name --zone $gke_zone --project $gke_project

        kubectl get nodes
        kubectl get pods
        echo "Kube config file fetched successfully"
        """
    }

    def k8sdeploy(fileName, docker_image, namespace) {
        jenkins.sh """#!/bin/bash 
        echo "Executing the K8s Deploy Method"
        echo "Final tag is $docker_image"
        sed -i "s|DIT|$docker_image|g" ./.cicd/$fileName
        kubectl apply -f ./.cicd/$fileName -n $namespace
        echo "K8s deploy done successfully"
        """
    }

    def helmChartDeploy(appName, env, helmChartPath) {
        jenkins.sh """#!/bin/bash
        echo "*************** Entering Helm Repo ***************"
        helm install ${appName}-${env}-chart -f ./.cicd/k8s/values_${env}.yaml  ${helmChartPath}
        """
    }

    def gitClone() {
        jenkins.sh """#!/bin/bash
        echo "*************** Cloning my shared lib ***************"
        git clone https://github.com/rdoonabo/shared-lib-ravi.git
        ls -la
        ls -la shared-lib-ravi/chart
        """
    }
}
