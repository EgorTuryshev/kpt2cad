# Backend-ONLY решение
# Java 17 + Alpine
FROM eclipse-temurin:17-jre-alpine

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем скомпилированный .jar файл в контейнер
COPY app.jar /app/app.jar

# Папки для работы
RUN mkdir -p /app/output
COPY xsl /app/xsl
COPY prj /app/prj
COPY cpg /app/cpg

COPY CHANGELOG.md /app/CHANGELOG.md
RUN chmod -R 777 /app/xsl /app/output /app/prj /app/cpg /app/CHANGELOG.md && \
    chown -R 1000:1000 /app/xsl /app/output /app/prj /app/cpg /app/CHANGELOG.md

USER 1000

# Открываем порт 8080
EXPOSE 8080

# Указываем команду запуска
CMD ["java", "-jar", "/app/app.jar"]