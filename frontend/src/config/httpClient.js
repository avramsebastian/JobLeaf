import axios from "axios";
import Cookies from "js-cookie";
import { API_BASE_URL } from "./api";
import { clearSession } from "../utils/authSession";

/**
 * Default API Accept Header
 * This value is used for the Accept header in all API requests
 * Follows the vendor-specific media type format: application/vnd.jobleaf+json;v=1.0
 * Developers can override this in individual requests if needed
 */
export const DEFAULT_ACCEPT_HEADER = "application/vnd.jobleaf+json;v=1.0";

/**
 * Create axios instance with default configuration
 */
const httpClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    // Request bodies use the same versioned media type the API accepts (application/json is rejected with 415)
    "Content-Type": DEFAULT_ACCEPT_HEADER,
  },
  withCredentials: true, // Required for CSRF cookie handling
});

/**
 * Request Interceptor
 * Automatically adds Accept header and handles CSRF token for non-safe HTTP methods
 */
httpClient.interceptors.request.use(
  async (config) => {
    // Add Accept header with default value
    // Developers can override this by passing headers in the request config

    config.headers.Accept = DEFAULT_ACCEPT_HEADER;

    // No Authorization header: the JWT is in an HttpOnly cookie that the browser sends by itself
    // (withCredentials: true), on public endpoints too, so the backend always knows who made the request.

    // Handle CSRF token for non-safe HTTP methods
    const safeMethods = ["GET", "HEAD", "OPTIONS"];
    if (!safeMethods.includes(config.method.toUpperCase())) {
      let csrfToken = Cookies.get("XSRF-TOKEN");

      // If CSRF token is not present in cookies, fetch it from the server
      if (!csrfToken) {
        try {
          await axios.get(`${API_BASE_URL}/csrf-token/public`, {
            withCredentials: true,
          });
          csrfToken = Cookies.get("XSRF-TOKEN");

          if (!csrfToken) {
            throw new Error("Failed to retrieve CSRF token from cookies");
          }
        } catch (error) {
          // Don't cancel the request here: without a token the backend's CsrfFilter rejects it with a 403
          // problem response, which the calling page shows like any other error.
          console.warn("[CSRF Token] Could not fetch a CSRF token, continuing without it", error.response?.status);
        }
      }

      // Add CSRF token to request header
      config.headers["X-XSRF-TOKEN"] = csrfToken;
    }

    // Log the request for debugging (can be removed in production)
    console.log(`[HTTP Request] ${config.method.toUpperCase()} ${config.url}`, {
      headers: config.headers,
      data: config.data,
    });

    return config;
  },
  (error) => {
    console.error("[HTTP Request Error]", error);
    return Promise.reject(error);
  }
);

/**
 * Response Interceptor
 * Handles response transformations and error handling
 */
httpClient.interceptors.response.use(
  (response) => {
    // Log the response for debugging (can be removed in production)
    console.log(
      `[HTTP Response] ${response.config.method.toUpperCase()} ${response.config.url
      }`,
      {
        status: response.status,
        data: response.data,
      }
    );

    return response;
  },
  (error) => {
    // Enhanced error handling
    if (error.response) {
      // Server responded with error status
      console.error("[HTTP Response Error]", {
        status: error.response.status,
        data: error.response.data,
        url: error.config.url,
      });

      // Handle specific status codes
      switch (error.response.status) {
        case 401:
          // Only redirect to login if this is NOT a login request
          // and we're NOT already on the login page
          const isLoginRequest = error.config.url.includes("/auth/login");
          const isOnLoginPage = window.location.pathname === "/login";

          if (!isLoginRequest && !isOnLoginPage) {
            // Unauthorized - clear token and user data, then redirect to login
            clearSession();
            window.location.href = "/login";
          } else {
            // If it's a login request or we're on login page, just clear token
            // but don't redirect (let the component handle the error)
            clearSession();
          }
          break;
        case 403:
          console.error("Access forbidden");
          break;
        case 404:
          console.error("Resource not found");
          break;
        case 500:
          console.error("Internal server error");
          break;
        default:
          console.error("An error occurred");
      }
    } else if (error.request) {
      // Request was made but no response received
      console.error("[HTTP No Response]", error.request);
    } else {
      // Something else happened
      console.error("[HTTP Error]", error.message);
    }

    return Promise.reject(error);
  }
);

/**
 * Helper function to make requests with custom API version
 *
 * @example
 * // Use default version (1.0)
 * const response = await httpClient.get('/companies');
 *
 * @example
 * // Use specific version (2.0)
 * const response = await httpClient.get('/companies', {
 *   headers: { 'Accept': 'application/vnd.jobleaf+json;v=2.0' }
 * });
 *
 * @example
 * // Use withApiVersion helper
 * const response = await withApiVersion('2.0').get('/companies');
 */
export const withApiVersion = (version) => {
  const acceptHeader = `application/vnd.jobleaf+json;v=${version}`;
  return {
    get: (url, config = {}) =>
      httpClient.get(url, {
        ...config,
        headers: { ...config.headers, Accept: acceptHeader },
      }),
    post: (url, data, config = {}) =>
      httpClient.post(url, data, {
        ...config,
        headers: { ...config.headers, Accept: acceptHeader },
      }),
    put: (url, data, config = {}) =>
      httpClient.put(url, data, {
        ...config,
        headers: { ...config.headers, Accept: acceptHeader },
      }),
    patch: (url, data, config = {}) =>
      httpClient.patch(url, data, {
        ...config,
        headers: { ...config.headers, Accept: acceptHeader },
      }),
    delete: (url, config = {}) =>
      httpClient.delete(url, {
        ...config,
        headers: { ...config.headers, Accept: acceptHeader },
      }),
  };
};

export default httpClient;
