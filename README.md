# jenkins-initial-dsl-job

1. Jenkins starts normally
2. At startup, Jenkins adds a initial Job visible only to Jenkins Admins to execute the Initial Groovy DSL script for teams configuration.
3. An Admin user login and executes the job "admin/00\_initial\_dsl\_job"
4. The "admin/00\_initial\_dsl\_job" Job clones the repository from *Custom configuration repository* containing the List of Teams and uses the teams.list file to create the Teams in Jenkins
5. The "admin/00\_initial\_dsl\_job" Job creates a Job for each Team (Job is *Team Name*/Auto-Generate Pipelines)
6. The "admin/00\_initial\_dsl\_job" Job creates the Job "admins/01\_team\_credentials\_and\_allocated\_slaves".
7. The Admin user executes the job "admin/01\_team\_credentials\_and\_allocated\_slaves". This will configure teams credentials and slaves allocations.
8. A Team member logs-in and executes *Team Name*/Auto-Generate Pipelines
9. The job scans the organisation using the Team token defined in the teams.list file, and adds a job for each repository containing a Jenkinsfile
10. The Team member configures the github service to notify the jenkins instance of changes in the repository (using the *Jenkins (github)* service not the *Jenkins (git)* one)
