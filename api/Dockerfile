# Usamos Java 25 para estar a la vanguardia, temurin es mas estable que openjdk
FROM eclipse-temurin:25-jdk-alpine

# Establecemos el directorio de trabajo dentro del contenedor
VOLUME /tmp

# Argumentos para optimizar la memoria si es necesario
ARG JAVA_OPTS
ENV JAVA_OPTS=$JAVA_OPTS

# Copiamos el JAR (usamos el comodín * para no sufrir con el nombre exacto)
COPY target/*.jar app.jar

# Exponemos el puerto 8080 para que Spring Boot pueda recibir tráfico
EXPOSE 8080

# Comando optimizado para el arranque
#Ese ENTRYPOINT que tiene el -Djava.security.egd=file:/dev/./urandom es un truco de viejos lobos de mar. 
#Sirve para que Java genere números aleatorios más rápido, lo que hace que Spring Boot arranque en la mitad de tiempo. 
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]