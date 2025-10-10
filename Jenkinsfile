pipeline {
    agent any

    environment {
        DEPLOY_DIR = '/home/dragun/project/dragun-app'
        GIT_REPO = 'https://github.com/dragun-2000/dragun-cloud.git'
        GIT_BRANCH = 'main'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "📦 Pulling latest source code..."
                dir("${DEPLOY_DIR}") {
                    git branch: "${GIT_BRANCH}", url: "${GIT_REPO}"
                }
            }
        }

        stage('Build & Test') {
            steps {
                echo '🧱 Running Maven build and unit tests...'
                dir("${DEPLOY_DIR}") {
                    sh 'mvn -B clean test'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                dir("${DEPLOY_DIR}") {
                    sh 'docker compose build --no-cache'
                }
            }
        }

        stage('Deploy Application') {
            steps {
                echo '🚀 Deploying updated containers...'
                dir("${DEPLOY_DIR}") {
                    sh 'docker compose down'
                    sh 'docker compose up -d'
                }
            }
        }
    }

    post {
        success {
            echo '✅ CI/CD pipeline completed successfully!'
            sh 'curl -X POST https://api.telegram.org/bot<YOUR_BOT_TOKEN>/sendMessage -d "chat_id=<YOUR_CHAT_ID>&text=✅ dragun-cloud deployed successfully!" || true'
        }
        failure {
            echo '❌ Build or deployment failed!'
            sh 'curl -X POST https://api.telegram.org/bot<YOUR_BOT_TOKEN>/sendMessage -d "chat_id=<YOUR_CHAT_ID>&text=❌ dragun-cloud build failed!" || true'
        }
    }
}
