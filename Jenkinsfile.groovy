pipeline {
    agent any
    
    environment {
        DOCKER_API_VERSION = "1.45"
        APP_NAME = "hello-kenzan"
        REGISTRY_HOST = "127.0.0.1:30400/"
        KUBECONFIG = credentials('kenzan_kubeconfig')
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
                sh "docker build -t ${env.IMAGE_NAME} -f applications/${env.APP_NAME}/Dockerfile applications/${env.APP_NAME}"
            }
        }
        
        stage('Push') {
            steps {
                sh "docker push ${env.IMAGE_NAME}"
            }
        }
        
        stage('Check Kubernetes Version') {
            steps {
                sh 'kubectl --kubeconfig $KUBECONFIG version'
            }
        }
        
        stage('Debug Kubernetes Resources') {
            steps {
                sh "cat applications/${env.APP_NAME}/k8s/*.yaml"
            }
        }
        
        stage('Update Deployment YAML') {
            steps {
                sh "sed -i 's|image: .*|image: ${env.IMAGE_NAME}|' applications/${env.APP_NAME}/k8s/deployment.yaml"
            }
        }
        
        stage('Deploy') {
            steps {
                sh "kubectl --kubeconfig $KUBECONFIG apply -f applications/${env.APP_NAME}/k8s/"
            }
        }
        
        stage('Verify Deployment') {
            steps {
                sh "kubectl --kubeconfig $KUBECONFIG get deployments -n default"
                sh "kubectl --kubeconfig $KUBECONFIG get pods -n default"
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}