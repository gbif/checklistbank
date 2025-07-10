@Library('gbif-common-jenkins-pipelines') _

pipeline {
  agent any
  tools {
    maven 'Maven 3.9.9'
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
    JETTY_PORT = utils.getPort()
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
        withMaven(globalMavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                  mavenOpts: '-Xms2048m -Xmx8192m', mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1396361652540') {
                 configFileProvider([
                    configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                                              variable: 'MAVEN_SETTINGS_XML')
                    ]) {
          sh 'mvn -s ${MAVEN_SETTINGS} -Djetty.port=${JETTY_PORT} clean test verify deploy dependency:analyze -Pclb-build -U'
        }
      }
    }

    stage('Trigger WS deploy dev2') {
          when {
            allOf {
              not { expression { params.RELEASE } };
              branch 'dev';
            }
          }
          steps {
            build job: "checklistbank-ws-dev-deploy", wait: false, propagate: false
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
          RELEASE_ARGS = utils.createReleaseArgs(params.RELEASE_VERSION, params.DEVELOPMENT_VERSION, params.DRY_RUN_RELEASE)
      }
      steps {
        withMaven(globalMavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                    mavenOpts: '-Xms2048m -Xmx8192m', mavenSettingsConfig: 'org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1396361652540') {
                     configFileProvider([
                        configFile(fileId: 'org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig1387378707709',
                                                  variable: 'MAVEN_SETTINGS_XML')
                        ]) {
              git 'https://github.com/gbif/checklistbank.git'
              sh 'mvn -s $MAVEN_SETTINGS_XML -B -Denforcer.skip=true release:prepare release:perform $RELEASE_ARGS'
          }
      }
    }

    stage('Docker Release: Checklistbank workflow') {
      when {
        allOf {
          expression { params.RELEASE };
          not { expression { params.DRY_RUN_RELEASE } }
          branch 'master';
        }
      }
      environment {
          VERSION = utils.getReleaseVersion(params.RELEASE_VERSION, POM_VERSION)
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
