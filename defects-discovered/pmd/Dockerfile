FROM node:20-alpine

# Getting all the tools needed:
# git: clone repos
# java: PMD runs on Java
# curl+unzip: download/install PMD
RUN apk add --no-cache git openjdk17-jre curl unzip

# Installing PMD as CLI
ARG PMD_VERSION=7.21.0
RUN curl -L -o /tmp/pmd.zip https://github.com/pmd/pmd/releases/download/pmd_releases%2F${PMD_VERSION}/pmd-dist-${PMD_VERSION}-bin.zip \
  && unzip /tmp/pmd.zip -d /opt \
  && rm /tmp/pmd.zip \
  && ln -s /opt/pmd-bin-${PMD_VERSION}/bin/pmd /usr/local/bin/pmd

WORKDIR /app
COPY package*.json ./
RUN npm i
COPY . .

CMD ["node", "server.js"]

