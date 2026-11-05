# ==========================================
# Etapa 1: Compilación con Maven y Java 17
# ==========================================
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

# Copiar descriptor pom.xml y descargar dependencias para aprovechar caché
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copiar el código fuente y empaquetar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================
# Etapa 2: Imagen ligera de ejecución JRE
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear usuario y grupo sin privilegios de administrador por seguridad
RUN addgroup -S aulasmart && adduser -S aulasmart -G aulasmart
USER aulasmart

# Copiar el JAR generado desde la etapa de compilación
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

