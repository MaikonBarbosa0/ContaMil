FROM node:26-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/contamil/browser /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=5s --timeout=3s --start-period=5s --retries=12 CMD wget -q -O /dev/null http://127.0.0.1/api/health || exit 1
