package com.fbla.businessboost.util;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Lightweight embedded HTTP server that serves:
 * <ul>
 * <li>Google reCAPTCHA v2 at {@code /captcha}</li>
 * <li>Google OAuth callback at {@code /oauth/callback}</li>
 * <li>Facebook/Yahoo OAuth pages at {@code /oauth/facebook},
 * {@code /oauth/yahoo}</li>
 * </ul>
 *
 * <p>
 * Uses a fixed port (8765) so Google OAuth redirect URIs can be pre-registered.
 * </p>
 *
 * @author FBLA Development Team
 * @version 3.0
 */
public class CaptchaServer {

    private static CaptchaServer instance;
    private HttpServer server;
    private int port;

    /** Google reCAPTCHA v2 site key */
    private static final String RECAPTCHA_SITE_KEY = "REDACTED_RECAPTCHA_SITE_KEY";

    /**
     * Google OAuth 2.0 Client ID — create at:
     * https://console.cloud.google.com/apis/credentials
     * Type: Web application
     * Authorized redirect URI: http://localhost:8765/oauth/callback
     */
    private static final String GOOGLE_CLIENT_ID = "REDACTED_GOOGLE_CLIENT_ID";

    /** Fixed port for OAuth redirect URI consistency */
    private static final int FIXED_PORT = 8765;

    private CaptchaServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", FIXED_PORT), 0);
            port = FIXED_PORT;

            // ---- reCAPTCHA endpoint ----
            server.createContext("/captcha", exchange -> {
                sendHtml(exchange, buildCaptchaHtml());
            });

            // ---- Google OAuth callback ----
            // Google redirects here with #access_token in the fragment
            server.createContext("/oauth/callback", exchange -> {
                sendHtml(exchange, buildGoogleCallbackPage());
            });

            // ---- Facebook login page ----
            server.createContext("/oauth/facebook", exchange -> {
                sendHtml(exchange, buildSimpleOAuthPage("Facebook",
                        "#1877F2", "#fff", ""));
            });

            // ---- Yahoo login page ----
            server.createContext("/oauth/yahoo", exchange -> {
                sendHtml(exchange, buildSimpleOAuthPage("Yahoo",
                        "#720E9E", "#fff", ""));
            });

            // ---- Apple login page ----
            server.createContext("/oauth/apple", exchange -> {
                sendHtml(exchange, buildSimpleOAuthPage("Apple",
                        "#000000", "#fff", ""));
            });

