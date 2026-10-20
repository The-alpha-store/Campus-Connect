// CampusConnect: send-push
// Triggered by a Database Webhook on INSERT into public.notifications.
// Looks up the recipient's phones in public.device_tokens and sends a
// Firebase Cloud Messaging (FCM v1) "data" message to each one.
//
// Required secrets (set with `supabase secrets set`):
//   WEBHOOK_SECRET       any long random string; the webhook sends it in the x-webhook-secret header
//   FCM_SERVICE_ACCOUNT  the Firebase service-account JSON, as ONE line
// Supabase provides SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY automatically.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const WEBHOOK_SECRET = Deno.env.get("WEBHOOK_SECRET") ?? "";
const SERVICE_ACCOUNT = JSON.parse(Deno.env.get("FCM_SERVICE_ACCOUNT") ?? "{}");

const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY);

// ---------- Google OAuth token for FCM (cached until shortly before it expires) ----------
let cachedToken: { value: string; expiresAt: number } | null = null;

const b64url = (data: ArrayBuffer | string): string => {
  const bytes = typeof data === "string" ? new TextEncoder().encode(data) : new Uint8Array(data);
  let bin = "";
  bytes.forEach((b) => (bin += String.fromCharCode(b)));
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
};

async function signJwt(claims: Record<string, unknown>): Promise<string> {
  const pem: string = SERVICE_ACCOUNT.private_key;
  const der = Uint8Array.from(
    atob(pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "")),
    (c) => c.charCodeAt(0),
  );
  const key = await crypto.subtle.importKey(
    "pkcs8",
    der,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const unsigned = `${b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }))}.${b64url(JSON.stringify(claims))}`;
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
  return `${unsigned}.${b64url(signature)}`;
}

async function accessToken(): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cachedToken && cachedToken.expiresAt - 60 > now) return cachedToken.value;

  const assertion = await signJwt({
    iss: SERVICE_ACCOUNT.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  });
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!res.ok) throw new Error(`Google token request failed: ${res.status} ${await res.text()}`);
  const json = await res.json();
  cachedToken = { value: json.access_token, expiresAt: now + (json.expires_in ?? 3600) };
  return cachedToken.value;
}

// ---------- Send one message ----------
async function sendToToken(token: string, data: Record<string, string>): Promise<"ok" | "dead" | "error"> {
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${SERVICE_ACCOUNT.project_id}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${await accessToken()}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token,
          data, // data-only: the app builds the notification so a tap opens the right screen
          android: { priority: "HIGH" },
        },
      }),
    },
  );
  if (res.ok) return "ok";
  const text = await res.text();
  // The phone uninstalled the app or the token expired: forget it.
  if (res.status === 404 || text.includes("UNREGISTERED") || text.includes("INVALID_ARGUMENT")) return "dead";
  console.error("FCM error", res.status, text);
  return "error";
}

// ---------- HTTP entry point ----------
Deno.serve(async (req) => {
  if (!WEBHOOK_SECRET || req.headers.get("x-webhook-secret") !== WEBHOOK_SECRET) {
    return new Response("Unauthorized", { status: 401 });
  }
  if (!SERVICE_ACCOUNT.private_key) {
    return new Response("FCM_SERVICE_ACCOUNT is not set", { status: 500 });
  }

  const payload = await req.json();
  if (payload.type !== "INSERT" || payload.table !== "notifications") {
    return new Response("Ignored", { status: 200 });
  }
  const n = payload.record;

  const { data: tokens, error } = await supabase
    .from("device_tokens")
    .select("fcm_token")
    .eq("user_id", n.recipient_id);
  if (error) return new Response(error.message, { status: 500 });
  if (!tokens || tokens.length === 0) return new Response("No devices", { status: 200 });

  const data: Record<string, string> = {
    title: String(n.title ?? ""),
    body: String(n.body ?? ""),
    kind: String(n.kind ?? ""),
    target_id: String(n.announcement_id ?? n.session_id ?? n.assignment_id ?? n.material_id ?? ""),
    notification_id: String(n.id),
  };

  let sent = 0;
  for (const t of tokens) {
    const result = await sendToToken(t.fcm_token, data);
    if (result === "ok") sent++;
    if (result === "dead") await supabase.from("device_tokens").delete().eq("fcm_token", t.fcm_token);
  }
  return new Response(JSON.stringify({ sent, devices: tokens.length }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
});
