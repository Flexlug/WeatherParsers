# WeatherParsers
Набор парсеров для некоторых сайтов с прогнозом погоды.

Веб-сайты, с которых идет сбор информации:
- [Gismeteo](https://www.gismeteo.ru/)
- [Meteoinfo](https://meteoinfo.ru/)
- [rp5](https://rp5.ru)

# Требования
- Docker

# Деплой
```docker
docker compose up -d
```

Результаты парсинга будут автоматически складываться в директории `./data`.