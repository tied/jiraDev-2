/*

Remplissage automatique des champs des Test Executions depuis le test plan auxquels ils sont associés (code dans une postfunction du test plan).

*/
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.util.JiraHome
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import java.sql.Timestamp

// Relative Jira_Home Path
def JIRA_HOME_PATH = ComponentAccessor.getComponentOfType(JiraHome.class).getHomePath()
// Read Json File
def JiraInterfaceParam = (Map)new JsonSlurper().parseText(new File(JIRA_HOME_PATH + '/env/JiraInterfaceParam.json').text)
// Get URL From JSON File
def Jira_URL = JiraInterfaceParam.url.toString()
def Jira_CREDENTIALS = 'Basic ' + JiraInterfaceParam.credentials

def http = new HTTPBuilder(Jira_URL)
def issueManager = ComponentAccessor.getIssueManager()
def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
//def contructeurCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(14104)
def dateDeReception = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11900)

def issueService = ComponentAccessor.issueService
//def mutableIssue = ComponentAccessor.getIssueManager().getIssueByCurrentKey("QHW-22")
def dateDeReceptionValue = dateDeReception.getValue(issue)
log.warn("Date de reception du Test Plan : " + dateDeReceptionValue)



def getTestExecFromTestPlan = http.request(Method.GET, ContentType.JSON) {
    uri.path = 'rest/raven/1.0/api/testplan/'+issue.key+'/testexecution' //issue.key ==> recuperer la clé du ticket dans la postfunction
    headers.Accept = 'application/json'
    headers.'Authorization' = Jira_CREDENTIALS
    response.failure = { failresp_inner ->
        failresp = failresp_inner.entity.content.text + " <br /> Code " + failresp_inner.status + " <br/> Headers" + failresp_inner.allHeaders
        log.info failresp
        log.info response
    }

}

//ajout de 10 jours ouvrés à la date du champ Date de reception du test Plan
Calendar calendar = Calendar.getInstance();
calendar.setTime(new Date ( dateDeReceptionValue.getTime()))
int days = 10;
while (days > 0){
    calendar.add(Calendar.DAY_OF_YEAR, 1);
    if(calendar.get(Calendar.DAY_OF_WEEK) <= 5)--days;
}
def newDateForTestExec = new Timestamp (calendar.getTimeInMillis())
log.warn("Date de reception plus 10 jours ouvres : " + newDateForTestExec)


for(i in getTestExecFromTestPlan){  
    //get issue
    def issueTestExec = ComponentAccessor.getIssueManager().getIssueByCurrentKey(i.key)
    //log.warn(issueTestExec.getClass())
    //update issue
    issueTestExec.setCustomFieldValue(dateDeReception,newDateForTestExec)
    issueManager.updateIssue(currentUser,issueTestExec,EventDispatchOption.DO_NOT_DISPATCH ,false)

}
