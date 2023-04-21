# WeatherParsers
Набор парсеров для некоторых сайтов с прогнозом погоды.

Веб-сайты, с которых идет сбор информации:
- [Gismeteo](https://www.gismeteo.ru/)
- [Meteoinfo](https://meteoinfo.ru/)
- [rp5](https://rp5.ru)

# Требования
- Docker

# Запуск
Перед запуском необходимо создать директории, в которые будут сохраняться данные. Необходимая структура каталогов:
```
├── data
│   ├── gismeteo
│   ├── meteoinfo
│   └── rp5
│
├── docker-compose.yml
├── gismeteoParser
├── meteoinfo
└── rp5Parser

```
Её можно изменить посредством редактирования `volume`-ов в `docker-compose.yml`.
Для запуска используйте `docker-compose.yml`.
```docker
docker compose up -d
```
Результаты парсинга будут автоматически складываться в указанной директории.