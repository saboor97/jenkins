def call() {

    pipeline {
        agent {
            label 'devops-agent'
        }

        parameters {
            string(name: 'IMAGE_NAME', defaultValue: 'myapp', description: 'Docker image name')
            string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker image tag')
            string(name: 'CONTAINER_NAME', defaultValue: 'myapp-container', description: 'Container name')
            booleanParam(name: 'BUILD_IMAGE', defaultValue: true, description: 'Build Docker image or skip')
        }

        environment {
            PROJECT_DIR = "${env.WORKSPACE}/project"
        }

        stages {

            stage('Checkout') {
                steps {
                    checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/saboor97/github-practice.git']])
                }
            }
            stage('SSH Command') {
                steps {
                        sh '''
                        hostname -I
                    '''
                    withCredentials([usernamePassword(
                        credentialsId: 'ssh-creds',
                        usernameVariable: 'SSH_USER',
                        passwordVariable: 'SSH_PASS'
                    )]) {
                        sh '''
                        sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no -p 55914 $SSH_USER@116.202.196.159 "hostname -I"
                        '''
                    }
                }
            }
            stage('Build Docker Image') {
                when {
                    expression { params.BUILD_IMAGE == true }
                }
                steps {
                    sh """
                        cd ${PROJECT_DIR}
                        docker build -t ${params.IMAGE_NAME}:${params.IMAGE_TAG} .
                    """
                }
            }

            stage('Skip Build') {
                when {
                    expression { params.BUILD_IMAGE == false }
                }
                steps {
                    echo "Skipping Docker build..."
                }
            }

            stage('List Images') {
                steps {
                    sh "docker images | grep ${params.IMAGE_NAME} || true"
                }
            }

            stage('Run Container') {
                steps {
                    sh """
                        docker rm -f ${params.CONTAINER_NAME} || true

                        docker run -d \
                        --name ${params.CONTAINER_NAME} \
                        -p 8080:8080 \
                        ${params.IMAGE_NAME}:${params.IMAGE_TAG}
                    """
                }
            }
        }

        post {
            success {
                echo "Pipeline completed successfully 🚀"
            }
            failure {
                echo "Pipeline failed ❌"
            }
        }
    }
}
