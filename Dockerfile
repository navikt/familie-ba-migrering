FROM navikt/java:17-appdynamics

COPY ./target/familie-ba-migrering.jar "app.jar"
