.PHONY: up down logs api-test admin-build mobile-test

up:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f api

api-test:
	cd services/api && mvn test

admin-build:
	cd apps/admin && npm install && npm run build

mobile-test:
	cd apps/mobile && flutter pub get && flutter analyze && flutter test
