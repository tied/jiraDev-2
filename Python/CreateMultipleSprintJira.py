import pandas as pd 
import requests
import urllib3
from tqdm import tqdm #==> permet d'avoir une barre de progression
urllib3.disable_warnings() #permet d'enlever les warning liés aux appeles API (sera corrigé en ajoutant le certificat dans l'appel api à la place de "vérify = false")

Username = ""
Password = ""

df = pd.read_excel("C:\\Users\\MBD7C62N\\Downloads\\sprint 25082022.ods",engine = "odf")
#print(df.head())
#print(df.loc[7, "Sprint"])
proxies = {'https':'http://vip.proxy:3131'}


for i in tqdm(range(len(df))):
    #je verifie que les valeurs de la ligne dans les colonnes date de début et date de fin ne sont pas nulles
    if not pd.isnull(df.loc[i,"date début"]) and not pd.isnull(df.loc[i,"date fin"]): #isnull permet de savoir si la valeur est NaT ou NaN (pas de valeur ou valeur null)
        #print(df.loc[i,"Sprint"],df.loc[i,"date début"].isoformat(),df.loc[i,"date fin"] ) parcourir le dataframe (fichier csv) en affichant les valeurs des colonnes de chaque ligne
        #création du payload avec les valeurs du csv
        payload = {    "name": df.loc[i,"Sprint"],
                        "startDate": df.loc[i,"date début"].isoformat(),
                        "endDate": df.loc[i,"date fin"].isoformat(),
                        "originBoardId": 621}
        r = requests.post("https://jira.fr/rest/agile/1.0/sprint",proxies=proxies,auth=(Username, Password),json=payload, verify=False)
        if r.status_code != 201:
            print(f'Création KO du sprint {df.loc[i,"Sprint"]} {r.text}, code erreur {r.status_code}')

    else:
        payload = {    "name": df.loc[i,"Sprint"],
                        "originBoardId": 621}
        r = requests.post("https://jira.fr/rest/agile/1.0/sprint",proxies=proxies,auth=(Username, Password),json=payload, verify=False)
        if r.status_code != 201:
            print(f'Création KO du sprint {df.loc[i,"Sprint"]} {r.text}, code erreur {r.status_code}')
