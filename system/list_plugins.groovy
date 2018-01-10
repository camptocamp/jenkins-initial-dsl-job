/**
List plugins with the output you can use for a plugins.txt file in the jenkins-conf container image
**/
import jenkins.*

def pluginsList = []
Jenkins.instance.pluginManager.plugins.each{
  plugin ->
    pluginsList << ("${plugin.getShortName()}:${plugin.getVersion()}")
}
pluginsList.sort().each{
  plugin ->
    println plugin
}