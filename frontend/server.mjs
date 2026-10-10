import http from "node:http";
import path from "node:path";
import { createReadStream } from "node:fs";
import { stat } from "node:fs/promises";
import { Readable } from "node:stream";
import { fileURLToPath, pathToFileURL } from "node:url";

const appRoot = path.dirname(fileURLToPath(import.meta.url));
const artifactRoot = path.resolve(process.env.BRIDGEFLOW_FRONTEND_ROOT ?? appRoot);
const clientRoot = path.resolve(artifactRoot, "dist/client");
const worker = (await import(pathToFileURL(path.join(artifactRoot, "dist/server/index.js")).href)).default;
const port = Number.parseInt(process.env.PORT ?? "3000", 10);

const contentTypes = new Map([
  [".css", "text/css; charset=utf-8"],
  [".gif", "image/gif"],
  [".html", "text/html; charset=utf-8"],
  [".ico", "image/x-icon"],
  [".jpeg", "image/jpeg"],
  [".jpg", "image/jpeg"],
  [".js", "text/javascript; charset=utf-8"],
  [".json", "application/json; charset=utf-8"],
  [".png", "image/png"],
  [".svg", "image/svg+xml"],
  [".txt", "text/plain; charset=utf-8"],
  [".webp", "image/webp"],
  [".woff", "font/woff"],
  [".woff2", "font/woff2"],
]);

function requestUrl(request) {
  const forwardedProtocol = request.headers["x-forwarded-proto"]?.split(",")[0].trim();
  const forwardedHost = request.headers["x-forwarded-host"]?.split(",")[0].trim();
  const protocol = forwardedProtocol || "http";
  const host = forwardedHost || request.headers.host || "localhost";
  return new URL(request.url || "/", `${protocol}://${host}`);
}

async function staticFile(pathname) {
  let decoded;
  try {
    decoded = decodeURIComponent(pathname);
  } catch {
    return null;
  }
  const candidate = path.resolve(clientRoot, `.${decoded}`);
  if (candidate !== clientRoot && !candidate.startsWith(`${clientRoot}${path.sep}`)) return null;
  try {
    const details = await stat(candidate);
    return details.isFile() ? { candidate, details } : null;
  } catch (error) {
    if (error.code === "ENOENT" || error.code === "ENOTDIR") return null;
    throw error;
  }
}

async function handle(request, response) {
  const url = requestUrl(request);
  const asset = await staticFile(url.pathname);
  if (asset) {
    response.statusCode = 200;
    response.setHeader("Content-Type", contentTypes.get(path.extname(asset.candidate)) ?? "application/octet-stream");
    response.setHeader("Content-Length", asset.details.size);
    response.setHeader(
      "Cache-Control",
      url.pathname.startsWith("/_next/static/")
        ? "public, max-age=31536000, immutable"
        : "public, max-age=3600",
    );
    if (request.method === "HEAD") return response.end();
    return createReadStream(asset.candidate).pipe(response);
  }

  const headers = new Headers();
  for (const [name, value] of Object.entries(request.headers)) {
    for (const item of Array.isArray(value) ? value : [value]) {
      if (item !== undefined) headers.append(name, item);
    }
  }
  const init = { method: request.method, headers };
  if (request.method !== "GET" && request.method !== "HEAD") {
    init.body = Readable.toWeb(request);
    init.duplex = "half";
  }

  const pending = [];
  const executionContext = {
    props: {},
    passThroughOnException() {},
    waitUntil(promise) {
      pending.push(Promise.resolve(promise));
    },
  };
  const workerResponse = await worker.fetch(new Request(url, init), {}, executionContext);
  response.statusCode = workerResponse.status;
  response.statusMessage = workerResponse.statusText;
  workerResponse.headers.forEach((value, name) => {
    if (name !== "set-cookie") response.setHeader(name, value);
  });
  const cookies = workerResponse.headers.getSetCookie?.() ?? [];
  if (cookies.length) response.setHeader("Set-Cookie", cookies);

  Promise.allSettled(pending).catch(() => {});
  if (request.method === "HEAD" || !workerResponse.body) return response.end();
  Readable.fromWeb(workerResponse.body).pipe(response);
}

const server = http.createServer((request, response) => {
  handle(request, response).catch((error) => {
    console.error("Frontend request failed", error);
    if (!response.headersSent) {
      response.statusCode = 500;
      response.setHeader("Content-Type", "text/plain; charset=utf-8");
    }
    response.end("Internal Server Error");
  });
});
server.headersTimeout = 10_000;
server.requestTimeout = 30_000;
server.keepAliveTimeout = 5_000;

server.listen(port, "0.0.0.0", () => {
  console.log(`BridgeFlow frontend listening on port ${port}`);
});

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => server.close(() => process.exit(0)));
}
