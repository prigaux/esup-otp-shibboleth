<!-- TOC -->
* [Esup Otp Shibboleth](#esup-otp-shibboleth)
  * [Description](#description)
  * [Installation](#installation)
    * [Copy package to the server](#copy-package-to-the-server)
    * [Enable Multifactor Module](#enable-multifactor-module)
    * [Install the plugin](#install-the-plugin)
  * [Configuration](#configuration)
  * [Log check](#log-check)
  * [Test](#test)
<!-- TOC -->

# Esup Otp Shibboleth

## Description

Esup Otp Shibboleth est un plugin Shibboleth.

Ce plugin est à utilisé au sein d'un login flow de type Multi-Factor. Il permet d'appeler l'api REST esup-otp


| Plugin ID                      | Module(s)         | Authentication Flow ID |
|--------------------------------|-------------------|------------------------|
| fr.renater.shibboleth.esup.otp | idp.authn.esupotp | authn/EsupOtp          |

## Build

### Prérequis

- OpenJDK 17
- Maven 3.8 ou supérieur
- Esup-Otp-Api 2.1.0 ou supérieur

```
  ./mvnw clean install -s resources/.m2/settings.xml -Dno-check-m2
```

Une fois la commande maven passée avec succès, le package du module se trouve dans le répertoire `esup-otp-dist/target` 

## Signature du plugin

Pour signer le plugin, il est nécessaire de posséder ou générer une paire clef privée/clef publique (par exemple avec gpg)
La clef publique devra être ajoutée au préalable du build dans le fichier `esup-otp-dist/src/main/resources/bootstrap/keys.txt`

La ligne de commande pour signer l'archive est la suivante : 
```
  gpg -u USER_A_REMPLACER -ab esup-otp-dist/target/shibboleth-esup-otp-${version}.tar.gz
```

## Installation

### Copie du package sur le serveur

```
$ scp module.tar.gz username@server:/tmp/.
$ scp module.tar.gz.asc username@server:/tmp/.
$ ssh username@server -i ~/.ssh/id_rsa
[username@server ~]$ sudo -i
[username@server ~]$ export idp_install_path=/usr/share/shibboleth-idp
[username@server ~]$ cp /tmp/module.tar.gz $idp_install_path/plugins/.
[username@server ~]$ cp /tmp/module.tar.gz.asc $idp_install_path/plugins/.
```

### Activer le module multifacteur Shibboleth

A partir de la documentation Shibboleth [MultiFactorAuthnConfiguration](https://shibboleth.atlassian.net/wiki/spaces/IDP5/pages/3199505534/MultiFactorAuthnConfiguration)

```
[username@server ~]$ $idp_install_path/bin/module.sh -t idp.authn.MFA || $idp_install_path/bin/module.sh -e idp.authn.MFA
```

Vérification

```
[username@server ~]$ $idp_install_path/bin/module.sh -l
```

### Installation du plugin

```
[username@server ~]$ $idp_install_path/bin/plugin.sh -i $idp_install_path/plugins/module.tar.gz --noCheck
[username@server ~]$ systemctl restart tomcat10.service
```

## Configuration

| property                        |                                       required                                       | Default value                                          | Description                                                                                                                                                                                                 |
|:--------------------------------|:------------------------------------------------------------------------------------:|--------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| idp.esup.otp.apiHost            |                                       &check;                                        |                                                        | Url de l'api esup-otp-api.                                                                                                                                                                                  |
| idp.esup.otp.apiPassword        |                                       &check;                                        |                                                        |                                                                                                                                                                                                             |
| idp.esup.otp.usersSecret        |                                       &check;                                        |                                                        |                                                                                                                                                                                                             |
| idp.esup.otp.username.strategy  |                                       &check;                                        | DefaultUsernameLookupStrategy                          | Stratégie du module pour récupérer l'identifiant pour appeler esup-otp-api (valeurs possible : DefaultUsernameLookupStrategy, SubjectContextUsernameLookupStrategy, AttributeContextUsernameLookupStrategy) |
| idp.esup.otp.attributeId        | &check; (si idp.esup.otp.username.strategy = AttributeContextUsernameLookupStrategy) |                                                        | Attribut de l'IDP à utiliser comme identifiant de l'utilisateur courant (peut être un StringAttribute ou un ScopedStringAttribute)                                                                          |
| idp.esup.otp.issuer             |                                       &check;                                        | %{idp.entityID}                                        | L'issuer correspondant au tenant pour esup-otp-api.                                                                                                                                                            |
| idp.esup.otp.endpoint.health    |                                                                                      |                                                        |                                                                                                                                                                                                             |
|                                 |                                                                                      |                                                        |                                                                                                                                                                                                             |


## Configuration des logs

Pour activer les logs debug il est nécessaire de modifier le fichier conf/logback.xml 
Par exemple : 

```
<!-- Pour logger les appels vers esup-otp-api -->
<logger name="fr.renater.shibboleth.esup.otp" level="DEBUG" />
<!-- logs debug pour le plugin -->
<logger name="fr.renater.shibboleth.idp.plugin.authn.esup.otp.impl" level="DEBUG" />
```

## Log check

- Main Log : ```$idp_install_path/logs/idp-process.log```
- Warn and error log : ```$idp_install_path/logs/idp-warn.log```

### Development

Java 17, Spring framework 6, Lombok

## License

Ce programme est un logiciel libre ; vous pouvez le redistribuer ou le modifier
suivant les termes de la licence publique générale GNU Affero telle que publiée
par la Free Software Foundation ; soit la version 3 de la licence, soit (à
votre gré) toute version ultérieure.
