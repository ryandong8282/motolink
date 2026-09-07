.PHONY: api-run api-test infra-up infra-down

api-run:
	cd services/api && mvn spring-boot:run

api-test:
	cd services/api && mvn verify

infra-up:
	docker compose -f infra/docker-compose.yml up -d

infra-down:
	docker compose -f infra/docker-compose.yml down
