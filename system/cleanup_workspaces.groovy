/**
Jenkins System Groovy script to clean up workspaces on all slaves.

Check if a slave has < X GB of free space, perform cleanup if it's less.
If slave is idle, wipe out everything in the workspace directory as well any extra configured directories.
If slave is busy, wipe out individual job workspace directories for jobs that aren't running.
Either way, remove custom workspaces also if they aren't in use.
**/

import hudson.model.*;
import hudson.util.*;
import jenkins.model.*;
import hudson.FilePath.FileCallable;
import hudson.slaves.OfflineCause;
import hudson.node_monitors.*;

//threshold is in GB and comes from a job parameter
def threshold = Integer.parseInt(build.buildVariableResolver.resolve("CLEAN_THRESHOLD"))
def skippedLabels = [ 'container' ] //don't clean docker slaves
def extraDirectoriesToDelete = [ 'temp' ] //additional paths under slave's root path that should be removed if found


def deleteRemote(def path, boolean deleteContentsOnly) {
  boolean result = true
  def pathAsString = path.getRemote()
  if (path.exists()) {
    try {
      if (deleteContentsOnly) {
        path.deleteContents()
        println ".... deleted ALL contents of ${pathAsString}"
      } else {
      	path.deleteRecursive()
        println ".... deleted directory ${pathAsString}"
      }
    } catch (Throwable t) {
      println "Failed to delete ${pathAsString}: ${t}"
      result = false
    }
  }
  return result
}


def failedNodes = []

for (node in Jenkins.instance.nodes) {
  computer = node.toComputer()
  if (computer.getChannel() == null) {
    continue
  }
      
  if (node.assignedLabels.find{ it.expression in skippedLabels }) {
    println "Skipping ${node.displayName} based on labels"
    continue
  }

  try {
    size = DiskSpaceMonitor.DESCRIPTOR.get(computer).size
    roundedSize = size / (1024 * 1024 * 1024) as int

    println("node: " + node.getDisplayName() + ", free space: " + roundedSize + "GB. Idle: ${computer.isIdle()}")
    if (roundedSize < threshold) {
      def prevOffline = computer.isOffline()
      if (prevOffline && computer.getOfflineCauseReason().startsWith('disk cleanup from job')) {
        prevOffline = false //previous run screwed up, ignore it and clear it at the end
      }
      if (!prevOffline) {
        //don't override any previosly set temporarily offline causes (set by humans possibly)
      	computer.setTemporarilyOffline(true, new hudson.slaves.OfflineCause.ByCLI("disk cleanup from job ${build.displayName}"))
      }
      if (computer.isIdle()) {
        //It's idle so delete everything under workspace
        def workspaceDir = node.rootPath.child('workspace')
        if (!deleteRemote(workspaceDir, true)) {
          failedNodes << node
        }
        
        //delete custom workspaces
        Jenkins.instance.getAllItems(TopLevelItem).findAll{item ->  item instanceof Job && !("${item.class}".contains('WorkflowJob')) && item.getCustomWorkspace()}.each{ item ->
          if (!deleteRemote(node.getRootPath().child(item.customWorkspace), false)) {
            failedNodes << node
          }
        }
        
        extraDirectoriesToDelete.each{
          if (!deleteRemote(node.getRootPath().child(it), false)) {
            failedNodes << node
          }
        }
        
      } else {
      
      	Jenkins.instance.getAllItems(TopLevelItem).findAll{item ->  item instanceof Job && !item.isBuilding() && !("${item.class}".contains('WorkflowJob')) }.each{ item ->
          jobName = item.getFullDisplayName()
          
          
          println(".. checking workspaces of job " + jobName)
          
          workspacePath = node.getWorkspaceFor(item)
          if (!workspacePath) {
            println(".... could not get workspace path for ${jobName}")
            return
              }
          
          println(".... workspace = " + workspacePath)
          
          customWorkspace = item.getCustomWorkspace()
          if (customWorkspace) {
            workspacePath = node.getRootPath().child(customWorkspace)
            println(".... custom workspace = " + workspacePath)
          }
          
          if (!deleteRemote(workspacePath, false)) {
            failedNodes << node
          }
        }
      }

      if (!prevOffline) {
        computer.setTemporarilyOffline(false, null)
      }
    }
  } catch (Throwable t) {
    println "Error with ${node.displayName}: ${t}"
    failedNodes << node
    
  }
}

println "\n\nSUMMARY\n\n"        
failedNodes.each{node ->
  println "\tERRORS with: ${node.displayName}"
}
assert failedNodes.size() == 0