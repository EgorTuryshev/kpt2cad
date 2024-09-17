FROM node:18-alpine AS frontend-build

WORKDIR /app

# Копируем package.json и package-lock.json для установки зависимостей
COPY frontend/package*.json ./

# Устанавливаем зависимости
RUN npm install

# Копируем фронт
COPY frontend/ .

# Собираем React
RUN npm run build

# Собираем Spring Boot
FROM maven:3.9.0-eclipse-temurin-17-alpine AS backend-build

WORKDIR /app

# Копируем все файлы проекта
COPY backend/ .

# Собираем Spring Boot приложение
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Копируем собранное Spring Boot приложение
COPY --from=backend-build /app/target/*.jar app.jar

# Копируем собранное React приложение в папку ресурсов Spring Boot
COPY --from=frontend-build /app/build /app/resources/static

# Указываем команду для запуска приложения Spring Boot
CMD ["java", "-jar", "app.jar"]

# Открываем порт 8080 для доступа к приложению
EXPOSE 8080