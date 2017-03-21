package utilities

import jenkins.model.*
import hudson.model.*
import hudson.slaves.*

import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.nodes.JobRestrictionProperty;
import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.restrictions.job.StartedByMemberOfGroupRestriction;
import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.restrictions.job.RegexNameRestriction;
import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.util.GroupSelector;
import java.util.List;

class slaveNodeRestriction {

    def restrictGroupsOnNodes(
        groupNameList,
        nodeList
    ){

        List grouplist = new LinkedList();
        groupNameList.each { groupName ->
            GroupSelector  g = new GroupSelector (groupName);
            grouplist.add(g);
        }

        StartedByMemberOfGroupRestriction startGrpRestr = new StartedByMemberOfGroupRestriction(grouplist, false );
        JobRestrictionProperty jobrestrict = new JobRestrictionProperty(startGrpRestr);

        List restrictlist = new LinkedList();
        restrictlist.add(jobrestrict);

        RetentionStrategy retStrat = new RetentionStrategy.Always()

        //for (aSlave in hudson.model.Hudson.instance.slaves) {
        hudson.model.Hudson.instance.slaves.eachWithIndex { aSlave, index ->
            if (nodeList.contains(index+1)) {
                aSlave.setRetentionStrategy(retStrat);
                aSlave.setNodeProperties(restrictlist);
                aSlave.save()
            }
        }

    }

    def restrictFoldersOnNodes(
        folderList,
        nodeList
    ){

        def regex = /^[${folderList.join('|')}].*/
        RegexNameRestriction regexRestr = new RegexNameRestriction(regex, false );
        JobRestrictionProperty jobrestrict = new JobRestrictionProperty(regexRestr);

        List restrictlist = new LinkedList();
        restrictlist.add(jobrestrict);

        RetentionStrategy retStrat = new RetentionStrategy.Always()

        //for (aSlave in hudson.model.Hudson.instance.slaves) {
        hudson.model.Hudson.instance.slaves.eachWithIndex { aSlave, index ->
            if (nodeList.contains(index+1)) {
                aSlave.setRetentionStrategy(retStrat);
                aSlave.setNodeProperties(restrictlist);
                aSlave.save()
            }
        }

    }
}

// // Uncomment if you want to test this in the online script page
// def snr = new slaveNodeRestriction()
// snr.restrictFoldersOnNodes(
//     ["infrastructure","business"],
//     [1]
// )
// snr.restrictFoldersOnNodes(
//     ["infrastructure","geospatial"],
//     [2]
// )
