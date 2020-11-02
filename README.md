# Composite Data Store
Composite Data Store ist eine Spring Boot Applikation (Java 8) mit Gradle als Build Engine und Docker als Deployment Lösung.

## Bauen
Das Projekt ist ein Gradle Projekt und lässt sich mit folgenden Befehlen bauen:

`gradle clean build`

Um ein Docker Image zu bauen, muss natürlich Docker auf der Maschine laufen. Der Baubefehl ist als Gradle task hinterlegt und kann wie folgt aufgerufen werden.

`gradle docker`

 Anschließend sollte ein Image namens '**compositedatastore:latest**' in dem lokalen Docker Repository vorhanden sein.

 ## Ausführen
 ### Lokal Java
 Die Applikation lässt sich lokal als Spring Boot Applikation starten, in dem **main** Methode der Klasse '**Application**' im Paket '*de.uni.sttg.ipvs.as.composite.data.store*' im Ordner *application* ausgeführt wird.

### Docker Container

Um die Applikation als Docker Container zu starten kann der folgende Befehl genutzt werden:

`docker run -p 8080:8080 -v cdsvolume:/tmp --name compositedatastore --network="host" compositedatastore`

**Ganz wichtig** ist hier das *Docker Volume* **cdsvolume**. Das ermöglicht, dass der zustand der Applikation (Policies und Datenmodell) auch nach dem Neustart der Applikation beibehalten wird.

### UI
Die UI der Applikation ist über **Port** **8080** erreichbar. Hier sei angemerkt, dass die Änderungen in der UI zwar auf die Festplatte gespeichert werden, jedoch die Laufzeit nicht beinfluss wird. Das heißt um die geänderten Regeln zu übernehmen, muss die Applikation neu gestartet werden.


 ## Umgebung
 Die Umgebung der Applikation besteht aus 4 Systeme: *Redis*, *MySQL*, *RabbitMQ* und *MongoDB*.
 Diese liegen vorkonfiguriert als eine **docker-compose.yml** Datei im Order */Docker* bereit. Die Zugangsdaten zu den jeweiligen Systemen sind der **docker-compose.yml** zu entnehmen.

 Um die Umgebung zu starten muss Docker Compose installiert sein. Im gleichen Verzeichnis der **docker-compose.yml** kann mit dem folgenden Befehl die Umgebung gestartet werden:

 `docker-compose up`

 Weiterhin befindet sich noch ein "*Datengenerator*", welcher manuell mit **Python** gestartet werden kann. Der Generator befindet sich im Ordner */Docker/sensorsender* und ist ein simples **Python** Skript. Gestartet kann dieser im gleichen Verzeichnis mit:

 `python ./sensorsender.py`

 Eine **Dockerfile** liegt hier auch bei, um den Sender auch als Docker Container nutzen zu können.

 ## Deployment
 **STAND 3.06.19**

 Die Applikation ist auf dem IPVS Open Stack mit zwei VMs deployed.
 Auf der VM **MA_CDS_MAIN** läuft als Docker Container diese Applikation, sowie der Python Datengenerator.
 Auf der VM **MA_CDS_ENV** laufen die 4 Systeme *Redis*, *MySQL*, *RabbitMQ* und *MongoDB*.

 Um sich auf die VMs per **SSH** einzuloggen kann der Key **ma_cds_key.pem** in diesem Repo genutzt werden.

 `ssh ubuntu@192.168.209.203 -i ./path/to/ma_cds_key.pem`

 `ssh ubuntu@192.168.209.241 -i ./path/to/ma_cds_key.pem`

 Über **Portainer** lassen sich alle Container und Logs über eine UI steuern. Auf beiden VMs läuft **Portainer** auf dem **Port 9000**. Die **Zugangsdaten** sind "admin/adminpwd".

 ### Deployen
 Das Deployment läuft (leider) manuell ab, da keine Docker Registry genutzt wird.
 Für das Deployment der Umgebung mit den 4 Systemen, muss jediglich die **docker-compose.yml** aktualisiert und auf die VM übertragen werden. 
 
`scp -i ./path/to/ma_cds_key.pem ./path/to/docker-compose.yml ubuntu@192.168.209.241:~/`

 Dort kann mit `docker-compose stop` bisherige Systeme angehalten werden. Mit `docker-compose up` kann die neue Konfiguration übernommen werden.

 Für das Deployment der Applikation muss das Docker Image mit folgendem Befehl als Tar Datei exportiert werden.

 `docker save --output cbs.tar compositedatastore`

 Die Tar Datei kann nun auf die VM mit folgendem Befehl geschoben werden

 `scp -i ./path/to/ma_cds_key.pem ./path/to/cbs.tar ubuntu@192.168.209.203:~/`

 Nun kann man sich per SSH auf die VM einloggen und dort mit folgendem Befehl die Tar Datei importieren

 `docker load --input cbs.tar`

 Anschließend kann der Docker Container ganz normal gestartet werden.
 DAs gleiche Verfahren kann auch für den Python Datengenerator genutzt werden.



 
