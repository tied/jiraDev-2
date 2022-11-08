//Listener lors de la création d'un ticket pour envoyer les infos à devix au sujet du ticket 


import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import org.apache.log4j.Logger //package pour créer les logs
import groovy.json.JsonSlurper //package pour lire le json (le message retour de l'api devix est au format json)
 
def log = Logger.getLogger("com.acme.workflows")
 
CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
CustomField customField = customFieldManager.getCustomFieldObjectByName("ID Devis")
def status = issue.getStatus().name
log.warn("Statut de la commande :" + status)
 
def id_devix_string =  issue.getCustomFieldValue(customField).toString() // récuperation valeur du champ "Id Devis)
log.warn(id_devix_string)

if (id_devix_string.length() == 4){
    log.warn("Aucune valeur saisie dans le champID Devis à la création du ticket")
}
else{
    id_devix_string = id_devix_string.substring(10,id_devix_string.indexOf(']')-1) // substring de l'id devix pour passer de {xxxx20100708} à 20100708
    def id_devix = id_devix_string.toInteger() //je passe la valeur en type integer car elle est en string pour que l'api l'accepte
    log.warn("id du devix: "+' '+ id_devix)

    def command_action_id = 1
    log.warn("commande action id : " + command_action_id)
    def issuekey = issue.getKey()
    log.warn("Command id : " + issuekey)

    def url_command = "https://com-dev.fr:8443/browse/" + issuekey
    log.warn(url_command)

    //création de l'appel API
    def post = new URL("https://dev-dev.fr/webapi/dev/commandDev").openConnection();
    def message = '{"devis_id":"'+id_devix+'" ,"command_id" :"'+ issuekey +'","command_action_id" :"'+ command_action_id +'","url_command" :"'+ url_command +'"}'
    log.warn(message)
    post.setRequestMethod("POST")
    post.setDoOutput(true)
    post.setRequestProperty("Content-Type", "application/json")
    post.getOutputStream().write(message.getBytes("UTF-8"));
    def postRC = post.getResponseCode();
    println(postRC);
    //if(postRC.equals(200)) {
    //    println(post.getInputStream().getText());
    //}

    log.warn("Code retour de devix : " +' ' + postRC)


    def response = [:] //variable initalisé à vide pour y mettre la valeur du message de retour de l'api plus tard

    if(postRC.equals(200)) {
        response = new JsonSlurper().parseText(post.getInputStream().getText('UTF-8')); // initialisation de la variable avec la valeur du message retour
        log.warn("message from devix: "+' '+ response.msg)
    }

    log.warn(response) // response est un json =>{retour:200,msg:ok}

    def returnmessage = (postRC +' '+ response.msg) //variable prenant le code retour et le message retour


}

 

