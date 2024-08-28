pipeline {
    agent any
    
    environment {
        DOCKER_API_VERSION = "1.47"
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
        
        stage('Build') {
            steps {
                sh "sudo docker build -t ${env.IMAGE_NAME} -f applications/${env.APP_NAME}/Dockerfile applications/${env.APP_NAME}"
            }
        }
        
        stage('Push') {
            steps {
                sh "sudo docker push ${env.IMAGE_NAME}"
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