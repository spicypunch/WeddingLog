import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";
import { GoogleAuth } from "npm:google-auth-library@9";

type ActivityType = "expense_added" | "schedule_added";

type PartnerActivityRequest = {
  type?: string;
  entity_id?: string;
};

type ActorProfile = {
  id: string;
  wedding_id: string | null;
  role: string | null;
  name: string | null;
};

type ActivityContext = {
  actorUserId: string;
  weddingId: string;
  title: string;
  body: string;
};

type ExpenseRow = {
  id: string;
  amount: number;
  category_id: string | null;
  budget_item_id: string | null;
  created_by: string | null;
  wedding_id: string | null;
};

type ScheduleRow = {
  id: string;
  title: string;
  event_date: string;
  created_by: string | null;
  wedding_id: string | null;
};

type NotificationInsertRow = {
  wedding_id: string;
  actor_user_id: string;
  target_user_id: string;
  type: ActivityType;
  entity_id: string;
  title: string;
  body: string;
};

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const FCM_SERVICE_ACCOUNT_JSON = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON") ?? "";

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  if (!SUPABASE_URL || !SUPABASE_ANON_KEY || !SUPABASE_SERVICE_ROLE_KEY) {
    return jsonResponse({ error: "Missing Supabase environment" }, 500);
  }

  const adminClient = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

  const body = (await req.json().catch(() => ({}))) as PartnerActivityRequest;
  const type = normalizeType(body.type);
  const entityId = body.entity_id?.trim();
  if (!type || !entityId) {
    return jsonResponse({ error: "Invalid request body" }, 400);
  }

  const activityContext = await resolveActivityContext({
    adminClient,
    type,
    entityId,
  });
  if (!activityContext) {
    return jsonResponse({ recipients: 0, notifications: 0, push_sent: 0, push_skipped: true });
  }

  const { data: recipientProfiles, error: recipientError } = await adminClient
    .from("profiles")
    .select("id")
    .eq("wedding_id", activityContext.weddingId)
    .neq("id", activityContext.actorUserId);

  if (recipientError) {
    return jsonResponse({ error: recipientError.message }, 500);
  }

  const recipientIds = (recipientProfiles ?? [])
    .map((row) => String((row as { id?: string }).id ?? "").trim())
    .filter(Boolean);

  if (recipientIds.length == 0) {
    return jsonResponse({ recipients: 0, notifications: 0, push_sent: 0, push_skipped: true });
  }

  const rows: NotificationInsertRow[] = recipientIds.map((targetUserId) => ({
    wedding_id: activityContext.weddingId,
    actor_user_id: activityContext.actorUserId,
    target_user_id: targetUserId,
    type,
    entity_id: entityId,
    title: activityContext.title,
    body: activityContext.body,
  }));

  const { data: insertedNotifications, error: insertError } = await adminClient
    .from("partner_activity_notifications")
    .insert(rows)
    .select("id,target_user_id");

  if (insertError) {
    return jsonResponse({ error: insertError.message }, 500);
  }

  if (!FCM_SERVICE_ACCOUNT_JSON.trim()) {
    return jsonResponse({
      recipients: recipientIds.length,
      notifications: insertedNotifications?.length ?? 0,
      push_sent: 0,
      push_skipped: true,
    });
  }

  const { data: deviceTokens, error: tokenError } = await adminClient
    .from("device_push_tokens")
    .select("user_id,push_token")
    .in("user_id", recipientIds)
    .eq("is_active", true);

  if (tokenError || !deviceTokens || deviceTokens.length === 0) {
    return jsonResponse({
      recipients: recipientIds.length,
      notifications: insertedNotifications?.length ?? 0,
      push_sent: 0,
      push_skipped: true,
    });
  }

  const credentials = JSON.parse(FCM_SERVICE_ACCOUNT_JSON) as {
    project_id?: string;
  };
  const projectId = credentials.project_id?.trim();
  if (!projectId) {
    return jsonResponse({ error: "FCM_SERVICE_ACCOUNT_JSON.project_id missing" }, 500);
  }

  const auth = new GoogleAuth({
    credentials: JSON.parse(FCM_SERVICE_ACCOUNT_JSON),
    scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
  });
  const accessToken = await resolveGoogleAccessToken(auth);
  if (!accessToken) {
    return jsonResponse({
      recipients: recipientIds.length,
      notifications: insertedNotifications?.length ?? 0,
      push_sent: 0,
      push_skipped: true,
    });
  }

  const notificationIdsByTarget = new Map<string, string[]>();
  for (const row of insertedNotifications ?? []) {
    const targetUserId = String((row as { target_user_id?: string }).target_user_id ?? "");
    const notificationId = String((row as { id?: string }).id ?? "");
    if (!targetUserId || !notificationId) continue;
    const current = notificationIdsByTarget.get(targetUserId) ?? [];
    current.push(notificationId);
    notificationIdsByTarget.set(targetUserId, current);
  }

  let pushSentCount = 0;
  const deliveredNotificationIds = new Set<string>();
  for (const tokenRow of deviceTokens) {
    const targetUserId = String((tokenRow as { user_id?: string }).user_id ?? "");
    const pushToken = String((tokenRow as { push_token?: string }).push_token ?? "");
    if (!targetUserId || !pushToken) continue;

    const pushed = await sendFcmMessage({
      accessToken,
      projectId,
      pushToken,
      title: activityContext.title,
      body: activityContext.body,
      type,
      entityId,
    });
    if (!pushed) continue;

    pushSentCount += 1;
    for (const notificationId of notificationIdsByTarget.get(targetUserId) ?? []) {
      deliveredNotificationIds.add(notificationId);
    }
  }

  if (deliveredNotificationIds.size > 0) {
    await adminClient
      .from("partner_activity_notifications")
      .update({ pushed_at: new Date().toISOString() })
      .in("id", Array.from(deliveredNotificationIds));
  }

  return jsonResponse({
    recipients: recipientIds.length,
    notifications: insertedNotifications?.length ?? 0,
    push_sent: pushSentCount,
    push_skipped: false,
  });
});

