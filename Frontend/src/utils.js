export function decodeJwt(token) {
  if (!token) {
    return null;
  }

  try {
    const payload = token.split(".")[1];
    return JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
  } catch (_error) {
    return null;
  }
}

export function isJwtExpired(token) {
  const payload = decodeJwt(token);
  if (!payload?.exp) {
    return false;
  }

  const expiresAtMs = Number(payload.exp) * 1000;
  if (!Number.isFinite(expiresAtMs)) {
    return false;
  }

  return expiresAtMs <= Date.now();
}

export function formatValue(value) {
  if (value === null || value === undefined || value === "") {
    return "N/A";
  }

  if (typeof value === "object") {
    return JSON.stringify(value, null, 2);
  }

  return String(value);
}

export function getErrorMessage(error) {
  if (!(error instanceof Error)) {
    return "Unexpected request failure";
  }

  if (error.details && typeof error.details === "object") {
    return `${error.message}\n${JSON.stringify(error.details, null, 2)}`;
  }

  return error.message;
}

export function toTokenBaseUnits(amount, decimals) {
  const normalizedAmount = String(amount ?? "").trim();
  const normalizedDecimals = Number(decimals);

  if (!normalizedAmount) {
    throw new Error("Amount is required");
  }

  if (!Number.isInteger(normalizedDecimals) || normalizedDecimals < 0) {
    throw new Error("Decimals must be zero or greater");
  }

  const [wholePartRaw, fractionPartRaw = ""] = normalizedAmount.split(".");
  const wholePart = wholePartRaw || "0";

  if (!/^\d+$/.test(wholePart) || !/^\d*$/.test(fractionPartRaw)) {
    throw new Error("Amount must be a valid number");
  }

  if (fractionPartRaw.length > normalizedDecimals) {
    throw new Error(`Amount has more than ${normalizedDecimals} decimal places`);
  }

  const paddedFraction = fractionPartRaw.padEnd(normalizedDecimals, "0");
  const combined = `${wholePart}${paddedFraction}`.replace(/^0+(?=\d)/, "");
  return BigInt(combined || "0");
}

export function encodeApproveData(spenderAddress, amount) {
  if (!/^0x[a-fA-F0-9]{40}$/.test(spenderAddress)) {
    throw new Error("Spender address must be a valid 0x wallet address");
  }

  if (typeof amount !== "bigint" || amount < 0n) {
    throw new Error("Amount must be a non-negative bigint");
  }

  const functionSelector = "0x095ea7b3";
  const encodedAddress = spenderAddress.toLowerCase().replace(/^0x/, "").padStart(64, "0");
  const encodedAmount = amount.toString(16).padStart(64, "0");

  return `${functionSelector}${encodedAddress}${encodedAmount}`;
}
