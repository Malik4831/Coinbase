import { useEffect, useMemo, useRef, useState } from "react";
import { Link, Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { api, setUnauthorizedHandler } from "./api";
import { decodeJwt, encodeApproveData, formatValue, getErrorMessage, isJwtExpired, toTokenBaseUnits } from "./utils";

const storageKey = "coinbaseclone.auth";

const COINGECKO_IDS = {
  BTC: "bitcoin",
  ETH: "ethereum",
  USDT: "tether",
  USDC: "usd-coin",
  BNB: "binancecoin",
  SOL: "solana",
  XRP: "ripple",
  ADA: "cardano",
  DOGE: "dogecoin",
  DAI: "dai",
};

function loadAuthState() {
  const raw = localStorage.getItem(storageKey);
  if (!raw) return { token: "", registeredUser: null, rememberedUserId: "" };
  try {
    return JSON.parse(raw);
  } catch (_error) {
    return { token: "", registeredUser: null, rememberedUserId: "" };
  }
}

function persistAuthState(state) {
  localStorage.setItem(storageKey, JSON.stringify(state));
}

function clearAuthState(state) {
  const next = { ...state, token: "" };
  persistAuthState(next);
  return next;
}

function extractAuthToken(payload) {
  if (!payload || typeof payload !== "object") {
    return "";
  }

  const directToken = payload.token || payload.accessToken || payload.jwt || payload.idToken;
  if (typeof directToken === "string" && directToken.trim()) {
    return directToken;
  }

  const nestedContainers = [
    payload.data,
    payload.result,
    payload.payload,
    payload.auth,
    payload.authentication,
    payload.user,
  ];

  for (const container of nestedContainers) {
    const nestedToken = extractAuthToken(container);
    if (nestedToken) {
      return nestedToken;
    }
  }

  return "";
}

function extractRegisteredUser(payload) {
  if (!payload || typeof payload !== "object") {
    return null;
  }

  if (payload.id != null) {
    return payload;
  }

  const nestedContainers = [payload.user, payload.data, payload.result, payload.payload];
  for (const container of nestedContainers) {
    const nestedUser = extractRegisteredUser(container);
    if (nestedUser) {
      return nestedUser;
    }
  }

  return null;
}

function normalizeWalletRows(walletRows) {
  if (!Array.isArray(walletRows)) {
    return [];
  }

  const deduped = new Map();
  walletRows.forEach((wallet) => {
    const key = [
      wallet?.currency?.toUpperCase() || "",
      wallet?.blockchainAddress || "",
    ].join("|");
    const existing = deduped.get(key);

    if (!existing || Number(wallet?.id ?? 0) > Number(existing?.id ?? 0)) {
      deduped.set(key, wallet);
    }
  });

  return Array.from(deduped.values());
}

function SectionCard({ eyebrow, title, actions, children }) {
  return (
    <section className="card">
      <div className="card-header">
        <div>
          <p className="eyebrow">{eyebrow}</p>
          <h2>{title}</h2>
        </div>
        {actions ? <div className="card-actions">{actions}</div> : null}
      </div>
      {children}
    </section>
  );
}

function Field({ label, hint, children }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint ? <small>{hint}</small> : null}
    </label>
  );
}

function StatusBanner({ tone, children }) {
  if (!children) return null;
  return <div className={`banner banner-${tone}`}>{children}</div>;
}

