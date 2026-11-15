/**
 * Turns a 429 Too Many Requests answer from the backend into a message for the user, or returns null for any
 * other error.
 *
 * The wait comes from "retryAfterSeconds" in the problem body. The standard Retry-After header carries the same
 * number, but the browser hides response headers from cross-origin JavaScript unless the server exposes them,
 * so the body is the reliable place to read it.
 */
export const getRateLimitMessage = (error) => {
  if (error?.response?.status !== 429) {
    return null;
  }
  const seconds = Number(error.response.data?.retryAfterSeconds);
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return "Too many attempts. Please wait a moment before trying again.";
  }
  return `Too many attempts. Please wait ${formatWait(seconds)} before trying again.`;
};

const formatWait = (seconds) => {
  if (seconds < 60) {
    return `${seconds} second${seconds === 1 ? "" : "s"}`;
  }
  const minutes = Math.ceil(seconds / 60);
  return `${minutes} minute${minutes === 1 ? "" : "s"}`;
};
