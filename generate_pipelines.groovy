import jenkins.*
import jenkins.model.*

def env = System.getenv()
def github_user = env['JENKINS_GITHUB_USER']
def github_cred_id = "${github_user}-admin-ssh"

def teams = readFileFromWorkspace('config/teams.list')

// get env vars
def admin_github_user = System.getenv()['JENKINS_GITHUB_USER']
def admin_group = System.getenv()['JENKINS_ADMIN_GROUPNAME']
def github_org = System.getenv()['JENKINS_GITHUB_ORG']

// embed utilities classes in strings to pass it to the 'Auto-Generate Pipelines' sub jobs
def github_api_class = readFileFromWorkspace("utilities/githubApi.groovy")

teams.eachLine { line ->
    if (line =~ /^[a-zA-Z]/) {
        def team_params  = line.split(';')
        def team_name = team_params[0]
        def team_ldap_group = team_params[1]
        def team_github_group = team_params[2]
        def team_github_token = team_params[3]

        // create scopped folder
        folder(team_name) {
            displayName(team_name)
            description("CI environment for ${team_name}")
            authorization {
                permissionAll(admin_group)
                permission('hudson.model.Item.Build', team_ldap_group)
                permission('hudson.model.Item.Read', team_ldap_group)
                permission('com.cloudbees.plugins.credentials.CredentialsProvider.Create', team_ldap_group)
                permission('com.cloudbees.plugins.credentials.CredentialsProvider.Delete', team_ldap_group)
                permission('com.cloudbees.plugins.credentials.CredentialsProvider.Update', team_ldap_group)
                permission('com.cloudbees.plugins.credentials.CredentialsProvider.View', team_ldap_group)
            }
        }

        // dsl script content for the 'Auto-Generate Pipelines' sub jobs
        scoped_dsl_generation_job = """
def team_github_group = "${team_github_group}"
def team_name = "${team_name}"
def github_org = "${github_org}"

def ga = new githubApi()
def github_cred_id = "${team_github_group}-token"

// Create a multibranch pipeline job for the repositories accessible to the github team
def team_repos = ga.getOrgTeamReposWithJenkinsfile(team_github_group)

team_repos.each { repo ->
    multibranchPipelineJob(team_name + "/" + repo.name) {
        branchSources {
            github {
                // Set a unique ID as workaround of issue https://issues.jenkins-ci.org/browse/JENKINS-43693
                id(team_name + "_" + repo.name)

                scanCredentialsId(github_cred_id)
                repoOwner(github_org)
                repository(repo.name)

                // // Build origin branches.
                // buildOriginBranch(true)

                // // Build origin branches also filed as PRs.
                // buildOriginBranchWithPR(true)

                // // Build origin PRs (merged with base branch).
                // buildOriginPRMerge(true)

                // // Build origin PRs (unmerged head).
                // buildOriginPRHead(false)

                // // Build fork PRs (merged with base branch).
                // buildForkPRMerge(true)

                // // Build fork PRs (unmerged head).
                // buildForkPRHead(false)
            }
        }

        orphanedItemStrategy {
            discardOldItems {
                daysToKeep(0)
                numToKeep(0)
            }
        }
    }
}
"""
        // add githubApi class content to the dsl script as it uses the class
        def dsl_full_script = "${github_api_class}\n${scoped_dsl_generation_job}"

        // create the dsl job
        job("${team_name}/Auto-Generate Pipelines") {
            steps {
                dsl {
                    text(dsl_full_script)
                    ignoreExisting()
                    removeAction('DELETE')
                    removeViewAction('DELETE')
                }
            }
        }
    }
}

job("admin/01_team_credentials_and_allocated_slaves") {
    description("Configuration of team's credentials and allocated slaves based on the teams.list file")
    // set the same node to retrieve class & config files
    wrappers {
        runOnSameNodeAs('admin/00_initial_dsl_job', true)
    }
    steps {
        systemGroovyCommand(readFileFromWorkspace('system/config_teams.groovy')) {}
    }
}

job("admin/02_cleanup_workspaces") {
    description("Cleanup workspaces to avoid disk full issues")

    parameters {
        stringParam('CLEAN_THRESHOLD', '200', 'threeshold in GB')
    }

    // set the same node to retrieve class & config files
    wrappers {
        runOnSameNodeAs('admin/00_initial_dsl_job', true)
    }

    steps {
        systemGroovyCommand(readFileFromWorkspace('system/cleanup_workspaces.groovy')) {}
    }
}