.PHONY: setup build ingest backend frontend eval-smoke eval-full demo clean

# Load .env.local into shell environment for all targets
ifneq (,$(wildcard ./.env.local))
    include .env.local
    export
endif

setup:
	brew services start postgresql@17 && brew services start redis

build:
	mvn clean install -DskipTests

ingest:
	cd server && mvn spring-boot:run -Dspring-boot.run.arguments=--ingest

backend:
	cd server && mvn spring-boot:run -Dspring-boot.run.profiles=local

frontend:
	cd ui && npm install && npm run dev

eval-smoke:
	cd eval && mvn verify -Peval-smoke

eval-full:
	cd eval && mvn verify -Peval-full

demo: setup build ingest
	$(MAKE) backend &
	$(MAKE) frontend

clean:
	mvn clean
