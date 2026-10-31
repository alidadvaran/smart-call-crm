FROM node:20-alpine

WORKDIR /app

# Install production dependencies first (better layer caching).
COPY package.json ./
RUN npm install --omit=dev

# App source.
COPY src ./src
COPY migrations ./migrations

ENV NODE_ENV=production
EXPOSE 8080

# Run migrations (idempotent) then start the server.
CMD ["sh", "-c", "node src/migrate.js && node src/server.js"]
