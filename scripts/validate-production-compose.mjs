import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import yaml from "js-yaml";

const root = path.resolve(import.meta.dirname, "..");
const composePath = path.join(root, "compose.production.yaml");
const compose = yaml.load(fs.readFileSync(composePath, "utf8"));
const dockerfile = fs.readFileSync(path.join(root, "backend", "Dockerfile"), "utf8");
const backupScript = fs.readFileSync(path.join(root, "scripts", "backup-production.ps1"), "utf8");
const restoreScript = fs.readFileSync(path.join(root, "scripts", "restore-production.ps1"), "utf8");

function requireCondition(condition, message) {
  if (!condition) throw new Error(`Invalid production Compose: ${message}`);
}

const postgres = compose?.services?.postgres;
const backend = compose?.services?.backend;
requireCondition(postgres && backend, "postgres and backend services are required");
requireCondition(!postgres.ports, "PostgreSQL must not publish a host port");
requireCondition(postgres.networks?.includes("database"), "PostgreSQL must use the private database network");
requireCondition(compose.networks?.database?.internal === true, "database network must be internal");
requireCondition(backend.networks?.includes("database"), "backend must reach the database network");
requireCondition(backend.networks?.includes("egress"), "backend needs controlled provider egress");
requireCondition(backend.read_only === true, "backend root filesystem must be read-only");
requireCondition(backend.cap_drop?.includes("ALL"), "backend must drop Linux capabilities");
requireCondition(
  backend.security_opt?.includes("no-new-privileges:true"),
  "backend must prohibit privilege escalation",
);
requireCondition(backend.healthcheck?.test?.includes("curl"), "backend readiness healthcheck is required");
requireCondition(backend.volumes?.some((item) => item.includes("bridgeflow-documents")), "document volume is required");
requireCondition(postgres.volumes?.some((item) => item.includes("bridgeflow-postgres")), "database volume is required");
requireCondition(/^USER bridgeflow$/m.test(dockerfile), "backend image must run as the bridgeflow user");
requireCondition(backupScript.includes("pg_dump --clean --if-exists --create"), "backup must contain a restorable database dump");
requireCondition(backupScript.includes("databaseSha256"), "backup manifest must contain a database checksum");
requireCondition(restoreScript.includes("[ValidateSet('RESTORE')]"), "restore must require explicit confirmation");
requireCondition(restoreScript.includes("Get-FileHash"), "restore must verify the database checksum");

console.log("Production Compose semantic validation passed.");
if (process.platform === "win32") {
  console.log("Runtime Docker validation remains required on the deployment host.");
}
