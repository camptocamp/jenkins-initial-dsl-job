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

    // As there is no pagination, there is a returned items limit of 100 teams, and 100 repos per teams.
    def getTeamRepos(team_github_group){
        // TODO add pagination as this is limited to the first 100 entries
        def all_teams = fetch("${this.api_url}/orgs/${org}/teams?per_page=100", infra_token)

        // Get team by name
        def github_team = all_teams.find{ it.name == team_github_group}

        // Get team repos with a Jenkinsfile
        def team_repos = fetch("${api_url}/teams/${github_team.id}/repos?per_page=100", infra_token)
        return team_repos
    }

    def getOrgTeamRepos(team_github_group){
        def team_repos = getTeamRepos(team_github_group)
        return team_repos.findAll { it.owner.login == org}
    }

    def filesWithJenkinsfile() {
        def query = "q=org:${this.org}+filename:Jenkinsfile"
        def result = this.fetch("${this.api_url}/search/code?${query}", this.infra_token)
        return result.items.findAll { it.path == 'Jenkinsfile' }
    }

    def getOrgTeamReposWithJenkinsfile(team_github_group) {
        def team_repos = getOrgTeamRepos(team_github_group)
        def files = filesWithJenkinsfile()
        def file_repos = files.collect { it.repository }
        def common_repos = []
        team_repos.each { team_repo ->
            common_repos.add(team_repo)
        }
        return common_repos
    }
}
