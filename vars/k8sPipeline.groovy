import com.i27academy.k8s.K8s

def call(Map pipelineparams){
    K8s k8s = new K8s(this)
    pipeline {
    agent {
        label 'k8s-slave'
    }
    tools {
        maven  'Maven3.8.8'
        jdk 'java17'
    }
    
    parameters {
        choice(name: 'sonarScans',
            choices: 'no\nyes',
            description: 'This will scan the applicaiton using sonar'
        )
        choice(name:'buildOnly',
            choices: 'no\nyes',
            description: 'This will only build the application'
        )
        choice(name: 'dockerPush',
                choices: 'no\nyes',
                description: "This will trigger the build, docker build and docker push"
            )
        choice(name: 'deployToDev',
                choices: 'no\nyes',
                description: "This will Deploy my app to Dev env"
            )
        choice(name: 'deployToTest',
                choices: 'no\nyes',
                description: "This will Deploy my app to Test env"
            )
        choice(name: 'deployToStage',
                choices: 'no\nyes',
                description: "This will Deploy my app to Stage env"
            )
        choice(name: 'deployToProd',
                choices: 'no\nyes',
                description: "This will Deploy my app to Prod env"
            )
    }

    environment {
        APPLICATION_NAME = "${pipelineparams.appName}"
     // APPLICATION_NAME = "eureka"
        POM_VERSION = readMavenPom().getVersion()
        POM_PACKAGING = readMavenPom().getPackaging()
        DOCKER_HUB = "docker.io/dravikumar442277"
        DOCKER_REPO = "eureka"
        DOCKER_CREDS = credentials('dravikumar442277_docker_creds')
        SONAR_URL = "http://34.123.190.159:9000/"
        SONAR_TOKENS = credentials('sonar_token')
    }

    stages {
        stage ('Authencated to Google Cloud GKE') {
            steps {
                echo "Excuitng in google cloud auth stage"
                echo "testing"
                script {
                   k8s.auth_login()
                }
                
            }

        }
        stage ('Building the application') { 
        when {
            anyOf {
                expression {
                params.dockerPush == 'yes'
                params.buildOnly == 'yes'
                
                } 
            }
        }
            steps {
                script {
                    
                    docker.buildApp("${env.APPLICATION_NAME}")
                }
            } 
        }
        stage ('unit test cases') {
        when {
            anyOf {
                expression {
                params.buildOnly == 'yes'
                params.dockerPush == 'yes'
                
                
                } 
            }
        } 
        steps {
            echo "Performing Unit test cases for ${env.APPLICATION_NAME} application"
            sh "mvn test"  
        }
        post {
        always {
            junit 'target/surefire-reports/*.xml'
        }
        }  
        }
        stage ('Sonar stage now') {
        when {
            anyOf {
                    expression {
                            params.sonarScans == 'yes'
                            params.buildOnly == 'yes'
                            params.dockerPush == 'yes'
                            
                            
                        }
                    }
                }
        steps {
            sh """
            echo " Now started sonar code quality coverage stage now"
            mvn clean verify sonar:sonar \
                -Dsonar.projectKey=127-eureka \
                -Dsonar.host.url=${env.SONAR_URL} \
                -Dsonar.login=${env.SONAR_TOKENS}
            """
        }
        }
        /*stage ('Docker && Custom Format') {
        steps {
        //application name-version:
        echo "actual format: ${env.APPLICATION_NAME}-${env.POM_VERSION}-${env.POM_PACKAGING}"
        // custom names for app jar
        // applicationname-buildnumber-branchnname-packaging
        echo "custm app: ${env.APPLICATION_NAME}-${currentBuild.number}-${BRANCH_NAME}-${env.POM_PACKAGING}"
        }
        } */
        stage ('Docker Build') {
        when {
                anyOf {
                        expression {
                            params.dockerPush == 'yes'
                        }
                    }
        } 
        steps {
            script  {
                dockerBuildandPush().call()
            }  
        }
        }
        stage ('Deploy to Dev') {
        when {
                anyOf {
                        expression {
                            params.deployToDev == 'yes'
                        }
                    }
            } 
        steps {
            script {
                imageValidation().call()
                dockerDeploy ('dev','5761','8761').call()
            }
        }
        }
        stage ('Deploy to test') {
        when {
                anyOf {
                    expression {
                            params.deployToTest == 'yes'
                        }
                    }
            }
        steps {
        script {
            imageValidation().call()
            dockerDeploy ('test','6761','8761').call()
        }
        }
        }
        stage ('Deploy to prod') {
            when {
                    anyOf {
                        expression {
                            params.deployToProd == 'yes'
                        }
                    }
                }
        steps {
        script {
            imageValidation().call()
            dockerDeploy ('prod','7761','8761').call()
        }
        }
        }
    }
    }
}
    // Define the dockerDeploy method outside the pipeline block
    def dockerDeploy(envDeploy, hostPort, contPort) {
        return {
                    echo "*****************Deploying to Test Environment here########################"
                    withCredentials([usernamePassword(credentialsId: 'maha_creds_docker', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
            
                script {
                        // some block
                        sh "sshpass -p ${PASSWORD} -v ssh -o  StrictHostKeyChecking=no  ${USERNAME}@${docker_server_ip} docker pull  ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
                        try {
                            echo "***********stopping the container *********************************************************"
                            sh "sshpass -p ${PASSWORD} -v ssh -o  StrictHostKeyChecking=no  ${USERNAME}@${docker_server_ip} docker stop  ${env.APPLICATION_NAME}-$envDeploy"
                            echo "**************** removing the container ****************************************************"
                            sh "sshpass -p ${PASSWORD} -v ssh -o  StrictHostKeyChecking=no  ${USERNAME}@${docker_server_ip} docker rm  ${env.APPLICATION_NAME}-$envDeploy"
                        } catch (err) {
                            echo "caught the error: $err"
                        }
                        echo "********************** creating the container ****************************************"
                    // Run the container
                    sh "sshpass -p '${PASSWORD}' -v ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} \"docker run --restart always --name ${env.APPLICATION_NAME}-${envDeploy} -p ${hostPort}:${contPort} -d ${env.DOCKER_HUB}/${env.DOCKER_REPO}:${GIT_COMMIT}\""

                    }
                }
        
        }
    }
    def imageValidation() {
        return {
            println("Pulling the Docker image")
            try {
                sh "docker pull ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:$GIT_COMMIT"
                println ("Pull Success,!!! Deploying !!!!!") 
            }
            catch (Exception) {
                println("OOPS, Docker image with this tag is not available")
                println("So, Building the app, creating the image and pushing to registry")
                buildApp().call()
                dockerBuildandPush().call()
            }
        }
    }

    def dockerBuildandPush() {
        return {
            
            sh "cp ${workspace}/target/i27-${env.APPLICATION_NAME}-${env.POM_VERSION}.${env.POM_PACKAGING} ./.cicd"
            echo "listing files in .cicd folder"
            sh "ls -la ./.cicd"
            echo "******************** Building Docker Image ********************"
            sh "docker build --force-rm --no-cache --pull --rm=true --build-arg JAR_SOURCE=i27-${env.APPLICATION_NAME}-${env.POM_VERSION}.${env.POM_PACKAGING} --build-arg JAR_DEST=i27-${env.APPLICATION_NAME}-${currentBuild.number}-${BRANCH_NAME}.${env.POM_PACKAGING} \
                -t ${env.DOCKER_HUB}/${env.DOCKER_REPO}:$GIT_COMMIT ./.cicd"
            
            echo "******************** Logging to Docker Registry ********************"
            sh "docker login -u ${DOCKER_CREDS_USR} -p ${DOCKER_CREDS_PSW}"
            sh "docker push ${env.DOCKER_HUB}/${env.DOCKER_REPO}:$GIT_COMMIT"
            echo "***** push done successfully ****************************"
        }
    }
