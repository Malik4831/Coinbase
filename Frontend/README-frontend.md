# React frontend for CoinbaseClone

This frontend lives at the repository root and targets the Spring Boot backend in `CoinbaseClone/`.

## Coverage

- `AuthController`
- `WalletController`
- `OrderController`
- `TokenController`
- `PlatformInventoryController`

Every endpoint in those controllers is represented in the UI.

## Run

1. Start the Spring backend on `http://localhost:8080`.
2. Install dependencies from the repo root.
3. Run the Vite dev server.

```bash
npm install
npm run dev
```

The Vite dev server proxies `/api/*` to `http://localhost:8080`.

## Backend constraints reflected in the UI

- Protected endpoints need `Authorization: Bearer <token>`.
- `WalletController` requires `userId` in the path.
- The backend does not expose a current-user endpoint, so the frontend stores the ID returned by registration and also allows manual entry.
- `/api/orders/getTokenBalance` currently returns token name in the backend implementation. The frontend follows that implemented contract.
