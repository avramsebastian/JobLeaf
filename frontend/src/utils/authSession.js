/**
 * The one place that reads and writes the login session in localStorage.
 *
 * The JWT itself is NOT here: the backend keeps it in an HttpOnly cookie that JavaScript cannot read, and the
 * browser sends it automatically. The frontend only stores when the session ends (expiresAt from the login
 * response) and the user object the UI needs.
 */

const EXPIRES_AT_KEY = "authTokenExpiresAt";
const USER_KEY = "user";
/** Where the token used to be stored; removed so no copy is left behind from older versions. */
const LEGACY_TOKEN_KEY = "authToken";

/**
 * @param {string} expiresAt ISO-8601 date from the login/register response, e.g. "2026-11-03T13:00:00Z"
 */
export const saveSession = (expiresAt) => {
  localStorage.setItem(EXPIRES_AT_KEY, expiresAt);
  localStorage.removeItem(LEGACY_TOKEN_KEY);
};

export const clearSession = () => {
  localStorage.removeItem(EXPIRES_AT_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(LEGACY_TOKEN_KEY);
};

/**
 * @returns {number|null} expiry as epoch milliseconds, or null when unknown
 */
export const getExpiresAt = () => {
  const value = Date.parse(localStorage.getItem(EXPIRES_AT_KEY));
  return Number.isNaN(value) ? null : value;
};

/**
 * A session without a known expiry counts as expired.
 */
export const isSessionExpired = () => {
  const expiresAt = getExpiresAt();
  return expiresAt === null || Date.now() >= expiresAt;
};
