import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

/**
 * Adds a Content-Security-Policy to index.html in production builds only:
 * the dev server injects inline scripts for hot reload, which a strict CSP would block.
 * Injected <script> tags or scripts from other domains will not run in the built app.
 */
const contentSecurityPolicy = (apiBaseUrl) => ({
  name: "content-security-policy",
  apply: "build",
  transformIndexHtml: () => [
    {
      tag: "meta",
      attrs: {
        "http-equiv": "Content-Security-Policy",
        content: [
          "default-src 'self'",
          "script-src 'self'",
          "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
          "font-src 'self' https://fonts.gstatic.com",
          "img-src 'self' data:",
          `connect-src 'self' ${new URL(apiBaseUrl).origin}`,
          "object-src 'none'",
          "base-uri 'self'",
          "form-action 'self'",
        ].join("; "),
      },
      injectTo: "head-prepend",
    },
  ],
});

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, ".");
  const apiBaseUrl = env.VITE_API_BASE_URL || "http://localhost:8080/jobleaf";

  return {
    plugins: [react(), tailwindcss(), contentSecurityPolicy(apiBaseUrl)],
  };
});
