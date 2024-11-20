<!--

    The MIT License
    Copyright © 2024-2024 Agence du Numérique en Santé (ANS)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.

-->
# Exemple de mis en œuvre d'un Proxy eSanté

# Build

## Que faire si le build échoue avec le message `Some files do not have the expected license header. Run license:format to update them.` ?

1.  Rectifier les en-têtes à l'aide de cette commande :  

	```bash
	mvn validate license:format
	```
	
1.  Vérifier puis committer les changements

## Construction de l'image docker

Exécuter la commande suivante à la racine du projet:

```bash
docker build . -t ans.gouv.fr/psc-edc-proxy-esante
```

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
java -Dlogging.level.fr.gouv.ans=<LEVEL> -Dspring.config.location=/path/to/cfg/application.yml -jar psc-esante-proxy-example-0.0.1-SNAPSHOT.jar
```

## En déployant l'image docker

Pour déployer le proxy sous forme de container Docker, il faut lui fournir un fichier de configuration monté
sur le chemin `/usr/app/config/application.yml`.

```bash
docker run -v /host/path/to/configuration/application.yml:/usr/app/config/application.yml ans.gouv.fr/psc-edc-proxy-esante
```

### Débuggage

Les logs de debug du code applicatif peuvent être activés en définissant la variable LOG_LEVEL avec la valeur `DEBUG`

```bash
docker run -e LOG_LEVEL=DEBUG -v /host/path/to/configuration/application.yml:/usr/app/config/application.yml ans.gouv.fr/psc-edc-proxy-esante
```

### Autorités de certification

Si vous souhaitez ajouter des autorités de certification spécifiques à votre environnement à celles qui
sont automatiquement reconnues, vous pouvez ajouter les paramètres `-v /path/to/AC/directory:/certificates -e USE_SYSTEM_CA_CERTS=1`

```bash
docker run -v /path/to/AC/directory:/certificates -e USE_SYSTEM_CA_CERTS=1 -v /host/path/to/configuration/application.yml:/usr/app/config/application.yml ans.gouv.fr/psc-edc-proxy-esante
```

Le répertoire /path/to/AC/directory doit contenir le ou les certificat(s) 
au format PEM avec l'extension `.crt`.