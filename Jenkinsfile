pipeline {
    agent none

    options {
        timestamps()
    }

    stages {
        stage('fanin-fanout — Build & Test') {
            agent {
                docker {
                    image 'maven:3.9.6-eclipse-temurin-17'
                    args '-v $JENKINS_HOME/.m2:/root/.m2:z'
                }
            }
            steps {
                checkout scm
                dir('fanin-fanout') {
                    sh '''
                        mvn -B -ntp clean verify \
                            -Dmaven.test.failure.ignore=false \
                            -Dmaven.repo.local=/root/.m2/repository
                    '''
                }
            }
            post {
                always {
                    junit testResults: 'fanin-fanout/**/surefire-reports/*.xml',
                          allowEmptyResults: false
                }
            }
        }

        stage('fanin-fanout — Metrics Computation') {
            agent {
                docker {
                    image 'maven:3.9.6-eclipse-temurin-17'
                    args '-v $JENKINS_HOME/.m2:/root/.m2:z'
                }
            }
            steps {
                checkout scm
                dir('fanin-fanout') {
                    sh '''
                        mkdir -p metrics-output
                        mvn -B -ntp exec:java \
                            -Dexec.args=". metrics-output" \
                            -Dmaven.repo.local=/root/.m2/repository
                    '''
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'fanin-fanout/metrics-output/**/*',
                                     fingerprint: true,
                                     allowEmptyArchive: true
                }
            }
        }

        stage('afferent-efferent — Build & Test') {
            agent {
                docker {
                    image 'gradle:8.7-jdk21'
                    args '-u root'
                }
            }
            steps {
                checkout scm
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    dir('afferent-efferent') {
                        sh 'chmod +x gradlew'
                        sh './gradlew :AfferentEfferentService:test :AfferentEfferentService:shadowJar --no-daemon'
                    }
                }
            }
            post {
                always {
                    junit testResults: 'afferent-efferent/**/test-results/**/*.xml',
                          allowEmptyResults: true
                }
            }
        }

        stage('defects-discovered — Build & Test') {
            agent {
                docker {
                    image 'node:20-bullseye'
                    args '-u root'
                }
            }
            steps {
                checkout scm
                dir('defects-discovered/pmd') {
                    sh 'npm ci'
                }
                
                dir('defects-discovered') {
                    sh 'npm ci'
                    sh 'npm test'
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'defects-discovered/**/coverage/**/*',
                                     allowEmptyArchive: true
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
            echo "One or more modules failed — check stage logs above."
        }
        unstable {
            echo "One or more modules are unstable — test failures detected."
        }
    }
}