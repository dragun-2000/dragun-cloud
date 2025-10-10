pipeline {
    agent any

    environment {
        DOCKER_COMPOSE_PATH = '/home/dragun/project/dragun-app/docker-compose.yml'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "📥 Pulling latest code from GitHub..."
                git branch: 'develop', url: 'https://github.com/dragun-2000/dragun-cloud.git'
            }
        }

        stage('Build Docker Images') {
            steps {
                echo "⚙️ Building Docker images..."
                sh '''
                    set -e
                    docker compose -f $DOCKER_COMPOSE_PATH build
                '''
            }
        }

        stage('Deploy Containers') {
            steps {
                echo "🚀 Restarting containers..."
                sh '''
                    set -e
                    docker compose -f $DOCKER_COMPOSE_PATH down
                    docker compose -f $DOCKER_COMPOSE_PATH up -d
                '''
            }
        }

        stage('Verify Deployment') {
            steps {
                echo "🔍 Checking running containers..."
                sh 'docker ps'
            }
        }
    }

    post {
        success {
            echo "✅ Deployment successful!"
        }
        failure {
            echo "❌ Deployment failed. Check logs for details."
        }
    }
}
