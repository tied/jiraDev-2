import pandas as pd
import numpy as np
import requests
import urllib3
urllib3.disable_warnings() #permet d'enlever les warning liés aux appeles API (sera corrigé en ajoutant le certificat dans l'appel api à la place de "vérify = false")


df = pd.read_excel('C:\\Users\\Mohamed\\Downloads\\RenommageVersionsAML_V2.xlsx')

Username = ""
Password = ""

excelLine = ""
ligneToListe = ""
finalListe = ""
newVersionName = ""
versionToMergeIn = ""


#nombre de ligne dans le fichier avec les headers. Header = ligne 0
numberOfRow = df.shape[0]


for i in range(0,numberOfRow):
    excelLine = df.loc[[i]] #recuperation de la ligne du fichier excel
    ligneToListe = excelLine.values.tolist() #créer une liste a partir de la ligne recuperer
    finalListe = ligneToListe[0] #recuperation de la premiere liste dans la liste de liste créé par la methode values.tolist()
    newVersionName = finalListe.pop() #recuperation de la derniere valeur de la liste et suppression de celle-ci dans la liste
    listeFinalSansNan = [int(i) for i in finalListe if not np.isnan(i)] #je mets les valeur au format int pour ne pas avoir des floats
    versionToMergeIn = listeFinalSansNan.pop(0) #récuperation et suppression du premier element de la liste qui correspond à l'id de la version dans laquelle on va merger les version
    #print(listeFinalSansNan)

    if len(listeFinalSansNan) == 0:
        #print("Liste avec une seule valeur : ", newVersionName, versionToMergeIn)
        #faire l'update du nom de la version
        payload = { "name" : newVersionName }
        updateVersionJira = requests.put(f'https://jira.fr/rest/api/2/version/{versionToMergeIn}',json = payload, auth = (Username,Password) , verify=False)
        if updateVersionJira.status_code != 200:
            print("Echec de l'update du nom de la version pour la ligne numéro : " , i , "," , newVersionName, updateVersionJira.status_code, " | ", updateVersionJira.text )
    else:
        for j in listeFinalSansNan:
            merge = requests.put(f'https:///jira.fr/rest/api/2/version/{j}/mergeto/{versionToMergeIn}', auth = (Username,Password) , verify=False)
            if merge.status_code != 204 :
                print(f"Echec du merge vers la version {versionToMergeIn} de la ligne {i} erreur ==> {merge.status_code}, {merge.text}")
                payload = { "name" : newVersionName }
                updateVersionJira = requests.put(f'https:///jira.fr/rest/api/2/version/{versionToMergeIn}',json = payload, auth = (Username,Password) , verify=False)
                if updateVersionJira.status_code != 200:
                    print("Echec de l'update du nom de la version pour la ligne numéro : " , i , "," , newVersionName, updateVersionJira.status_code, " | ", updateVersionJira.text )
            else:
                print(f"Merge réussie vers la version {versionToMergeIn}")
