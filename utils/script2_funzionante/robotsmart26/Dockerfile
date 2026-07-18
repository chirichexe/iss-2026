## ----------------------------------------------------------
## Dockerfile for unibo.robotsmart26
## ----------------------------------------------------------

FROM eclipse-temurin:17.0.5_8-jre-focal AS builder
## EXPOSE 8020
## ADD extracts the tar
ADD ./build/distributions/robotsmart26-1.0.tar /
WORKDIR /robotsmart26-1.0/bin
COPY ./*.pl ./
COPY ./*.json ./
COPY ./*.bin ./
COPY ./*.txt ./

## RUN apt-get update -y
## RUN apt-get install -y wiringpi
## RUN sudo apt-get install -y python

CMD ["bash", "robotsmart26"]

## 0) docker rmi robotsmart26:1.0
## 1) gradlew distTar
## 2) docker build -t robotsmart26:1.0 .
## OCCORRE APRIRE UDP PER PERMETTERE COAP
## docker run -it --rm --name robotsmart26 -p8020:8020/tcp -p8020:8020/udp  --privileged robotsmart26:1.0  /bin/bash
## MODIFICA DEL FILE DI CONFIGURAZIONE
# docker cp basicrobotConfig.json 89193dba02a2:/robotsmart26-1.0/bin/basicrobotConfig.json
# docker cp stepTimeConfig.json cab3aa29f8eb:/robotsmart26-1.0/bin/stepTimeConfig.json

# docker tag robotsmart26:1.0 natbodocker/robotsmart26:1.0

#type docker_password.txt | docker login --username natbodocker --password-stdin
#docker login --username natbodocker --password xyz
# docker push natbodocker/robotsmart26:1.0

# docker tag virtualrobotdisi25:2.3 natbodocker/virtualrobotdisi25:2.3
# docker push natbodocker/virtualrobotdisi25:2.3

# docker-compose -f robotsmart26.yaml  up