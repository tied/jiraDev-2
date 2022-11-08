import re
import requests
import json
import pandas as pd
import urllib3
urllib3.disable_warnings() #permet d'enlever les warning liés aux appeles API (sera corrigé en ajoutant le certificat dans l'appel api à la place de "verify = false")

#login Jira
Username = ""
Password = ""

#element à chercher
pattern = re.compile("Lan&Gy")

#Requete ppour recuperer les tickets
r = requests.get('https://jira.fr/rest/api/2/search?jql=project in ("Test") and comment ~ "Landi%26Gil"&maxResults=500',auth=(Username,Password), verify=False)
print(r.status_code)
data = r.json()

#ticket à ne pas modifier
issueLIst =  ["KL-857", "MA-11240"] 
count = 0

for i in data["issues"]:
    d = requests.get(i["self"],auth=(Username,Password), verify=False)
    data_c = d.json()
    for j in data_c["fields"]["comment"]["comments"]:
        result = pattern.findall(j["body"])
        #si le result est True cela signifie qu'on a trouvé un commentaire avec le mot recherché.
        if result and data_c["key"] not in issueLIst:
            count += 1
            newCommentBody = re.sub(r'Landis&Gyr', "Landis+Gyr", j["body"]) #fonction permettant de modifier un élément dans une texte et renvoi ce texte avec l'élément modifié
            payload = { 

                "body":newCommentBody   
            }
            updateComment = requests.put(f'https://jira.fr/rest/api/2/issue/{data_c["key"]}/comment/{j["id"]}',json = payload, auth = (Username,Password) , verify=False)
            #print(updateComment.url)
            print(updateComment.status_code)
            print(j["id"], data_c["key"])
print(count)

