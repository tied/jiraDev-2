import requests
import urllib3
urllib3.disable_warnings() #permet d'enlever les warning liés aux appeles API (sera corrigé en ajoutant le certificat dans l'appel api à la place de "vérify = false")
import csv

"""
Script pour recuperer toutes les verions mise sur les tickets en fonction d'une JQL

"""

def getAllVersions():
    block_size = 50
    block_num = 0
    listIssue = []
    while True:
            start_idx = block_num*block_size
            re = requests.get(f'https://jira.fr/rest/api/2/search?jql=project="Test" and (affectedVersion is not empty or fixVersion is not EMPTY)&startAt={start_idx}&maxResults={block_size}',auth=('login','Password'), verify=False)
            if len(re.json()['issues']) == 0:
                # recuperer les issues jusqu'a ce qu'il y en ai plus
                break
            block_num += 1
            for i in re.json()['issues']:
                if i["fields"]["versions"]:
                     for j in i["fields"]["versions"]:
                         #print("affected version : ",j['name'],i["key"])
                         listIssue.append(j['name']+"--"+j["id"])
                if i["fields"]["fixVersions"]:
                    for k in i["fields"]["fixVersions"]:
                         #print("fixVersion: ",k['name'], i["key"])
                         listIssue.append(k['name']+"--"+k["id"])
    return listIssue

toto = getAllVersions()
print(len(toto))
myset = set(toto)#permet de ne pas avoir de doublon dans la liste
print(len(myset))

with open("VersionListAML.csv","a+",newline='') as csvfile:
     csvwriter = csv.writer(csvfile)
     for i in list(myset):
         litotosting = [i] #la valeur est mis dans une liste pour que python puisse ajouter la valeur dans chaque cellule comme cela "toto" et non pas "t,o,t,o"
         csvwriter.writerow(litotosting)
          
