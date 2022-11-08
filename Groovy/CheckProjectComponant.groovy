/*
*
***
*
*  Script a utiliser dans la console pour vérifier les composant d'un projet (est-ce que le nom du composant correspond a un projet Jira, si la description n'est pas vide, y a t-il un Lead)
*
*
*
*
*/


import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.project.component.MutableProjectComponent

//def mutableComponent = ComponentAccessor.getComponent(MutableProjectComponent)
def projectComponent = ComponentAccessor.getProjectComponentManager()
def projectGJA = ComponentAccessor.getProjectManager().getProjectByCurrentKey("GJA")
def projectId = projectGJA.getId()


def allComponentGJA = projectComponent.findAllForProject(projectId)
def componentWithoutProject = []
def componentWithoutlead = []
def componentWithoutDescription = []

//Composant sans description
for(i in allComponentGJA){
    //projet sans description
    if(i.description == null){

        componentWithoutDescription.add(i.getName())
        MutableProjectComponent componentCopy = MutableProjectComponent.copy(i) // en utilisant le constructeur MutableProjectComponent on peut remplacer cette ligne par ==>def toto = new MutableProjectComponent(i.id,i.name,i.description,i.lead,i.assigneeType,i.projectId,i.isArchived())
        def projectName = ComponentAccessor.getProjectManager().getProjectByCurrentKey(i.name)
        
        //verification si le composant est une cle de projet Jira
        if(!projectName){
            //log.warn("Le component ${i.name} n'a pas de projet")
            componentWithoutProject.add(i.name)

        //mise a jour de la description du composant avec le nom du projet
        }else{
            log.warn("Mise à jour de la description du composant ${i.name}")
            componentCopy.setDescription("Projet " +  ComponentAccessor.getProjectManager().getProjectByCurrentKey(i.name).getName())
            def updateComponent = projectComponent.update(componentCopy)
            
        }
        
        //mise à jour de la description si description inferieur à 7 caracteres (projet = 7 caracteres)
    }else 
        if(i.description.length() < 7 && i.description.length() > 1){

        log.warn("Description composant incomplete : " + i.description)
        MutableProjectComponent componentCopyUpdateDescription = MutableProjectComponent.copy(i)
        componentCopyUpdateDescription.setDescription("Projet ${i.description}")
        projectComponent.update(componentCopyUpdateDescription)


    }else if(i.lead == null){
        componentWithoutlead.add(i.name)
    //verification si le default assignee des tickets lié a ce composant est different de la valeur "Component Lead"
    }else if(i.assigneeType != 1){
        log.warn(i)
    }     
    
    
}

//log.warn("Nombre de composant sans description : " + componentWithoutDescription.size())
log.warn("Liste de composant sans Projet : " + componentWithoutProject )
log.warn("Liste de composant sans Lead : " + componentWithoutlead)
