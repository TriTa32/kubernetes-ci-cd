pipeline {
    agent any
    
    environment {
        DOCKER_API_VERSION = "1.23"
        APP_NAME = "hello-kenzan"
        REGISTRY_HOST = "127.0.0.1:30400/"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Set Build Variables') {
            steps {
                script {
                    env.GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    env.IMAGE_NAME = "${env.REGISTRY_HOST}${env.APP_NAME}:${env.GIT_COMMIT_SHORT}"
                }
            }
        }
        
        stage('Debug Info') {
            steps {
                sh 'whoami'
                sh 'groups'
                sh 'ls -l /var/run/docker.sock'
            }
        }
        
        stage('Build') {
            steps {
                script {
                    try {
                        sh "docker build -t ${env.IMAGE_NAME} -f applications/${env.APP_NAME}/Dockerfile applications/${env.APP_NAME}"
                    } catch (Exception e) {
                        echo "Docker build failed. Error: ${e.getMessage()}"
                        sh 'docker version'
                        sh 'docker info'
                        error "Docker build failed"
                    }
                }
            }
        }
        
        stage('Push') {
            steps {
                sh "docker push ${env.IMAGE_NAME}"
            }
        }
        
        stage('Deploy') {
            steps {
                kubernetesDeploy(
                    configs: "applications/${env.APP_NAME}/k8s/*.yaml",
                    kubeconfigId: 'kenzan_kubeconfig'
                )
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}