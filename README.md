# familie-ba-migrering

## Kjøring lokalt
For å kjøre opp appen lokalt kan en kjøre `DevLauncher` med miljø-variabel AZURE_APP_CLIENT_ID satt til
ID'en returnert fra:
```
gcloud auth login # hvis utlogget
kubectl get azureapp familie-ba-migrering-lokal --context dev-gcp --namespace teamfamilie # krever naisdevice tilkobling
```
Appen tilgjengeliggjøres da på `localhost:8098`.

### Database
Dersom man vil kjøre med postgres, kan man bytte til Spring-profilen `postgres`.
Da må man sette opp postgres-databasen, dette gjøres slik:
```
docker run --name familie-ba-migrering-postgres -e POSTGRES_PASSWORD=test -d -p 5432:5432 postgres
docker ps (finn container id)
docker exec -it <container_id> bash
winpty docker exec -it <container_id> bash(fra git-bash windows)
psql -U postgres
CREATE DATABASE "familie-ba-migrering";
\l (til å verifisere om databasen er opprettet)
```
### Swagger
http://localhost:8098/swagger-ui.html
