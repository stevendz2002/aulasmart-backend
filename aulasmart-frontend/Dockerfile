# ==========================================
# Etapa 1: Compilación de la SPA Angular
# ==========================================
FROM node:22-alpine AS build
WORKDIR /app

# Instalar dependencias
COPY package*.json ./
RUN npm ci

# Copiar archivos de configuración y código fuente
COPY angular.json tsconfig*.json ./
COPY public ./public
COPY src ./src

# Compilar para producción
RUN npm run build -- --configuration=production

# ==========================================
# Etapa 2: Servidor Web Nginx
# ==========================================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Limpiar archivos estáticos por defecto
RUN rm -rf ./*

# Configuración personalizada de Nginx con soporte SPA y proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar artefactos compilados de Angular
COPY --from=build /app/dist/aulasmart-frontend/browser ./

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

