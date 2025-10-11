pipeline {
    agent any

    environment {
        IMAGE_NAME = "dragun-cloud"
        IMAGE_TAG  = "develop-${BUILD_NUMBER}"
    }

    options {
        timestamps()
        timeout(time: 20, unit: 'MINUTES')
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                echo "🔹 Checking out branch: ${env.BRANCH_NAME}"
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo "🔹 Building Spring Boot application..."
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                echo "🐳 Building Docker image..."
                sh """
                    docker compose build --no-cache
                    docker tag ${IMAGE_NAME}:latest ${IMAGE_NAME}:${IMAGE_TAG}
                """
            }
        }

        stage('Deploy') {
            when {
                branch 'develop'
            }
            steps {
                echo "🚀 Deploying Docker containers..."

                withCredentials([[
                    $class: 'AmazonWebServicesCredentialsBinding',
                    credentialsId: 'aws-jenkins-creds'
                ]]) {
                    sh """
                        export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                        export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                        docker compose down
                        docker compose up -d
                    """
                }
            }
        }
    }

    post {
        success {
            echo "✅ Build & Deploy successful! - ${IMAGE_TAG}"
        }
        failure {
            echo "❌ Build or deploy failed!"
        }
    }
}
