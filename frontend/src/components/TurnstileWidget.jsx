import { forwardRef, useEffect, useImperativeHandle, useRef, useState } from "react";

/**
 * Cloudflare Turnstile "Verify you are human" box.
 *
 * The widget runs in an iframe from Cloudflare. When the check passes it hands us a one-time token, which the
 * form sends to the backend in the X-Captcha-Token header; the backend asks Cloudflare whether it is valid.
 * A token works once and for at most 5 minutes, so after a failed submit call ref.reset() to get a new one.
 */
const SCRIPT_URL = "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";
const SITE_KEY = import.meta.env.VITE_TURNSTILE_SITE_KEY;

// Load Cloudflare's script once for the whole app, even if several forms use the widget
let scriptPromise = null;
const loadTurnstile = () => {
  if (window.turnstile) {
    return Promise.resolve(window.turnstile);
  }
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement("script");
      script.src = SCRIPT_URL;
      script.async = true;
      script.onload = () => resolve(window.turnstile);
      script.onerror = () => {
        scriptPromise = null; // allow a retry on the next mount
        reject(new Error("Could not load Cloudflare Turnstile"));
      };
      document.head.appendChild(script);
    });
  }
  return scriptPromise;
};

const TurnstileWidget = forwardRef(({ action, theme = "light", onToken }, ref) => {
  const containerRef = useRef(null);
  const widgetIdRef = useRef(null);
  const onTokenRef = useRef(onToken);
  const [loadError, setLoadError] = useState(false);

  // Always call the latest onToken without re-creating the widget when the parent re-renders
  useEffect(() => {
    onTokenRef.current = onToken;
  }, [onToken]);

  useImperativeHandle(ref, () => ({
    reset: () => {
      onTokenRef.current(null);
      if (widgetIdRef.current !== null) {
        window.turnstile?.reset(widgetIdRef.current);
      }
    },
  }), []);

  useEffect(() => {
    let cancelled = false;
    loadTurnstile()
      .then((turnstile) => {
        if (cancelled || !containerRef.current) {
          return;
        }
        widgetIdRef.current = turnstile.render(containerRef.current, {
          sitekey: SITE_KEY,
          action,
          theme,
          callback: (token) => onTokenRef.current(token),
          "expired-callback": () => onTokenRef.current(null),
          "error-callback": () => onTokenRef.current(null),
        });
      })
      .catch(() => {
        if (!cancelled) {
          setLoadError(true);
        }
      });

    // Unmount (or StrictMode's test unmount): remove the widget so it is not rendered twice
    return () => {
      cancelled = true;
      if (widgetIdRef.current !== null) {
        window.turnstile?.remove(widgetIdRef.current);
        widgetIdRef.current = null;
      }
    };
  }, [action, theme]);

  if (!SITE_KEY) {
    return <p className="text-sm text-red-600">Captcha is not configured (VITE_TURNSTILE_SITE_KEY is missing).</p>;
  }
  if (loadError) {
    return <p className="text-sm text-red-600">Could not load the human check. Please refresh the page.</p>;
  }
  return <div ref={containerRef} className="flex justify-center min-h-[65px]" />;
});

TurnstileWidget.displayName = "TurnstileWidget";

export default TurnstileWidget;
