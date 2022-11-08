import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.mail.Email
import com.atlassian.mail.server.SMTPMailServer
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.config.IssueTypeManager
import groovy.transform.Field

def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchService = ComponentAccessor.getComponent(SearchService)
def gojiraAdminUser = ComponentAccessor.getUserManager().getUserByName("gojira-admin")

//methode pour avoir les id d'un issuetype a partir d'un label
@Field String idTache

def getTypeId (String labelIssueType) {
    def IssueType = ComponentAccessor.getComponent(IssueTypeManager)
	def listIssue = IssueType.getIssueTypes()
	for (i in listIssue){
    	if(i.getName() == labelIssueType){
       		idTache = i.getId() 
    }
    
}
    return idTache
}
//id des issuetype tache, us et us Enabler
def tacheId = getTypeId("Tâche")
def usId = getTypeId("User Story")
def useId = getTypeId("User Story Enabler")
//log.warn(tacheId)

//methode envoi de mail
String sendEmail (String message){
    def today = new Date().format("dd-MM-yyyy HH:mm:ss")
    Email email = new Email("mohamed-externe.benziane@enedis.fr")
	email.setMimeType("text/html")
    email.setCc("")
    email.setSubject("Job transition automatique en erreur")
    email.setBody("""
    				Bonjour,<br>	                    
                    <br>Le job permettant le passage de transition des Fonctionnalité et Fonctionnalité Enabler du projet PXF s'est terminé avec une erreur le ${today}.<br>
                    <br>Merci de vous rapprocher de votre administrateur de Proximité pour résoudre ce problème.<br>
                    
                    
                    <br>Information complémentaire : Passage vers le statut ${message} KO <br>
                    
                    <br>Cordialement,<br>
                    L'équipe GoJira
                    
    				""")
    SMTPMailServer mailServer = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer()
    if(mailServer){
        mailServer.send(email)
    }
    else{
        log.warn("Problème avec le serveur mail impossible d'envoyer le mail")
    } 
    
}

// jql query
def query = jqlQueryParser.parseQuery("project = GJA and type in ('Fonctionnalité', 'Fonctionnalité Enabler') and status in ('A Réaliser')")
def query_2 = jqlQueryParser.parseQuery("project = PXF and type in ('Fonctionnalité', 'Fonctionnalité Enabler') and status in ('En réalisation')")

def search = searchService.search(gojiraAdminUser, query, PagerFilter.getUnlimitedFilter())
def search_2 = searchService.search(gojiraAdminUser, query_2, PagerFilter.getUnlimitedFilter())

log.warn("Total issues de la premiere JQL: ${search.total}")
log.warn("Total issues de la seconde JQL: ${search_2.total}")

//get des tickets retourné par la requete au format DocumentIssueImpl permettant ensuite de recuperer l'issue ID ou la clé
def totalIssue = search.getResults()
def totalIssue_2 = search_2.getResults()

def transition = 161 // id transition emmenant vers le statut En réalisation
def transition_2 = 171 //id transition emmenant vers le statut pret pour validation 

//date du jour


def issuLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager)
def issueManager = ComponentAccessor.getIssueManager()
def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def issueService = ComponentAccessor.getComponent(IssueService)

//Get id du type de lien Couverture

