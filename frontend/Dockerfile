# 阶段7：前端镜像（Node 构建产物交给 Nginx 托管）
FROM node:22-alpine AS build
WORKDIR /app

RUN npm install -g pnpm@11.28.5

# 只拷贝依赖清单，lockfile 不变时复用依赖层
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q --spider http://127.0.0.1/ || exit 1
