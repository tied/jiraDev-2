from typing import List
import requests
import json
import urllib3
from tqdm import tqdm
import getpass
urllib3.disable_warnings() #permet d'enlever les warning liés aux appeles API (sera corrigé en ajoutant le certificat dans l'appel api à la place de "vérify = false")

base_url = "https://jira.jira.gate.fr"
login = input("Saisissez votre username GoJira : ")
password = getpass.getpass()

#methode pour avoir tout les projets
def getAllProject(baseURL):
    if baseURL == "https://jira.fr":
        raise ValueError("/!\ Url de production détecté /!\ ")
    r = requests.get(base_url+'/rest/api/2/project',auth=(login,password), verify=False)
    listIssueKey = []
    if r.ok :
        listIssueKey = [i["key"] for i in r.json()]

    return listIssueKey

id_project_role = 10002

payload =  { "group" : ["gojira-admin"] }

toto = getAllProject(base_url)
for i in toto: #utilisation du post car le PUT va remplacer les valeurs existantes par les valeurs envoyés dans l'appel API. Alors que le post va simplement ajouter et non remplacer
    re = requests.post(base_url + f'/rest/api/2/project/{i}/role/{id_project_role}',json = payload,auth=(login,password), verify=False)
    if re.status_code == 200:
        print(f"Ajout du groupe dans le projet {i} OK")
