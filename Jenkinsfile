pipeline {
    agent {
        kubernetes {
            inheritFrom 'molgenis-jdk11'
        }
    }
    environment {
        LOCAL_REPOSITORY = "${LOCAL_REGISTRY}/molgenis/molgenis-expressions_2.13"
        TIMESTAMP = sh(returnStdout: true, script: "date -u +'%F_%H-%M-%S'").trim()
    }
    stages {
        stage('Retrieve build secrets') {
            steps {
                container('vault') {
                    script {
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.sbt"
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.rancher"
                        sh(script: "vault read -field=value secret/ops/jenkins/rancher/cli2.json > ${JENKINS_AGENT_WORKDIR}/.rancher/cli2.json")
                        sh(script: "vault read -field=credentials secret/ops/jenkins/sbt > ${JENKINS_AGENT_WORKDIR}/.sbt/.credentials")
                        env.SONAR_TOKEN = sh(script: 'vault read -field=value secret/ops/token/sonar', returnStdout: true)
                        env.GITHUB_TOKEN = sh(script: 'vault read -field=value secret/ops/token/github', returnStdout: true)
                        env.GITHUB_USER = sh(script: 'vault read -field=username secret/ops/token/github', returnStdout: true)
                    }
                }
            }
        }
        stage('Steps [ PR ]') {
            when {
                changeRequest()
            }
            stages {
                stage('Build, Test') {
                    steps {
                        container('maven') {
                            sh "./sbtx test sonarScan"
                        }
                    }
                    post {
                        always {
                            junit '**/target/test-reports/**.xml'
                        }
                    }
                }
            }
        }
        stage('Steps [ master ]') {
            when {
                branch 'master'
            }
            stages {
                stage('Build, Test, Push to Registries [ master ]') {
                    steps {
                        container('maven') {
                            sh "./sbtx test sonarScan"
//                             sh "git fetch --no-tags origin master:refs/remotes/origin/master"
//                             sh "git push --set-upstream origin ${CHANGE_TARGET}"
                            sh "./sbtx release with-defaults"
                        }
                    }
                    post {
                        always {
                            junit '**/target/test-reports/**.xml'
                        }
                    }
                }
            }
        }
    }
}
