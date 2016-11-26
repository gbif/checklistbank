#!make

DB=tmp_clbddl
USER=postgres

schema: init-db dump-db

init-db:
	@echo "Initialize new $(DB) db with liquibase. \nIf you continue this will wipe a local db $(DB) if existing already.\n"
	@psql -h localhost -U $(USER) -c "DROP DATABASE IF EXISTS $(DB);"; \
	psql -h localhost -U $(USER) -c "CREATE DATABASE $(DB) ENCODING 'UTF-8';"; \
	while [ -z "$$PGPASSWORD" ]; do \
		read -r -p "Password for user $(USER) : " PGPASSWORD; \
	done; \
	mvn -Dchecklistbank.db.host=localhost \
		-Dchecklistbank.db.name=$(DB) \
		-Dchecklistbank.db.username=$(USER) \
		-Dchecklistbank.db.password=$$PGPASSWORD \
		liquibase:update;

dump-db:
	@echo "Dump $(DB) to sql ..."
	@pg_dump -h localhost -U $(USER) -d $(DB) --encoding=UTF8 --exclude-table=databasechange* > ../docs/schema.sql
	@psql -h localhost -U $(USER) -c "DROP DATABASE IF EXISTS $(DB);"; \
