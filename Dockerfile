FROM navikt/java:16-appdynamics

COPY ./target/familie-ba-migrering.jar "app.jar"
