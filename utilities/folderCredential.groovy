package utilities

import jenkins.*
import jenkins.model.*
import com.cloudbees.hudson.plugins.folder.*;
import com.cloudbees.hudson.plugins.folder.properties.*;
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider.FolderCredentialsProperty
import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;
import hudson.util.Secret

class folderCredential {
    def addFolderUserPasswordCredential(
            folderName,
            credId,
            credDesc,
            credUser,
            credPassword
        ){

        Credentials pwc = (Credentials) new UsernamePasswordCredentialsImpl(
            CredentialsScope.GLOBAL,
            credId,
            credDesc,
            credUser,
            credPassword
        )
        def inst = Jenkins.getInstance()
        for (folder in inst.getAllItems(Folder.class)) {
            if(folder.name.equals(folderName)){
                AbstractFolder<?> folderAbs = AbstractFolder.class.cast(folder)
                FolderCredentialsProperty property = folderAbs.getProperties().get(FolderCredentialsProperty.class)
                if(property) {
                    property.getStore().addCredentials(Domain.global(), pwc)
                } else {
                    property = new FolderCredentialsProperty([pwc])
                    folderAbs.addProperty(property)
                }
            }
        }
    }
}


// def cred = new folderCredentials()
// cred.addFolderUserPasswordCredential(
//     "infrastructure",
//     "test-token",
//     "test-token",
//     "testuser",
//     "testpw"
// )
