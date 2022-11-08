
/***********************************************
*
*
*
*Script pour un listener
*
*Dans un listener l'issue est accessible directement via "issue" sans declaration prealable ou "event.issue"
*
*
************************************************/
package GoJiraIN
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import com.atlassian.jira.event.type.EventDispatchOption
log.setLevel(org.apache.log4j.Level.DEBUG)

def issueManager =  ComponentAccessor.getIssueManager()
def customfieldManager = ComponentAccessor.getCustomFieldManager()
//def issueKey = ComponentAccessor.getIssueManager().getIssueByCurrentKey("GJA-9784")
def user = ComponentAccessor. getJiraAuthenticationContext().getLoggedInUser()

Issue issue = event.issue

//log.warn("id de l'event : " + event.getEventTypeId())
//log.warn("Statut actuel du ticket ${issue.key} : ${issue.status.getName()}")

//id de l'event generic qui se declenche apres chaque transition ==> 13
def idEventToCheck = 13L
def eventId = event.getEventTypeId()

String url = "https://spiceformation.edge.enedis.fr/"
//Issue issue = ComponentAccessor.getIssueManager().getIssueByCurrentKey("GJA-6366")
def spiceIdCf = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10147L)
def spiceIdCfValue = issue.getDescription()

def http = new HTTPBuilder(url)
String data = '{"u_project":"'+issue.key+'"}'
if(spiceIdCfValue != null) {
    log.warn("Envoi de l'issue key a Spice")
    def response = http.request(Method.PATCH, ContentType.JSON) {
        uri.path = 'api/now/table/incident/' + spiceIdCfValue
        headers.'Authorization' = "Basic ${"spice_rest_gojira_inc:SPICE2021!".bytes.encodeBase64().toString()}"
        headers.Accept = 'application/json'
        body = data
        response.failure = { failresp_inner ->
            failresp = failresp_inner.entity.content.text + " <br /> Code " + failresp_inner.status + " <br/> Headers" + failresp_inner.allHeaders
            log.debug failresp
            log.debug response
        }
    }
}


/***************************************************
*
* Envoi du statut a Spice dans les notes de travail
*
****************************************************/


//liste des status à checker, peut-être completer modifier directement ci-dessous
def listLabelStatusToCheck = ["Terminé","En cours"]
def issueStatus = issue.getStatus().getName()

if(listLabelStatusToCheck.contains(issueStatus) && eventId == idEventToCheck ){
    log.warn("Envoi du statut actuel a Spice")
    String url_comment = "https://spiceformation.edge.enedis.fr/"
    def http_comment = new HTTPBuilder(url_comment)
    String data_comment = '{"work_notes":"'+"Le ticket Jira (${issue.getKey()}) vient de passer au statut ==> "+issueStatus+'"}'

    def response = http_comment.request(Method.PUT, ContentType.JSON) {
        uri.path = 'api/now/table/incident/' + spiceIdCfValue
        headers.'Authorization' = "Basic ${"c3BpY2VfcmVzdF9nb2ppcmFfaW5jOn1xakpQVywzK0F2Vw=="}"
        headers.Accept = 'application/json'
        body = data_comment
        response.failure = { failresp_inner ->
            failresp = failresp_inner2.entity.content.text + " <br /> Code " + failresp_inner.status + " <br/> Headers" + failresp_inner.allHeaders
            log.debug failresp
            log.debug response
        }
    }
    
}
   

/***************************************************
*
* Update du champ Lien Spice
*
****************************************************/


//mise a jour du champ permettant de visualiser l'url du ticket spice
def spiceLinkCustomfield = customfieldManager.getCustomFieldObject(19000L)
def spiceLinkValue = spiceLinkCustomfield.getValue(issue)


if(!spiceLinkValue){
    
    def newValueForSpiceLink = "https://spiceformation.edge.enedis.fr/nav_to.do?uri=%2Fincident.do%3Fsys_id%3D"+spiceIdCfValue
    log.warn("Update du champ Lien Spice avec la valeur : " + newValueForSpiceLink)
    issue.setCustomFieldValue(spiceLinkCustomfield,newValueForSpiceLink)
    def newIssue = issueManager.updateIssue(user,issue,EventDispatchOption.DO_NOT_DISPATCH,false)
    if(newIssue){
        log.warn("Lien Spice Update successful")
    }else {
        log.warn("Unsuccessful update of the Lien Spice")
    }
    
}