/*

Ajouter les utilisateurs d'un project role dans un groupe Jira

*/

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.project.Project
import com.atlassian.jira.user.ApplicationUser

def userUtil = ComponentAccessor.getUserUtil()

//get manager
def groupManager = ComponentAccessor.getGroupManager()
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
def projectManager = ComponentAccessor.getComponent(ProjectManager)

//get groupe de dev
def goJiraUserGrp = groupManager.getGroup("gojira-com-dev")
//log.warn(groupManager.getUsersInGroup(goJiraUserGrp).size())

//get all project
def allProject = projectManager.getProjectObjects()
//log.warn(allProject)

//get project role dev
def devProjectRole = projectRoleManager.getProjectRole("Equipe de développement")
//log.warn(devProjectRole)

//global variable pour l'utiliser dans la boucle
def Set<ApplicationUser> userList

for(Project key:allProject){
    userList = projectRoleManager.getProjectRoleActors(devProjectRole, key).getUsers()
    //log.warn("Project User Count : " + userList.size() + "Nom Projet : " + key )
    for(ApplicationUser user:userList){
        groupManager.addUserToGroup(user, goJiraUserGrp)
        //log.warn(user)
    }
}


log.warn(groupManager.getUsersInGroup(goJiraUserGrp).size())
