/*

Script permettant de masquer certain type de lien (ne pas afficher  block by <> block, contenu, contient etccc)

*/


import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import static com.atlassian.jira.issue.IssueFieldConstants.*

def issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager)

def equipe = getFieldById("customfield_10107")
def linkedIssue = getFieldById("issuelinks")
def domaines = getFieldById ("customfield_10145")
def component = getFieldById (COMPONENTS)
def porteurMetier = getFieldById ("customfield_10207")
def allowedOutwardTypes = ["est contenu dans","est lié à"]
def allowedInwardTypes = ["contient","est lié à"]
def inwardAllowedLinks 
def outwardAllowedLinks
def allowedLinks
def availableIssueTypes = []
def constantsManager = ComponentAccessor.getConstantsManager()
def allIssueTypes = constantsManager.getAllIssueTypeObjects()

def linkTypes = getFieldById("issuelinks-linktype")
if (getActionName() == "Créer" || getActionName() == "Create") {
    if (issueContext.issueType.name == "Macrofonction" || issueContext.issueType.name == "Macrofonction Enabler" ) {
        //get the outward link names you want
        outwardAllowedLinks = issueLinkTypeManager.getIssueLinkTypes(false).findAll{ it.outward in allowedOutwardTypes }.collectEntries{
            [it.outward,it.outward]
        }
        //get the inward link names you want
        inwardAllowedLinks = issueLinkTypeManager.getIssueLinkTypes(false).findAll{ it.inward in allowedInwardTypes }.collectEntries{
            [it.inward,it.inward]
        }
        //combine Maps of allowed Link direction names
        allowedLinks = outwardAllowedLinks << inwardAllowedLinks
        linkedIssue.setRequired(true)
        linkTypes.setFieldOptions(allowedLinks)
        component.setRequired(true)
        porteurMetier.setRequired(true)
        domaines.setRequired(true)
    }
}else{
    if (issueContext.issueType.name == "Macrofonction" || issueContext.issueType.name == "Macrofonction Enabler" ) {
		component.setRequired(true)
        porteurMetier.setRequired(true)
        domaines.setRequired(true)
    }
}
if (issueContext.issueType.name == "Besoin métier") {
    equipe.setHidden(true)
    domaines.setRequired(true)
    component.setHidden(true)
    porteurMetier.setRequired(true)
    //get the outward link names you want
    outwardAllowedLinks = issueLinkTypeManager.getIssueLinkTypes(false).findAll{ it.outward in allowedOutwardTypes }.collectEntries{
        [it.outward,it.outward]
    }
    //get the inward link names you want
    inwardAllowedLinks = issueLinkTypeManager.getIssueLinkTypes(false).findAll{ it.inward in allowedInwardTypes }.collectEntries{
        [it.inward,it.inward]
    }
    //combine Maps of allowed Link direction names
    allowedLinks = outwardAllowedLinks << inwardAllowedLinks
    linkedIssue.setRequired(true)
    linkTypes.setFieldOptions(allowedLinks)
}

availableIssueTypes.add(allIssueTypes.find { it.name == "Besoin métier" })
availableIssueTypes.add(allIssueTypes.find { it.name == "Macrofonction" })
availableIssueTypes.add(allIssueTypes.find { it.name == "Macrofonction Enabler" })

getFieldById(ISSUE_TYPE).setFieldOptions(availableIssueTypes)