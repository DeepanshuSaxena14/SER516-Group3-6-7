pipeline {
	agent any

	// environment {
	// 	COMPOSE_PROJECT_NAME = "ser516"
	// }

	tools {
		nodejs 'NodeJS'
	}

	options {
		timestamps()
	}

	stages {
		stage('Checkout') {
			steps {
				echo "Checking out source code..."
				checkout scm
			}
		}

		// stage('Stop Old Containers') {
		// 	steps {
		// 		echo "Stopping existing containers..."
		// 		sh 'docker compose down || true'
		// 	}
		// }

		stage('Install Dependencies') {
			steps {
				echo "Installing npm dependencies..."
				sh 'npm install'
			}
		}

		// stage('Build Docker Images') {
		// 	steps {
		// 		echo "Building Docker images..."
		// 		sh 'docker compose build'
		// 	}
		// }

		// stage('Run PMD Tests') {
		// 	steps {
		// 		echo "Running PMD uni tests..."
		// 		sh 'docker compose run --rm pmd npm test'
		// 	}
		// }

		// stage('Deploy Application') {
		// 	steps {
		// 		echo "Starting application..."
		// 		sh 'docker compose up -d'
		// 	}
		// }

		stage('Run Vitest') {
			steps {
				echo "Running vitest unit tests..."
				sh 'npm run test'
			}
		}
	}

	post {
		success {
			echo "Build succeeded. Application deployed successfully."
		}

		failure {
			echo "Build failed. Fix defects before merging."
		}

		always {
			echo "Pipeline execution completed."
		}
	}
}
