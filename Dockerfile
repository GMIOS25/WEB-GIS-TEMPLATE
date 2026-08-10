# syntax=docker/dockerfile:1
#
# Multi-stage build theo đúng mô hình đã chốt trong
# docs/en/DEPLOYMENT & FLEET STRATEGY.md (Section 3): FE + BE build thành DUY NHẤT
# 1 image runtime - Spring Boot phục vụ luôn cả /api/** lẫn static assets của React
# ("no separate Nginx required" - xem PROJECT_OVERVIEW.md Section 3.4).

# ---- Stage 1: Build Frontend (Vite/React) ----
FROM node:20-alpine AS fe-build
WORKDIR /fe
RUN corepack enable
# Pin cứng version pnpm theo đúng lockfileVersion đang dùng (pnpm-lock.yaml hiện tại
# là lockfileVersion 9.0) thay vì chỉ "corepack enable" suông - corepack không tự biết
# version nào nếu package.json không có field "packageManager", có thể fetch nhầm
# major version pnpm khác gây lệch lockfile.
RUN corepack prepare pnpm@9 --activate
COPY FE/package.json FE/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY FE/ .
RUN pnpm build
# Output: /fe/dist

# ---- Stage 2: Build Backend (Spring Boot) + embed FE assets ----
FROM maven:3.9-eclipse-temurin-17 AS be-build
WORKDIR /be
COPY BE/pom.xml ./
COPY BE/.mvn .mvn
COPY BE/mvnw ./
# Defensive chmod: mvnw cần bit thực thi. Repo đã fix ở mức git (100755), nhưng một số
# cách lấy source (vd. giải nén từ zip GitHub "Download ZIP" thay vì git clone) không
# giữ lại execute bit, nên chmod lại ở đây cho chắc thay vì phụ thuộc hoàn toàn vào
# git mode.
RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline
COPY BE/src ./src
# Embed the built React assets as Spring Boot static resources
COPY --from=fe-build /fe/dist ./src/main/resources/static
RUN ./mvnw -B clean package -DskipTests

# ---- Stage 3: Runtime ----
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S gis && adduser -S gis -G gis
WORKDIR /app
COPY --from=be-build /be/target/*.jar app.jar
USER gis
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
