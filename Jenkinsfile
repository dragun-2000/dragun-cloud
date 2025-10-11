pipeline {
    agent any

    environment {
        // Lấy AWS credentials từ Jenkins Credentials Manager
        AWS_ACCESS_KEY_ID     = 'AKIA3TD2SE6JKYAZHT7X'
        AWS_SECRET_ACCESS_KEY = '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'

        // Tag Docker image theo branch
        IMAGE_NAME = "dragun-cloud"
        IMAGE_TAG  = "develop-${BUILD_NUMBER}"
    }

    options {
        // Giữ log sạch và timeout hợp lý
        timestamps()
        timeout(time: 20, unit: 'MINUTES')
    }

    triggers {
        // Tự động build khi push branch develop
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
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                echo "🐳 Building Docker image..."
                sh """
                    # Build image với tag chính xác
                    docker build -t ${IMAGE_NAME}:latest .
                    docker tag ${IMAGE_NAME}:latest ${IMAGE_NAME}:${IMAGE_TAG}
                """
            }
        }

        // stage('Test (optional)') {
        //     when {
        //         branch 'develop'
        //     }
        //     steps {
        //         echo "🧪 Running unit tests..."
        //         sh './mvnw test'
        //     }
        // }

        stage('Deploy') {
            when {
                branch 'develop'
            }
            steps {
                echo "🚀 Deploying Docker containers..."
                sh """
                    export AWS_ACCESS_KEY_ID=${env.AWS_ACCESS_KEY_ID}
                    export AWS_SECRET_ACCESS_KEY=${env.AWS_SECRET_ACCESS_KEY}
                    echo "===> Current directory: $(pwd)"
                    echo "===> Files:"
                    ls -al
                    echo "===> Docker Compose version:"
                    docker compose version
                    echo "===> Bringing containers up..."
                    docker compose down
                    docker compose up -d
                    docker ps -a
                """
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
