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
        stage('Docker Build') {
            steps { sh 'docker build -t jeu-allumettes:${BUILD_NUMBER} .' }
        }
        stage('Smoke Test') {
            steps { sh 'docker run --rm jeu-allumettes:${BUILD_NUMBER} A@expert B@rapide' }
        }
    }
    post {
        always { junit 'target/surefire-reports/*.xml' }
        success { archiveArtifacts artifacts: 'target/*.jar' }
    }
}