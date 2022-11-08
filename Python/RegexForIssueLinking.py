##########################################################################
#
# 
# Regex permettant de créer des liens entre tickets a partir de requete JQL
#
#
##########################################################################


import re
import requests
import json
import pandas as pd
import urllib3
urllib3.disable_warnings() #permet d'enlever les warning liés aux appeles API (sera corrigé en ajoutant le certificat dans l'appel api à la place de "vérify = false")


login = ""
password =""


#Requete ppour recuperer les tickets à linker
r = requests.get('https://jira.fr/rest/api/2/search?jql=project = Test and summary ~ "*ALM-*"&field=summary&maxResults=500',auth=('Username','Password'), verify=False)
print(r.status_code)
data = r.json()

#pattern recherché, par exemple ici je recherche les tickets ayant dans le summary
#la valeur ALM-XXXX ou alm-XXXX ou les XX sont des chiffres (un ou plusieurs qui ce suivent)
pattern = re.compile("ALM-+[0-9]*|alm-+[0-9]*")
patternNumber = re.compile("-[0-9]*")



for i in data['issues']:
    result = pattern.findall(i["fields"]["summary"]) #je regarde si j'ai mon pattern dans le summary
    #print(i["key"])
    #print(result)
    if result: #la méthode findall renvoi une liste, je regarde si la liste est vide.   
        for j in result:
            string = "".join(result)#je créé un string a partir de ma liste sinon findall renvoi une liste avec des valeurs vide
            resultat = patternNumber.findall(string)
            print(resultat)
            for k in resultat:
                print(k[1:]) # je récupere tout sauf le premier caractère car les élements dans la liste ressemble à "-1455" je supprime donc le trait d'union
                s = requests.get(f'https://jira.fr/rest/api/2/search?jql=project = AML AND "Référence externe" ~ "{k[1:]}"&field=summary&maxResults=500',auth=('login','password'), verify=False)
                print("Statut code de la second requête : " , s.status_code)
                data2 = s.json()
                for l in data2["issues"]:
                    #payload pour l'appel API permettant de faire le linking
                    payload = {
                                    "type": {
                                        "name": "Blocage"
                                    },
                                    "inwardIssue": {
                                        "key": f"{l['key']}" #bloque 
                                    },
                                    "outwardIssue": {
                                        "key": f"{i['key']}" #est bloqué par
                                    }
                                }
                    r = requests.post('https://jira.fr/rest/api/2/issueLink',auth=('login','password'),data= payload, verify=False)
                    print("Statut code de la requête d'issuelink : " ,r.status_code)
                    print(f"Le ticket {i['key']} est lié à {l['key']}")
