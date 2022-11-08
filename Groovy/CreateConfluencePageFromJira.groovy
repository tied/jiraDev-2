/*

Création d'une page confluence avec un tableau contenant les informations de plusieurs tickets Jira. C'est un job scriptrunner qui tourne chaque jour depuis Jira


*/


import groovyx.net.http.HTTPBuilder
import groovy.json.JsonSlurper
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.util.JiraHome
import groovyx.net.http.ContentType
import groovyx.net.http.Method
import org.apache.groovy.json.internal.LazyMap
import groovy.transform.Field
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.applinks.api.ApplicationLink
import com.atlassian.applinks.api.ApplicationLinkService
import com.atlassian.applinks.api.application.confluence.ConfluenceApplicationType
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.sal.api.net.Request
import com.atlassian.sal.api.net.Response
import com.atlassian.sal.api.net.ResponseException
import com.atlassian.sal.api.net.ResponseHandler
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

@Field issueManager
issueManager = ComponentAccessor.getIssueManager()


/**
 * Retrieve the primary confluence application link
 * @return confluence app link
 */
def ApplicationLink getPrimaryConfluenceLink() {
    def applicationLinkService = ComponentLocator.getComponent(ApplicationLinkService)
    final ApplicationLink conflLink = applicationLinkService.getPrimaryApplicationLink(ConfluenceApplicationType)
    conflLink
}
 
 
 
def confluenceLink = getPrimaryConfluenceLink()
assert confluenceLink // must have a working app link set up
 
def authenticatedRequestFactory = confluenceLink.createImpersonatingAuthenticatedRequestFactory()


def getTestExec(testKey){
    def JIRA_HOME_PATH = ComponentAccessor.getComponentOfType(JiraHome.class).getHomePath()
    // Read Json File
    def JiraInterfaceParam = (Map)new JsonSlurper().parseText(new File(JIRA_HOME_PATH + '/env/JiraInterfaceParam.json').text)
    // Get URL From JSON File
    def Jira_URL = JiraInterfaceParam.url.toString()
    def Jira_CREDENTIALS = 'Basic ' + JiraInterfaceParam.credentials
    def http = new HTTPBuilder(Jira_URL)
    http.setHeaders(Accept: 'application/json')
    //log.debug Jira_URL
    //log.debug username
    http.request(Method.GET, ContentType.JSON) {
        uri.path = "/rest/raven/1.0/api/testexec/${testKey}/test"
        uri.query = ['detailed': "true"]
        headers.Accept = 'application/json; charset=Windows-1252'
        headers.'Authorization' = Jira_CREDENTIALS
        response.failure = { failresp_inner ->
            failresp = failresp_inner.entity.content.text + " <br /> Code " + failresp_inner.status + " <br/> Headers" + failresp_inner.allHeaders
            log.debug failresp
            log.debug response
        }
    }
}


@Field JqlQueryParser jqlQueryParser
@Field SearchService searchService
@Field ApplicationUser jiraAdminUser
jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
searchService = ComponentAccessor.getComponent(SearchService)
jiraAdminUser = ComponentAccessor.getUserManager().getUserByName("le compte admin Jira")

// jql query
def query = jqlQueryParser.parseQuery('project = "<le nom du projet>" and issuetype = "Test Execution" and created >= startOfDay() and status = Terminé')
def search = searchService.search(jiraAdminUser, query, PagerFilter.getUnlimitedFilter())
log.warn("Total issues de la premiere JQL: ${search.total}")
log.warn(search.getResults().getClass())
def totalIssue = search.getResults()



