FROM openjdk
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/compose-postgres
ENV SPRING_DATASOURCE_USERNAME=compose-postgres
ENV SPRING_DATASOURCE_PASSWORD=compose-postgres
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=update
COPY ./target/clisthenes*.jar application.jar
ENTRYPOINT ["java", "-jar", "application.jar"]