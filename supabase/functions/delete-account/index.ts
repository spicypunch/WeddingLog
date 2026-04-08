import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";
import { SignJWT, importPKCS8 } from "npm:jose@5";

type DeleteAccountRequest = {
  provider?: string;
  apple_authorization_code?: string | null;
};

type JsonRow = Record<string, unknown>;

type DeleteBackup = {
  pushTokens: JsonRow[];
  notifications: JsonRow[];
  profiles: JsonRow[];
};

type DeletedFlags = {
  pushTokens: boolean;
  notifications: boolean;
  profiles: boolean;
};

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

const APPLE_TEAM_ID = Deno.env.get("APPLE_TEAM_ID") ?? "";
const APPLE_KEY_ID = Deno.env.get("APPLE_KEY_ID") ?? "";
const APPLE_CLIENT_ID = Deno.env.get("APPLE_CLIENT_ID") ?? "";
const APPLE_PRIVATE_KEY = Deno.env.get("APPLE_PRIVATE_KEY") ?? "";

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

  const body = (await req.json().catch(() => ({}))) as DeleteAccountRequest;
  const appleAuthorizationCode = body.apple_authorization_code?.trim();
  const provider = resolveUserProvider(user);

  if (provider === "apple") {
    if (!appleAuthorizationCode) {
      return jsonResponse({ error: "Apple 재인증 코드가 필요합니다." }, 400);
    }
    const revokeResult = await revokeAppleAccess(appleAuthorizationCode);
    if (!revokeResult.ok) {
      return jsonResponse({ error: revokeResult.error }, 400);
    }
  }

  const backupResult = await createDeleteBackup(adminClient, user.id);
  if (!backupResult.ok) {
    return jsonResponse({ error: backupResult.error }, 500);
  }

  const deletePublicRowsResult = await deletePublicRows(adminClient, user.id, backupResult.backup);
  if (!deletePublicRowsResult.ok) {
    return jsonResponse({ error: deletePublicRowsResult.error }, 500);
  }

  const deleteAuthUser = await adminClient.auth.admin.deleteUser(user.id);
  if (deleteAuthUser.error) {
    const restoreResult = await restoreDeletedRows(
      adminClient,
      backupResult.backup,
      deletePublicRowsResult.deletedFlags,
    );
    if (!restoreResult.ok) {
      return jsonResponse(
        {
          error: "회원탈퇴 도중 일부 데이터 복구에 실패했습니다. 관리자에게 문의해주세요.",
          detail: deleteAuthUser.error.message,
          restore_error: restoreResult.error,
        },
        500,
      );
    }
    return jsonResponse({ error: deleteAuthUser.error.message }, 500);
  }

  return jsonResponse({
    ok: true,
    user_id: user.id,
  });
});

async function createDeleteBackup(
  adminClient: ReturnType<typeof createClient>,
  userId: string,
): Promise<{ ok: true; backup: DeleteBackup } | { ok: false; error: string }> {
  const [{ data: pushTokens, error: pushTokensError }, { data: notifications, error: notificationsError }, { data: profiles, error: profilesError }] = await Promise.all([
    adminClient
      .from("device_push_tokens")
      .select("*")
      .eq("user_id", userId),
    adminClient
      .from("partner_activity_notifications")
      .select("*")
      .or(`actor_user_id.eq.${userId},target_user_id.eq.${userId}`),
    adminClient
      .from("profiles")
      .select("*")
      .eq("id", userId),
  ]);

  if (pushTokensError) {
    return { ok: false, error: pushTokensError.message };
  }
  if (notificationsError) {
    return { ok: false, error: notificationsError.message };
  }
  if (profilesError) {
    return { ok: false, error: profilesError.message };
  }

  return {
    ok: true,
    backup: {
      pushTokens: (pushTokens ?? []) as JsonRow[],
      notifications: (notifications ?? []) as JsonRow[],
      profiles: (profiles ?? []) as JsonRow[],
    },
  };
}

async function deletePublicRows(
  adminClient: ReturnType<typeof createClient>,
  userId: string,
  backup: DeleteBackup,
): Promise<{ ok: true; deletedFlags: DeletedFlags } | { ok: false; error: string }> {
  const deletedFlags: DeletedFlags = {
    pushTokens: false,
    notifications: false,
    profiles: false,
  };

  if (backup.pushTokens.length > 0) {
    const { error } = await adminClient
      .from("device_push_tokens")
      .delete()
      .eq("user_id", userId);
    if (error) {
      return { ok: false, error: error.message };
    }
    deletedFlags.pushTokens = true;
  }

  if (backup.notifications.length > 0) {
    const { error } = await adminClient
      .from("partner_activity_notifications")
      .delete()
      .or(`actor_user_id.eq.${userId},target_user_id.eq.${userId}`);
    if (error) {
      const restoreResult = await restoreDeletedRows(adminClient, backup, deletedFlags);
      return {
        ok: false,
        error: restoreResult.ok
          ? error.message
          : `${error.message} (복구 실패: ${restoreResult.error})`,
      };
    }
    deletedFlags.notifications = true;
  }

  if (backup.profiles.length > 0) {
    const { error } = await adminClient
      .from("profiles")
      .delete()
      .eq("id", userId);
    if (error) {
      const restoreResult = await restoreDeletedRows(adminClient, backup, deletedFlags);
      return {
        ok: false,
        error: restoreResult.ok
          ? error.message
          : `${error.message} (복구 실패: ${restoreResult.error})`,
      };
    }
    deletedFlags.profiles = true;
  }

  return { ok: true, deletedFlags };
}