            server.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "CaptchaServer");
                t.setDaemon(true);
                return t;
            }));

            server.start();
            System.out.println("CaptchaServer started on http://localhost:" + port);

        } catch (IOException e) {
            System.err.println("Failed to start CaptchaServer: " + e.getMessage());
        }
    }

    public static synchronized CaptchaServer getInstance() {
        if (instance == null) {
            instance = new CaptchaServer();
        }
        return instance;
    }

    public String getCaptchaUrl() {
        return "http://localhost:" + port + "/captcha";
    }

    public int getPort() {
        return port;
    }

    /**
     * Returns the full Google OAuth 2.0 authorization URL.
     * Uses prompt=select_account to always show the account picker.
     */
    public String getGoogleOAuthUrl() {
        String redirectUri = "http://localhost:" + port + "/oauth/callback";
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + GOOGLE_CLIENT_ID
                + "&redirect_uri=" + redirectUri
                + "&response_type=token"
                + "&scope=email%20profile"
                + "&prompt=select_account";
    }

    public String getOAuthUrl(String provider) {
        return "http://localhost:" + port + "/oauth/" + provider.toLowerCase();
    }

    // ==================== HTML Builders ====================

    private void sendHtml(com.sun.net.httpserver.HttpExchange exchange, String html) throws IOException {
        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private String buildCaptchaHtml() {
        return "<!DOCTYPE html>\n"
                + "<html><head>\n"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>\n"
                + "<script src='https://www.google.com/recaptcha/api.js' async defer></script>\n"
                + "<style>\n"
                + "  body { margin: 0; padding: 6px; background: transparent; "
                + "    display: flex; justify-content: center; }\n"
                + "  .g-recaptcha { transform: scale(0.88); transform-origin: center top; }\n"
                + "</style>\n"
                + "</head><body>\n"
                + "<div class='g-recaptcha' data-sitekey='" + RECAPTCHA_SITE_KEY + "' "
                + "data-callback='onSuccess' data-theme='light' data-size='normal'></div>\n"
                + "<script>\n"
                + "function onSuccess(token) { document.title = 'CAPTCHA_OK:' + token; }\n"
                + "</script>\n"
                + "</body></html>";
    }

    /**
     * Google OAuth callback page. After Google redirects with #access_token in
     * fragment,
     * this page extracts it via JavaScript and fetches the user's profile from
     * Google's API.
     * Then signals the WebView via document.title.
     */
    private String buildGoogleCallbackPage() {
        return "<!DOCTYPE html>\n"
                + "<html><head>\n"
                + "<style>\n"
                + "body { font-family: 'Segoe UI', Arial, sans-serif; display: flex; "
                + "justify-content: center; align-items: center; height: 100vh; margin: 0; "
                + "background: #f5f5f5; }\n"
                + ".card { background: white; border-radius: 12px; padding: 40px; "
                + "text-align: center; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }\n"
                + ".spinner { width: 40px; height: 40px; border: 4px solid #ddd; "
                + "border-top: 4px solid #4285F4; border-radius: 50%; "
                + "animation: spin 1s linear infinite; margin: 0 auto 16px; }\n"
                + "@keyframes spin { to { transform: rotate(360deg); } }\n"
                + "</style>\n"
                + "</head><body>\n"
                + "<div class='card'>\n"
                + "  <div class='spinner'></div>\n"
                + "  <p id='status'>Signing you in...</p>\n"
                + "</div>\n"
                + "<script>\n"
                + "var hash = window.location.hash.substring(1);\n"
                + "var params = new URLSearchParams(hash);\n"
                + "var token = params.get('access_token');\n"
                + "if (token) {\n"
                + "  fetch('https://www.googleapis.com/oauth2/v3/userinfo', {\n"
                + "    headers: { 'Authorization': 'Bearer ' + token }\n"
                + "  }).then(r => r.json()).then(user => {\n"
                + "    document.title = 'OAUTH_OK:' + encodeURIComponent(user.name || user.email) "
                + "      + ':' + encodeURIComponent(user.email) + ':google';\n"
                + "  }).catch(e => {\n"
                + "    document.getElementById('status').textContent = 'Error: ' + e.message;\n"
                + "  });\n"
                + "} else {\n"
                + "  document.getElementById('status').textContent = 'Sign-in cancelled.';\n"
                + "}\n"
                + "</script>\n"
                + "</body></html>";
    }

    /**
     * Simple OAuth login page for Facebook/Yahoo/Apple (demo — collects name/email).
     */
    private String buildSimpleOAuthPage(String provider, String brandColor, String textColor, String icon) {
        return "<!DOCTYPE html>\n"
                + "<html><head>\n"
                + "<style>\n"
                + "* { margin: 0; padding: 0; box-sizing: border-box; }\n"
                + "body { font-family: 'Segoe UI', Arial, sans-serif; background: #f5f5f5; "
                + "  display: flex; justify-content: center; align-items: center; min-height: 100vh; }\n"
                + ".card { background: white; border-radius: 12px; padding: 30px; width: 360px; "
                + "  box-shadow: 0 4px 20px rgba(0,0,0,0.1); text-align: center; }\n"
                + ".logo { font-size: 40px; margin-bottom: 8px; }\n"
                + "h2 { color: #333; margin-bottom: 4px; font-size: 20px; }\n"
                + ".subtitle { color: #888; font-size: 13px; margin-bottom: 20px; }\n"
                + "label { display: block; text-align: left; font-size: 12px; font-weight: 600; "
                + "  color: #555; margin-bottom: 4px; margin-top: 12px; }\n"
                + "input { width: 100%; padding: 10px 12px; border: 1px solid #ddd; border-radius: 8px; "
                + "  font-size: 14px; outline: none; }\n"
                + "input:focus { border-color: " + brandColor + "; }\n"
                + "button { width: 100%; padding: 12px; margin-top: 18px; border: none; border-radius: 8px; "
                + "  background: " + brandColor + "; color: " + textColor + "; font-size: 14px; "
                + "  font-weight: bold; cursor: pointer; }\n"
                + "button:hover { opacity: 0.9; }\n"
                + ".footer { margin-top: 16px; font-size: 11px; color: #aaa; }\n"
                + "</style>\n"
                + "</head><body>\n"
                + "<div class='card'>\n"
                + "  <div class='logo'>" + icon + "</div>\n"
                + "  <h2>Sign in with " + provider + "</h2>\n"
                + "  <p class='subtitle'>to continue to Trove</p>\n"
                + "  <form onsubmit='handleSubmit(event)'>\n"
                + "    <label>Email</label>\n"
                + "    <input type='email' id='email' placeholder='you@example.com' required />\n"
                + "    <label>Display Name</label>\n"
                + "    <input type='text' id='name' placeholder='Your name' required />\n"
                + "    <button type='submit'>Sign in</button>\n"
                + "  </form>\n"
                + "  <p class='footer'>By signing in, you agree to Trove's Terms of Service.</p>\n"
                + "</div>\n"
                + "<script>\n"
                + "function handleSubmit(e) {\n"
                + "  e.preventDefault();\n"
                + "  var email = document.getElementById('email').value.trim();\n"
                + "  var name = document.getElementById('name').value.trim();\n"
                + "  if (email && name) {\n"
                + "    document.title = 'OAUTH_OK:' + encodeURIComponent(name) + ':' + encodeURIComponent(email) "
                + "      + ':" + provider.toLowerCase() + "';\n"
                + "  }\n"
                + "}\n"
                + "</script>\n"
                + "</body></html>";
    }
}
