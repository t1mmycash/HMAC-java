# HMAC Service

Минимальный REST API сервер для подписи и проверки целостности сообщений с использованием HMAC-SHA256.

## 📋 Оглавление
- [Описание](#описание)
- [Требования](#требования)
- [Быстрый старт](#быстрый-старт)
- [API Эндпоинты](#api-эндпоинты)
- [Ротация секретов](#ротация секретов)
- [Тестирование](#тестирование)
- [Ограничения учебной реализации](#ограничения-учебной-реализации)
- [Архитектура проекта](#архитектура-проекта)
- [Безопасность](#безопасность)

## 🔍 Описание

Сервис реализует симметричную электронную подпись сообщений по алгоритму HMAC-SHA256. Позволяет:
- **Подписывать** сообщения с использованием общего секретного ключа
- **Проверять** целостность и подлинность подписанных сообщений

**Важно:** Это учебный проект, реализующий симметричный MAC (Message Authentication Code), а не асимметричную электронную подпись.

## 🛠️ Требования

- **Java**: версия 21 или выше
- **Память**: минимум 128 MB RAM
- **Порты**: свободный порт (по умолчанию 8080)
- **ОС**: Ubuntu или его wsl версия

Проверьте версию Java:
```bash
java -version
```

# 🚀 Быстрый старт
## 1. Генерация секретного ключа
Создайте секрет в формате base64
```bash
echo -n "your-secret-key-here" | base64
```
## 2. Создание конфигурации
Загрузите вашу конфигурацию в config.json
```bash
echo '{
"hmacAlg": "HmacSHA256",
"secret": "Ваш_Ключ",
"listenPort": 8080,
"maxMsgSizeBytes": 1048576
}' > config.json
```
## 3. Установка прав доступа (Linux/Mac)
```bash
   chmod 600 config.json
```
## 4. Компиляция проекта
```bash
   javac -cp "lib/*" -d target/classes   src/ru/yandex/practicum/*.java  
   src/ru/yandex/practicum/*/*.java
```
5. Запуск сервера
```bash
   java -cp "target/classes:lib/*" ru.yandex.practicum.ServerHMAC
```
   После успешного запуска вы увидите:

```text
[2024-01-19 12:00:00] INFO: ✅ HMAC Service started on http://localhost:8080
[2024-01-19 12:00:00] INFO: Available endpoints:
[2024-01-19 12:00:00] INFO:   POST http://localhost:8080/sign
[2024-01-19 12:00:00] INFO:   POST http://localhost:8080/verify
```
## Graceful shutdown
Сервер автоматически обрабатывает SIGINT (Ctrl+C) и SIGTERM.
# 📡 API Эндпоинты
## POST /sign - Подпись сообщения
Возможные ошибки:

```text
400 invalid_json - невалидный JSON

400 invalid_msg - отсутствует поле msg

413 message_too_large - сообщение превышает максимальный размер

415 unsupported_media_type - неверный Content-Type
```
Пример 1: Подпись сообщения
```bash
curl -v -X POST http://localhost:8080/sign \
-H 'Content-Type: application/json' \
-d '{"msg":"Hello, World!"}'
```
Ответ:
```json
{"signature":"7J28P4BZQmhC6dG8vX9wY2zA1bN3cK5jL7M0pQrS2tU4"}
```
## POST /verify - Проверка подписи
Возможные ошибки:
```text
400 invalid_json - невалидный JSON

400 invalid_msg - отсутствует поле msg

400 invalid_signature - отсутствует поле signature

400 invalid_signature_format - подпись не в формате base64url

400 invalid_signature_length - неверная длина подписи

413 message_too_large - сообщение превышает максимальный размер

415 unsupported_media_type - неверный Content-Type
```
Пример 2: Проверка верной подписи
```bash
curl -v -X POST http://localhost:8080/verify \
-H 'Content-Type: application/json' \
-d '{"msg":"Hello, World!","signature":"7J28P4BZQmhC6dG8vX9wY2zA1bN3cK5jL7M0pQrS2tU4"}'
```
Ответ:
```json
{"ok":true}
```
Пример 3: Проверка неверной подписи
```bash
curl -v -X POST http://localhost:8080/verify \
-H 'Content-Type: application/json' \
-d '{"msg":"Hello, World!","signature":"неправильная_подпись"}'
```
Ответ:
```json
{"ok":false}
```
Пример 4: Ошибка - неверный Content-Type
```bash
curl -v -X POST http://localhost:8080/sign \
-H 'Content-Type: text/plain' \
-d '{"msg":"test"}'
```
Ответ:

```http
HTTP/1.1 415 Unsupported Media Type
{"error":"unsupported_media_type"}
```
Пример 5: Ошибка - слишком большое сообщение
```bash
LARGE_MSG=$(python3 -c "print('A'*2000000)")
curl -v -X POST http://localhost:8080/sign \
-H 'Content-Type: application/json' \
-d "{\"msg\":\"$LARGE_MSG\"}"
```
Ответ:
```http
HTTP/1.1 413 Payload Too Large
{"error":"request_too_large"}
```
# Ротация секретов
Команда для ротации секрета изменит файл config.json, обновив ключ, а также сделает бэкап файл старого конфига в корне.

## Команда для ротации секрета
```bash
java -cp "target/classes:lib/*" ru.yandex.practicum.util.SecretRotator
````
После ротации необходим перезапуск сервера, так как секрет загружается в систему из файла config.json при запуске сервера

Пример восстановления старого ключа из бэкапа
```bash
cp config.json.backup_20240120_153045 config.json
```
# Тестирование
## Компиляция всех тестов
```bash
javac -cp "lib/*:target/classes" -d target/test-classes \
test/ru/yandex/practicum/*.java
```
## Запуск Юнит тестов
```bash
java -cp "target/test-classes:target/classes:lib/*" \
ru.yandex.practicum.TestRunner
```
## Запуск HTTP тестов
Возможен из IDE или с помощью библиотеки junit-platform-console-standalone.jar
```bash
java -jar junit-platform-console-standalone.jar \
    --class-path "target/test-classes:target/classes:lib/*" \
    --select-class ru.yandex.practicum.HttpEndpointTest
```
# ⚠️ Ограничения учебной реализации
```text
Симметричная, а не асимметричная подпись

Используется общий секрет, а не пара ключей (публичный/приватный)

Нет свойства неотказуемости

Без инфраструктуры PKI

Нет сертификатов

Нет цепочек доверия

Нет временных меток (timestamp)

Упрощенная безопасность

Без защиты от replay-атак

Без ротации ключей (базовая реализация)

Без контроля доступа к API

Отсутствуют расширенные функции

Нет пакетной обработки

Нет метрик и мониторинга

Нет кластеризации

Для production использования требуется:

HTTPS (TLS)

Аутентификация клиентов

Ротация секретов

Система аудита

Мониторинг и алертинг
```

# 🏗️ Архитектура проекта
```text
hmac-service/
├── src/main/java/ru/yandex/practicum/
│   ├── ServerHMAC.java                # Точка входа
│   ├── model/                         # DTO классы
│   │   ├── SignRequest.java
│   │   ├── SignResponse.java
│   │   ├── VerifyRequest.java
│   │   ├── VerifyResponse.java
│   │   └── ErrorResponse.java
│   ├── config/                        # Конфигурация
│   │   ├── AppConfig.java
│   │   └── ConfigLoader.java
│   ├── crypto/                        # Криптография
│   │   ├── Codec.java                 # Base64url
│   │   ├── SecureComparator.java      # Тайминг-стойкое сравнение
│   │   └── HmacService.java           # HMAC-SHA256
│   ├── server/                        # HTTP сервер
│   │   ├── HttpServerStarter.java
│   │   └── BaseHandler.java
│   │   └── SignHandler.java
│   │   └── VerifyHandler.java
│   └── util/                          # Утилиты
│       └── SafeLogger.java            # Безопасное логирование
        └── SecretRotator.java         # Утилита ротации секрета
├── src/test/java/                     # Тесты
├── lib/                               # Зависимости (JAR)
├── config.json                        # Конфигурация приложения
└── README.md                          # Эта документация
```
# Зависимости
```text
Gson 2.8.7 - JSON парсинг

JUnit Jupiter 5.4.2 - тестирование

OpenTest4J 1.1.1 - assertions для тестов

JUnit Platform 1.4.2 - запуск тестов
```
# 🔒 Безопасность
```text
Хранение секрета
Секрет хранится в config.json в формате base64

Файл должен иметь права 600 (только владелец может читать/писать)

Сервис проверяет права доступа при запуске

Защита от timing-атак
Используется тайминг-стойкое сравнение подписей

Время сравнения не зависит от совпадения байтов

Валидация входных данных
Проверка Content-Type

Ограничение размера сообщений

Валидация формата base64url

Проверка длины подписи

Логирование
Не логируются секретные ключи

Не логируются полные сообщения

Логируются только метаданные (длина, статус операции)

Рекомендации для production
Используйте HTTPS для защиты передаваемых данных

Регулярно ротируйте секреты

Настройте firewall для ограничения доступа к порту
```

# 📞 Поддержка
```text
При возникновении проблем:

Проверьте права доступа к config.json: chmod 600 config.json

Убедитесь что порт не занят: netstat -tulpn | grep :8080

Проверьте формат секрета: должен быть valid base64

Проверьте логи сервера в консоли
```

# 📄 Лицензия
```text
Учебный проект в рамках курса Яндекс Практикум.
```