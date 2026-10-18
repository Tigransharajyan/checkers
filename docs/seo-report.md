# Технический SEO-отчёт — 18 октября 2026

## Проект и границы проверки

Приложение для русских шашек 8×8: игра с ботом, случайным соперником и другом, ежедневная задача, личная история и прогресс. Spring Boot 3.3.4 / Java 17, Thymeleaf SSR, PostgreSQL в production, H2 в dev, vanilla JS и STOMP/SockJS. Языки: английский, русский, армянский. Развёртывание описано Docker/Render Blueprint.

Изменения сохраняют существующие цвета, композицию, шрифты, игровые правила и возможности. Рейтинги сайта, отзывы, цены, объёмы запросов, позиции и посещаемость не добавлялись. Счётчики главной продолжают использовать реальные данные приложения.

Проверены исходники, HTML, HTTP-ответы, XML, JSON-LD, браузерные сценарии и локальная мобильная производительность. Доступа к Render, Search Console, Bing Webmaster Tools и данным реальных посетителей не было. Завершение облачного развёртывания и фактическая индексация не подтверждены.

## Найденные проблемы и исправления

| Было | Реализовано |
|---|---|
| Canonical русской/армянской страницы указывал на английскую | Canonical соответствует текущему языку; рекламные параметры, ID партии и приглашения исключены |
| Статические sitemap/robots содержали example.com | Динамические контроллеры используют настроенный публичный origin; Render автоматически предоставляет `RENDER_EXTERNAL_URL` |
| В sitemap присутствовали страницы входа/регистрации | Только главная и правила/FAQ на трёх языках: шесть URL |
| Не было описаний, социальных метаданных и JSON-LD | Локализованные уникальные title/description для восьми экранов; OG, Twitter Card и изображение 1200×630 |
| Служебные страницы могли индексироваться; robots блокировал чтение noindex | `noindex,follow` в HTML и HTTP-заголовке; авторизация, личные экраны и игровые состояния вне sitemap, но HTML доступен обходу |
| Dev не был защищён от индексации | Dev/localhost и явно отключённая индексация: noindex, robots Disallow и пустой sitemap |
| Языковой переключатель состоял из кнопок | Настоящие ссылки с lang/hreflang; работают до JS, сохраняют контекст страницы |
| Альтернативные URL и нормализация были неполными | Взаимные hreflang en/ru/hy/x-default в HTML и sitemap; 301 для `/index`, завершающего `/` и некорректного lang |
| Несуществующие HTML-страницы возвращали 403 | Настоящий 404; защищённые API сохраняют проверку авторизации |
| Вложенные CSS-импорты и внешние CDN в критическом пути | Параллельные CSS, локальные WOFF2, локальные библиотеки, defer у скриптов |
| Полный набор Lucide | Официальный renderer и только 41 используемая иконка, bundle 17 060 байт |
| Изображение первого экрана загружалось поздно | Preload/fetchpriority high; существующий WebP уменьшен с 2172×724 до 1620×540 с сохранением композиции |
| Не было production-кэширования и сжатия | HTTP-сжатие текстовых ресурсов; content hash URL и публичный кэш статики на 365 дней в prod |
| Общие model advice выполнялись и для REST | I18n/SEO advice ограничены HTML-контроллером; игровые API не собирают лишнюю модель |

Не добавлялись искусственные страницы ради ключевых слов, новые фреймворки, аналитические трекеры и runtime-зависимости. Пагинации публичного индексируемого контента сейчас нет. Главная и FAQ доступны обычными ссылками навигации, без обязательного выполнения JavaScript. В FAQ сохранены вопросы и добавлены реальные правила движения, обязательного взятия и превращения; вопросы имеют семантические H2 без изменения их внешнего вида.

## Индексация и данные

| Маршрут | Назначение | Production |
|---|---|---|
| `/` | Публичное описание игры и вход в игровые режимы | index; три языковых URL |
| `/faq` | Правила и ответы | index; три языковых URL |
| `/play` | Выбор режима/состояние конкретной партии | noindex |
| `/tasks` | Меняющаяся задача и индивидуальный прогресс | noindex |
| `/rating`, `/history` | Личные данные | noindex |
| `/login`, `/register` | Авторизация | noindex |
| `/api/**`, `/ws/**`, `/h2-console/**` | Служебные ресурсы | Вне sitemap, закрыты robots |

