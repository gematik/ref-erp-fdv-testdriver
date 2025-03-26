# syntax=docker/dockerfile:1

# Comments are provided throughout this file to help you get started.
# If you need more help, visit the Dockerfile reference guide at
# https://docs.docker.com/go/dockerfile-reference/

# Want to help us make this template better? Share your feedback here: https://forms.gle/ybq9Krt8jtBL3iCk7

################################################################################



################################################################################

# Create a new stage for running the application that contains the minimal
# runtime dependencies for the application. This often uses a different base
# image from the install or build stage where the necessary files are copied
# from the install stage.
#
# The example below uses eclipse-turmin's JRE image as the foundation for running the app.
# By specifying the "17-jre-jammy" tag, it will also use whatever happens to be the
# most recent version of that tag when you build your Dockerfile.
# If reproducibility is important, consider using a specific digest SHA, like
# eclipse-temurin@sha256:99cede493dfd88720b610eb8077c8688d3cca50003d76d1d539b0efc8cca72b4.
FROM eclipse-temurin:17-jre-jammy

# Create a non-privileged user that the app will run under.
# See https://docs.docker.com/go/dockerfile-user-best-practices/
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser
USER appuser

# Copy the executable from the "package" stage.
#COPY --from=package build/target/app.jar app.jar
#COPY ./target/ app.jar
COPY target/tdi-server-jar-with-dependencies.jar/ app.jar
COPY src/main/webapp webapp/
COPY src/main/java/de/gematik/test/erezept/remotefdv/server/config/config.yaml app/config.yaml
COPY src/main/java/de/gematik/test/erezept/remotefdv/server/config/secret.ppcs app/secret.ppcs

ENV WEBAPP=webapp
ENV CONFIG_PATH=app/config.yaml

EXPOSE 443

ENTRYPOINT [ "java","-Djdk.tls.namedGroups='secp256r1,secp384r1,ffdhe2048,ffdhe3072'", "-jar", "app.jar" ]
