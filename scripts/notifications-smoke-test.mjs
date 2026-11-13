#!/usr/bin/env node
// End-to-end smoke test for the notifications API (webserver#144).
//
// Needs the webserver running locally with the dev profile and Turnstile off. From webserver/:
//   docker compose -f docker-compose-dev.yml up -d
//   ./gradlew bootRun --args="--spring.profiles.active=dev --turnstile.enabled=false"
// Then, in another terminal from webserver/:
//   node scripts/notifications-smoke-test.mjs
//
// Every run creates fresh users/competitions with a random suffix, so it can be re-run against the
// same database. Exits non-zero if any check fails. Signup leaves accounts unconfirmed, so the
// script confirms them in the dev auth database via `docker exec` (container name overridable with
// AUTH_DB_CONTAINER).

import { execFileSync } from "node:child_process";

const API = process.env.API_URL ?? "http://localhost:8080";
const AUTH = process.env.AUTH_URL ?? "http://localhost:9999";
const ADMIN_EMAIL = "admin@example.com";
const ADMIN_PASSWORD = "password";
const run = Math.random().toString(36).slice(2, 8);

let passed = 0;
let failed = 0;

function check(name, condition, detail) {
  if (condition) {
    passed++;
    console.log(`  PASS  ${name}`);
  } else {
    failed++;
    console.log(`  FAIL  ${name}${detail === undefined ? "" : `\n        got: ${JSON.stringify(detail)}`}`);
  }
}

function section(title) {
  console.log(`\n== ${title}`);
}

