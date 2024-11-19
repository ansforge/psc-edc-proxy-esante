#
# The MIT License
# Copyright © 2024-2024 Agence du Numérique en Santé (ANS)
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#

FROM maven:3.9.9-eclipse-temurin-21-jammy AS builder

COPY . /src
WORKDIR /src
RUN mvn clean package

FROM eclipse-temurin:21.0.5_11-jdk
COPY --from=builder /src/target/psc-esante-proxy-example-*.jar /usr/app/psc-esante-proxy-example.jar
EXPOSE 8080
USER daemon
ENV LOG_LEVEL=INFO
ENTRYPOINT ["java","-Dlogging.level.fr.gouv.ans=${LOG_LEVEL}","-Dspring.config.location=/usr/app/config/application.yml","-jar","/usr/app/psc-esante-proxy-example.jar"]
