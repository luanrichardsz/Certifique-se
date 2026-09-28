# Cria o build 
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copia primeiro os arquivos de dependência
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Agora copia o código e faz o build
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# Executa a aplicação
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
