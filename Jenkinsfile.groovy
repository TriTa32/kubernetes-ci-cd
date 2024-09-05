pipeline {
    agent any
    
    environment {
        DOCKER_API_VERSION = "1.45"
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
                sh "docker build -t ${env.IMAGE_NAME} -f applications/${env.APP_NAME}/Dockerfile applications/${env.APP_NAME}"
            }
        }
        
        stage('Push') {
            steps {
                sh "docker push ${env.IMAGE_NAME}"
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([string(credentialsId: 'kenzan_kubeconfig', variable: 'KUBECONFIG_CONTENT')]) {
                    sh '''
                        echo "$KUBECONFIG_CONTENT" > kubeconfig
                        chmod 600 kubeconfig
                        
                        kubectl --kubeconfig ./kubeconfig version
                        
                        sed -i 's|image: .*|image: ${IMAGE_NAME}|' applications/${APP_NAME}/k8s/deployment.yaml
                        
                        kubectl --kubeconfig ./kubeconfig apply -f applications/${APP_NAME}/k8s/
                        
                        kubectl --kubeconfig ./kubeconfig get deployments -n default
                        kubectl --kubeconfig ./kubeconfig get pods -n default
                        
                        rm kubeconfig
                    '''
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}