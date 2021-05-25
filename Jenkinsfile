pipeline {
    agent {
        kubernetes {
            inheritFrom 'molgenis-jdk11'
        }
    }
    environment {
        LOCAL_REPOSITORY = "${LOCAL_REGISTRY}/molgenis/molgenis-expressions_2.13"
        TIMESTAMP = sh(returnStdout: true, script: "date -u +'%F_%H-%M-%S'").trim()
        // https://stackoverflow.com/a/47684072/1973271
        SBT_OPTS = "-Duser.home=${JENKINS_AGENT_WORKDIR}"
        NVM_DIR = "${JENKINS_AGENT_WORKDIR}/.nvm"
        NODE_VERSION = "v12.22.1"
    }
    stages {
        stage('Prepare') {
            steps {
                container('vault') {
                    script {
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.nvm"
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.sbt"
                        sh "mkdir ${JENKINS_AGENT_WORKDIR}/.rancher"
                        sh(script: "vault read -field=value secret/ops/jenkins/rancher/cli2.json > ${JENKINS_AGENT_WORKDIR}/.rancher/cli2.json")
                        sh(script: "vault read -field=credentials secret/ops/jenkins/sbt > ${JENKINS_AGENT_WORKDIR}/.sbt/.credentials")
                        env.SONAR_TOKEN = sh(script: 'vault read -field=value secret/ops/token/sonar', returnStdout: true)
                        env.GITHUB_TOKEN = sh(script: 'vault read -field=value secret/ops/token/github', returnStdout: true)
                        env.GITHUB_USER = sh(script: 'vault read -field=username secret/ops/token/github', returnStdout: true)
                        env.NPM_TOKEN = sh(script: 'vault read -field=value secret/ops/token/npm', returnStdout: true)
                    }
                }
                container('maven') {
                    sh "curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.38.0/install.sh | bash"
                    sh ". ${NVM_DIR}/nvm.sh && nvm install ${NODE_VERSION}"
                    sh "ln -s ${NVM_DIR}/versions/node/${NODE_VERSION}/bin/node /usr/local/bin/node"
                    sh "ln -s ${NVM_DIR}/versions/node/${NODE_VERSION}/bin/npm /usr/local/bin/npm"
                    sh "node --version"
                    sh '''
                      set +x
                      git remote set-url origin https://$GITHUB_TOKEN@github.com/molgenis/molgenis-expressions.git
                    '''
                    sh "git config remote.origin.fetch +refs/heads/*:refs/remotes/origin/*"
                    sh "git config branch.master.remote origin"
                    sh "git config branch.master.merge refs/heads/master"
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
                            sh "./sbtx test"
                            sh "./sbtx expressions/sonarScan"
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
                allOf {
                    branch 'master'
                    not {
                        changelog '^Setting version to'
                    }
                }
            }
            stages {
                stage('Build, Test, Push to Registries [ master ]') {
                    steps {
                        container('maven') {
                            sh "./sbtx test fullOptJS"
                            sh "./sbtx expressions/sonarScan"
                            sh "./sbtx 'project expressions' 'release with-defaults'"
                            sh "set +x; npm set //registry.npmjs.org/:_authToken ${NPM_TOKEN}"
                            sh "npm publish"
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
