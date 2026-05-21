SHELL := /bin/zsh

.PHONY: check-env setup-web-env web-install web-dev web-build backend-dev backend-build app-get app-run app-build-apk all-build

check-env:
	@echo "Node: $$(node -v 2>/dev/null || echo 'missing')"
	@echo "npm: $$(npm -v 2>/dev/null || echo 'missing')"
	@echo "Java: $$(java -version 2>&1 | head -n 1 || echo 'missing')"
	@echo "Maven: $$(mvn -v 2>/dev/null | head -n 1 || echo 'missing')"
	@echo "Flutter: $$(flutter --version 2>/dev/null | head -n 1 || echo 'missing')"

setup-web-env:
	@test -f library-admin-dashboard/.env || cp library-admin-dashboard/.env.example library-admin-dashboard/.env

web-install:
	npm --prefix library-admin-dashboard install

web-dev: setup-web-env
	npm --prefix library-admin-dashboard run dev

web-build: setup-web-env
	npm --prefix library-admin-dashboard run build

backend-dev:
	mvn -f library-admin-backend/pom.xml spring-boot:run

backend-build:
	mvn -f library-admin-backend/pom.xml clean package

app-get:
	cd app && flutter pub get

app-run:
	cd app && flutter run

app-build-apk:
	cd app && flutter build apk

all-build: backend-build web-build app-build-apk
