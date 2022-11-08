/*

Update du champ due date en fonction de la valeur des dates (date de début et fin) du champ sprint

*/

import com.atlassian.jira.component.ComponentAccessor
import groovy.transform.Field
import com.atlassian.jira.config.IssueTypeManager
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import com.atlassian.jira.bc.issue.IssueService
import org.joda.time.DateTime



def issue = ComponentAccessor.getIssueManager().getIssueByCurrentKey("GJA-10376")
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
def useId = getTypeId("User Story")
//log.warn(tacheId)



def issuLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager)
def issueManager = ComponentAccessor.getIssueManager()
def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def issueService = ComponentAccessor.getComponent(IssueService)

//Get id du type de lien "Lien"

def linkCouverture = issuLinkTypeManager.getIssueLinkTypesByName("Couverture") // retourne une collection d'issuelinktype
def couvertureLinkId 
for(l in linkCouverture){
    log.warn(l)
    couvertureLinkId = l.getId()
}
def sprintField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Sprint")



//création d'une date antèrieur a Gojira avec la classe import org.joda.time.DateTime
def dateSprint = new DateTime(100)
//log.warn(dateSprint.getClass())
def issueLink = issueLinkManager.getInwardLinks(issue.id)
for(i in issueLink){
    log.warn(i.getSourceObject())
    def toto = i.getSourceObject().getCustomFieldValue(sprintField)
    if(toto){
        if(toto.get(0).getEndDate() > dateSprint){
            dateSprint = toto.get(0).getEndDate()
            //log.warn(dateSprint)
        }
        }
        
    }
log.warn(dateSprint.millisOfSecond().getMillis())

import com.atlassian.jira.event.type.EventDispatchOption
import java.sql.Timestamp

def gojiraAdminUser = ComponentAccessor.getUserManager().getUserByName("gojira-admin")
def timeStam = new Timestamp(dateSprint.millisOfSecond().getMillis())
log.warn(timeStam)
issue.setDueDate(new Timestamp(dateSprint.millisOfSecond().getMillis()))     
issueManager.updateIssue(gojiraAdminUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
    


