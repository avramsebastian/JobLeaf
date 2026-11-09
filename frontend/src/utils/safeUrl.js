/**
 * Returns the URL only if it is safe to put in an href/src, otherwise undefined.
 *
 * React escapes text, but not URLs: a value like "javascript:alert(1)" in an href would run
 * code when clicked (DOM-based XSS). Only http(s) links and same-site paths are allowed.
 */
export const safeUrl = (url) => {
  if (typeof url !== "string" || url.trim() === "") {
    return undefined;
  }
  const value = url.trim();

  // Same-site paths like "/logos/derpcorp.png" (but not protocol-relative "//evil.com")
  if (value.startsWith("/") && !value.startsWith("//")) {
    return value;
  }

  try {
    const { protocol } = new URL(value);
    return protocol === "http:" || protocol === "https:" ? value : undefined;
  } catch {
    return undefined;
  }
};
