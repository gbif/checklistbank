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
  parameters {
    separator(name: "release_separator", sectionHeader: "Release Parameters")
    booleanParam(name: 'RELEASE', defaultValue: false, description: 'Do a Maven release')
    string(name: 'RELEASE_VERSION', defaultValue: '', description: 'Release version (optional)')
    string(name: 'DEVELOPMENT_VERSION', defaultValue: '', description: 'Development version (optional)')
    booleanParam(name: 'DRY_RUN_RELEASE', defaultValue: false, description: 'Dry Run Maven release')
  }
  environment {
    JETTY_PORT = getPort()
    POM_VERSION = readMavenPom().getVersion()
  }
  stages {

    stage('Maven build: Main project') {
      when {
        allOf {
          not { expression { params.RELEASE } };
        }
      }
      steps {
        configFileProvider([
            configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709', variable: 'MAVEN_SETTINGS')
          ]) {
          sh 'mvn -s ${MAVEN_SETTINGS} -Djetty.port=${JETTY_PORT} clean test verify dependency:analyze -Pclb-build -U'
        }
      }
    }

    stage('Build and push Docker images: Checklistbank workflow') {
      when {
        allOf {
          not { expression { params.RELEASE } };
        }
      }
      steps {
        sh 'build/checklistbank-workflow-docker-build.sh $POM_VERSION'
      }
    }

    stage('Maven release: Main project') {
      when {
          allOf {
              expression { params.RELEASE };
              branch 'master';
          }
      }
      environment {
          RELEASE_ARGS = createReleaseArgs(params.RELEASE_VERSION, params.DEVELOPMENT_VERSION, params.DRY_RUN_RELEASE)
      }
      steps {
          configFileProvider(
                  [configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                          variable: 'MAVEN_SETTINGS_XML')]) {
              git 'https://github.com/gbif/checklistbank.git'
              sh 'mvn -s $MAVEN_SETTINGS_XML -B -Denforcer.skip=true release:prepare release:perform $RELEASE_ARGS'
          }
      }
    }

    stage('Docker Release: Checklistbank workflow') {
      when {
        allOf {
          expression { params.RELEASE };
          branch 'master';
        }
      }
      environment {
          VERSION = getReleaseVersion(params.RELEASE_VERSION)
      }
      steps {
        sh 'build/checklistbank-workflow-docker-build.sh ${VERSION}'
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

def createReleaseArgs(inputVersion, inputDevVersion, inputDryrun) {
    def args = ""
    if (inputVersion != '') {
        args += " -DreleaseVersion=" + inputVersion
    }
    if (inputDevVersion != '') {
        args += " -DdevelopmentVersion=" + inputDevVersion
    }
    if (inputDryrun) {
        args += " -DdryRun=true"
    }

    return args
}

def getReleaseVersion(inputVersion) {
    if (inputVersion != '') {
        return inputVersion
    }
    return "${POM_VERSION}".substring(0, "${POM_VERSION}".indexOf("-SNAPSHOT"))
}
