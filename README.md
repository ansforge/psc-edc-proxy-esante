<!--

    (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.

-->
# Exemple de mis en œuvre d'un Proxy eSanté

# Build

## Que faire si le build échoue avec le message `Some files do not have the expected license header. Run license:format to update them.` ?

1.  Rectifier les en-têtes à l'aide de cette commande :  

	```bash
	mvn validate license:format
	```
	
1.  Vérifier puis committer les changements

# Exécution

## En ligne de commande

Lancer la ligne de commande ci-dessous, où <LEVEL> peut être :

* `OFF`
* `ERROR`
* `WARN`
* `INFO`
* `DEBUG`
* `TRACE`

```bash
java java -Dlogging.level.fr.gouv.ans=<LEVEL> spring.config.location=/home/ericdegenetais/ciphered_data/missions/ANS/outil_homologation_proxy_CIBA/ -jar psc-esante-proxy-example-0.0.1-SNAPSHOT.jar
```
