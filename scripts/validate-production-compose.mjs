import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import yaml from "js-yaml";

const root = path.resolve(import.meta.dirname, "..");
const composePath = path.join(root, "compose.production.yaml");
const compose = yaml.load(fs.readFileSync(composePath, "utf8"));
const backendDockerfile = fs.readFileSync(path.join(root, "backend", "Dockerfile"), "utf8");
const frontendDockerfile = fs.readFileSync(path.join(root, "frontend", "Dockerfile"), "utf8");
const caddyfile = fs.readFileSync(path.join(root, "deploy", "Caddyfile"), "utf8");
const localCaddyfile = fs.readFileSync(path.join(root, "deploy", "Caddyfile.local"), "utf8");
const demoCaddyfile = fs.readFileSync(path.join(root, "deploy", "Caddyfile.demo"), "utf8");
const demoCompose = fs.readFileSync(path.join(root, "compose.demo-tunnel.yaml"), "utf8");
const startDemoScript = fs.readFileSync(path.join(root, "scripts", "start-demo-tunnel.ps1"), "utf8");
const stopDemoScript = fs.readFileSync(path.join(root, "scripts", "stop-demo-tunnel.ps1"), "utf8");
const backupScript = fs.readFileSync(path.join(root, "scripts", "backup-production.ps1"), "utf8");
const restoreScript = fs.readFileSync(path.join(root, "scripts", "restore-production.ps1"), "utf8");
const evalScript = fs.readFileSync(path.join(root, "scripts", "evaluate-ai-offline.mjs"), "utf8");
const evalCorpus = JSON.parse(fs.readFileSync(path.join(root, "eval", "corpus", "corpus-v1.json"), "utf8"));

function requireCondition(condition, message) {
  if (!condition) throw new Error(`Invalid production Compose: ${message}`);
}

