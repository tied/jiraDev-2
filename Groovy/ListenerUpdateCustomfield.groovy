import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.event.type.EventDispatchOption

//Accesseur
def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def securityManager = ComponentAccessor.getIssueSecurityLevelManager()
def issueManager = ComponentAccessor.getIssueManager()
def jiraAuthentication =  ComponentAccessor.getJiraAuthenticationContext() 
def user = jiraAuthentication.getLoggedInUser()

//Get du customfield à ecouter
def personneAutorise = customFieldManager.getCustomFieldObject(16000)
def issue = event.issue
def change = event?.getChangeLog()?.getRelated("ChildChangeItem").find{it.field == "Personne(s) autorisee(s) CYBER"}


//Recuperation des valeurs du champ Personne Autorise Cyber
def customFieldValue = issue.getCustomFieldValue(personneAutorise)

//Type de ticket à scanner
def issueType = issue.getIssueType().getId()
log.warn("Nom de l'issuetype : "  + issue.getIssueType().getName())
log.warn("ID de l'issueType : "  + issue.getIssueType().getId())

def securityLevel = issue.getSecurityLevelId()
log.warn("Id de level de securite : " + securityLevel)
def securityLevelName = securityManager.getIssueSecurityName(securityLevel) 
log.warn("Nom du level de securite : " + securityLevelName)


//log.warn(issueType.getClass()) //retourne un string

//ticket lie via un IssueLink à scanner
def LinkOfIssue = issueLinkManager.getInwardLinks(event.issue.getId())

if(change){
    log.warn("Nouvelle valeur saisie : " + change.newvalue)
	log.warn("Nouvelle valeur saisie (String): " + change.newstring)
    //10008 est l'id de l'issuetype Besoin metier
    if (issueType == "10008"){
    for(link in LinkOfIssue){
        def issueTypeOfLink = link.getIssueLinkType().getId()
        log.warn("Nom de type de lien utilise : " + link.getIssueLinkType().getName())
        //log.warn(issueLinkManager.getOutwardLinks())
        log.warn("Id du type de lien utilise : " + issueTypeOfLink)
        //On verifie si le lien utilise est bien couverture (ID 10003)
        if (issueTypeOfLink == 10003L){
            log.warn("Destination ID : " + link.getDestinationId())
            log.warn("Destination Object : " + link.getDestinationObject())
            def issueKeyofLinkedIssue = link.getSourceObject()
            def issueLinked = issueManager.getIssueByCurrentKey(issueKeyofLinkedIssue.getKey()) 
            //Get de l'issue security level ID
            def issueSecurityLevelIdOfdLinkedIssue  = issueLinked.getSecurityLevelId()
            def securityLevelNameOfLinkedIssue
            //Get de l'issue security Name
            if (issueSecurityLevelIdOfdLinkedIssue !=null){
                securityLevelNameOfLinkedIssue = securityManager.getIssueSecurityName(issueSecurityLevelIdOfdLinkedIssue)
            }
            
            log.warn("Nom de l'issue security level dans le ticket Tâche : " + securityLevelNameOfLinkedIssue)//Niveau de securite dans le projet distant
            //Je verifie que l'issue security name du besoin metier et du ticket lie sont identiques
            if(securityLevelNameOfLinkedIssue == securityLevelName ){
                
                
                issueLinked.setCustomFieldValue(personneAutorise,customFieldValue)
				issueManager.updateIssue(user, issueLinked, EventDispatchOption.DO_NOT_DISPATCH, false)
                
            }
        }
    }
}
   
}
