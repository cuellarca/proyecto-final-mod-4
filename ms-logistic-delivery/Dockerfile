# syntax=docker/dockerfile:1.7

# =============================================================================
# 1) build — compila el multi-modulo y extrae las capas
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY pom.xml .
COPY domain/pom.xml          domain/pom.xml
COPY application/pom.xml     application/pom.xml
COPY infrastructure/pom.xml  infrastructure/pom.xml
COPY bootstrap/pom.xml       bootstrap/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline -Dmaven.test.skip=true || true

COPY domain          domain
COPY application     application
COPY infrastructure  infrastructure
COPY bootstrap       bootstrap

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp clean package -Dmaven.test.skip=true

RUN cp bootstrap/target/ms-logistic-delivery-exec.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --destination /extracted


# =============================================================================
# 2) jre — runtime Java a medida con jlink
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS jre

RUN jlink \
      --add-modules \
java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,\
java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,\
java.sql,java.sql.rowset,jdk.crypto.ec,jdk.jfr,jdk.localedata,jdk.management,\
jdk.unsupported,jdk.zipfs \
      --include-locales=en,es \
      --exclude-files='glob:/java.desktop/lib/*' \
      --vm=server \
      --strip-debug --no-man-pages --no-header-files --compress=zip-9 \
      --output /javaruntime


# =============================================================================
# 3) imagen final — Alpine + JRE recortado + aplicacion en capas
# =============================================================================
FROM alpine:3.21

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

RUN apk add --no-cache libstdc++ \
    && addgroup -S spring && adduser -S spring -G spring

COPY --from=jre /javaruntime $JAVA_HOME

WORKDIR /app

COPY --from=build --chown=spring:spring /extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /extracted/application/ ./

USER spring

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
