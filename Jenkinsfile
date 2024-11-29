pipeline {
  agent any
  tools {
    maven 'Maven 3.8.5'
    jdk 'OpenJDK11'
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    skipStagesAfterUnstable()
    timestamps()
  }
  environment {
    JETTY_PORT = getPort()
  }
  stages {

    stage('Maven build: Main project') {
      steps {
        configFileProvider([
            configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709', variable: 'MAVEN_SETTINGS')
          ]) {
          sh 'mvn -s ${MAVEN_SETTINGS} -Djetty.port=${JETTY_PORT} clean test verify dependency:analyze -Pclb-build -U'
        }
      }
    }

    stage('Build and push Docker images: Checklistbank workflow') {
      steps {
        sh 'build/checklistbank-workflow-docker-build.sh'
      }
    }
  }

    post {
      success {
        echo 'Pipeline executed successfully!'
      }
      failure {
        echo 'Pipeline execution failed!'
    }
  }
}

def getPort() {
  try {
      return new ServerSocket(0).getLocalPort()
  } catch (IOException ex) {
      System.err.println("no available ports");
  }
}