async function call(method, path, { token, body } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const res = await fetch(`${API}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let json;
  try {
    json = text ? JSON.parse(text) : undefined;
  } catch {
    json = text;
  }
  return { status: res.status, json, headers: res.headers };
}

async function expectOk(method, path, opts, expected = 200) {
  const res = await call(method, path, opts);
  if (res.status !== expected) {
    const message = res.json?.message ?? JSON.stringify(res.json);
    throw new Error(`${method} ${path} -> ${res.status}: ${String(message).slice(0, 300)}`);
  }
  return res.json;
}

async function login(email, password) {
  const res = await fetch(`${AUTH}/token?grant_type=password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(`login ${email} -> ${res.status}: ${JSON.stringify(json)}`);
  return json.access_token;
}

/**
 * Signup leaves the Supabase account unconfirmed (prod sends a confirmation email, the dev stack
 * has no mailer), so mark it confirmed directly in the local auth database container.
 */
function confirmEmail(email) {
  const container = process.env.AUTH_DB_CONTAINER ?? "bytefight-webserver-dev-auth-db-1";
  execFileSync("docker", [
    "exec", container, "psql", "-U", "postgres", "-q", "-c",
    `UPDATE auth.users SET email_confirmed_at = now() WHERE email = '${email}'`,
  ]);
}

async function signupAndLogin(name) {
  const email = `${name}-${run}@example.com`;
  await expectOk("POST", "/api/v1/auth/signup", {
    body: { email, password: "password123", name: `${name}_${run}` },
  });
  confirmEmail(email);
  return login(email, "password123");
}

const titles = (list) => list.map((n) => n.title).sort();
const t = (label) => `${label} [${run}]`;

async function main() {
  section("Setup");
  const admin = await login(ADMIN_EMAIL, ADMIN_PASSWORD);
  const alice = await signupAndLogin("alice"); // will be on a team in demoComp
  const bob = await signupAndLogin("bob"); // has a player but no team
  console.log("  logged in as admin, alice and bob");

  const createCompetition = (slug) =>
    expectOk("POST", "/api/v1/admin/competition", { token: admin, body: { slug, name: slug } }, 201);
  const demoComp = await createCompetition(`notif_demo_${run}`);
  const otherComp = await createCompetition(`notif_other_${run}`);
  await expectOk("PATCH", `/api/v1/admin/competition/${demoComp.id}`, {
    token: admin,
    body: { isActive: true, allowCreateTeam: true },
  });
  await expectOk("POST", `/api/v1/competition/${demoComp.slug}/teams`, {
    token: alice,
    // displayMembers is required in practice: leaving it out 500s in TeamService.createTeam.
    body: { name: `Alice Team ${run}`, displayMembers: false },
  });
  console.log(`  competitions ${demoComp.slug} (alice's team) and ${otherComp.slug}`);

  section("Admin CRUD");
  const create = (body) => call("POST", "/api/v1/admin/notification", { token: admin, body });

  const siteNormal = await create({ title: t("Site normal"), body: "Welcome to **ByteFight**" });
  check("create returns 201", siteNormal.status === 201, siteNormal.status);
  check("priority defaults to NORMAL", siteNormal.json.priority === "NORMAL", siteNormal.json);
  check("createdByEmail is the admin", siteNormal.json.createdByEmail === ADMIN_EMAIL, siteNormal.json);

  const siteUrgent = (await create({ title: t("Site urgent"), body: "Maintenance tonight", priority: "URGENT" })).json;
  const demoNormal = (await create({ title: t("Demo normal"), body: "Deadline Friday", competitionId: demoComp.id })).json;
  const demoUrgent = (await create({ title: t("Demo urgent"), body: "Deadline moved", priority: "URGENT", competitionId: demoComp.id })).json;
  const otherNormal = (await create({ title: t("Other normal"), body: "Not for alice", competitionId: otherComp.id })).json;
  const expiring = (await create({
    title: t("Expires in 3s"),
    body: "gone soon",
    expireAt: new Date(Date.now() + 3000).toISOString(),
  })).json;

  const got = await call("GET", `/api/v1/admin/notification/${demoNormal.id}`, { token: admin });
  check("GET one returns the record", got.status === 200 && got.json.title === t("Demo normal"), got.json);

  const list = await call("GET", `/api/v1/admin/notification?range=${encodeURIComponent("[0,9]")}`, { token: admin });
  check("list has a Content-Range header", /^notification \d+-\d+\/\d+$/.test(list.headers.get("content-range") ?? ""), list.headers.get("content-range"));
  check("list is newest first", list.json[0].id === expiring.id, list.json.slice(0, 2));

  const urgentOnly = await call("GET", `/api/v1/admin/notification?filter=${encodeURIComponent(JSON.stringify({ priority: "URGENT", q: run }))}`, { token: admin });
  check("filter priority=URGENT + q", JSON.stringify(titles(urgentOnly.json)) === JSON.stringify([t("Demo urgent"), t("Site urgent")].sort()), titles(urgentOnly.json));

  const byComp = await call("GET", `/api/v1/admin/notification?filter=${encodeURIComponent(JSON.stringify({ competitionId: demoComp.id }))}`, { token: admin });
  check("filter competitionId", byComp.json.length === 2, titles(byComp.json));

  section("Admin validation and access");
  check("past expireAt -> 400", (await create({ title: "x", body: "x", expireAt: "2000-01-01T00:00:00Z" })).status === 400);
  check("title over 200 chars -> 400", (await create({ title: "x".repeat(201), body: "x" })).status === 400);
  check("missing body -> 400", (await create({ title: "x" })).status === 400);
  check("unknown competition -> 404", (await create({ title: "x", body: "x", competitionId: 999999999 })).status === 404);
  check("non-admin list -> 403", (await call("GET", "/api/v1/admin/notification", { token: alice })).status === 403);
  check("non-admin create -> 403", (await call("POST", "/api/v1/admin/notification", { token: alice, body: { title: "x", body: "x" } })).status === 403);

  section("User feed (alice is on a team in the demo competition)");
  const aliceFeed = await call("GET", "/api/v1/notification?size=50", { token: alice });
  const aliceTitles = titles(aliceFeed.json.content).filter((x) => x.includes(run));
  check(
    "alice sees site-wide + demo, not other",
    JSON.stringify(aliceTitles) === JSON.stringify([t("Demo normal"), t("Demo urgent"), t("Expires in 3s"), t("Site normal"), t("Site urgent")].sort()),
    aliceTitles,
  );
  const demoItem = aliceFeed.json.content.find((n) => n.id === demoNormal.id);
  check("feed item carries competition slug + name", demoItem?.competitionSlug === demoComp.slug && !!demoItem?.competitionName, demoItem);
  check("feed items start unread", aliceFeed.json.content.filter((n) => n.title.includes(run)).every((n) => n.unread), aliceFeed.json.content.map((n) => n.unread));
  check("feed is a page with metadata", typeof aliceFeed.json.page?.totalElements === "number", aliceFeed.json.page);

  const bobFeed = await call("GET", "/api/v1/notification?size=50", { token: bob });
  const bobTitles = titles(bobFeed.json.content).filter((x) => x.includes(run));
  check("bob (no team) sees site-wide only", JSON.stringify(bobTitles) === JSON.stringify([t("Expires in 3s"), t("Site normal"), t("Site urgent")].sort()), bobTitles);

  check("anonymous feed -> 401", (await call("GET", "/api/v1/notification")).status === 401);

  section("Unread count and mark-read");
  const count1 = (await call("GET", "/api/v1/notification/unread-count", { token: alice })).json.count;
  check("alice unread count = 5", count1 === 5, count1);
  const mark = await call("POST", "/api/v1/notification/mark-read", { token: alice });
  check("mark-read -> 204", mark.status === 204, mark.status);
  const count2 = (await call("GET", "/api/v1/notification/unread-count", { token: alice })).json.count;
  check("unread count drops to 0", count2 === 0, count2);
  const afterRead = await call("GET", "/api/v1/notification?size=50", { token: alice });
  check("feed items now read", afterRead.json.content.every((n) => !n.unread));

  await call("PATCH", `/api/v1/admin/notification/${siteNormal.json.id}`, { token: admin, body: { title: t("Site normal (edited)") } });
  const count3 = (await call("GET", "/api/v1/notification/unread-count", { token: alice })).json.count;
  check("editing does NOT make it unread again", count3 === 0, count3);

  const later = (await create({ title: t("Later"), body: "new after mark-read" })).json;
  const count4 = (await call("GET", "/api/v1/notification/unread-count", { token: alice })).json.count;
  check("a new notification counts as unread", count4 === 1, count4);

  section("Urgent banner");
  const aliceUrgent = await call("GET", "/api/v1/notification/urgent", { token: alice });
  check(
    "alice urgent = site urgent + demo urgent",
    JSON.stringify(titles(aliceUrgent.json).filter((x) => x.includes(run))) === JSON.stringify([t("Demo urgent"), t("Site urgent")].sort()),
    titles(aliceUrgent.json),
  );
  const publicUrgent = await call("GET", "/api/v1/public/notification/urgent");
  const publicTitles = titles(publicUrgent.json).filter((x) => x.includes(run));
  check("public urgent works without login", publicUrgent.status === 200, publicUrgent.status);
  check("public urgent is site-wide only", JSON.stringify(publicTitles) === JSON.stringify([t("Site urgent")]), publicTitles);

  section("PATCH clearing nullable fields");
  const cleared = await call("PATCH", `/api/v1/admin/notification/${otherNormal.id}`, { token: admin, body: { competitionId: null } });
  check("competitionId: null makes it site-wide", cleared.status === 200 && cleared.json.competitionId === null, cleared.json);
  const bobFeed2 = await call("GET", "/api/v1/notification?size=50", { token: bob });
  check("bob now sees the formerly scoped one", bobFeed2.json.content.some((n) => n.id === otherNormal.id));
  const titleOnly = await call("PATCH", `/api/v1/admin/notification/${demoNormal.id}`, { token: admin, body: { title: t("Demo normal (renamed)") } });
  check("PATCH without competitionId keeps the competition", titleOnly.json.competitionId === demoComp.id, titleOnly.json);

  section("Expiry");
  const msLeft = new Date(expiring.expireAt).getTime() - Date.now();
  if (msLeft > 0) await new Promise((r) => setTimeout(r, msLeft + 500));
  const aliceFeed2 = await call("GET", "/api/v1/notification?size=50", { token: alice });
  check("expired one is gone from the feed", !aliceFeed2.json.content.some((n) => n.id === expiring.id));
  const adminSeesExpired = await call("GET", `/api/v1/admin/notification/${expiring.id}`, { token: admin });
  check("admin still sees the expired one", adminSeesExpired.status === 200);

  section("Delete");
  const del = await call("DELETE", `/api/v1/admin/notification/${later.id}`, { token: admin });
  check("DELETE -> 200 with the deleted record", del.status === 200 && del.json.id === later.id, del);
  check("GET after delete -> 404", (await call("GET", `/api/v1/admin/notification/${later.id}`, { token: admin })).status === 404);
  const count5 = (await call("GET", "/api/v1/notification/unread-count", { token: alice })).json.count;
  check("deleted notification no longer counts as unread", count5 === 0, count5);

  console.log(`\n${passed} passed, ${failed} failed (run id ${run})`);
  process.exit(failed === 0 ? 0 : 1);
}

main().catch((err) => {
  console.error(`\nAborted: ${err.message}`);
  process.exit(2);
});
