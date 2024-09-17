# KPT2CAD

## Запуск

1. Соберите Docker образ:

   ```bash
   docker build -t kpt2cad .
   ```

2. Запустите контейнер:

   ```bash
   docker run -d -p 8080:8080 -p 3000:3000 kpt2cad
   ```

## Порты (внутренние)

- **API**: `8080`
- **Frontend**: `3000`

## Внешний вид веб-панели

![React Screenshot](react.png)