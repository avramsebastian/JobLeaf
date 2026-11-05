/**
 * The one place that reads and writes the login token in localStorage.
 *
 * The backend says when the token stops being accepted (expiresAt from the login response).
 * An expired token is treated as no token at all, so the UI never shows a user as logged in
 * with a token the backend will reject.
 */

const TOKEN_KEY = "authToken";
const EXPIRES_AT_KEY = "authTokenExpiresAt";
const USER_KEY = "user";

/**
 * @param {string} token JWT from the login response
 * @param {string} expiresAt ISO-8601 date from the login response, e.g. "2026-11-03T13:00:00Z"
 */
export const saveSession = (token, expiresAt) => {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(EXPIRES_AT_KEY, expiresAt);
};

export const clearSession = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(EXPIRES_AT_KEY);
  localStorage.removeItem(USER_KEY);
};

/**
 * @returns {number|null} expiry as epoch milliseconds, or null when unknown
 */
export const getExpiresAt = () => {
  const value = Date.parse(localStorage.getItem(EXPIRES_AT_KEY));
  return Number.isNaN(value) ? null : value;
};

/**
 * A token without a known expiry (e.g. saved before expiresAt was stored) counts as expired.
 */
export const isSessionExpired = () => {
  const expiresAt = getExpiresAt();
  return expiresAt === null || Date.now() >= expiresAt;
};

/**
 * @returns {string|null} the token, or null when there is none or it has expired (an expired one is removed)
 */
export const getToken = () => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (!token) {
    return null;
  }
  if (isSessionExpired()) {
    clearSession();
    return null;
  }
  return token;
};
