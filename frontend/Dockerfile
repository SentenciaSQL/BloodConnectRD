# syntax=docker/dockerfile:1

FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=4000
COPY --from=build /app/dist/frontend ./dist/frontend
COPY --from=build /app/package.json ./
EXPOSE 4000
HEALTHCHECK --interval=20s --timeout=5s --retries=5 \
  CMD node -e "fetch('http://127.0.0.1:4000/').then((r)=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["node", "dist/frontend/server/server.mjs"]
