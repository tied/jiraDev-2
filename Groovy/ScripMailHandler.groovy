/*


MailHandler de scriptrunner permet de créer un mail en récuperant ce qui y est présent entre certain caractèere uniquement dans ce script c'est ce qui est
présent entre >< qui est récuperer. Par exemple >Bonjour la team< on va récuperer ==>"Bonjour la team", ou >Bonjour 
la 
team< 
va donner ==> "Bonjour 
la 
team"

Si le sujet du mail contient une clé de ticket on va ajouter un commentaire sinon on va regarder si le sujet du mail est égal à un summary dans jira et créer un commentaire
si le sujet du mail n'est pas égale un summary on créer un nouveau ticket a partir du mail.



*/

import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.Issue
import java.util.regex.Pattern
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.IssueTypeManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.service.util.ServiceUtils
import com.atlassian.mail.MailUtils
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager
import com.atlassian.mail.MailUtils
import org.apache.commons.io.FileUtils
import com.atlassian.jira.config.util.JiraHome
import com.atlassian.jira.service.services.file.FileService


def commentManager = ComponentAccessor.getComponentOfType(CommentManager)
def issueManager = ComponentAccessor.getIssueManager()
def searchService = ComponentAccessor.getComponentOfType(SearchService)
def projectManager = ComponentAccessor.getProjectManager()
def issueFactory = ComponentAccessor.getIssueFactory()
def issuetypeManager = ComponentAccessor.getComponent(IssueTypeManager)
def userManager = ComponentAccessor.getComponent(UserManager)
def issueTypeSchemeManager = ComponentAccessor.getComponent(IssueTypeSchemeManager)
JiraHome jiraHome = ComponentAccessor.getComponent(JiraHome)

//def issueManager = ComponentAccessor.getIssueManager()
//def issue = issueManager.getIssueByCurrentKey("GJA-10089")

//clé de projet ou le ticket/commentaire sera créé à modifier comme souhaité
def projectKey = "test"

ApplicationUser user = userManager.getUserByName("gojira-admin")
def project = projectManager.getProjectObjByKey(projectKey)

//recuperer l'issuetype par défaut du projet
def defaultIssueType = issueTypeSchemeManager.getDefaultIssueType(project)

def subject = message.getSubject()
def body = MailUtils.getBody(message)
def indexOfDe = body.find('De :') 
if(indexOfDe){
    body = body.substring(0, body.indexOf(body.find('De :'))) //permet de récuperer que le premier mail et non les réponses précèdentes du mail
}else{
    body = body
}
Issue  issue = ServiceUtils.findIssueObjectInString(subject)

if (issue) { // si le numéro de ticket est dans le sujet alors je mets à jour le ticket
    MutableIssue mutableIssue = issueManager.getIssueObject(issue.id)

    def patt = Pattern.compile(">((.+?))<",Pattern.DOTALL)//==> dotall permet de matcher ce qu'il y a sur plusieurs afin de ne pas s'arreter avant la breakline.
    def match = patt.matcher(body)
    List<String> stringList = []
    while(match.find()){

        //log.warn("Body du mail : " + m.group(1))
        stringList.add(match.group(1))

    }
    String newBody = stringList.toString()
            .replace("[","")
            .replace("]","")
            .replace(",","\n")
    log.warn("Body du mail : " + newBody)
    log.warn("Clé de ticket qui doit être mis à jour "  + issue.key)
    comment = commentManager.create(issue,user, newBody, false)
    if(!issue.summary.contains("|")) {
        mutableIssue.setSummary("|" + issue.summary)
        issueManager.updateIssue(user,mutableIssue, EventDispatchOption.DO_NOT_DISPATCH,false)
    }
    //ajout des pièces jointe
    def attachments = MailUtils.getAttachments(message)
    attachments.each { MailUtils.Attachment attachment ->
    def destination = new File(jiraHome.home, FileService.MAIL_DIR).getCanonicalFile()
    def file = FileUtils.getFile(destination, attachment.filename) as File 
    FileUtils.writeByteArrayToFile(file, attachment.contents)
    messageHandlerContext.createAttachment(file, attachment.filename, attachment.contentType, user, issue) 
}
}else{
    //recherche du sujet du mail
    def q = Pattern.quote("Re: ")// permet de créer un regex qui reprends littérallement la chaine de caractere qu'on a mis en parametre. Exemple pattern.quote('+au()') permettra de rechercher la chaine de caractere '+au()' sans avoir a echapper le signe + et les parenthese.
    def p = Pattern.compile(q + "([a-z].*)",Pattern.DOTALL)
    def m = p.matcher(subject)
//log.warn(m)
    if(m.find()){
        log.warn("Sujet du mail : " + m.group(1))
        subject = m.group(1)
    }

//recherche du body du mail
    p = Pattern.compile(">((.+?))<",Pattern.DOTALL)
    m = p.matcher(body)
//log.warn(m)
    List<String> bodyList = []
    while(m.find()){

        //log.warn("Body du mail : " + m.group(1))
        bodyList.add(m.group(1))

    }

    String bodyListString = bodyList.toString()
            .replace("[","")
            .replace("]","")
            .replace(",","\n")
    log.warn("Body du mail : " + bodyListString)

//vérification si le mail a déjà été traité en fonction du summary, si je trouve un ticket avec le meme summary j'arrete le traitement
    final jqlSearch = """ issueFunction in issueFieldMatch("project = ${projectKey}", "summary", "^${subject}\$")"""

    def parseResult = searchService.parseQuery(user, jqlSearch)
    if (!parseResult.valid) {
        log.warn('Invalid query')
        return null
    }

//lancement de la recherche
    def results = searchService.search(user, parseResult.query, PagerFilter.unlimitedFilter)
    def issues = results.results
    if(issues){
        log.warn("Un ou plusieurs ticket(s) contiennent le même résumé dans Jira ==> ${issues.collect { it.key }}, création du commentaire")
        def issueToUpdate = issues.first()
        comment = commentManager.create(issueToUpdate,user, bodyListString, false)
                
        def attachments = MailUtils.getAttachments(message)
        attachments.each { MailUtils.Attachment attachment ->
        def destination = new File(jiraHome.home, FileService.MAIL_DIR).getCanonicalFile()
        def file = FileUtils.getFile(destination, attachment.filename) as File 
        FileUtils.writeByteArrayToFile(file, attachment.contents)
        messageHandlerContext.createAttachment(file, attachment.filename, attachment.contentType, user, issueToUpdate) 
        }


        return
    }else{// si pas de summary identique dans GoJira alors création du ticket
        log.warn("création du ticket")
        MutableIssue issueObject = issueFactory.getIssue()
        issueObject.setProjectObject(project)
        issueObject.setSummary("|"+subject)
        issueObject.setDescription(bodyListString)
        issueObject.setIssueType(defaultIssueType)
        issueObject.setReporter(user)

        def newIssue = messageHandlerContext.createIssue(user, issueObject)
        
        def attachments = MailUtils.getAttachments(message)
        attachments.each { MailUtils.Attachment attachment ->
        def destination = new File(jiraHome.home, FileService.MAIL_DIR).getCanonicalFile()
        def file = FileUtils.getFile(destination, attachment.filename) as File 
        FileUtils.writeByteArrayToFile(file, attachment.contents)
        messageHandlerContext.createAttachment(file, attachment.filename, attachment.contentType, user, newIssue) 

    }

}
}