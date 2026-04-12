pipeline {
    agent {
        label 'devops-agent'
    }

    parameters {

        string(name: 'IMAGE_NAME', defaultValue: 'myapp', description: 'Docker image name')

        string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker image tag')

        string(name: 'CONTAINER_NAME', defaultValue: 'myapp-container', description: 'Container name')

        booleanParam(name: 'BUILD_IMAGE', defaultValue: true, description: 'Build Docker image or skip it')
    }

    environment {
        PROJECT_DIR = "${WORKSPACE}/project"
    }

    stages {

        stage('Checkout') {
            steps {
                git url: 'https://github.com/saboor97/github-practice.git', branch: 'main'
            }
        }

        stage('Build Docker Image') {
            when {
                expression { params.BUILD_IMAGE == true }
            }
            steps {
                script {
                    sh """
                        cd ${PROJECT_DIR}
                        docker build -t ${params.IMAGE_NAME}:${params.IMAGE_TAG} .
                    """
                }
            }
        }

        stage('Skip Build Message') {
            when {
                expression { params.BUILD_IMAGE == false }
            }
            steps {
                echo "Docker build skipped as BUILD_IMAGE is false"
            }
        }

        stage('List Docker Images') {
            steps {
                sh "docker images | grep ${params.IMAGE_NAME} || true"
            }
        }

        stage('Run Container') {
            steps {
                script {
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
    }

    post {
        success {
            echo "Pipeline executed successfully"
        }

        failure {
            echo "Pipeline failed"
        }
    }
}
