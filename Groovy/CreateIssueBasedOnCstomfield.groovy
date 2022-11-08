import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import com.atlassian.jira.issue.security.IssueSecuritySchemeManager



//récupération du user réalisant l'action
def loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
log.warn(loggedInUser.name)

//Initialisation des différents manager nécessaire
def issueService = ComponentAccessor.issueService
def issueManager = ComponentAccessor.issueManager
def securityManager = ComponentAccessor.getIssueSecurityLevelManager() 
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def Customfield = ComponentAccessor.getComponent(CustomField)
//def issue = issueManager.getIssueByCurrentKey("GJA-6550") 


//récupération de la valeur contenu des champs Personne autorisé et Clé du projet:
def customField = customFieldManager.getCustomFieldObject("customfield_11400")
log.warn("CF = " + customField)
def customFieldValue = issue.getCustomFieldValue(customField)
log.warn("CFValue = " + customFieldValue)
def customCleProjet = customFieldManager.getCustomFieldObject("customfield_10176")
def cleProjetValue = issue.getCustomFieldValue(customCleProjet)
cleProjetValue = cleProjetValue.trim().toUpperCase() //permet d'enlever les espaces et mettre en majuscule car la fonction plus bas getProjectByCurrentKey prend un argument en Majuscule
log.warn(cleProjetValue)
//création d'une liste vide qui sera remplis avec tous les users présent dans le champ Personne(s) autorisée(s) 
def userList = []
customFieldValue.each{userList.add(it.name)}
//Pour chaque nom de user dans ma liste userList, je les concatène avec un "," dans un String pour les passer comme valeur lors de la création du nouveau ticket plus tard
def userListString = userList.join(",")
log.warn("List to string : "+ userListString)

//récuperation de l'id de l'issue source
def issueId = issue.getId()
log.warn("Id de l'issue : " + issueId)

//GET security level
def securityLevel //variable initialisé sans valeur
def securityLevelId = issue.getSecurityLevelId()
log.warn("Level security Id: " +  securityLevelId) 
if(securityLevelId != null){
    securityLevel = securityManager.getSecurityLevel(securityLevelId)
    log.warn("Level security Id: " +  securityLevel.name)  
}
//log.warn(securityLevel.getProperties()) obtenir les propriétés de l'objet. 

//Get issue security level ID du projet distant
def issueSecurityLevelManager = ComponentAccessor.getIssueSecurityLevelManager()
def issueSecurityScheme = ComponentAccessor.getComponent(IssueSecuritySchemeManager)
def projectManager = ComponentAccessor.getProjectManager()

//Methode pour recuperer le projet, puis l'issue security scheme utilise par ce projet et l'id du scheme
def project = projectManager.getProjectByCurrentKeyIgnoreCase(cleProjetValue)
def iSSUsedByPrj = issueSecurityScheme.getSchemeFor(project)
def iSSUsedByPrjId = iSSUsedByPrj.id

//Get de tout les levels de security utilise dans le scheme recuperer la ligne au-dessus
def issueSecurityLevels = issueSecurityLevelManager.getIssueSecurityLevels(iSSUsedByPrjId)

def issueLevelForNewIssue 
//je cherche dans la liste le level de security qui a le nom Groupe Cyber Factory et je prends son id pour le setter dans le nouveau ticket qu'on va creer
for(i in issueSecurityLevels){
    if(i.getName() == "Groupe Cyber Factory"){
        log.warn("Issue Security Level projet distant : " + i.getId())
        issueLevelForNewIssue = i.getId()
    }
}



//On passait par les composants mais plus besoin on passe par un CF qui contient la valeur de la clé du projet target
//GET Component
//def componentList = issue.getComponents()
//componentList.each{component -> log.warn("Component name : "  + component.name)}

def issueResult

//GET projet ID with component name

//def project =  projectManager.getProjectByCurrentKey(cleProjetValue)
def projectId = project.getId()
log.warn("Project ID  :" + projectId + " ProjectName : " + project.getName())
def issueInputParameters =  issueService.newIssueInputParameters() 
issueInputParameters
.setProjectId(project.id)
.setIssueTypeId("10002")
.setReporterId(loggedInUser.name)
.setSummary(issue.summary)
.setSecurityLevelId(issueLevelForNewIssue)
.setDescription(issue.getDescription())
.addCustomFieldValue("customfield_11400",userListString )
//log.warn(issueInputParameters.getProperties())
def validationResult = issueService.validateCreate(loggedInUser, issueInputParameters) //méthode permettant de savoir si l'issue que l'on veut créer est valide ou non
def result = validationResult.isValid()//Si true on peut créer l'issue
if(!result){
    log.warn("Erreur lors de la validation de l'issue : " + validationResult.errorCollection)//message d'erreur lorsque la validation est false
}
else{

    issueResult = issueService.create(loggedInUser, validationResult)//permet de créer l'issue une fois l'étape de validation OK
    log.warn("Validation de la création de l'issue (false ou true) : " + issueResult.isValid())//permet de savoir si l'issue est bien créé ou non
    if(!issueResult.isValid()){
        log.warn("Message d'erreur lors de la création de l'issue: " + issueResult.errorCollection )
    }
    else{
        log.warn("IssueKey créé: " + issueResult.getIssue())
    }
}

 

def NewIssue = issueResult.getIssue()



log.warn("Id de l'issue nouvellement créé: " + NewIssue.getId())

//Manager pour les liens
def linkManager = ComponentAccessor.getIssueLinkManager()
//Manager pour les types d'issueLink
def issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager)

//def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser() 

//def linkType = issueLinkTypeManager.getIssueLinkTypes(false)

//Get issue link type ayant le nom Couverture
def linkTypeContient = issueLinkTypeManager.getIssueLinkTypesByName("Couverture")
//Issue Id de l'issue Link Couverture
def issueLinkId 
linkTypeContient.each{issueLinkId = (it.getId())}

log.warn("ID de l'issue link Couverture : " + issueLinkId)

//linkType.each{type -> log.warn(type.getId())}

//Création du link entre les deux issues
def createLink = linkManager.createIssueLink(NewIssue.getId(),issue.id, issueLinkId, 1, loggedInUser)