async function restoreDeletedRows(
  adminClient: ReturnType<typeof createClient>,
  backup: DeleteBackup,
  deletedFlags: DeletedFlags,
): Promise<{ ok: true } | { ok: false; error: string }> {
  if (deletedFlags.profiles && backup.profiles.length > 0) {
    const { error } = await adminClient
      .from("profiles")
      .insert(backup.profiles);
    if (error) {
      return { ok: false, error: error.message };
    }
  }

  if (deletedFlags.notifications && backup.notifications.length > 0) {
    const { error } = await adminClient
      .from("partner_activity_notifications")
      .insert(backup.notifications);
    if (error) {
      return { ok: false, error: error.message };
    }
  }

  if (deletedFlags.pushTokens && backup.pushTokens.length > 0) {
    const { error } = await adminClient
      .from("device_push_tokens")
      .insert(backup.pushTokens);
    if (error) {
      return { ok: false, error: error.message };
    }
  }

  return { ok: true };
}

async function revokeAppleAccess(authorizationCode: string): Promise<{ ok: true } | { ok: false; error: string }> {
  if (!APPLE_TEAM_ID || !APPLE_KEY_ID || !APPLE_CLIENT_ID || !APPLE_PRIVATE_KEY) {
    return { ok: false, error: "Apple revoke 설정이 누락되었습니다." };
  }

  const clientSecret = await createAppleClientSecret();
  const tokenResponse = await fetch("https://appleid.apple.com/auth/token", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams({
      grant_type: "authorization_code",
      code: authorizationCode,
      client_id: APPLE_CLIENT_ID,
      client_secret: clientSecret,
    }),
  });

  if (!tokenResponse.ok) {
    const body = await tokenResponse.text();
    return {
      ok: false,
      error: extractAppleError(body) ?? "Apple 재인증 정보를 확인하지 못했습니다.",
    };
  }

  const tokenData = await tokenResponse.json() as { refresh_token?: string };
  const refreshToken = tokenData.refresh_token?.trim();
  if (!refreshToken) {
    return { ok: false, error: "Apple refresh token을 발급받지 못했습니다." };
  }

  const revokeResponse = await fetch("https://appleid.apple.com/auth/revoke", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams({
      token: refreshToken,
      token_type_hint: "refresh_token",
      client_id: APPLE_CLIENT_ID,
      client_secret: clientSecret,
    }),
  });

  if (!revokeResponse.ok) {
    const body = await revokeResponse.text();
    return {
      ok: false,
      error: extractAppleError(body) ?? "Apple 계정 연결 해제에 실패했습니다.",
    };
  }

  return { ok: true };
}

async function createAppleClientSecret(): Promise<string> {
  const privateKey = APPLE_PRIVATE_KEY.includes("\\n")
    ? APPLE_PRIVATE_KEY.replace(/\\n/g, "\n")
    : APPLE_PRIVATE_KEY;
  const signingKey = await importPKCS8(privateKey, "ES256");
  const now = Math.floor(Date.now() / 1000);

  return await new SignJWT({})
    .setProtectedHeader({
      alg: "ES256",
      kid: APPLE_KEY_ID,
    })
    .setIssuer(APPLE_TEAM_ID)
    .setSubject(APPLE_CLIENT_ID)
    .setAudience("https://appleid.apple.com")
    .setIssuedAt(now)
    .setExpirationTime(now + 300)
    .sign(signingKey);
}

function extractAppleError(rawBody: string): string | null {
  if (!rawBody.trim()) return null;
  try {
    const parsed = JSON.parse(rawBody) as { error?: string; error_description?: string };
    return parsed.error_description?.trim() || parsed.error?.trim() || null;
  } catch {
    return rawBody.trim().slice(0, 200);
  }
}

function resolveUserProvider(user: {
  app_metadata?: Record<string, unknown> | null;
  identities?: Array<{ provider?: string | null }> | null;
}): string {
  const metadataProvider = typeof user.app_metadata?.provider === "string"
    ? normalizeProviderName(user.app_metadata.provider)
    : null;
  if (metadataProvider) return metadataProvider;

  for (const identity of user.identities ?? []) {
    const provider = normalizeProviderName(identity.provider);
    if (provider) return provider;
  }

  const providers = Array.isArray(user.app_metadata?.providers)
    ? user.app_metadata?.providers
    : [];
  for (const providerEntry of providers) {
    if (typeof providerEntry !== "string") continue;
    const provider = normalizeProviderName(providerEntry);
    if (provider) return provider;
  }

  return "unknown";
}

function normalizeProviderName(value: string | null | undefined): string | null {
  const normalized = value?.trim().toLowerCase();
  return normalized === "apple" || normalized === "google" || normalized === "kakao"
    ? normalized
    : null;
}

function jsonResponse(payload: unknown, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: {
      "Content-Type": "application/json",
    },
  });
}
