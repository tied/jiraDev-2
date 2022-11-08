import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.bc.user.ApplicationUserBuilderImpl
import com.atlassian.jira.component.ComponentAccessor
import org.apache.commons.lang3.RandomStringUtils 
import com.atlassian.jira.avatar.AvatarService
import com.atlassian.jira.icon.IconType
import com.atlassian.jira.avatar.Avatar
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.avatar.AvatarServiceImpl


//doc pour la class RandomString https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/RandomStringUtils.html

def AvatarServiceImpl = ComponentAccessor.getComponent(AvatarServiceImpl)
def JiraAuthentification = ComponentAccessor.getComponent(JiraAuthenticationContext)
def avatar = ComponentAccessor.getAvatarManager()
def UserManager = ComponentAccessor.getUserManager()
def AvatarManager = ComponentAccessor.getAvatarManager()
def AvatarService = ComponentAccessor.getAvatarService()
def userBuilder = ComponentAccessor.getComponent(ApplicationUserBuilderImpl)
//def remoteUser = UserManager.getUserByName("userUBE5tDiqlCO4")
def user = JiraAuthentification.getLoggedInUser()

//liste des users a modifier, mettre l'username
def userList = ["user1","user2"]


//GET All system avatar
def avatars = AvatarManager.getAllSystemAvatars(IconType.USER_ICON_TYPE)
List avatarsIdSytem = new ArrayList()
for(i in avatars ){
    avatarsIdSytem.add(i.getId())
}
log.info("Liste des avatars systeme (defaut) " + avatarsIdSytem)

//Get id du premier avatar systeme de Jira
def avatarIdToSet = avatars.first().getId()
log.info("Id de l'avatar qui remplacera les avatars custom " + avatarIdToSet )



for(i in userList){
    //test si l'utilisateur peut-être renomme
    def userToUpdate = UserManager.getUserByName(i)
    def canBeUpdated = UserManager.canUpdateUser(userToUpdate)
    log.info("Est-ce que l'utlisateur " + userToUpdate + " peut-être mis à jour : " + canBeUpdated)
    //modification de l'utilisateur
    if(canBeUpdated){
        def randomString = RandomStringUtils.random(12, true, true)
        log.info("User à modifier " + userToUpdate)
		def newData = new ApplicationUserBuilderImpl(userToUpdate).name("user"+randomString).displayName(randomString).emailAddress(randomString + "@enedis.fr").build()
		UserManager.updateUser(newData)
        log.info("Nouvelles valeurs attribue a l'utilisateur : " + newData)
        def userAvatar = AvatarService.getAvatar(user, userToUpdate)//peut etre null mais la methode issystemAvatar ne fonctionne pas avec des objets null
        def avatarUserId
        if(userAvatar){
            avatarUserId = userAvatar.getId()
        }
        log.info("Id de l'avatar de l'utilisateur : " + avatarUserId)
        if(!avatarsIdSytem.contains(avatarUserId) & avatarUserId != null){
            log.info("instruction pour avatar non system")
            //Mise en place d'un avatar par defaut
            AvatarServiceImpl.setConfiguredAvatarIdFor(userToUpdate, avatarIdToSet) //la methode retourne un void
            //suppression de l'avatar cree par l'utilisateur
            def avatarDeletion = AvatarManager.delete(avatarUserId)
			log.info("Reussite de la suppresion de l'avatar : " + avatarDeletion)
            if(!avatarDeletion){
                log.info("Echec de la suppression de l'avatar custom de l'utilisateur")
            }
            else{
                log.info("Suppression de l'avatar dans la BDD reussie")
            }
            
        }
        
    }
    else{
        log.info("Les informations de l'utilisateur "+ i + " Ne peuvent pas être mise à jour")
    }
    
    }
    