# Render Deployment

This repo is configured for a Render blueprint deploy:

- `coinbaseclone-db`: managed PostgreSQL
- `coinbaseclone-api`: Spring Boot API built from [`CoinbaseClone/Dockerfile`](/C:/Users/Abdul%20Malik/OneDrive/Documents/Image-Line/FL%20Studio/Projects/campus/CoinbaseClone/CoinbaseClone/Dockerfile)
- `coinbaseclone-frontend`: static Vite/React site

## Before You Deploy

Set these Render environment values for the API service:

- `WEB3J_PROVIDER_URL`
- `WALLET_PRIVATE_KEY`
- `WALLET_MASTER_MNEMONIC`
- `WALLET_MASTER_PASSWORD` if you use one

Set this Render environment value for the frontend service after the API service gets its real Render URL:

- `VITE_API_BASE_URL=https://<your-api-service>.onrender.com`

## Deploy Steps

1. Push this repo to GitHub.
2. In Render, create a new Blueprint and point it at the repo root.
3. Render will read [`render.yaml`](/C:/Users/Abdul%20Malik/OneDrive/Documents/Image-Line/FL%20Studio/Projects/campus/CoinbaseClone/render.yaml) and provision the database, API service, and frontend.
4. After the API service is created, copy its public URL into the frontend service as `VITE_API_BASE_URL`.
5. Redeploy the frontend service so it rebuilds with the correct API base URL.

## Notes

- The backend now expects PostgreSQL via `SPRING_DATASOURCE_*`.
- `server.port` is bound to Render's `PORT` environment variable.
- JWT secret is generated automatically by Render from the blueprint.
- Frontend route rewrites are configured so `/app` and other client routes resolve to `index.html`.

Render docs used for alignment: https://render.com/docs/