def linkCouverture = issuLinkTypeManager.getIssueLinkTypesByName("Couverture") // retourne une collection d'issuelinktype
def couvertureLinkId 
for(l in linkCouverture){
    couvertureLinkId = l.getId()
}
try{
    for(i in totalIssue_2){
    log.warn("Clé de ticket de la Fonctionnalité : " + i.getKey())
    def currentKey = issueManager.getIssueByCurrentKey(i.getKey())
    //get des status des tickets lié au ticket currentKey
    def issueLink = issueLinkManager.getInwardLinks(currentKey.getId())
        //log.warn(issueLink.size())
        //log.warn(issueLink)
        def statusName = []
        
        for(j in issueLink){
            

            if(j.getLinkTypeId() == couvertureLinkId ){
                //log.warn(j.getSourceObject().getIssueTypeId()) 
                if(j.getSourceObject().getIssueTypeId() in [tacheId,usId, useId] ){                
                    statusName.add(j.getSourceObject().getStatus().getSimpleStatus().getName())
                	//log.warn(statusFor_US_Use)
                    
                }

            }
        
        }
    log.warn("Statut des US/USE rattaché au ticket ${i.getKey()} " + statusName)
    //verification que tous les tickets lié sont au statut terminé
    def value = statusName.findAll{status -> status == "Terminé"}//la methode 'findAll' permet de trouver tous les éléments de la liste qui respecte le critère de recherche dans la closure {status -> status == "Terminé"}, ici je recherche tout les elements de la liste qui correspondent a "Terminé"
    if(value.size() == statusName.size()){
    	log.warn("Début de traitement pour le ticket ${i.getKey()}")
        def validationTransition = issueService.validateTransition(gojiraAdminUser, currentKey.getId() , transition ,  issueService.newIssueInputParameters())
                        if(validationTransition.isValid()){
                            //ici on execute la transtition

                            issueService.transition(gojiraAdminUser, validationTransition)
                            log.warn("Passage de la transition réussie")
                            }
                        else{
                            log.warn("Transition imposible pour le ticket ${currentKey} : " + validationTransition.getErrorCollection().getErrorMessages())
                        }
    }
    else{
        log.warn("Les tickets rattaché à ${i.getKey()} ne sont pas tous au statut Terminé")
    }
    
}
}catch(Exception e){
                log.warn("Exception leve lors du lancement de la methode autoTransition ${e.getMessage()}")
                try{
                    sendEmail("Prêt pour validation") 
                }catch (Exception f){
                    log.warn("Message d'erreur lors de l'envoi du mail ${f.getMessage()}")
                    } 

            }

try{
    for(i in totalIssue){
    log.warn("Clé de ticket de la Fonctionnalité : " + i.getKey())
    def currentKey = issueManager.getIssueByCurrentKey(i.getKey())
    //get des status des tickets lié au ticket currentKey
    def issueLink = issueLinkManager.getInwardLinks(currentKey.getId())
        //log.warn(issueLink.size())
        //log.warn(issueLink)
        def statusNameFor_Us_Use = []
        def statusNameFor_Tache = []
        for(j in issueLink){
            

            if(j.getLinkTypeId() == couvertureLinkId ){
                //log.warn(j.getSourceObject().getIssueTypeId()) 
                if(j.getSourceObject().getIssueTypeId() in [usId, useId] ){                
                    statusNameFor_Us_Use.add(j.getSourceObject().getStatus().getSimpleStatus().getName())
                	//log.warn(statusNameFor_Us_Use + statusNameFor_Tache )
                    
                }else if(j.getSourceObject().getIssueTypeId() in [tacheId] ) {
                    statusNameFor_Tache.add(j.getSourceObject().getStatus().getSimpleStatus().getName())
                    //log.warn(statusNameFor_Us_Use + statusNameFor_Tache )
                }

            }
        
        }
    log.warn("Statut des US/USE rattaché au ticket ${i.getKey()} " + statusNameFor_Us_Use)
     log.warn("Statut des Tâche rattaché au ticket ${i.getKey()} " + statusNameFor_Tache)
    //verification qu'unau moins un ticket lié de type US ou USE est au satut En réalisation
    def value = statusNameFor_Us_Use.find{status -> status == "En réalisation"}
    //verification qu'un moins un ticket lié de type US ou USE est au satut En cours
    def value_Tache = statusNameFor_Tache.find{status -> status == "En cours"} //la methode 'find' permet de trouver la premiere valeur dans une liste qui respecte le critère de recherche dans la closure {status -> status == "En cours"}
    if(value || value_Tache){
        log.warn("Début de traitement pour le ticket ${i.getKey()}")
        def validationTransition = issueService.validateTransition(gojiraAdminUser, currentKey.getId() , transition ,  issueService.newIssueInputParameters())
                        if(validationTransition.isValid()){
                            //ici on execute la transtition

                            issueService.transition(gojiraAdminUser, validationTransition)
                            log.warn("Passage de la transition réussie pour le ticket ${i.getKey()}")
                            }
                        else{
                            log.warn("Transition imposible pour le ticket ${currentKey} : " + validationTransition.getErrorCollection().getErrorMessages())
                        }
    }
    else{
        log.warn("Aucun des tickets rattaché à ${i.getKey()} n'est au statut 'En cours' ou 'En réalisation'")
    }
    
}
}catch(Exception e){
                log.warn("Exception leve lors du traitement ${e.getMessage()}")
                try{
                    sendEmail("En réalisation") 
                }catch (Exception f){
                    log.warn("Message d'erreur lors de l'envoi du mail ${f.getMessage()}")
                    } 

            }

