FROM node:24-alpine AS build

WORKDIR /app
COPY package*.json ./
RUN npm ci

# Inclui .env.local para incorporar VITE_API_URL ao JavaScript gerado.
COPY . .
ARG VITE_API_URL=
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# imagem nginx
FROM nginx:alpine 

ARG VERSION=1.0.0

RUN echo "criando imagem version: $VERSION"

# sobreescrevemos nginx conf
COPY nginx.conf /etc/nginx/nginx.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80/tcp

EXPOSE 80/udp

LABEL version="vue-monitoring"

CMD ["nginx", "-g", "daemon off;"]
