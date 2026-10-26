pipeline {
    agent any
    tools {
        maven 'maven3'
    }
    triggers {
        pollSCM('H/2 * * * *')   // vérifie GitHub toutes les ~2 min
    }
    stages {
        stage('Build') {
            steps { sh 'mvn -B compile' }
        }
        stage('Test') {
            steps { sh 'mvn -B test' }
        }
        stage('Package') {
            steps { sh 'mvn -B package -DskipTests' }
        }
    }
    post {
        always { junit 'target/surefire-reports/*.xml' }
        success { archiveArtifacts artifacts: 'target/*.jar' }
    }
}