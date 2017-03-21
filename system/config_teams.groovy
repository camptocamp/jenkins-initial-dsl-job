import jenkins.*
import jenkins.model.*
import hudson.model.*
import hudson.slaves.*
import hudson.FilePath

// get env vars
def thr = Thread.currentThread()
def build = thr?.executable
def envVarsMap = build.parent.builds[0].properties.get("envVars")
jobName = envVarsMap['JOB_NAME']

// Groovy system script is always run on jenkins master node, while the workspace is on the jenkins slave node.
// So first get the needed files in the workspace of the upstream job on the same node where the initial job has last ran

// sync utilities folder from slave on master node
fp = new FilePath(build.workspace, ".")
systemGroovyFolderOnMaster = "/var/jenkins_home/system_groovy"
systemGroovyJobFolderOnMaster = "${systemGroovyFolderOnMaster}/${jobName}"
targetDir = new File(systemGroovyJobFolderOnMaster)
targetDir.mkdirs()
targetDirPath = new FilePath(new File(systemGroovyJobFolderOnMaster))
println "Adding synchronization folder ${targetDir}"
fp.copyRecursiveTo(targetDirPath)

// Read class files contents
String folderCredentialScript = new File("${systemGroovyJobFolderOnMaster}/utilities/folderCredential.groovy").text
String slaveNodeRestrictionScript = new File("${systemGroovyJobFolderOnMaster}/utilities/slaveNodeRestriction.groovy").text

// Parse the script content to have the corresponding classes
Class folderCredential = new GroovyClassLoader(getClass().getClassLoader()).parseClass(folderCredentialScript);
Class slaveNodeRestriction = new GroovyClassLoader(getClass().getClassLoader()).parseClass(slaveNodeRestrictionScript);

// Create new instances
GroovyObject folderCredentialInst = (GroovyObject) folderCredential.newInstance();
GroovyObject slaveNodeRestrictionInst = (GroovyObject) slaveNodeRestriction.newInstance();

// Read config files contents
String teams = new File("${systemGroovyJobFolderOnMaster}/config/teams.list").text
println "-------------------- Credentials -----------------------"
def slavesIndexHash = [:]
teams.eachLine { line ->
    if (line =~ /^[a-zA-Z]/) {
        def team_params  = line.split(';')
        def team_name = team_params[0]
        def team_ldap_group = team_params[1]
        def team_github_group = team_params[2]
        def team_github_token = team_params[3]
        def slave_numbers = team_params[4]
        println "Add credential ${team_github_group} for ${team_name}"
        folderCredentialInst.addFolderUserPasswordCredential(
            team_name,
            "${team_github_group}-token",
            "Github token for ${team_name}",
            team_github_group,
            team_github_token
        )

        String[] strArray = slave_numbers.split(",");
        int[] intSlaveArray = new int[strArray.length];
        for(int i = 0; i < strArray.length; i++) {
            intSlaveArray[i] = Integer.parseInt(strArray[i]);
        }

        intSlaveArray.each {
            if (slavesIndexHash.containsKey(it)) {
                    slavesIndexHash[it].add(team_name)
            } else {
                    slavesIndexHash[it] = [team_name]
            }
        }
    }
}
println "--------------------------------------------------------"

println "-------------------- Slave Access ----------------------"
slavesIndexHash.each{ k, v ->
    println "Grant Access to Slave #${k} for ${v}"
    slaveNodeRestrictionInst.restrictFoldersOnNodes(
        v,
        [k]
    )
}
println "--------------------------------------------------------"

// Cleanup
println "Removing synchronization folder ${targetDir}"
targetDir.deleteDir()