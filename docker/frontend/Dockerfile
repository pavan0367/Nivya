# ==============================================================================
# Multi-Stage Dockerfile for Nivya React Frontend (Node 20 / Vite / Nginx)
# ==============================================================================

# Stage 1: Build Stage
FROM node:20-alpine AS builder
WORKDIR /app

# Cache dependency installations
COPY web/package.json web/package-lock.json ./
RUN npm ci

# Copy web source files
COPY web/ ./

# Accept build arguments for environment-specific URLs if necessary
ARG VITE_API_BASE_URL=""
ARG VITE_WS_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_BASE_URL=$VITE_WS_BASE_URL

# Build optimized production bundle
RUN npm run build

# Stage 2: Minimal Static Web Server Stage
FROM nginx:1.27-alpine AS runner

# Remove default Nginx welcome page and configs
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/default.conf

# Copy production bundle from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy internal Nginx configuration
COPY docker/frontend/nginx-frontend.conf /etc/nginx/conf.d/default.conf

# Expose internal frontend container port
EXPOSE 80

# Health check via /healthz endpoint
HEALTHCHECK --interval=20s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q --spider http://localhost:80/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