const postgres = compose?.services?.postgres;
const backend = compose?.services?.backend;
const frontend = compose?.services?.frontend;
const gateway = compose?.services?.gateway;
requireCondition(postgres && backend && frontend && gateway, "gateway, frontend, backend, and postgres services are required");
requireCondition(!postgres.ports, "PostgreSQL must not publish a host port");
requireCondition(!backend.ports, "backend must not publish a host port");
requireCondition(!frontend.ports, "frontend must not publish a host port");
requireCondition(gateway.ports?.length === 2, "gateway must be the only HTTP/HTTPS entry point");
requireCondition(postgres.networks?.includes("database"), "PostgreSQL must use the private database network");
requireCondition(compose.networks?.database?.internal === true, "database network must be internal");
requireCondition(compose.networks?.application?.internal === true, "application network must be internal");
requireCondition(gateway.networks?.includes("edge") && gateway.networks?.includes("application"), "gateway must bridge edge and application networks");
requireCondition(frontend.networks?.length === 1 && frontend.networks[0] === "application", "frontend must remain on the private application network");
requireCondition(backend.networks?.includes("application"), "backend must use the private application network");
requireCondition(backend.networks?.includes("database"), "backend must reach the database network");
requireCondition(backend.networks?.includes("egress"), "backend needs controlled provider egress");
for (const [name, service] of [["gateway", gateway], ["frontend", frontend], ["backend", backend]]) {
  requireCondition(service.read_only === true, `${name} root filesystem must be read-only`);
  requireCondition(service.cap_drop?.includes("ALL"), `${name} must drop Linux capabilities`);
  requireCondition(service.security_opt?.includes("no-new-privileges:true"), `${name} must prohibit privilege escalation`);
}
requireCondition(backend.healthcheck?.test?.includes("curl"), "backend readiness healthcheck is required");
requireCondition(frontend.healthcheck?.test?.includes("node"), "frontend healthcheck is required");
requireCondition(gateway.healthcheck?.test?.includes("wget"), "gateway healthcheck is required");
requireCondition(backend.volumes?.some((item) => item.includes("bridgeflow-documents")), "document volume is required");
requireCondition(postgres.volumes?.some((item) => item.includes("bridgeflow-postgres")), "database volume is required");
requireCondition(/^USER bridgeflow$/m.test(backendDockerfile), "backend image must run as the bridgeflow user");
requireCondition(/^USER node$/m.test(frontendDockerfile), "frontend image must run as the node user");
requireCondition(frontendDockerfile.includes('ENTRYPOINT ["node", "/app/server.mjs"]'), "frontend image must run the built bundle without a development server");
requireCondition(frontend.build?.args?.NEXT_PUBLIC_API_URL === "/api/v1", "frontend API URL must use the same-origin gateway route");
requireCondition(gateway.image === "caddy:2.11.7-alpine", "gateway image must use the reviewed Caddy release");
requireCondition(caddyfile.includes("@api path /api/*"), "gateway must route API requests explicitly");
requireCondition(caddyfile.includes("reverse_proxy @api backend:8080"), "gateway must route API requests to backend");
requireCondition(caddyfile.includes("reverse_proxy frontend:3000"), "gateway must route web requests to frontend");
requireCondition(caddyfile.includes("Strict-Transport-Security"), "gateway must set HSTS");
requireCondition(!caddyfile.includes("tls internal"), "production gateway must use publicly trusted automatic HTTPS");
requireCondition(localCaddyfile.includes("tls internal"), "local gateway must use Caddy's internal test CA");
requireCondition(demoCaddyfile.includes("auto_https off"), "demo origin must leave public TLS termination to the tunnel");
requireCondition(demoCaddyfile.includes("@api path /api/*"), "demo gateway must retain same-origin API routing");
requireCondition(demoCaddyfile.includes("reverse_proxy @api backend:8080"), "demo gateway must route API requests privately");
requireCondition(demoCaddyfile.includes("reverse_proxy frontend:3000"), "demo gateway must route frontend requests privately");
requireCondition((demoCaddyfile.match(/header_up X-Forwarded-Proto https/g) ?? []).length === 2, "demo gateway must preserve the visitor HTTPS scheme for both upstreams");
requireCondition(demoCaddyfile.includes("Strict-Transport-Security"), "demo responses must retain reviewed security headers");
requireCondition(demoCompose.includes("ports: !override"), "demo must replace production public port bindings");
requireCondition(demoCompose.includes('127.0.0.1:${BRIDGEFLOW_DEMO_GATEWAY_PORT:-8082}:80'), "demo gateway must bind only to loopback");
requireCondition(demoCompose.includes("cloudflare/cloudflared:2026.9.3"), "demo tunnel image must use the reviewed pinned release");
requireCondition(demoCompose.includes("http://gateway:80"), "demo tunnel must target the private gateway service");
requireCondition(!/tunnel:[\s\S]*?ports:/m.test(demoCompose), "demo tunnel must not publish a host port");
requireCondition(startDemoScript.includes("bridgeflow-demo"), "demo start script must use an isolated Compose project");
requireCondition(startDemoScript.includes("RandomNumberGenerator"), "demo start script must generate a secret locally");
requireCondition(stopDemoScript.includes("--remove-orphans"), "demo stop script must clean its isolated containers");
requireCondition(backupScript.includes("pg_dump --clean --if-exists --create"), "backup must contain a restorable database dump");
requireCondition(backupScript.includes("databaseSha256"), "backup manifest must contain a database checksum");
requireCondition(restoreScript.includes("[ValidateSet('RESTORE')]"), "restore must require explicit confirmation");
requireCondition(restoreScript.includes("Get-FileHash"), "restore must verify the database checksum");
requireCondition(evalCorpus.version === "1.0.0", "offline AI evaluation corpus version 1.0.0 is required");
requireCondition(evalScript.includes("ai-component-evaluation.json"), "offline AI gate must consume production Java component evidence");
requireCondition(evalScript.includes("requirePassingSurefireTest"), "offline AI gate must consume named backend integration evidence");
requireCondition(!evalScript.includes("class StubRequirementExtractionProvider"), "offline AI gate must not reimplement the Java provider in JavaScript");

console.log("Production Compose semantic validation passed.");
if (process.platform === "win32") {
  console.log("Runtime Docker validation remains required on the deployment host.");
}
