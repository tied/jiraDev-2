from tkinter import E
import requests
import urllib3
urllib3.disable_warnings()
from urllib3.exceptions import HTTPError
import logging
import smtplib
from email.message import EmailMessage

#config du logger qui ecrasera a chaque lancement de script les valeurs du fichier (filemode="w")
logging.basicConfig(filename='example.log', encoding='utf-8',
                    filemode="w", level=logging.INFO, 
                    format='%(asctime)s %(levelname)-8s %(message)s', 
                    datefmt='%Y-%m-%d %H:%M:%S') 


username = ""
password = ""

class Version:
    def __init__(self, pluginName, currentVersion, marketplaceVersion):
        self.name = pluginName
        self.currentVersion = currentVersion
        self.versionTarget = marketplaceVersion


versionList = []

r = requests.get("https://confluence.fr/rest/plugins/1.0/",auth=(username, password), verify=False)
#print(r.status_code)

data =r.json()

pluginExcluded = ['Analytics for Confluence - Plugin']

proxie = {'https':'http://vip.proxy.fr:3131'}
for i in data['plugins']:
    if not "applicationKey" in i and i["userInstalled"] == True and i["enabled"] == True and i['name'] not in pluginExcluded :
        #print(i['name']," ", i['version'])
        r2 = requests.get(f"https://marketplace.atlassian.com/rest/2/addons/{i['key']}/versions",proxies=proxie, verify=False)
        try:
            r2.raise_for_status()
            marketplaceData = r2.json()
            if i['version'] != marketplaceData["_embedded"]["versions"][0]["name"]:
                version = Version(i['name'], i['version'], marketplaceData["_embedded"]["versions"][0]["name"])
                versionList.append(version)

        except  requests.exceptions.RequestException  as e:
            logging.info(f"Echec de l'appel API vers le marketplace pour le plugin {i['name']}==> {r2.reason}, code erreur ==> {r2.status_code}")


#envoi du mail
messageBody = """<br>Bonjour,</br> 
<br>Les plugins suivants de l'instance GoJira IN ont une ou des version(s) d'écart avec la dernière version publiée sur le marketplace d'Atlassian : </br> 
<br></br>
<html><body>
<table border-collapse = "collapse" border = "1" cellspacing="0" cellpadding = "3" padding = "4">
<tr>
<th >Nom du plugin</th>
    <th>Version actuel</th>
    <th>Dernière version sur le marketplace</th>
</tr>"""

messageFooter = """</table>
</body>
</html>
<br></br>
<p style="margin:0px" >L'equipe Jira
<p style="margin:0px" >Cordialement,</p>
"""
for i in versionList:
    messageBody = messageBody +f""" 
<tr>
    <th>{i.name}</th>
    <th>{i.currentVersion}</th>
    <th>{i.versionTarget}</th>
</tr>
     """
finalMessage = messageBody+messageFooter
print(finalMessage)

msg = EmailMessage()
msg['To'] = "mohamed.benziane@hotmail.fr"
msg['Subject'] = "bonjour test"

# Send the message via our own SMTP server.
#s = smtplib.SMTP("mailhost.fr",25)
msg['From'] = "noreply@enedis.fr"
with smtplib.SMTP("mailhost.der.edf.fr",25) as s:
    print(s.noop())
    s.send_message(msg)
    
