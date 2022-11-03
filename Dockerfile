FROM ghcr.io/navikt/baseimages/temurin:17

COPY ./target/familie-ba-migrering.jar "app.jar"
