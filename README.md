# Exemple de mis en œuvre d'un Proxy eSanté

# Build

## Que faire si le build échoue avec le message `Some files do not have the expected license header. Run license:format to update them.` ?

1.  Rectifier les en-têtes à l'aide de cette commande :  

	```bash
	mvn validate license:format
	```
	
1.  Vérifier puis committer les changements
