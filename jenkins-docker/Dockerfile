FROM jenkins/jenkins:lts-jdk21
USER root

#Installer le client docker
COPY --from=docker:27-cli /usr/local/bin/docker /usr/local/bin/docker
USER jenkins