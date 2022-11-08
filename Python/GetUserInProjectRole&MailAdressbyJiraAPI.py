import requests
import json
import csv
import urllib3
urllib3.disable_warnings() #permet d'enlever les warning liés aux appeles API (sera corrigé en ajoutant le certificat dans l'appel api à la place de "vérify = false")

#r = requests.get('https://jira.fr/rest/api/2/project/GJA/role/10002',auth=('',''),verify=False)

#print(r.json())


f = open("C:\\Users\\User\\Desktop\\listProjets.txt","r")


for i in f:
    line = i.split(",")
    for i in line:
        projectRequest = requests.get(f'https://jira.fr/rest/api/2/project/{i}',auth=('',''),verify=False)
        projectRequest_data = projectRequest.json()
        #permet d'obtenir le nom du projet a partir de la clé du projet via l'api rest utilisé plus haut
        projectName = projectRequest_data["name"]
        r = requests.get(f'https://jira.fr/rest/api/2/project/{i}/role/10002',auth=('',''),verify=False)
        r_data = r.json()
        for i in r_data["actors"]:
            #print(i['displayName'])
            if i["type"] != "atlassian-group-role-actor":
                #permet d'avoir le nom de l'utilisateur complet
                req = requests.get('https://jira.fr/rest/api/2/user?username='+i['name'],auth=('',''),verify=False)
                req_data = req.json()
                #print(projectName + "," +req_data["displayName"] +","+  req_data["emailAddress"])
                with open("C:\\Users\\User\\Desktop\\mailAdress.csv", 'w',newline='',encoding='utf-8',) as f:
                    writer = csv.writer(f)
                    writer.writerow(projectName + "," +req_data["displayName"] +","+  req_data["emailAddress"])




        



