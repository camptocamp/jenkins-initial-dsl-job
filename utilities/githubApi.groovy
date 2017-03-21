package utilities
import groovy.json.JsonSlurper

class githubApi {

    def api_url = "https://api.github.com"
    def org = System.getenv()['JENKINS_GITHUB_ORG']
    def infra_token = System.getenv()['JENKINS_GITHUB_TOKEN']
    def infra_github_user = System.getenv()['JENKINS_GITHUB_USER']

    def fetch(addr, token, params = [:]) {
        def json = new JsonSlurper()
        return json.parse(
            addr.toURL().newReader(requestProperties: [
            "Authorization": "token ${token}".toString(),
            "Accept": "application/json"]
            )
        )
    }

    def getTeamRepos(team_github_group){
        // TODO add pagination as this is limited to the first 100 entries
        def all_teams = fetch("${this.api_url}/orgs/${org}/teams?per_page=100", infra_token)

        // Get team by name
        def github_team = all_teams.find{ it.name == team_github_group}

        // Get team repos with a Jenkinsfile
        def team_repos = fetch("${api_url}/teams/${github_team.id}/repos?per_page=100", infra_token)
        return team_repos
    }

    def reposWithJenkinsfile(repos) {
        def repos_with_jenkinsfile = []

        // Get repos with a jenkinsfile
        repos.each { repo ->
            def content = fetch("${api_url}/repos/${org}/${repo.name}/contents", infra_token)
            if (content.any { it.path == "Jenkinsfile"}) {
                repos_with_jenkinsfile.add(repo)
            }
        }
        return repos_with_jenkinsfile
    }
}