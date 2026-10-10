# syntax=docker/dockerfile:1.7
FROM node:22.13.0-bookworm-slim AS dependencies
WORKDIR /workspace
ENV SHARP_IGNORE_GLOBAL_LIBVIPS=1
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --workspaces=false --include=dev --include=optional --no-audit --no-fund

FROM dependencies AS build
COPY . .
ARG NEXT_PUBLIC_API_URL=/api/v1
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}
RUN npm run build

FROM node:22.13.0-bookworm-slim
WORKDIR /app
ENV NODE_ENV=production PORT=3000 HOME=/tmp
COPY --from=build --chown=node:node /workspace/dist ./dist
COPY --chown=node:node frontend/server.mjs ./server.mjs
USER node
EXPOSE 3000
HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=12 \
    CMD ["node", "-e", "fetch('http://127.0.0.1:3000/').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"]
ENTRYPOINT ["node", "/app/server.mjs"]
