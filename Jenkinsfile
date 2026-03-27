pipeline {
    agent none

    options {
        skipDefaultCheckout(true)
        timestamps()
    }

    stages {
        stage('Checkout') {
            agent any
            steps {
                checkout scm
            }
        }

        stage('Build All Modules') {
            parallel {
                stage('fanin-fanout') {
                    agent {
                        docker {
                            image 'maven:3.9.6-eclipse-temurin-17'
                            args '-v $JENKINS_HOME/.m2:/root/.m2:z'
                        }
                    }
                    steps {
                        dir('fanin-fanout') {
                            sh 'mvn -B -ntp clean verify'
                            sh 'mvn -B -ntp exec:java -Dexec.args=". json out"'
                            sh 'bash scripts/service-test.sh || true'
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'fanin-fanout/out/**/*', fingerprint: true, allowEmptyArchive: true
                            junit allowEmptyResults: true, testResults: 'fanin-fanout/target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('afferent-efferent') {
                    agent {
                        docker {
                            image 'gradle:8.7-jdk17'
                        }
                    }
                    steps {
                        dir('afferent-efferent/AfferentEfferentService') {
                            sh './gradlew test build'
                        }
                        dir('afferent-efferent/TaigaService') {
                            sh './gradlew test build'
                        }
                    }
                }

                stage('defects-discovered') {
                    agent {
                        docker {
                            image 'node:20-bullseye'
                        }
                    }
                    steps {
                        dir('defects-discovered') {
                            sh 'npm ci'
                            sh 'npm test'
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'defects-discovered/**/coverage/**/*', allowEmptyArchive: true
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Combined build finished with status: ${currentBuild.currentResult}"
        }
        success {
            echo "All integrated modules built successfully."
        }
        failure {
            echo "One or more modules failed."
        }
    }
}