def createHtmlBeginning(Issue issueKey){
    def hmltBeginning = """
        <p style="text-align: center;"><br /></p>
        <p>
           Test Execution 
           <ac:structured-macro ac:name="jira" ac:schema-version="1" ac:macro-id="0308af85-4d82-4a74-91d4-c93f2718ea35">
              <ac:parameter ac:name="server">Jira</ac:parameter>
              <ac:parameter ac:name="serverId">18012623-033d-3aca-8975-57a9151e336c</ac:parameter>
              <ac:parameter ac:name="key">${issueKey.getKey()}</ac:parameter>
           </ac:structured-macro>
        </p>
        <p>Heure de cr&eacute;ation de la campagne <span>${issueKey.getCreated()}</span></p>
        """
    return hmltBeginning
}


@Field assignee
def getAssigneeName(String issueKey){
    def issue = issueManager.getIssueObject(issueKey)
    if(issue.getAssignee() != null){
        assignee = issue.getAssignee().getDisplayName()
        
    }
    
    
    return assignee
    
}



def createHtmlLine(test){
    def htmlLine
    for(i in test){
        
        if(i.status == "PASS"){
        def assigneDisplayName = getAssigneeName(i.key)
        
        htmlLine = htmlLine.toString() +"""
            <tr>
         <td>
            <div class="content-wrapper">
               <p>
                  <ac:structured-macro ac:name="jira" ac:schema-version="1" ac:macro-id="e07350d0-abd5-4f42-a677-57be1cde6e25">
                     <ac:parameter ac:name="server">Jira</ac:parameter>
                     <ac:parameter ac:name="serverId">18012623-033d-3aca-8975-57a9151e336c</ac:parameter>
                     <ac:parameter ac:name="key">${i.key}</ac:parameter>
                  </ac:structured-macro>
               </p>
            </div>
         </td>
         <td class="highlight-green" style="text-align: center;" data-highlight-colour="green">PASS</td>
         <td>
            <p>${i.comment}</p>
         </td>
         <td>${assigneDisplayName}</td>
         <td colspan="1"><br /></td>
         <td colspan="1">
            <div class="content-wrapper">
               <p>
                  <ac:structured-macro ac:name="jira" ac:schema-version="1" ac:macro-id="01f3b342-bd4b-485b-986a-8e802f04edef">
                     <ac:parameter ac:name="server">Jira</ac:parameter>
                     <ac:parameter ac:name="columnIds">issuekey,summary,created,reporter,priority,status,assignee,customfield_10107,customfield_10105</ac:parameter>
                     <ac:parameter ac:name="columns">key,summary,created,reporter,priority,status,assignee,Equipe,Sprint</ac:parameter>
                     <ac:parameter ac:name="maximumIssues">100</ac:parameter>
                     <ac:parameter ac:name="jqlQuery">project = ZSEV2 AND issuetype = Anomalie and &quot;Plateforme(s)&quot; = &quot;POD1&quot; and  issue in linkedIssues(&quot;${i.key}&quot;)  and not  ( created &lt; startOfDay() and status = Termin&eacute;) AND created &lt; startOfDay("+1") order by createdDate </ac:parameter>
                     <ac:parameter ac:name="serverId">18012623-033d-3aca-8975-57a9151e336c</ac:parameter>
                  </ac:structured-macro>
               </p>
            </div>
         </td>
      </tr>"""
        
        }else{
            def assigneDisplayName = getAssigneeName(i.key)
            htmlLine = htmlLine.toString() +  """
            <tr>
         <td>
            <div class="content-wrapper">
               <p>
                  <ac:structured-macro ac:name="jira" ac:schema-version="1" ac:macro-id="e07350d0-abd5-4f42-a677-57be1cde6e25">
                     <ac:parameter ac:name="server">Jira</ac:parameter>
                     <ac:parameter ac:name="serverId">18012623-033d-3aca-8975-57a9151e336c</ac:parameter>
                     <ac:parameter ac:name="key">${i.key}</ac:parameter>
                  </ac:structured-macro>
               </p>
            </div>
         </td>
         <td class="highlight-red" style="text-align: center;" data-highlight-colour="red">FAIL</td>
         <td>
            <p>${i.comment}</p>
         </td>
         <td>${assigneDisplayName}</td>
         <td colspan="1"><br /></td>
         <td colspan="1">
            <div class="content-wrapper">
               <p>
                  <ac:structured-macro ac:name="jira" ac:schema-version="1" ac:macro-id="01f3b342-bd4b-485b-986a-8e802f04edef">
                     <ac:parameter ac:name="server">Jira</ac:parameter>
                     <ac:parameter ac:name="columnIds">issuekey,summary,created,reporter,priority,status,assignee,customfield_10107,customfield_10105</ac:parameter>
                     <ac:parameter ac:name="columns">key,summary,created,reporter,priority,status,assignee,Equipe,Sprint</ac:parameter>
                     <ac:parameter ac:name="maximumIssues">100</ac:parameter>
                     <ac:parameter ac:name="jqlQuery">project = ZSEV2 AND issuetype = Anomalie and &quot;Plateforme(s)&quot; = &quot;POD1&quot; and  issue in linkedIssues(&quot;${i.key}&quot;)  and not  ( created &lt; startOfDay() and status = Termin&eacute;) AND created &lt; startOfDay("+1") order by createdDate </ac:parameter>
                     <ac:parameter ac:name="serverId">18012623-033d-3aca-8975-57a9151e336c</ac:parameter>
                  </ac:structured-macro>
               </p>
            </div>
         </td>
      </tr>"""
    }
    
        }
    
		return htmlLine
    
}

