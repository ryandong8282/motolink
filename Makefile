.PHONY: help infra-up infra-down backend admin mobile-init test

help:
	@echo "MotoLink MVP commands"
	@echo "  make infra-up     Start PostGIS and Redis"
	@echo "  make backend      Run the Spring Boot API"
	@echo "  make admin        Run the Vue admin console"
	@echo "  make mobile-init  Generate Flutter iOS/Android hosts"
	@echo "  make test         Run backend, admin and Flutter checks"

infra-up:
	docker compose up -d

infra-down:
	docker compose down

backend:
	cd backend && ./mvnw spring-boot:run

admin:
	cd admin && npm install && npm run dev

mobile-init:
	cd mobile && flutter create --platforms=android,ios --org com.motolink --project-name motolink_mobile .

test:
	cd backend && ./mvnw test
	cd admin && npm install && npm run build
	cd mobile && flutter test
