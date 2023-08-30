# familie-ba-migrering

Arkivert og slettet app siden automatisk migrering nå er skrudd av

## Kjøring lokalt
For oppsett av utviklingsmiljø, så kjør
```
docker-compose up
```
Denne starter mock-oauth-server, postgres på port 5439 og wiremock-server på port 8300

Wiremock-serveren kan brukes til å simulere ba-sak og ba-infotrygd. Hvis man kjører main-metoden i no.nav.familie.ba.migrering.testdata.GenererTestdataWiremock.kt så genereres det et sett med testdata for wiremock

Appen tilgjengeliggjøres da på `localhost:8098`.

### Database
```
docker run --name familie-ba-migrering-postgres -e POSTGRES_USER=familie -e POSTGRES_PASSWORD=familie-pwd -d -p 5439:5432 postgres
docker ps (finn container id)
docker exec -it <container_id> bash
winpty docker exec -it <container_id> bash(fra git-bash windows)
psql -U familie
CREATE DATABASE "familiebamigrering";
\l (til å verifisere om databasen er opprettet)
```
### Swagger
http://localhost:8098/swagger-ui.html