@Field String html
   html = """<p><span><br /></span></p>
      <p><br /></p>
      <h2><span>Tests et R&eacute;sultats</span></h2>
      <table class="relative-table wrapped" style="width: 90.709%;">
         <colgroup>
            <col style="width: 18.7927%;" />
            <col style="width: 10.1411%;" />
            <col style="width: 34.8643%;" />
            <col style="width: 11.3278%;" />
            <col style="width: 11.5974%;" />
            <col style="width: 13.2704%;" />
         </colgroup>
         <tbody>
            <tr>
               <th style="text-align: center;">Lien du test</th>
               <th style="text-align: center;">Statut</th>
               <th style="text-align: center;">Description d'ex&eacute;cution</th>
               <th style="text-align: center;">Responsable du test</th>
               <th colspan="1">Analyse</th>
               <th colspan="1">Anomalies rattach&eacute;es au test le jour de la Test Execution</th>
            </tr>
            """

for(i in totalIssue){

String fin = """
</tbody>
      </table>
<p class="auto-cursor-target"><br /></p>
"""
    //log.warn(i.getClass())
    def beginningLine = createHtmlBeginning(i)
	def allTest = getTestExec(i.getKey())
    def linesHtml = createHtmlLine(allTest)
    //log.warn( linesHtml)
    
def pageTitle =  "Hello"
   
def pageBody = beginningLine.toString() + html+ linesHtml + fin
     //log.warn(pageBody)
def params = [
    type : "page",
    title: i.getKey(),
    space: [
        key: "~MBD7C62N" // set the space key - or calculate it from the project or something
    ],
     // if you want to specify create the page under another, do it like this:
     ancestors: [
         [
             type: "page",
             id: "245472778",
         ]
     ],
    body : [
        storage: [
            value         : pageBody,
            representation: "storage"
        ],
    ],
]
 
authenticatedRequestFactory
    .createRequest(Request.MethodType.POST, "rest/api/content")
    .addHeader("Content-Type", "application/json")
    .setRequestBody(new JsonBuilder(params).toString())
    .execute(new ResponseHandler<Response>() {
        @Override
        void handle(Response response) throws ResponseException {
            if (response.statusCode != HttpURLConnection.HTTP_OK) {
                throw new Exception(response.getResponseBodyAsString())
            } else {
                def webUrl = new JsonSlurper().parseText(response.responseBodyAsString)["_links"]["webui"]
            }
        }
    })    


}
	
    