async function resolveActivityContext({
  adminClient,
  type,
  entityId,
}: {
  adminClient: ReturnType<typeof createClient>;
  type: ActivityType;
  entityId: string;
}): Promise<ActivityContext | null> {
  if (type === "expense_added") {
    const { data: expense } = await adminClient
      .from("expenses")
      .select("id,amount,category_id,budget_item_id,created_by,wedding_id")
      .eq("id", entityId)
      .maybeSingle<ExpenseRow>();
    if (!expense?.created_by || !expense.wedding_id) return null;

    const actorProfile = await fetchActorProfile(adminClient, expense.created_by);
    const actorLabel = roleToLabel(actorProfile?.role ?? null, actorProfile?.name ?? null);

    const [categoryName, budgetItemName] = await Promise.all([
      fetchNameById(adminClient, "categories", expense.category_id),
      fetchNameById(adminClient, "budget_items", expense.budget_item_id),
    ]);
    const parts = [categoryName, budgetItemName, formatKrw(expense.amount)].filter(Boolean);
    return {
      actorUserId: expense.created_by,
      weddingId: expense.wedding_id,
      title: `${actorLabel} 지출을 추가했어요`,
      body: parts.join(" · "),
    };
  }

  const { data: schedule } = await adminClient
    .from("schedule_events")
    .select("id,title,event_date,created_by,wedding_id")
    .eq("id", entityId)
    .maybeSingle<ScheduleRow>();
  if (!schedule?.created_by || !schedule.wedding_id) return null;

  const actorProfile = await fetchActorProfile(adminClient, schedule.created_by);
  const actorLabel = roleToLabel(actorProfile?.role ?? null, actorProfile?.name ?? null);

  return {
    actorUserId: schedule.created_by,
    weddingId: schedule.wedding_id,
    title: `${actorLabel} 일정을 추가했어요`,
    body: `${schedule.title} · ${formatDate(schedule.event_date)}`,
  };
}

async function fetchActorProfile(
  adminClient: ReturnType<typeof createClient>,
  actorUserId: string,
): Promise<ActorProfile | null> {
  const { data } = await adminClient
    .from("profiles")
    .select("id,wedding_id,role,name")
    .eq("id", actorUserId)
    .maybeSingle<ActorProfile>();
  return data ?? null;
}

async function fetchNameById(
  adminClient: ReturnType<typeof createClient>,
  table: "categories" | "budget_items",
  id: string | null,
): Promise<string | null> {
  const normalizedId = id?.trim();
  if (!normalizedId) return null;
  const { data } = await adminClient
    .from(table)
    .select("name")
    .eq("id", normalizedId)
    .maybeSingle<{ name?: string | null }>();
  return data?.name?.trim() || null;
}

function normalizeType(rawType?: string): ActivityType | null {
  const normalized = rawType?.trim().toLowerCase();
  if (normalized === "expense_added" || normalized === "schedule_added") return normalized;
  return null;
}

function roleToLabel(role: string | null, fallbackName: string | null): string {
  switch ((role ?? "").trim().toLowerCase()) {
    case "groom":
      return "신랑이";
    case "bride":
      return "신부가";
    default:
      return `${(fallbackName ?? "파트너").trim() || "파트너"}가`;
  }
}

function formatKrw(amount: number): string {
  return `₩${Math.max(0, Math.trunc(amount)).toLocaleString("ko-KR")}`;
}

function formatDate(rawDate: string): string {
  const [year, month, day] = rawDate.split("-");
  if (!year || !month || !day) return rawDate;
  return `${year}.${month.padStart(2, "0")}.${day.padStart(2, "0")}`;
}

async function resolveGoogleAccessToken(auth: GoogleAuth): Promise<string | null> {
  const client = await auth.getClient();
  const accessToken = await client.getAccessToken();
  if (typeof accessToken === "string") return accessToken;
  return accessToken?.token ?? null;
}

async function sendFcmMessage({
  accessToken,
  projectId,
  pushToken,
  title,
  body,
  type,
  entityId,
}: {
  accessToken: string;
  projectId: string;
  pushToken: string;
  title: string;
  body: string;
  type: ActivityType;
  entityId: string;
}): Promise<boolean> {
  const response = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      message: {
        token: pushToken,
        notification: {
          title,
          body,
        },
        data: {
          type,
          entity_id: entityId,
        },
      },
    }),
  });

  if (response.ok) return true;
  console.error("FCM send failed", await response.text());
  return false;
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