Публичные страницы получают один H1, self-canonical и четыре alternate-ссылки. URL без lang использует разрешённую локаль и канонизируется к явному языковому URL. URL с несуществующим языком получает 301 к en; `ru-RU` нормализуется к ru. Параметры приглашений при редиректах сохраняются, но не включаются в canonical.

JSON-LD использует WebSite, VideoGame, WebPage и FAQPage, только реальные названия, языки, режимы игры и видимый текст вопросов/ответов. Сериализация экранирует HTML-значимые символы. Нет AggregateRating, review, offers, адресов и выдуманной организации. Проверены разбор JSON, типы/поля, соответствие видимому FAQ и отсутствие выдуманных коммерческих данных. Удалённая проверка опубликованного URL в Schema Markup Validator/Google Rich Results Test не выполнялась.

FAQPage здесь служит семантическим описанием. Google прекратил показ FAQ rich results в мае 2026 и удалил документацию в июне: обещаний такого расширенного результата нет. [Официальный журнал Google](https://developers.google.com/search/updates).

## Поисковые намерения и содержание

Выполнен качественный просмотр выдачи по запросам `Russian checkers online play bot rules 8x8`, `русские шашки онлайн играть с другом против компьютера`, `շաշկի առցանց խաղալ կանոններ`. Среди релевантных страниц: [Checkerso](https://checkerso.com/ru), [Checkers.to](https://checkers.to/ru), [Foony](https://foony.com/ru/games/russian-draughts). Они подтверждают наличие игровых и обучающих формулировок в выдаче, но не дают достоверной частотности или конкурентности.

| Язык | Главная: игровой запрос | FAQ: обучающий запрос |
|---|---|---|
| EN | Russian checkers online, play with a friend, play against a bot | Russian checkers rules, captures, promotion |
| RU | русские шашки онлайн, шашки с другом, шашки против бота | правила русских шашек, обязательное взятие, дамка |
| HY | շաշկի առցանց, շաշկի ընկերոջ հետ, բոտի դեմ | շաշկի կանոններ, խփում, տամա |

Для армянских запросов надёжных данных спроса не получено: формулировки выбраны по реальному продукту и языку интерфейса, а не по подтверждённой частотности. Доступа к Keyword Planner и Search Console не было; численные SEO-прогнозы не составлялись. Английский текст явно уточняет Russian checkers, чтобы не обещать отличающиеся правила English/American checkers.

SSR, обычные ссылки, ясное описание игры, понятные правила и согласованные сущности в JSON-LD также помогают автоматическим системам интерпретировать содержание. Специальные «AI ranking factors», вымышленные авторы и спекулятивный llms.txt не добавлялись. [Рекомендации Google по AI-функциям](https://developers.google.com/search/docs/appearance/ai-features).

## Производительность: реальные локальные замеры

Lighthouse 13.5.0, Chromium, мобильный профиль, simulated throttling: RTT 150 мс, throughput 1638,4 Kbps, CPU ×4; `http://localhost:8080/?lang=ru`, новый браузерный запуск. До: 12:42:08 UTC, после: 12:58:13 UTC. Это два отдельных лабораторных прогона на машине разработки, не медиана, не production и не данные CrUX. Финальный прогон выполнен после завершения тестов.

| Метрика | До | После |
|---|---:|---:|
| Lighthouse Performance | 68 | 96 |
| FCP | 4,799 с | 1,656 с |
| LCP | 5,249 с | 2,554 с |
| Speed Index | 5,197 с | 1,656 с |
| TBT | 0 мс | 15 мс |
| CLS | 0 | 0,00072 |
| Accessibility | 100 | 100 |
| Best Practices | 77 | 81 |

LCP существенно уменьшился, но в этом прогоне цель ≤2,5 с ещё не достигнута. CLS ниже 0,1. INP не измерен: TBT не является заменой INP; соответствие реальным Core Web Vitals не заявляется. [Определения и пороги Core Web Vitals](https://web.dev/articles/vitals).

Локальный Lighthouse SEO: 83 до, 66 после. Итоговый score включает намеренные dev-защиты noindex и robots Disallow; это не оценка индексируемой production-конфигурации. Production-поведение метаданных отдельно проверено интеграционными тестами с публичным origin, H2 и включённой индексацией.

Оставшееся предупреждение Best Practices: SockJS 1.6.1 использует устаревающий unload listener. Библиотека сохранена без модификаций, чтобы не менять поведение существующего соединения в рамках SEO. Обновление транспортного слоя требует отдельной проверки переподключения и уходов со страницы.

Краткие исходные результаты сохранены в [lighthouse-summary.json](seo/lighthouse-summary.json). Команда воспроизведения (Lighthouse устанавливается отдельно, не нужен для сборки проекта):

```bash
CHROME_PATH=/path/to/chrome lighthouse 'http://localhost:8080/?lang=ru' \
  --chrome-flags='--headless --no-sandbox' \
  --only-categories=performance,accessibility,best-practices,seo \
  --output=json --output-path=/tmp/checkers-lighthouse.json --quiet
```

## Проверки

- `./mvnw test`: 54 теста, 0 ошибок/падений. Включают восемь экранов × три языка; уникальность метаданных; canonical/OG/hreflang; sitemap/robots; приватность; 301/404; сохранение параметров; валидность origin; JSON-LD; verification tokens; хэширование URL и долгий кэш; запрет dev-индексации.
- `npm run test:browser`: четыре успешных сценария. Обычные партии с ботом и другом, premove, анимации/взятия/превращение/drag/клавиатура/reduced motion, ежедневная задача с серверным движком, сохранение результата, мультиязычный интерфейс.
- Новый SEO-сценарий: главная/FAQ × три языка × ширины 390/1440, правильные ссылки после выполнения JS, реальное переключение языка, отсутствие переполнения, ошибок JS, сломанных ресурсов и сторонних сетевых запросов.
- Существующий сценарий задачи дополнительно проверяет адаптивность на ширинах 320, 390, 768, 1280 и 1774 во всех языках.
- Визуально просмотрены русский desktop и армянский mobile: фирменный вид сохранён.
- `./mvnw package -DskipTests`: JAR собран после успешных тестов. Отдельный запуск собранного JAR с H2 и публичным тестовым origin подтвердил index/noindex, canonical, шесть URL sitemap, 404, доступность хэшированных CSS/изображения и кэш 365 дней. Production PostgreSQL и внешний Render в этом smoke-тесте не участвовали.

## Изменённые файлы

- `src/main/java/com/checkers/seo/{SeoService,SeoHeadersFilter}.java`: политика индексации, метаданные, JSON-LD, HTTP noindex.
- `src/main/java/com/checkers/controller/SeoController.java`: динамические robots/sitemap.
- `src/main/java/com/checkers/config/{SeoWebConfig,SeoModelAdvice,SiteProperties,LocaleConfig,I18nModelAdvice,SecurityConfig}.java`: нормализация URL, origin/locale, HTML-модель, корректные 404.
- `src/main/java/com/checkers/assistant/FaqKnowledgeBase.java`: фактические правила и понятные ответы на трёх языках.
- `src/main/resources/application.yml`: env, сжатие, хэширование и кэш.
- `src/main/resources/i18n/messages*.properties`: локализованные метаданные и описание.
- Все HTML-шаблоны восьми экранов и общие фрагменты в `src/main/resources/templates/`: единый head, ссылки языка, семантика, отложенные локальные скрипты.
- `src/main/resources/static/css/{app,design-system,theme,fonts}.css`, `js/common.js`: загрузка, переключатель, стабильность счётчиков.
- `src/main/resources/static/{fonts,js/vendor}/`: локальные ресурсы, лицензии и происхождение; `scripts/build-icons.cjs`: воспроизводимая сборка иконок.
- `src/main/resources/static/images/{checkers-hero.webp,checkers-social.jpg,favicon.svg}`: оптимизированный существующий фон, социальное изображение, favicon.
- Статические `robots.txt` и `sitemap.xml` удалены, их заменяют контроллеры.
- `src/test/java/com/checkers/{controller/SeoExperienceTest,controller/SiteExperienceTest,config/SeoSafetyTest}.java`, `src/test/browser/seo.test.cjs`: проверки.
- `README.md`, `.env.example`, `.gitignore`, `render.yaml`, `docs/seo-report.md`, `docs/seo/lighthouse-summary.json`: настройка и результаты; безопасный шаблон env теперь включён в Git.

## Настройка владельцем после развёртывания

1. Убедиться, что Render запустил новую версию. Для стандартного адреса origin берётся из `RENDER_EXTERNAL_URL`. Для своего домена задать `SITE_BASE_URL` равным реальному HTTPS-origin без пути/query. Пустой или устаревший override удалить. На staging установить `SITE_INDEXING_ENABLED=false`; dev блокируется независимо от этой настройки. [Переменные Render](https://render.com/docs/environment-variables).
2. Проверить исходник `/?lang=ru`: правильный публичный canonical, `index,follow`, доступные CSS/JS/изображения; открыть `/robots.txt` и `/sitemap.xml`, проверить origin и шесть URL. HTTP→HTTPS и редирект доменных алиасов настроить на Render/домене, поскольку схема TLS и реальные алиасы из репозитория неизвестны.
3. В Google Search Console добавить фактический URL-prefix property. Получить значение `content` из HTML verification tag, сохранить только его в `GOOGLE_SITE_VERIFICATION`, развернуть и нажать «Подтвердить». Для Domain property вместо этого нужна DNS-проверка у провайдера. [Инструкция Google](https://support.google.com/webmasters/answer/9008080).
4. В Bing Webmaster Tools добавить тот же сайт. HTML meta token сохранить в `BING_SITE_VERIFICATION`, затем подтвердить владение. [Инструкция Bing](https://www2.bing.com/webmasters/help/add-and-verify-site-12184f8b).
5. В обеих консолях отправить sitemap по фактическому публичному адресу `/sitemap.xml`. В Search Console проверить URL главной/FAQ во всех языках, доступность robots, выбранный canonical и причину исключения личных страниц. [Отправка sitemap](https://developers.google.com/search/docs/crawling-indexing/sitemaps/build-sitemap).
6. Проверить опубликованные URL в PageSpeed Insights, Schema Markup Validator и, для поддерживаемых Google типов, Rich Results Test. Отсутствие отдельного rich result для WebSite/FAQPage/VideoGame само по себе не означает ошибку Schema.org. В отчёте Core Web Vitals ждать достаточных реальных данных; наблюдать LCP/INP/CLS и поисковые запросы по языкам, не подставлять лабораторные значения вместо field data.

Внешние аккаунты не подключены, sitemap через их интерфейс не отправлен, рост трафика и позиции не обещаются.

## Следующие улучшения по подтверждённым данным

После публикации измерить стабильность LCP и задержку первого ответа на реальном хостинге; по результатам рассмотреть адаптивные варианты фонового изображения и настройки инфраструктуры. Собрать реальные INP, оценить переподключения SockJS перед обновлением библиотеки. После накопления Search Console данных уточнить языковые формулировки и полезные разделы правил по фактическим запросам. Новые индексируемые маршруты следует добавлять через общий реестр `SeoService.INDEXABLE_PATHS`, вместе с качественным SSR-контентом и тестами.

Технические решения сверены с [hreflang Google](https://developers.google.com/search/docs/specialty/international/localized-versions), [noindex](https://developers.google.com/search/docs/crawling-indexing/block-indexing), [canonical](https://developers.google.com/search/docs/crawling-indexing/consolidate-duplicate-urls), [WebSite/site names](https://developers.google.com/search/docs/appearance/site-names), [VideoGame](https://schema.org/VideoGame) и [FAQPage](https://schema.org/FAQPage).