function TokenContractPicker({ tokens, value, onSelect, onOpen, disabled }) {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState("");
  const rootRef = useRef(null);

  useEffect(() => {
    function handlePointerDown(event) {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }

    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, []);

  useEffect(() => {
    setQuery(value || "");
  }, [value]);

  const filteredTokens = useMemo(() => {
    const normalizedQuery = query.trim().toUpperCase();
    if (!normalizedQuery) return tokens;
    return tokens.filter((token) => {
      const symbol = token.symbol?.toUpperCase() || "";
      const address = token.contractAddress?.toUpperCase() || "";
      return symbol.includes(normalizedQuery) || address.includes(normalizedQuery);
    });
  }, [query, tokens]);

  function openPicker() {
    setIsOpen(true);
    if (onOpen) onOpen();
  }

  function handleSelect(token) {
    onSelect(token);
    setQuery(token.contractAddress || "");
    setIsOpen(false);
  }

  return (
    <div className="token-picker" ref={rootRef}>
      <input
        value={query}
        placeholder={disabled ? "No active tokens available" : "Click to select a token contract"}
        onFocus={openPicker}
        onClick={openPicker}
        onChange={(e) => {
          setQuery(e.target.value);
          setIsOpen(true);
          if (onOpen) onOpen();
        }}
        disabled={disabled}
      />
      {isOpen && !disabled ? (
        <div className="token-picker-menu">
          {filteredTokens.length ? (
            filteredTokens.map((token) => (
              <button
                className="token-picker-option"
                key={token.id ?? token.contractAddress}
                onClick={() => handleSelect(token)}
                type="button"
              >
                <strong>{token.symbol}</strong>
                <span>{token.contractAddress}</span>
              </button>
            ))
          ) : (
            <div className="token-picker-empty">No matching tokens found.</div>
          )}
        </div>
      ) : null}
    </div>
  );
}

function CurrencyPicker({ options, value, onSelect, disabled, placeholder = "Click to select a currency" }) {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState("");
  const rootRef = useRef(null);

  useEffect(() => {
    function handlePointerDown(event) {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }

    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, []);

  useEffect(() => {
    setQuery(value || "");
  }, [value]);

  const filteredOptions = useMemo(() => {
    const normalizedQuery = query.trim().toUpperCase();
    if (!normalizedQuery) return options;
    return options.filter((option) => option.includes(normalizedQuery));
  }, [options, query]);

  return (
    <div className="token-picker" ref={rootRef}>
      <input
        value={query}
        placeholder={disabled ? "No supported currencies available" : placeholder}
        onFocus={() => setIsOpen(true)}
        onClick={() => setIsOpen(true)}
        onChange={(e) => {
          setQuery(e.target.value.toUpperCase());
          setIsOpen(true);
        }}
        disabled={disabled}
      />
      {isOpen && !disabled ? (
        <div className="token-picker-menu">
          {filteredOptions.length ? (
            filteredOptions.map((option) => (
              <button
                className="token-picker-option"
                key={option}
                onClick={() => {
                  onSelect(option);
                  setQuery(option);
                  setIsOpen(false);
                }}
                type="button"
              >
                <strong>{option}</strong>
              </button>
            ))
          ) : (
            <div className="token-picker-empty">No matching currencies found.</div>
          )}
        </div>
      ) : null}
    </div>
  );
}

function DataTable({ columns, rows, emptyText }) {
  if (!rows?.length) return <p className="empty-state">{emptyText || "No data yet."}</p>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c.key}>{c.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={row.id ?? index}>
              {columns.map((c) => (
                <td key={c.key}>{formatValue(row[c.key])}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function LineChart({ values }) {
  if (!values?.length) return <div className="chart-empty">No data</div>;
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const width = 220;
  const height = 90;
  const points = values
    .map((v, i) => {
      const x = (i / Math.max(values.length - 1, 1)) * width;
      const y = height - ((v - min) / range) * height;
      return `${x},${y}`;
    })
    .join(" ");
  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="line-chart" preserveAspectRatio="none">
      <polyline points={points} />
    </svg>
  );
}

function ProtectedRoute({ token, children }) {
  return token && !isJwtExpired(token) ? children : <Navigate to="/auth" replace />;
}

function AuthPage({ authState, setAuthState }) {
  const navigate = useNavigate();
  const [register, setRegister] = useState({ email: "", password: "", firstName: "", lastName: "" });
  const [login, setLogin] = useState({ email: "", password: "" });
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");

  if (authState.token && !isJwtExpired(authState.token)) {
    return <Navigate to="/app" replace />;
  }

  async function onRegister(event) {
    event.preventDefault();
    setError("");
    setStatus("");
    try {
      const response = await api.register(register);
      const user = extractRegisteredUser(response);
      if (!user) {
        throw new Error("Registration succeeded but the API did not return a valid user payload.");
      }
      const issuedToken = extractAuthToken(response);
      const next = {
        ...authState,
        token: issuedToken || authState.token,
        registeredUser: user,
        rememberedUserId: String(user.id ?? ""),
      };
      setAuthState(next);
      persistAuthState(next);
      setStatus(issuedToken ? `Registered user ${user.id} and signed in.` : `Registered user ${user.id}.`);
      setRegister({ email: "", password: "", firstName: "", lastName: "" });
      if (issuedToken) {
        navigate("/app", { replace: true });
      }
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  async function onLogin(event) {
    event.preventDefault();
    setError("");
    setStatus("");
    try {
      const data = await api.login(login);
      const token = extractAuthToken(data);
      if (!token) {
        throw new Error("Login succeeded but the API did not return a valid token payload.");
      }
      const next = { ...authState, token };
      setAuthState(next);
      persistAuthState(next);
      setStatus("Login successful.");
      setLogin({ email: "", password: "" });
      navigate("/app", { replace: true });
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }

  const payload = useMemo(() => decodeJwt(authState.token), [authState.token]);

  return (
    <div className="page-grid auth-grid">
      <SectionCard eyebrow="AuthController" title="Register">
        <StatusBanner tone="success">{status}</StatusBanner>
        <StatusBanner tone="danger">{error}</StatusBanner>
        <form className="form-grid" onSubmit={onRegister}>
          <Field label="Email"><input required type="email" value={register.email} onChange={(e) => setRegister((s) => ({ ...s, email: e.target.value }))} /></Field>
          <Field label="Password"><input required type="password" value={register.password} onChange={(e) => setRegister((s) => ({ ...s, password: e.target.value }))} /></Field>
          <Field label="First name"><input required value={register.firstName} onChange={(e) => setRegister((s) => ({ ...s, firstName: e.target.value }))} /></Field>
          <Field label="Last name"><input required value={register.lastName} onChange={(e) => setRegister((s) => ({ ...s, lastName: e.target.value }))} /></Field>
          <button className="primary-button" type="submit">Create account</button>
        </form>
      </SectionCard>

      <SectionCard eyebrow="AuthController" title="Login">
        <form className="form-grid" onSubmit={onLogin}>
          <Field label="Email"><input required type="email" value={login.email} onChange={(e) => setLogin((s) => ({ ...s, email: e.target.value }))} /></Field>
          <Field label="Password"><input required type="password" value={login.password} onChange={(e) => setLogin((s) => ({ ...s, password: e.target.value }))} /></Field>
          <button className="primary-button" type="submit">Sign in</button>
        </form>
        <div className="meta-panel">
          <dl>
            <div><dt>JWT subject</dt><dd>{payload?.sub || "Not signed in"}</dd></div>
            <div><dt>User id</dt><dd>{authState.registeredUser?.id ?? "N/A"}</dd></div>
            <div><dt>Wallet</dt><dd>{authState.registeredUser?.walletAddress || "N/A"}</dd></div>
          </dl>
        </div>
      </SectionCard>
    </div>
  );
}

function DashboardPage({ authState, setAuthState }) {
  const navigate = useNavigate();
  const payload = useMemo(() => decodeJwt(authState.token), [authState.token]);
  const isAdmin = payload?.role === "ADMIN";
  const [walletForm, setWalletForm] = useState({ currency: "" });
  const [wallets, setWallets] = useState([]);
  const [walletStatus, setWalletStatus] = useState("");
  const [walletError, setWalletError] = useState("");
  const [transfer, setTransfer] = useState({ currency: "", contractAddress: "", fromAddress: "", amount: "", decimals: "18" });
  const [depositContext, setDepositContext] = useState(null);
  const [mintForm, setMintForm] = useState({ currency: "", contractAddress: "", toAddress: "", amount: "", decimals: "18" });
  const [withdraw, setWithdraw] = useState({ currency: "", contractAddress: "", toAddress: "", amount: "", decimals: "18" });

  const [order, setOrder] = useState({ baseCurrency: "", quoteCurrency: "USD", amount: "", price: "", type: "BUY" });
  const [orders, setOrders] = useState([]);
  const [orderId, setOrderId] = useState("");
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [orderStatus, setOrderStatus] = useState("");
  const [orderError, setOrderError] = useState("");

  const [tokenAdd, setTokenAdd] = useState({ symbol: "", name: "", contractAddress: "", decimals: "18" });
  const [tokenUpdate, setTokenUpdate] = useState({ tokenId: "", name: "", contractAddress: "", decimals: "", isActive: "" });
  const [tokenQuery, setTokenQuery] = useState({ symbol: "", contractAddress: "" });
  const [allTokens, setAllTokens] = useState([]);
  const [activeTokens, setActiveTokens] = useState([]);
  const [tokenDetail, setTokenDetail] = useState(null);
  const [tokenAddresses, setTokenAddresses] = useState(null);
  const [tokenMeta, setTokenMeta] = useState({ name: "", symbol: "" });
  const [tokenStatus, setTokenStatus] = useState("");
  const [tokenError, setTokenError] = useState("");

  const [inventoryCreate, setInventoryCreate] = useState({ currency: "" });
  const [inventorySeed, setInventorySeed] = useState({ currency: "", amount: "" });
  const [inventoryBootstrap, setInventoryBootstrap] = useState({ currencies: "BTC,ETH,USD,USDT" });
  const [inventory, setInventory] = useState(null);
  const [inventoryStatus, setInventoryStatus] = useState("");
  const [inventoryError, setInventoryError] = useState("");
  const [inventoryBusyAction, setInventoryBusyAction] = useState("");

  const [chartData, setChartData] = useState({});
  const supportedCurrencies = useMemo(() => {
    const values = new Set(["USD", "ETH"]);
    activeTokens.forEach((token) => {
      if (token.symbol) values.add(token.symbol.toUpperCase());
    });
    wallets.forEach((wallet) => {
      if (wallet.currency) values.add(wallet.currency.toUpperCase());
    });
    return Array.from(values).sort();
  }, [activeTokens, wallets]);
  const selectedWallet = useMemo(() => {
    const preferredCurrency = transfer.currency?.trim().toUpperCase() || walletForm.currency?.trim().toUpperCase();
    if (preferredCurrency) {
      const matchedWallet = wallets.find((wallet) => wallet.currency?.toUpperCase() === preferredCurrency);
      if (matchedWallet) return matchedWallet;
    }
    return wallets[0] || null;
  }, [transfer.currency, walletForm.currency, wallets]);

  useEffect(() => {
    if (!authState.token) return;
    loadActiveTokens();
    loadWallets();
    if (isAdmin) {
      loadInventory();
    }
  }, [authState.token, isAdmin]);

  useEffect(() => {
    if (!activeTokens.length) return;
    loadCharts(activeTokens);
  }, [activeTokens]);

  async function loadWallets(event) {
    if (event) event.preventDefault();
    setWalletError("");
    setWalletStatus("");
    try {
      const data = await api.getWallets(authState.token);
      const normalizedWallets = normalizeWalletRows(data);
      setWallets(normalizedWallets);
      const duplicateCount = Array.isArray(data) ? data.length - normalizedWallets.length : 0;
      setWalletStatus(duplicateCount > 0 ? `Wallets loaded. Hidden ${duplicateCount} duplicate wallet entr${duplicateCount === 1 ? "y" : "ies"}.` : "Wallets loaded.");
    } catch (err) {
      setWalletError(getErrorMessage(err));
    }
  }

  async function createWallet() {
    setWalletError("");
    setWalletStatus("");
    try {
      await api.createWallet(walletForm.currency, authState.token);
      setWalletStatus("Wallet created.");
      setWalletForm({ currency: "" });
      await loadWallets();
    } catch (err) {
      setWalletError(getErrorMessage(err));
    }
  }

  async function connectMetaMask() {
    setWalletError("");
    setWalletStatus("");
    try {
      if (!window.ethereum) {
        throw new Error("MetaMask is not available in this browser");
      }

      const accounts = await window.ethereum.request({ method: "eth_requestAccounts" });
      const account = accounts?.[0];
      if (!account) {
        throw new Error("No MetaMask account returned");
      }

      setTransfer((s) => ({ ...s, fromAddress: account }));
      setWalletStatus(`Connected MetaMask account ${account}.`);
    } catch (err) {
      setWalletError(getErrorMessage(err));
    }
  }

  function resolveTransferContractAddress() {
    const directContractAddress = transfer.contractAddress.trim();
    if (directContractAddress) {
      return directContractAddress;
    }

    const normalizedCurrency = transfer.currency.trim().toUpperCase();
    const matchedToken = activeTokens.find((token) => token.symbol?.toUpperCase() === normalizedCurrency);
    return matchedToken?.contractAddress || "";
  }

  async function loadDepositContext() {
    const currency = transfer.currency.trim();
    if (!currency) {
      throw new Error("Currency is required before approval");
    }

    const data = await api.getDepositContext(currency, authState.token);
    console.info("Deposit context loaded", data);
    setDepositContext(data);
    return data;
  }

  async function runTransfer(mode) {
    setWalletError("");
    setWalletStatus("");
    try {
      if (mode === "deploy") {
        const response = await api.deploy(transfer, authState.token);
        setWalletStatus(response?.message || "Transfer sent.");
        setTransfer({ currency: "", contractAddress: "", fromAddress: "", amount: "", decimals: "18" });
        setDepositContext(null);
        await loadWallets();
        return;
      }

      if (!transfer.fromAddress.trim()) {
        throw new Error("Connect MetaMask before depositing");
      }

      if (!window.ethereum) {
        throw new Error("MetaMask is not available in this browser");
      }

      const contractAddress = resolveTransferContractAddress();
      if (!contractAddress) {
        throw new Error("Contract address is required to approve the deposit token");
      }

      const context = await loadDepositContext();
      const amountBaseUnits = toTokenBaseUnits(transfer.amount, transfer.decimals);
      const approvalData = encodeApproveData(context.spenderAddress, amountBaseUnits);
      const depositDebug = {
        mode,
        currency: transfer.currency.trim().toUpperCase(),
        contractAddress,
        fromAddress: transfer.fromAddress.trim(),
        toAddress: context.depositAddress,
        spenderAddress: context.spenderAddress,
        amount: transfer.amount,
        decimals: transfer.decimals,
        amountBaseUnits: amountBaseUnits.toString(),
      };

      console.info("Deposit approval prepared", depositDebug);

      setWalletStatus("Waiting for MetaMask approval...");
      const approvalTxHash = await window.ethereum.request({
        method: "eth_sendTransaction",
        params: [
          {
            from: transfer.fromAddress,
            to: contractAddress,
            data: approvalData,
          },
        ],
      });

      console.info("Deposit approval submitted", { approvalTxHash, ...depositDebug });
      setWalletStatus(`Approval submitted: ${approvalTxHash}. Finalizing deposit...`);
      const response = await api.deposit(
        {
          ...transfer,
          contractAddress,
          fromAddress: transfer.fromAddress,
        },
        authState.token
      );
      console.info("Deposit response", response);
      setWalletStatus(response?.message || "Transfer sent.");
      setTransfer({ currency: "", contractAddress: "", fromAddress: "", amount: "", decimals: "18" });
      setDepositContext(null);
      await loadWallets();
    } catch (err) {
      setWalletError(getErrorMessage(err));
    }
  }

  async function runWithdraw() {
    setWalletError("");
    setWalletStatus("");
    try {
      const response = await api.withdraw(withdraw, authState.token);
      setWalletStatus(response?.message || "Withdrawal sent.");
      setWithdraw({ currency: "", contractAddress: "", toAddress: "", amount: "", decimals: "18" });
      await loadWallets();
    } catch (err) {
      setWalletError(getErrorMessage(err));
    }
  }

  async function mintToken() {
    setWalletError("");
    setWalletStatus("");
    try {
      const response = await api.mintToken(mintForm, authState.token);
      setWalletStatus(response?.message || "Token minted.");
      setMintForm({ currency: "", contractAddress: "", toAddress: "", amount: "", decimals: "18" });
    } catch (err) {
      setWalletError(getErrorMessage(err));
    }
  }

  async function loadOrders() {
    setOrderError("");
    setOrderStatus("");
    try {
      const data = await api.getUserOrders(authState.token);
      setOrders(data?.orders || []);
      setOrderStatus(`Loaded ${data?.count ?? 0} orders.`);
    } catch (err) {
      setOrderError(getErrorMessage(err));
    }
  }

  async function submitOrder(mode) {
    setOrderError("");
    setOrderStatus("");
    try {
      const payload = { ...order, price: order.price || undefined };
      const data = mode === "create"
        ? await api.createOrder(payload, authState.token)
        : await api.createAndExecuteOrder(payload, authState.token);
      setOrderStatus(data?.message || "Order sent.");
      await loadOrders();
      setOrder({ baseCurrency: "", quoteCurrency: "USD", amount: "", price: "", type: "BUY" });
    } catch (err) {
      setOrderError(getErrorMessage(err));
    }
  }

  async function orderAction(type) {
    setOrderError("");
    setOrderStatus("");
    try {
      if (!orderId) throw new Error("Order ID is required");
      if (type === "lookup") {
        const data = await api.getOrder(orderId, authState.token);
        setSelectedOrder(data);
        setOrderStatus(`Loaded order ${orderId}.`);
      }
      if (type === "execute") {
        const data = await api.executeOrder(orderId, authState.token);
        setOrderStatus(data?.message || "Order executed.");
        await loadOrders();
      }
      if (type === "cancel") {
        const data = await api.cancelOrder(orderId, authState.token);
        setOrderStatus(data?.message || "Order cancelled.");
        await loadOrders();
      }
    } catch (err) {
      setOrderError(getErrorMessage(err));
    }
  }

  async function loadAllTokens() {
    const data = await api.getAllTokens(authState.token);
    setAllTokens(Array.isArray(data) ? data : []);
  }

  async function loadActiveTokens() {
    const data = await api.getActiveTokens(authState.token);
    setActiveTokens(Array.isArray(data) ? data : []);
  }

  async function ensureActiveTokensLoaded() {
    if (activeTokens.length) return;
    await loadActiveTokens();
  }

  async function loadAddresses() {
    const data = await api.getTokenAddresses(authState.token);
    setTokenAddresses(data);
  }

  async function tokenAction(type, event) {
    if (event) event.preventDefault();
    setTokenError("");
    setTokenStatus("");
    try {
      if (type === "add") {
        await api.addToken(tokenAdd, authState.token);
        setTokenStatus("Token added.");
        setTokenAdd({ symbol: "", name: "", contractAddress: "", decimals: "18" });
      }
      if (type === "update") {
        await api.updateToken(tokenUpdate.tokenId, {
          name: tokenUpdate.name || undefined,
          contractAddress: tokenUpdate.contractAddress || undefined,
          decimals: tokenUpdate.decimals || undefined,
          isActive: tokenUpdate.isActive === "" ? undefined : tokenUpdate.isActive === "true",
        }, authState.token);
        setTokenStatus("Token updated.");
      }
      if (type === "activate") await api.activateToken(tokenUpdate.tokenId, authState.token);
      if (type === "deactivate") await api.deactivateToken(tokenUpdate.tokenId, authState.token);
      if (type === "delete") await api.deleteToken(tokenUpdate.tokenId, authState.token);
      if (type === "detail") setTokenDetail(await api.getTokenBySymbol(tokenQuery.symbol, authState.token));
      if (type === "name") {
        const name = await api.getTokenName(tokenQuery.contractAddress, authState.token);
        setTokenMeta((s) => ({ ...s, name }));
      }
      if (type === "symbol") {
        const symbol = await api.getTokenSymbol(tokenQuery.contractAddress, authState.token);
        setTokenMeta((s) => ({ ...s, symbol }));
      }
      if (type === "refresh") await api.refreshTokenCache(authState.token);

      await Promise.all([loadAllTokens(), loadActiveTokens(), loadAddresses()]);
      if (["activate", "deactivate", "delete", "refresh"].includes(type)) setTokenStatus(`Token action ${type} completed.`);
    } catch (err) {
      setTokenError(getErrorMessage(err));
    }
  }

  async function loadInventory() {
    setInventoryError("");
    setInventoryStatus("");
    try {
      const data = await api.getPlatformInventory(authState.token);
      setInventory(data);
      setInventoryStatus("Platform inventory loaded.");
    } catch (err) {
      setInventoryError(getErrorMessage(err));
    }
  }

  async function inventoryAction(type, event) {
    if (event) event.preventDefault();
    setInventoryError("");
    setInventoryStatus("");
    setInventoryBusyAction(type);
    try {
      if (type === "create") {
        await api.createPlatformWallet(inventoryCreate.currency, authState.token);
        setInventoryCreate({ currency: "" });
      }
      if (type === "seed") {
        await api.seedPlatformInventory(inventorySeed, authState.token);
        setInventorySeed({ currency: "", amount: "" });
      }
      if (type === "bootstrap") {
        const currencies = inventoryBootstrap.currencies
          .split(",")
          .map((c) => c.trim().toUpperCase())
          .filter(Boolean);
        await api.bootstrapPlatformWallets(currencies, authState.token);
      }
      await loadInventory();
      setInventoryStatus("Platform inventory updated.");
    } catch (err) {
      setInventoryError(getErrorMessage(err));
    } finally {
      setInventoryBusyAction("");
    }
  }

  async function loadCharts(tokens) {
    const symbols = [...new Set(tokens.map((t) => t.symbol?.toUpperCase()).filter(Boolean))];
    const next = {};
    await Promise.all(symbols.map(async (symbol) => {
      const coinId = COINGECKO_IDS[symbol];
      if (!coinId) {
        next[symbol] = { values: [], status: "unsupported" };
        return;
      }
      try {
        const res = await fetch(`https://api.coingecko.com/api/v3/coins/${coinId}/market_chart?vs_currency=usd&days=7&interval=daily`);
        if (!res.ok) throw new Error("Market API request failed");
        const json = await res.json();
        const values = (json?.prices || []).map((p) => p[1]);
        if (!values.length) {
          next[symbol] = { values: [], status: "empty" };
          return;
        }
        const start = values[0];
        const last = values[values.length - 1];
        next[symbol] = { values, status: "ok", latest: last, changePct: ((last - start) / start) * 100 };
      } catch (_error) {
        next[symbol] = { values: [], status: "error" };
      }
    }));
    setChartData(next);
  }

  function logout() {
    const next = clearAuthState(authState);
    setAuthState(next);
    navigate("/auth", { replace: true });
  }

  return (
    <div className="page-grid">
      <div className="hero-panel">
        <div>
          <p className="eyebrow">Control Room</p>
          <h1>Exchange dashboard</h1>
        </div>
        <div className="hero-stack">
          <div className="stat-pill"><span>JWT subject</span><strong>{payload?.sub || "Unknown"}</strong></div>
          <div className="stat-pill"><span>Role</span><strong>{payload?.role || "USER"}</strong></div>
          <div className="stat-pill"><span>Wallets</span><strong>{wallets.length}</strong></div>
          <div className="stat-pill"><span>Wallet address</span><strong>{selectedWallet?.blockchainAddress || "Load wallets"}</strong></div>
          <button className="ghost-button" onClick={logout} type="button">Back to Auth</button>
        </div>
      </div>

      <SectionCard
        eyebrow="Market Dashboard"
        title="7-day token rise/fall"
        actions={<button className="secondary-button" onClick={() => loadCharts(activeTokens)} type="button">Refresh charts</button>}
      >
        <div className="charts-grid">
          {activeTokens.map((token) => {
            const symbol = token.symbol?.toUpperCase();
            const chart = chartData[symbol];
            return (
              <div className="chart-card" key={token.id ?? symbol}>
                <div className="chart-head">
                  <strong>{symbol}</strong>
                  <span>{token.name}</span>
                </div>
                <LineChart values={chart?.values || []} />
                <div className="chart-meta">
                  <span>{chart?.latest ? `$${Number(chart.latest).toFixed(2)}` : "N/A"}</span>
                  <span className={(chart?.changePct ?? 0) >= 0 ? "positive" : "negative"}>
                    {chart?.changePct !== undefined ? `${chart.changePct >= 0 ? "+" : ""}${chart.changePct.toFixed(2)}%` : "N/A"}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </SectionCard>

      <SectionCard eyebrow="WalletController" title="Wallets + mint/deposit/deploy/withdraw">
        <StatusBanner tone="success">{walletStatus}</StatusBanner>
        <StatusBanner tone="danger">{walletError}</StatusBanner>
        <form className="form-grid compact-grid" onSubmit={loadWallets}>
          <Field label="Currency for new wallet"><input value={walletForm.currency} onChange={(e) => setWalletForm((s) => ({ ...s, currency: e.target.value }))} /></Field>
          <div className="button-row">
            <button className="primary-button" type="submit">Refresh wallets</button>
            <button className="secondary-button" onClick={createWallet} type="button">Create wallet</button>
          </div>
        </form>
        <div className="meta-panel">
          <dl>
            <div><dt>Selected wallet currency</dt><dd>{selectedWallet?.currency || "Load wallets"}</dd></div>
            <div><dt>Your wallet address</dt><dd>{selectedWallet?.blockchainAddress || "Load wallets to view address"}</dd></div>
          </dl>
        </div>
        <div className="triple-layout">
          <form className="form-grid" onSubmit={(e) => e.preventDefault()}>
            <h3>Deposit / Deploy</h3>
            <Field label="Currency"><input value={transfer.currency} onChange={(e) => setTransfer((s) => ({ ...s, currency: e.target.value }))} /></Field>
            <Field label="Contract address" hint="Select from active tokens fetched from the database.">
              <TokenContractPicker
                tokens={activeTokens}
                value={transfer.contractAddress}
                onOpen={ensureActiveTokensLoaded}
                onSelect={(token) => setTransfer((s) => ({
                  ...s,
                  currency: token.symbol || s.currency,
                  contractAddress: token.contractAddress || "",
                  decimals: String(token.decimals ?? s.decimals),
                }))}
                disabled={!activeTokens.length && !authState.token}
              />
            </Field>
            <Field label="From address">
              <input value={transfer.fromAddress} onChange={(e) => setTransfer((s) => ({ ...s, fromAddress: e.target.value }))} />
            </Field>
            <Field label="Amount"><input type="number" step="any" min="0" value={transfer.amount} onChange={(e) => setTransfer((s) => ({ ...s, amount: e.target.value }))} /></Field>
            <Field label="Decimals"><input value={transfer.decimals} onChange={(e) => setTransfer((s) => ({ ...s, decimals: e.target.value }))} /></Field>
            <div className="meta-panel">
              <dl>
                <div><dt>Protocol spender</dt><dd>{depositContext?.spenderAddress || "Load via approve flow"}</dd></div>
                <div><dt>Your deposit address</dt><dd>{depositContext?.depositAddress || "Load via approve flow"}</dd></div>
                <div><dt>Resolved contract</dt><dd>{resolveTransferContractAddress() || "Missing"}</dd></div>
              </dl>
            </div>
            <div className="button-row">
              <button className="secondary-button" onClick={connectMetaMask} type="button">Connect MetaMask</button>
              <button className="primary-button" onClick={() => runTransfer("deposit")} type="button">Approve + Deposit</button>
              <button className="secondary-button" onClick={() => runTransfer("deploy")} type="button">Deploy</button>
            </div>
          </form>
          <form className="form-grid" onSubmit={(e) => e.preventDefault()}>
            <h3>Withdraw</h3>
            <Field label="Currency"><input value={withdraw.currency} onChange={(e) => setWithdraw((s) => ({ ...s, currency: e.target.value }))} /></Field>
            <Field label="Contract address" hint="Select from active tokens fetched from the database.">
              <TokenContractPicker
                tokens={activeTokens}
                value={withdraw.contractAddress}
                onOpen={ensureActiveTokensLoaded}
                onSelect={(token) => setWithdraw((s) => ({
                  ...s,
                  currency: token.symbol || s.currency,
                  contractAddress: token.contractAddress || "",
                  decimals: String(token.decimals ?? s.decimals),
                }))}
                disabled={!activeTokens.length && !authState.token}
              />
            </Field>
            <Field label="To address"><input value={withdraw.toAddress} onChange={(e) => setWithdraw((s) => ({ ...s, toAddress: e.target.value }))} /></Field>
            <Field label="Amount"><input type="number" step="any" min="0" value={withdraw.amount} onChange={(e) => setWithdraw((s) => ({ ...s, amount: e.target.value }))} /></Field>
            <Field label="Decimals"><input value={withdraw.decimals} onChange={(e) => setWithdraw((s) => ({ ...s, decimals: e.target.value }))} /></Field>
            <button className="primary-button" onClick={runWithdraw} type="button">Withdraw</button>
          </form>
          <form className="form-grid" onSubmit={(e) => e.preventDefault()}>
            <h3>Mint synthetic token</h3>
            <Field label="Currency"><input value={mintForm.currency} onChange={(e) => setMintForm((s) => ({ ...s, currency: e.target.value }))} /></Field>
            <Field label="Contract address" hint="Select the synthetic ERC-20 token contract to mint.">
              <TokenContractPicker
                tokens={activeTokens}
                value={mintForm.contractAddress}
                onOpen={ensureActiveTokensLoaded}
                onSelect={(token) => setMintForm((s) => ({
                  ...s,
                  currency: token.symbol || s.currency,
                  contractAddress: token.contractAddress || "",
                  decimals: String(token.decimals ?? s.decimals),
                }))}
                disabled={!activeTokens.length && !authState.token}
              />
            </Field>
            <Field label="To address"><input value={mintForm.toAddress} onChange={(e) => setMintForm((s) => ({ ...s, toAddress: e.target.value }))} /></Field>
            <Field label="Amount"><input type="number" step="any" min="0" value={mintForm.amount} onChange={(e) => setMintForm((s) => ({ ...s, amount: e.target.value }))} /></Field>
            <Field label="Decimals"><input value={mintForm.decimals} onChange={(e) => setMintForm((s) => ({ ...s, decimals: e.target.value }))} /></Field>
            <div className="button-row">
              <button className="secondary-button" onClick={() => setMintForm((s) => ({ ...s, toAddress: transfer.fromAddress || s.toAddress }))} type="button">Use from address</button>
              <button className="primary-button" onClick={mintToken} type="button">Mint token</button>
            </div>
          </form>
        </div>
        <DataTable
          columns={[{ key: "id", label: "ID" }, { key: "currency", label: "Currency" }, { key: "balance", label: "Balance" }, { key: "lockedBalance", label: "Locked" }, { key: "blockchainAddress", label: "Address" }]}
          rows={wallets}
          emptyText="Load wallets to inspect balances."
        />
      </SectionCard>

      {isAdmin ? (
        <SectionCard eyebrow="Platform Inventory" title="Bootstrap and seed">
          <StatusBanner tone="success">{inventoryStatus}</StatusBanner>
          <StatusBanner tone="danger">{inventoryError}</StatusBanner>
          <div className="triple-layout">
            <form className="form-grid" onSubmit={(e) => inventoryAction("create", e)}>
              <h3>Create wallet</h3>
              <Field label="Currency"><input value={inventoryCreate.currency} onChange={(e) => setInventoryCreate({ currency: e.target.value })} /></Field>
              <button className="primary-button" disabled={inventoryBusyAction === "create"} type="submit">
                {inventoryBusyAction === "create" ? "Creating..." : "Create"}
              </button>
            </form>
            <form className="form-grid" onSubmit={(e) => inventoryAction("seed", e)}>
              <h3>Seed wallet</h3>
              <Field label="Currency"><input value={inventorySeed.currency} onChange={(e) => setInventorySeed((s) => ({ ...s, currency: e.target.value }))} /></Field>
              <Field label="Amount"><input type="number" step="any" min="0" value={inventorySeed.amount} onChange={(e) => setInventorySeed((s) => ({ ...s, amount: e.target.value }))} /></Field>
              <button className="primary-button" disabled={inventoryBusyAction === "seed"} type="submit">
                {inventoryBusyAction === "seed" ? "Seeding..." : "Seed"}
              </button>
            </form>
            <form className="form-grid" onSubmit={(e) => inventoryAction("bootstrap", e)}>
              <h3>Bootstrap set</h3>
              <Field label="Currencies CSV"><input value={inventoryBootstrap.currencies} onChange={(e) => setInventoryBootstrap({ currencies: e.target.value })} /></Field>
              <button className="primary-button" disabled={inventoryBusyAction === "bootstrap"} type="submit">
                {inventoryBusyAction === "bootstrap" ? "Bootstrapping..." : "Bootstrap"}
              </button>
            </form>
          </div>
          <DataTable
            columns={[{ key: "id", label: "Wallet ID" }, { key: "currency", label: "Currency" }, { key: "balance", label: "Balance" }, { key: "lockedBalance", label: "Locked" }, { key: "blockchainAddress", label: "Address" }]}
            rows={inventory?.wallets || []}
            emptyText="Load platform inventory."
          />
        </SectionCard>
      ) : null}

      <SectionCard eyebrow="OrderController" title="Order lifecycle" actions={<button className="secondary-button" onClick={loadOrders} type="button">Refresh orders</button>}>
        <StatusBanner tone="success">{orderStatus}</StatusBanner>
        <StatusBanner tone="danger">{orderError}</StatusBanner>
        <div className="split-layout">
          <form className="form-grid" onSubmit={(e) => e.preventDefault()}>
            <Field label="Base">
              <CurrencyPicker
                options={supportedCurrencies}
                value={order.baseCurrency}
                onSelect={(currency) => setOrder((s) => ({ ...s, baseCurrency: currency }))}
                disabled={!supportedCurrencies.length}
              />
            </Field>
            <Field label="Quote">
              <CurrencyPicker
                options={supportedCurrencies}
                value={order.quoteCurrency}
                onSelect={(currency) => setOrder((s) => ({ ...s, quoteCurrency: currency }))}
                disabled={!supportedCurrencies.length}
              />
            </Field>
            <Field label="Amount"><input type="number" step="any" min="0" value={order.amount} onChange={(e) => setOrder((s) => ({ ...s, amount: e.target.value }))} /></Field>
            <Field label="Price"><input type="number" step="any" min="0" value={order.price} onChange={(e) => setOrder((s) => ({ ...s, price: e.target.value }))} /></Field>
            <Field label="Type"><select value={order.type} onChange={(e) => setOrder((s) => ({ ...s, type: e.target.value }))}><option value="BUY">BUY</option><option value="SELL">SELL</option></select></Field>
            <div className="button-row">
              <button className="primary-button" onClick={() => submitOrder("create")} type="button">Create</button>
              <button className="secondary-button" onClick={() => submitOrder("execute")} type="button">Create+Execute</button>
            </div>
          </form>
          <div className="form-grid">
            <Field label="Order ID"><input value={orderId} onChange={(e) => setOrderId(e.target.value)} /></Field>
            <div className="button-row">
              <button className="secondary-button" onClick={() => orderAction("lookup")} type="button">Load</button>
              <button className="secondary-button" onClick={() => orderAction("execute")} type="button">Execute</button>
              <button className="danger-button" onClick={() => orderAction("cancel")} type="button">Cancel</button>
            </div>
            <div className="code-panel"><pre>{selectedOrder ? JSON.stringify(selectedOrder, null, 2) : "No order loaded."}</pre></div>
          </div>
        </div>
        <DataTable columns={[{ key: "id", label: "ID" }, { key: "type", label: "Type" }, { key: "status", label: "Status" }, { key: "baseCurrency", label: "Base" }, { key: "quoteCurrency", label: "Quote" }, { key: "amount", label: "Amount" }, { key: "price", label: "Price" }, { key: "filledAmount", label: "Filled" }]} rows={orders} emptyText="No orders loaded." />
      </SectionCard>

      {isAdmin ? (
        <SectionCard eyebrow="TokenController" title="Token admin and metadata" actions={<button className="secondary-button" onClick={() => tokenAction("refresh")} type="button">Refresh cache</button>}>
          <StatusBanner tone="success">{tokenStatus}</StatusBanner>
          <StatusBanner tone="danger">{tokenError}</StatusBanner>
          <div className="triple-layout">
            <form className="form-grid" onSubmit={(e) => tokenAction("add", e)}>
              <h3>Add token</h3>
              <Field label="Symbol"><input value={tokenAdd.symbol} onChange={(e) => setTokenAdd((s) => ({ ...s, symbol: e.target.value }))} /></Field>
              <Field label="Name"><input value={tokenAdd.name} onChange={(e) => setTokenAdd((s) => ({ ...s, name: e.target.value }))} /></Field>
              <Field label="Contract"><input value={tokenAdd.contractAddress} onChange={(e) => setTokenAdd((s) => ({ ...s, contractAddress: e.target.value }))} /></Field>
              <Field label="Decimals"><input value={tokenAdd.decimals} onChange={(e) => setTokenAdd((s) => ({ ...s, decimals: e.target.value }))} /></Field>
              <button className="primary-button" type="submit">Add</button>
            </form>
            <form className="form-grid" onSubmit={(e) => tokenAction("update", e)}>
              <h3>Update token</h3>
              <Field label="Token ID"><input value={tokenUpdate.tokenId} onChange={(e) => setTokenUpdate((s) => ({ ...s, tokenId: e.target.value }))} /></Field>
              <Field label="Name"><input value={tokenUpdate.name} onChange={(e) => setTokenUpdate((s) => ({ ...s, name: e.target.value }))} /></Field>
              <Field label="Contract"><input value={tokenUpdate.contractAddress} onChange={(e) => setTokenUpdate((s) => ({ ...s, contractAddress: e.target.value }))} /></Field>
              <Field label="Decimals"><input value={tokenUpdate.decimals} onChange={(e) => setTokenUpdate((s) => ({ ...s, decimals: e.target.value }))} /></Field>
              <Field label="isActive"><select value={tokenUpdate.isActive} onChange={(e) => setTokenUpdate((s) => ({ ...s, isActive: e.target.value }))}><option value="">Unchanged</option><option value="true">true</option><option value="false">false</option></select></Field>
              <div className="button-row">
                <button className="primary-button" type="submit">Update</button>
                <button className="secondary-button" onClick={() => tokenAction("activate")} type="button">Activate</button>
                <button className="secondary-button" onClick={() => tokenAction("deactivate")} type="button">Deactivate</button>
                <button className="danger-button" onClick={() => tokenAction("delete")} type="button">Delete</button>
              </div>
            </form>
            <div className="form-grid">
              <h3>Queries</h3>
              <Field label="Symbol"><input value={tokenQuery.symbol} onChange={(e) => setTokenQuery((s) => ({ ...s, symbol: e.target.value }))} /></Field>
              <Field label="Contract"><input value={tokenQuery.contractAddress} onChange={(e) => setTokenQuery((s) => ({ ...s, contractAddress: e.target.value }))} /></Field>
              <div className="button-row">
                <button className="secondary-button" onClick={() => tokenAction("detail")} type="button">Get by symbol</button>
                <button className="secondary-button" onClick={() => tokenAction("name")} type="button">Get name</button>
                <button className="secondary-button" onClick={() => tokenAction("symbol")} type="button">Get symbol</button>
              </div>
              <div className="meta-panel">
                <dl>
                  <div><dt>Name</dt><dd>{tokenMeta.name || "N/A"}</dd></div>
                  <div><dt>Symbol</dt><dd>{tokenMeta.symbol || "N/A"}</dd></div>
                </dl>
              </div>
            </div>
          </div>
          <div className="button-row spaced-row">
            <button className="secondary-button" onClick={loadAllTokens} type="button">Load all</button>
            <button className="secondary-button" onClick={loadActiveTokens} type="button">Load active</button>
            <button className="secondary-button" onClick={loadAddresses} type="button">Load addresses</button>
          </div>
          <div className="split-layout">
            <div className="code-panel"><pre>{tokenDetail ? JSON.stringify(tokenDetail, null, 2) : "No token loaded."}</pre></div>
            <div className="code-panel"><pre>{tokenAddresses ? JSON.stringify(tokenAddresses, null, 2) : "No addresses loaded."}</pre></div>
          </div>
          <DataTable columns={[{ key: "id", label: "ID" }, { key: "symbol", label: "Symbol" }, { key: "name", label: "Name" }, { key: "contractAddress", label: "Contract" }, { key: "decimals", label: "Decimals" }, { key: "isActive", label: "Active" }]} rows={allTokens} emptyText="No tokens loaded." />
        </SectionCard>
      ) : null}
    </div>
  );
}

export default function App() {
  const [authState, setAuthState] = useState(loadAuthState);

  useEffect(() => {
    if (!authState.token || !isJwtExpired(authState.token)) {
      return;
    }

    setAuthState((current) => clearAuthState(current));
  }, [authState.token]);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setAuthState((current) => clearAuthState(current));
    });

    return () => setUnauthorizedHandler(null);
  }, []);

  return (
    <div className="shell">
      <header className="topbar">
        <div>
          <h1>CoinbaseClone</h1>
        </div>
      </header>

      <Routes>
        <Route path="/" element={<Navigate replace to="/auth" />} />
        <Route path="/auth" element={<AuthPage authState={authState} setAuthState={setAuthState} />} />
        <Route
          path="/app"
          element={
            <ProtectedRoute token={authState.token}>
              <DashboardPage authState={authState} setAuthState={setAuthState} />
            </ProtectedRoute>
          }
        />
      </Routes>
    </div>
  );
}
