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

    // This fonction is not used, as it makes issues with rate_limit on github api
    def reposWithJenkinsfile() {
        def query = "q=org:${this.org}+filename:Jenkinsfile"
        files = this.fetch("${this.api_url}/search/code?${query}", this.infra_token)

        def repos_with_jenkinsfile = []
        files.items.each { file ->
            repos_with_jenkinsfile.add(file.repository['name'])
        }
        return repos_with_jenkinsfile
    }
}