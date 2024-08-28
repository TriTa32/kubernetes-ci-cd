pipeline {
    agent {
        kubernetes {
            yaml '''
                apiVersion: v1
                kind: Pod
                spec:
                  containers:
                  - name: docker
                    image: docker:dind
                    securityContext:
                      privileged: true
                    volumeMounts:
                    - name: dind-storage
                      mountPath: /var/lib/docker
                  volumes:
                  - name: dind-storage
                    emptyDir: {}
            '''
        }
    }
    
    environment {
        APP_NAME = "hello-kenzan"
        REGISTRY_HOST = "127.0.0.1:30400"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build and Push') {
            steps {
                container('docker') {
                    script {
                        def gitCommit = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                        def imageName = "${REGISTRY_HOST}/${APP_NAME}:${gitCommit}"
                        
                        sh "docker build -t ${imageName} -f applications/${APP_NAME}/Dockerfile applications/${APP_NAME}"
                        sh "docker push ${imageName}"
                    }
                }
            }
        }
        
        stage('Deploy') {
            steps {
                script {
                    def gitCommit = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    def imageName = "${REGISTRY_HOST}/${APP_NAME}:${gitCommit}"
                    
                    kubernetesDeploy(
                        configs: "applications/${APP_NAME}/k8s/*.yaml",
                        kubeconfigId: 'kenzan_kubeconfig',
                        enableConfigSubstitution: true,
                        dockerCredentials: [
                            [credentialsId: 'docker-registry-credentials', url: "http://${REGISTRY_HOST}"]
                        ]
                    )
                }
            }
        }
    }
}