FROM node:26-alpine AS build
WORKDIR /app
COPY package.json package-lock.json .npmrc ./
RUN npm ci
COPY . .
RUN npm run build

# adapter-node bundles every dependency into build/: the runtime image needs Node only.
FROM node:26-alpine
WORKDIR /app
ENV NODE_ENV=production \
    PORT=3000
COPY --from=build /app/package.json ./
COPY --from=build /app/build ./build
USER node
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s \
    CMD wget -qO /dev/null http://localhost:3000/login || exit 1
CMD ["node", "build"]
