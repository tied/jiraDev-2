from jira import JIRA
from jira import JIRAError
import csv

#script pour recuperer la plus récente modification de ticket par projet.

jira_server = JIRA(basic_auth=("m",""), options={'server': 'https://issues.net'})#ajouter les identifiants

issues_in_proj = ""
project = jira_server.projects()

for i in project:
    #print(i.key)
    try:
        issues_in_proj = jira_server.search_issues(f'project = {i.key} ORDER BY updatedDate DESC')
        for j in issues_in_proj[:1]:#get only the first element
            with open ("C:\\Users\\Mohamed\\Git\\listeProject.csv","a", encoding="UTF-8", newline='') as f:
                writer = csv.writer(f, delimiter =',')
                data =[i.name +"," + j.fields.updated] 
                #le module csv attends une liste ou tuple pour les ajouter au csv, 
                # si je mets un string seul, chaque caractère sera séparé par une virgule, 
                #data = "bonjour" ==> b,o,n,j,o,u,r
                #data = ["bonjour"] ==> bonjour
                #print(data)
                writer.writerow(data)
                 #f.write(data)

            #print(i.name , " ",  type(j.fields.updated))
    except JIRAError:
        print("Impossible de récuperer les données pour le projet " + i.name)

