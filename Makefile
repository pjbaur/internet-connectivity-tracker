# ================================
# Internet Connectivity Tracker
# Docker Workflow Makefile
# ================================

# Default compose file
COMPOSE_FILE=docker-compose.yml

# Convenience wrapper for compose commands
DC=docker compose -f $(COMPOSE_FILE)

.PHONY: up down logs rebuild restart clean images ps

# Start the entire stack in the background
up:
	$(DC) up -d

# Stop and remove containers
down:
	$(DC) down

# Follow logs for all services
logs:
	$(DC) logs -f

# Rebuild the app image and restart only the app container
# (useful during development)
rebuild:
	$(DC) build app
	$(DC) up -d app

# Restart all services
restart:
	$(DC) down
	$(DC) up -d

# Remove all containers, images, volumes (DANGEROUS)
clean:
	$(DC) down --volumes --rmi all

# Show running containers
ps:
	$(DC) ps

# Show image list
images:
	$(DC) images
