import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

type DevicePushTokenRequest = {
  action?: string;
  installation_id?: string;
  platform?: string | null;
  push_token?: string | null;
};

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  if (!SUPABASE_URL || !SUPABASE_ANON_KEY || !SUPABASE_SERVICE_ROLE_KEY) {
    return jsonResponse({ error: "Missing Supabase environment" }, 500);
  }

  const authHeader = req.headers.get("Authorization")?.trim();
  const jwt = authHeader?.replace(/^Bearer\s+/i, "").trim();
  if (!authHeader || !jwt) {
    return jsonResponse({ error: "Missing authorization header" }, 401);
  }

  const userClient = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
    global: {
      headers: {
        Authorization: authHeader,
      },
    },
  });
  const adminClient = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

  const {
    data: { user },
    error: userError,
  } = await userClient.auth.getUser(jwt);
  if (userError || !user) {
    return jsonResponse({ error: "Invalid JWT" }, 401);
  }

  const body = (await req.json().catch(() => ({}))) as DevicePushTokenRequest;
  const action = body.action?.trim().toLowerCase();
  const installationId = body.installation_id?.trim();

  if (!installationId) {
    return jsonResponse({ error: "installation_id is required" }, 400);
  }

  const now = new Date().toISOString();

  if (action === "deactivate") {
    const { error } = await adminClient
      .from("device_push_tokens")
      .update({
        is_active: false,
        updated_at: now,
      })
      .eq("installation_id", installationId);

    if (error) {
      return jsonResponse({ error: error.message }, 500);
    }

    return jsonResponse({
      action,
      installation_id: installationId,
      user_id: user.id,
      ok: true,
    });
  }

  if (action !== "sync") {
    return jsonResponse({ error: "Invalid action" }, 400);
  }

  const platform = body.platform?.trim();
  const pushToken = body.push_token?.trim();
  if (!platform || !pushToken) {
    return jsonResponse({ error: "platform and push_token are required for sync" }, 400);
  }

  const deleteByInstallation = await adminClient
    .from("device_push_tokens")
    .delete()
    .eq("installation_id", installationId);
  if (deleteByInstallation.error) {
    return jsonResponse({ error: deleteByInstallation.error.message }, 500);
  }

  const deleteByToken = await adminClient
    .from("device_push_tokens")
    .delete()
    .eq("push_token", pushToken);
  if (deleteByToken.error) {
    return jsonResponse({ error: deleteByToken.error.message }, 500);
  }

  const insertResult = await adminClient
    .from("device_push_tokens")
    .insert({
      user_id: user.id,
      installation_id: installationId,
      platform,
      push_token: pushToken,
      is_active: true,
      last_seen_at: now,
      updated_at: now,
    });

  if (insertResult.error) {
    return jsonResponse({ error: insertResult.error.message }, 500);
  }

  return jsonResponse({
    action,
    installation_id: installationId,
    user_id: user.id,
    ok: true,
  });
});

function jsonResponse(payload: unknown, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: {
      "Content-Type": "application/json",
    },
  });
}
