#
# (c) Copyright 2024-2024, Agence du Numérique en Santé (ANS) (https://esante.gouv.fr). All rights reserved.
#

FROM maven:3.9.9-eclipse-temurin-21-jammy

COPY src /src
WORKDIR /src
RUN mvn clean package
