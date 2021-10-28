# familie-ba-migrering


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