# Базовый образ Nginx для раздачи статических файлов
FROM nginx:alpine

# Копирование собранного фронтенда в директорию Nginx
COPY build /usr/share/nginx/html

# Копирование кастомного конфигурационного файла Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Открытие порта для доступа к контейнеру
EXPOSE 80

# Запуск Nginx
CMD ["nginx", "-g", "daemon off;"]