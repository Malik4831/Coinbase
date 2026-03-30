const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
let unauthorizedHandler = null;

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = typeof handler === "function" ? handler : null;
}

function buildUrl(path, params) {
  const url = new URL(`${API_BASE_URL}${path}`, window.location.origin);

  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (Array.isArray(value)) {
        value.forEach((entry) => {
          if (entry !== undefined && entry !== null && entry !== "") {
            url.searchParams.append(key, entry);
          }
        });
      } else if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, value);
      }
    });
  }

  return API_BASE_URL ? url.toString() : `${path}${url.search}`;
}

async function request(path, options = {}) {
  const { method = "GET", params, body, token } = options;
  const headers = {};

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let payload;
  if (body) {
    payload = new URLSearchParams();
    Object.entries(body).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        payload.append(key, value);
      }
    });
    headers["Content-Type"] = "application/x-www-form-urlencoded";
  }

  const response = await fetch(buildUrl(path, params), {
    method,
    headers,
    body: payload,
  });

  const text = await response.text();
  let data = null;

  if (text) {
    try {
      data = JSON.parse(text);
    } catch (_error) {
      data = text;
    }
  }

  if (!response.ok) {
    const message =
      (typeof data === "object" && data?.error) ||
      (typeof data === "object" && data?.message) ||
      data ||
      `Request failed with status ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    if (typeof data === "object" && data !== null) {
      error.details = data.details || data;
    }
    if ((response.status === 401 || response.status === 403) && unauthorizedHandler) {
      unauthorizedHandler(error);
    }
    throw error;
  }

  return data;
}

export const api = {
  register: (payload) =>
    request("/api/auth/register", { method: "POST", body: payload }),
  login: (payload) => request("/api/auth/login", { method: "POST", body: payload }),

  getWallets: (token) =>
    request("/api/wallets/me", { token }),
  createWallet: (currency, token) =>
    request("/api/wallets/me", {
      method: "POST",
      body: { currency },
      token,
    }),
  getDepositContext: (currency, token) =>
    request("/api/wallets/deposit-context", {
      params: { currency },
      token,
    }),
  mintToken: (payload, token) =>
    request("/api/wallets/mint-token", { method: "POST", body: payload, token }),
  deposit: (payload, token) =>
    request("/api/wallets/deposit", { method: "POST", body: payload, token }),
  deploy: (payload, token) =>
    request("/api/wallets/deploy", { method: "POST", body: payload, token }),
  withdraw: (payload, token) =>
    request("/api/wallets/withdraw", { method: "POST", body: payload, token }),

  createOrder: (payload, token) =>
    request("/api/orders/create", { method: "POST", body: payload, token }),
  createAndExecuteOrder: (payload, token) =>
    request("/api/orders/create-and-execute", {
      method: "POST",
      body: payload,
      token,
    }),
  executeOrder: (orderId, token) =>
    request(`/api/orders/${orderId}/execute`, { method: "POST", token }),
  cancelOrder: (orderId, token) =>
    request(`/api/orders/${orderId}/cancel`, { method: "POST", token }),
  getUserOrders: (token) =>
    request("/api/orders/getUserOrders", { token }),
  getOrder: (orderId, token) =>
    request(`/api/orders/${orderId}`, { token }),
  getTokenName: (contractAddress, token) =>
    request("/api/orders/getTokenBalance", {
      params: { contractAddress },
      token,
    }),
  getTokenSymbol: (contractAddress, token) =>
    request("/api/orders/getTokenSymbol", {
      params: { contractAddress },
      token,
    }),

  addToken: (payload, token) =>
    request("/api/admin/tokens", { method: "POST", body: payload, token }),
  updateToken: (tokenId, payload, token) =>
    request(`/api/admin/tokens/${tokenId}`, {
      method: "PUT",
      body: payload,
      token,
    }),
  activateToken: (tokenId, token) =>
    request(`/api/admin/tokens/${tokenId}/activate`, {
      method: "PUT",
      token,
    }),
  deactivateToken: (tokenId, token) =>
    request(`/api/admin/tokens/${tokenId}/deactivate`, {
      method: "PUT",
      token,
    }),
  getAllTokens: (token) => request("/api/admin/tokens", { token }),
  getActiveTokens: (token) => request("/api/admin/tokens/active", { token }),
  getTokenBySymbol: (symbol, token) =>
    request(`/api/admin/tokens/${symbol}`, { token }),
  getTokenAddresses: (token) =>
    request("/api/admin/tokens/addresses", { token }),
  deleteToken: (tokenId, token) =>
    request(`/api/admin/tokens/${tokenId}`, { method: "DELETE", token }),
  refreshTokenCache: (token) =>
    request("/api/admin/tokens/refresh-cache", { method: "POST", token }),

  getPlatformInventory: (token) =>
    request("/api/admin/platform-inventory", { token }),
  createPlatformWallet: (currency, token) =>
    request("/api/admin/platform-inventory/wallets", {
      method: "POST",
      body: { currency },
      token,
    }),
  seedPlatformInventory: (payload, token) =>
    request("/api/admin/platform-inventory/seed", {
      method: "POST",
      body: payload,
      token,
    }),
  bootstrapPlatformWallets: (currencies, token) =>
    request("/api/admin/platform-inventory/bootstrap", {
      method: "POST",
      params: { currencies },
      token,
    }),
};
