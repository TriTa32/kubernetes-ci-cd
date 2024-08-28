pipeline {
    agent {
        docker {
            image 'maven:latest'
        }
    }
    stages {
        stage('Checkout SCM') {
            steps {
                checkout scm
            }
        }
        stage('Set Environment Variables') {
            steps {
                script {
                    env.DOCKER_API_VERSION = "1.23"
                    
                    sh "git rev-parse --short HEAD > commit-id"
                    
                    tag = readFile('commit-id').replace("\n", "").replace("\r", "")
                    appName = "hello-kenzan"
                    registryHost = "127.0.0.1:30400/"

                    imageName = "${registryHost}${appName}:${tag}"
                    env.BUILDIMG = imageName
                }
            }
        }
        stage('Build') {
            steps {
                sh "docker build -t ${env.BUILDIMG} -f applications/hello-kenzan/Dockerfile applications/hello-kenzan"
            }
        }
        stage('Push') {
            steps {
                sh "docker push ${env.BUILDIMG}"
            }
        }
        stage('Deploy') {
            steps {
                kubernetesDeploy configs: "applications/${appName}/k8s/*.yaml", kubeconfigId: 'kenzan_kubeconfig'
            }
        }
    }
}