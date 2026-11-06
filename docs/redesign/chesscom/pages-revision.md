# Постраничная ревизия по референсу Chess.com

Дата анализа: 6 ноября 2026. Это новая ревизия после замечания пользователя, а не подтверждение уже сделанного редизайна. Первый круг независимого ревью завершён; ниже записаны решения по его замечаниям. Код приложения на этом этапе не менялся.

## Что было открыто и что нельзя было проверить

- Вживую в браузере открыты публичные страницы Chess.com `/`, `/play`, `/play/computer`, `/puzzles`, `/leaderboard`, `/games/archive` и `/home`.
- Гостевой переход на `/home` перенаправляет на форму входа; доступ к авторизованной версии домашней страницы не предоставлялся. Её структуру сверил с актуальной справкой Chess.com, но пиксельный снимок личной страницы отсутствует. Не буду утверждать обратное.
- Публичная `/` — маркетинговая и новостная страница с отдельной шапкой, одним главным действием, подборкой игровых/учебных путей и новостями. Это другой продуктовый экран, не авторизованный `/home`.
- Публичная `/play` в текущем узком браузерном окне показывает заголовок и вертикальный список режимов. Ранее сохранённый широкоформатный снимок реального Chess.com `/play` показывает композицию с доской и меню режимов справа. Итого: страница меняется вместе с шириной и контекстом; узкий экран не копирует desktop-композицию буквально.
- `/play/computer` на гостевой сессии сначала открывает собственный onboarding-dialog над выбором ботов. Снимок зафиксирован в [screenshots/computer.png](screenshots/computer.png).
- `/puzzles` показывает свою страницу с игровым путём, персонажем-подсказчиком и закреплённым нижним действием. Это реальная, но чужая иллюстративная система; переносить её рисунки нельзя и для русских шашек не нужно.
- `/leaderboard` имеет собственный заголовок, общие фильтры и отдельные рейтинговые блоки по категориям. Гостевой экран дополнительно затемняет рекламный overlay; рекламный блок и чужие показатели не являются частью рекомендуемого направления.
- `/games/archive` для гостя переходит на вход. Детали авторизованного архива напрямую проверить нельзя.
- Актуальная справка Chess.com описывает домашний экран как быстрый доступ к игре, следующему уроку, задачам и разбору последней партии; рекомендуемые матчи, трансляции/события и дневная задача настраиваются отдельно. Она также указывает, что можно менять расположение пунктов меню и переключать мобильную компоновку. Ссылки: [Welcome to Chess.com](https://support.chess.com/en/articles/8615318-welcome-to-chess-com), [Homepage customization](https://support.chess.com/en/articles/8652316-how-can-i-change-my-homepage).

## Предметный аудит текущих страниц

Наблюдения ниже относятся к текущим исходникам и реальному локальному UI при 1270×720, а не к предполагаемым состояниям.

| Страница | Наблюдаемая проблема | Влияние | Приоритет | Направление исправления |
|---|---|---|---|---|
| `/` и `/play` | В обеих страницах одна и та же трёхколоночная сцена: левая навигация, большая стартовая доска, одинаковый блок режимов справа. `/play` убирает только дневную задачу и историю. | Пункты «Лобби» и «Игра» выглядят дублями; по первому экрану не ясно, чем задача каждой страницы отличается. Доска занимает основную площадь даже там, где нужен выбор режима. | Critical | Домашний экран сделать сводкой и продолжением активности. `/play` оставить специализированным пусковым экраном: режимы и настройки первыми, без домашней ленты. На desktop отдельно проверить доска+режимы как композицию из широкого Chess.com `/play`; на mobile сначала оставить выбор действия. |
| `/` / запрошенный `/home` | Контент home — режимы игры с крупным превью начальной позиции, а не пользовательский обзор. Нет отдельного `/home` маршрута, хотя интерфейс уже называет экран лобби. | Плохо работает как стартовая точка после возвращения: отсутствуют приоритеты «продолжить / начать / решить задачу / посмотреть результат». | High | Добавить публичный локализованный маршрут `/home` для той же домашней страницы; сохранить `/` и `/index`, назначить `/` каноническим для поисковиков и не добавлять alias в sitemap. Home становится коротким действующим обзором, а не пустой лентой: быстрый онлайн-старт, переходы к боту/другу, настоящая задача дня; персональные данные и продолжение только при ответе API. |
| `/play` | В правом узком столбце режимы оформлены одинаковыми обведёнными панелями; та же информация видна на `/`. | Усложняет выбор и уменьшает полезную ширину; карточки выглядят механически повторёнными. | High | Из одной страницы выбора режимов сделать самостоятельный экран: явный title, приоритетный быстрый старт, вторичные способы игры ниже, компактные настройки рядом с выбранным режимом. Сохранить настоящий поиск матча, сложность бота, создание и ввод приглашения. |
| `/play?id=…` | Игровая сцена уже отделена от лобби, но использует тот же широкий app-shell и глобальную навигацию. | Съедается площадь доски; во время партии часть лобби-навигации постоянно конкурирует с ходом, статусом, соперником и журналом. | High | Режим партии со своей шапкой и контекстной навигацией. Оставить доске максимум ширины, статус и действия связать с игроками/ходами; явно сохранить возврат в lobby. Не менять игровой движок и состояния. |
| `/tasks` | Это уже рабочая отдельная двухколоночная задача с доской, условием и ходами. При 1270×720 доска около 497 px, а инструкция/дата/прогресс вынесены в отдельную правую колонку. | Сценарий понятен, но ширина и плотность похожи на общее desktop-приложение, а не на самостоятельный тактический режим. | Medium | Сохранить отдельную композицию puzzle-экрана: заголовок/прогресс и короткое условие над доской; крупная доска слева; обратная связь и управляющие действия справа. Порядок условия до доски обязателен и визуально, и в DOM на всех ширинах. Не добавлять вымышленные уровни, рейтинги, подсказки или рисунки. |
| `/rating` | Страница называется «Ваш прогресс», однако гостевой вариант показывает прочерк, две пустые панели и пояснительный текст в верхнем левом углу; основная часть viewport пустая. | Пользователь не понимает, что именно появится после входа и что фиксируется. Пустота похожа на незаполненный макет. | High | Специализировать как личную статистику: сводка wins/losses/draws/total только из реальных данных, отчётливое guest-state и история результатов. Не изображать глобальный leaderboard: в проекте нет leaderboard API и источника чужих рейтингов. График не показывать, пока нет реального временного ряда. |
| `/history` | Гостю показывается узкий текстовый блок слева и большая пустая поверхность. Авторизованная история — таблица, но она не видна в гостевом состоянии. | Экран кажется недоделанным; причина запроса регистрации не связана визуально с будущей ценностью архива. | High | Сделать archive-specific страницу с ясным названием и объяснением хранения партий. Гостевой state — полноценная объясняющая панель, но без фиктивных строк. Авторизованный state — плотная таблица настоящих партий с результатом, игроками, режимом и доступом к повтору. |
| `/faq` | Поиск и раскрываемые ответы выполняют свою задачу, но все разделы используют тот же каркас и тёмную непрерывную плоскость. | Длинный справочник сложнее сканировать и визуально не отделён от игрового пространства. | Medium | Оформить как справочник правил: компактный intro, заметный поиск, последовательные раскрытия и сдержанная ширина чтения. Сохранить настоящее содержимое, фильтр поиска и клавиатурный доступ. |
| `/login`, `/register` | Уже имеют отдельную от sidebar форму; центрированная карточка. | Структурно это отдельные состояния, но визуальная иерархия форм повторяет те же большие поля и карточный паттерн продукта. | Low | Оставить standalone auth-layout; уточнить заголовок, поля, ошибку и путь гостя; никакие social-login/функции не выдумывать. |

Текущие изображения полного локального desktop UI лежат в `screenshots/final-desktop.png`, `screenshots/game-registered-1440.png` и соседних артефактах предыдущего прохода; маршрутный осмотр этого прохода зафиксирован выше. Текущий широкий `/home`-экран открыт отдельно в настоящем браузере 1270×720. Текущие проверки 384 комбинаций locale/route/width и прежние ревью полезны как техническая база, но не доказывают постраничное сходство с Chess.com.

## Ключевые принципы, взятые из реального референса

1. У Chess.com публичная главная, домашний экран, список режимов, игровая партия, задачи и рейтинг не сведены к одной и той же сетке. Каждая поверхность отвечает на отдельный вопрос пользователя.
2. `/play` ведёт к конкретному выбору формата; на узком viewport режимы становятся вертикальными и читаемыми. Широкая версия может использовать доску рядом с меню, если доска поддерживает назначение страницы.
3. Домашний экран отбирает несколько частых действий и актуальных модулей; элементы home настраиваются. Для текущего продукта переносим только существующие реальные сценарии — активная партия, начало партии, задача дня, своя статистика.
4. Puzzle-интерфейс изолирует решение задачи от общего меню. Полезен принцип «одна доска, ясная задача, немедленная обратная связь», не иллюстрации Chess.com.
5. Рейтинговый раздел строится вокруг фильтров и сравнимых данных, но локальный продукт пока не имеет публичного рейтингового API. Поэтому `/rating` должен быть личной статистикой и не копировать данные / таблицы референса.
6. В Chess.com используются узнаваемые тёмные поверхности, светлый текст, зелёное действие, понятные игровые значки и плотные список/панели. Переносим цветовые отношения, ритм и фокус действия; не импортируем логотип, аватары, персонажей, рекламный контент или шахматную нотацию.

Реальные страницы для сравнения: [Chess.com Play](https://www.chess.com/play), [Play Bots](https://www.chess.com/play/computer), [Puzzles](https://www.chess.com/puzzles), [Leaderboard](https://www.chess.com/leaderboard). Гостевой `/games/archive` был перенаправлен на login; приватную архивную страницу не удалось осмотреть. Принципы входа в домашние модули и типы puzzle-страниц сверены с [официальной справкой о Chess.com home](https://support.chess.com/en/articles/8615318-welcome-to-chess-com), [настройке домашней страницы](https://support.chess.com/en/articles/8652316-how-can-i-change-my-homepage) и [задачах](https://support.chess.com/en/articles/8608686-how-do-puzzles-work-on-chess-com).

## Три полноценных направления

### A — Chess.com, но по задаче страницы (рекомендовано)

- **Идея:** общая узнаваемая система навигации и цвета, разная композиция и плотность для каждого маршрута.
- **Характер:** тёмный игровой клуб: графитовая навигация, тёплый графитовый холст, светлая/зелёная доска, насыщённый, но контролируемый зелёный action.
- **Home:** персональная сводка действий и настоящей активности; без большой пустой стартовой доски.
- **Play:** самостоятельный экран выбора режима; при достаточной ширине доска слева и игровые режимы справа, на mobile режимы идут раньше превью.
- **Game:** игровой workspace, где доска — главный объект, а controls/notation собраны в контекстной панели.
- **Tasks:** отдельный puzzle-workbench с текущей задачей и обратной связью.
- **Rating:** личный performance profile на реальных результатах.
- **History:** архивная поверхность с честными пустым и гостевым состояниями.
- **FAQ:** читаемый справочник; auth остаётся формой с самостоятельной иерархией.
- **Риски:** несколько схем сетки нужно внимательно адаптировать; общие токены и повторные компоненты удерживают систему вместе.
- **Foundation:** графитовый холст `#302e2b`, тёмный sidebar `#262421`, raised neutral `#3d3b38`, зелёный action `#81b64c`, доска `#ebecd0/#779556`; локальный Inter/Noto; скейл 4/8/12/16/24/32/48; body16px, section20–24px, h1 30–32px; постоянная навигация для разделов, game header для партии; 6–8px контейнеры и 3–6px контролы.
- **Ключевые компоненты:** семантическая navigation list, mode launcher, game board+control rail, puzzle board+answer state, real-stat summary, archive table, search+details FAQ, standalone auth form.
- **Интерактивность:** короткие status feedback, visible focus, 44px targets; только необходимые состояния доски/очереди/ошибки/решения; reduced-motion.

### B — Единое игровое рабочее место

- **Идея:** как можно больше маршрутов используют постоянную доску и правую контекстную панель; nav и player-zone не меняют положение.
- **Характер:** плотное, привычное игровое приложение с короткими переходами между play, task, history и stats.
- **Преимущества:** мало перестановок интерфейса, доска сразу узнаваема на большинстве экранов.
- **Недостатки:** прямо воспроизводит проблему нынешней версии — домашний экран, выбор режима и задачи начинают выглядеть вариантами одного layout; справочник и архив плохо помещаются в board workspace.
- **Foundation:** тот же graphite/green, более плотный интерфейс 12–16px gap, постоянная 170px nav и board region; одна панель справа переключается между действиями, задачей, ходами и статистикой. Типографика 28px page title, 18px section,16px labels. Одинаковые controls и таблица на нескольких маршрутах.
- **Интерактивность:** переходы вкладки/панели 140ms, постоянно видимая сетка разделов, компактный game controls stack; mobile сворачивает панель в bottom sheet. Сильная практика-плотность, но слабее страничное отличие и хуже сводка.

### C — Разделённые тематические разделы

- **Идея:** каждой странице дать отдельный визуальный контекст и оформление, сохраняя только logo, типографику и основные цвета.
- **Характер:** домашний экран — activity stream; игра — полноэкранный; задачи — как тренажёр; история — таблица; FAQ — документ.
- **Преимущества:** сильное различие страниц, максимально свободное пространство для каждого сценария.
- **Недостатки:** без общей навигации и единого каркаса переключение между сценариями становится непредсказуемым; нужно больше кода и появляется риск визуального долга.
- **Foundation:** домашняя страница 3-zone activity layout; игра без боковой панели; puzzle full-board; личный прогресс на открытой поверхности с типографикой и реальными цифрами; FAQ как центр помощи с собственным toc; auth — светлая компактная форма. Используются один нейтральный набор fonts и контрастов, но разные максимальные ширины (дом 1240, board зависит от viewport, FAQ 880, stats 1080) и navigation (sidebar, game header, FAQ index).
- **Интерактивность:** каждая зона имеет собственные hover/selected/busy/error state; navigation меняется по разделу, что повышает различимость экранов, но заставляет заново находить переходы.

## Выбор

Выбран вариант A. Он прямо устраняет наблюдаемое дублирование `/` и `/play`, соответствует тому, как Chess.com отделяет игровую навигацию от домашнего обзора и задач, но оставляет общие цветовые/типографические правила. Вариант не копирует брендинг и не притворяется, что в шашках есть шахматные фичи Chess.com.

## Спецификация, требующая предварительного ревью

### Слои и маршруты

- **Обычная оболочка** остаётся у `/home`, `/play`, `/tasks`, `/rating`, `/history`, `/faq`: текущая sidebar для desktop, мобильный header/drawer и нижняя навигация. Сохраняются skip-link, `main`, `aria-current`, language switch и account controls. Tasks остаётся в этой оболочке.
- **Игровая оболочка** заменяет sidebar на собственный компактный header: двухдисковый знак и название «Русские шашки», видимая ссылка «В лобби», status игры, кнопка «Меню». Меню открывает ту же полную навигацию, язык и account actions, которые были в sidebar. Использовать существующий overlay owner (Escape, focus trap, inert background, возврат фокуса), не оставлять скрытую/битую `aria-controls`-кнопку. Мобильный header тот же по смыслу; board и controls естественно скроллятся.
- **Auth-оболочка** остаётся standalone: собственный двухдисковый знак, локализованное название, переключатель языка и компактная форма.
- Добавить публичный `GET /home` на ту же home page; сохранить `/` и `/index`. В SecurityConfig явно разрешить `/home`. `common.js` должен сохранять активный язык при переключении на `/home`. SEO title/description соответствуют home; canonical и hreflang остаются на `/?lang=…`; `/home` не попадает в sitemap и не становится второй индексируемой копией. Текущие URL `/`/`/index` и `play?id`/`play?code` остаются.
- **Home / `/home`:** всегда показывать локализованное имя «Русские шашки» (`redesign.lobby.title`) и правило-подсказку/ссылку «Правила» (`app.tagline` либо `lobby.hero.subtitle`, уже переведённые на EN/RU/HY). Домашний CTA «Найти соперника» использует тот же matchmaking flow и состояния, что `/play`; переход «Против бота»/«С другом» ведёт на соответствующую строку Play. Нет большого превью доски.
- Home: если `/api/games/active` успешно вернул активную партию, первая строка — реальный соперник/режим и действия «Продолжить», «Сдаться/отменить» с текущим подтверждением. Если ответ пустой, строка отсутствует. Если запрос упал, строка ошибки + retry занимает ту же область, а старт партии не блокируется. Ни ошибка, ни loading не притворяются пустым списком.
- Home desktop: main `max-width:1180px`; title+rule link; затем 2-column composition `minmax(0, 1.65fr) / minmax(280px, .85fr)`, gap24px. Main first column — open quick-start row с зелёным «Найти соперника» и компактными text links «Против бота»/«С другом»; second column — одна настоящая дневная задача с board thumbnail ≤192px, date and CTA. Authenticated recent actual results + inline W/L/D appear below first column. No nested stat cards.
- Home guest: без активной игры, stats и фиктивной recent history. Сразу доступны quick start и настоящая задача. Не выводить пустой feed/пустую правую колонку: если задача не загрузилась, её область показывает локализованную ошибку и retry. Registered zero-history state показывает настоящие 0 W/L/D и одну строку «Ещё нет завершённых партий» с link к игре; не показывает win-rate 0%.
- Home returning state: real active game strip first; thereafter quick start + daily task; below them real latest games and real W/L/D. При недоступной истории сообщение и retry находятся в области списка; данные активной партии, задачи и аккаунта загружаются независимо.
- Home mobile (≤900px): heading/rules; active/recoverable state; online CTA; bot/friend links; daily task; account stats and recent games only when available. Source order совпадает с visual/Tab order; нет CSS reorder. Общие controls ≥44px, bottom nav safe-area padded.
- `/play`: отдельный игровой launcher без home feed. Title «Играть в русские шашки» + явный ruleset «Русские шашки · 8×8» и ссылка правил до запуска. На ≥1200px композиция сверяется с настоящим desktop screenshot Chess.com: sidebar170px; board starts near x188px, target up to728×728px (ограничен высотой и доступной шириной после rail), menu rail x≈948px, width340px, gap32px. Уменьшать можно по available height/width, но не растягивать в безмерный холст. Снизу/рядом доски только текст «Начальная позиция · 8×8»; не выдавать игроков/гостей за участников активной партии.
- Play menu на desktop: три **действующие строки**, не три form-card: online + `Найти соперника`/queue status/cancel; bot + native difficulty selector и `Играть`; friend + `Пригласить друга`. Иконки Lucide одного размера/веса и системной семантики, не emoji. Разделять строки типографикой и правилами, не набором вложенных bordered cards. Join-by-code form виден ниже строк, submit по Enter; ошибки у соответствующего поля. Рядом только ссылка на правила и, если остаётся место, настоящая история/leaderboard не выдумываются.
- Play at `<1200px`: превью/доска убрана из layout и accessibility tree; full-width mode rows and actions at top, forms in natural order. На `<900px` standard mobile nav/drawer; mobile first content action is «Найти соперника». No hidden focusable controls in preview.
- `/play?id=…` / `?code=…`: единственная доска centered left and control rail right. Desktop board `min(728px, width available, usable viewport height)`; player/status bars immediately above/below board; controls/notation in rail. Header is 56px; menu contains every global route, locale/account and lobby return. Lobby return never resigns. Mobile puts board/player turn at top, then queue/draw/end, notation; page scrolls instead of forcing nested/covered panels. Preserve websocket lifecycle, draw/resign dialogs, mouse/touch/keyboard, premove and review states.
- `/tasks`: standard nav and a standalone puzzle header with date/progress. Short side-to-move/objective line is **before and `aria-describedby` on the board**. Desktop then board max560px left + answer/status/control rail320px right. Wrong/correct feedback stays in stable region and never shifts board; reset/retry/practice remain beside state. Mobile: title → short objective → board → live feedback → actions; no CSS reorder. Keep puzzle auth/server validation and no invented map/rating/art.
- `/rating`: title «Ваш прогресс» consistently across nav/title (never “leaderboard/Elo”); on registered accounts four open typographic measures based on `UserResponse`: completed total, W/L/D and win-rate only when total>0. Zero-game account sees a short honest empty explanation and Play CTA; unavailable `me`/stats sees an error with retry. Guest sees one bounded explanation and login/register actions, no grid of em dashes or empty metric tiles. Below: concise explanation how actual completed outcomes produce stats; no chart without time series and no global competitor data.
- `/history`: archive page has full-width titled content. Guest state one 560–680px explanation aligned with main column and clear login/create-account actions, no fake row skeleton/sample table; do not stretch the message into a narrow card. Registered state shows only API rows: date (`startedAt`/`finishedAt`), mode, players, result and replay action. Zero rows = separate honest empty state + Play action. Loading, error/retry and guest states are mutually exclusive and share one stable content region. At mobile use wrapped row items with visible replay action; never force clipped columns or keyboard traps.
- `/faq`: 680–760px readable text column; title/one-sentence intro, search max520px, then open typographic details rows with separators. No outer card plus cards per item. Existing instant search and native disclosure remain; open/closed icon and focus ring visible.
- `/login`/`/register`: standalone 400–440px form surface centered under the existing identity mark/language switch. Clear 30–32px heading, persistent labels, fields, inline errors with reserved space, primary submit, guest continuation, cross-link. No imported social-auth or Chess.com logo/assets.

### Annotated page compositions (reviewable before implementation)

The sidebar and outer brand stay familiar. Inside the shell, the page first screen, board proportion, content density and interaction region deliberately change by route.

**Home — desktop, authenticated returning user:**

```text
┌─────────────── 170px navigation ─────────────┬──────────── max 1180px main ─────────────────────────┐
│ two-disc mark + Russian draughts             │ Русские шашки · 8×8                 Правила →         │
│ Home / Play / Tasks / Progress / Rules / ... │ [active game + real opponent] [Continue] [End…]     │
│                                               │ ┌── quick start, open ─────────────┐ ┌─ Daily ────┐ │
│                                               │ │ [Find opponent]                  │ │ date       │ │
│                                               │ │ Play a bot  ·  Play a friend      │ │ real mini  │ │
│                                               │ └──────────────────────────────────┘ │ board≤192  │ │
│                                               │ Latest games — real, compact rows  │ [Solve]     │ │
│                                               │ Wins / losses / draws — real strip │ └────────────┘ │
└──────────────────────────────────────────────┴─────────────────────────────────────────────────────┘
```

Active-game bar appears only after API success with an active session. Without an active session the next row rises; no empty active-game box remains. Guest home is this same compact composition with “Find opponent” first, “Bot/Friend” direct links and daily task; recent games and personal stats are absent. A signed-in account with zero finished games instead gets true W/L/D zeros and one “No completed games yet” line. Network failures retain the action and show a small retry state in the affected row. The homepage never displays a large initial-position board.

**Home — mobile, source order is visual and keyboard order:** `Russian draughts + Rules` → `active/retry row if needed` → `Find opponent` → `Bot / Friend` links → `daily task` → `actual stats and latest games only when available`. No board preview before a playable action. Do not use CSS `order` to change reading order.

**Play launcher — 1440×1000 target, aligned to the supplied real Chess.com reference:**

```text
┌── 170 sidebar ──┬─18─┬──── board up to 728×728 ────┬─32─┬── 300 mode rail ─┬── free canvas ──┐
│                  │   │ Русские шашки · 8×8         │   │ Играть в русские │                │
│                  │   │ Начальная позиция           │   │ шашки            │                │
│                  │   │                              │   │ Online row       │                │
│                  │   │ actual flat draughts pieces │   │ Bot + difficulty │                │
│                  │   │ cream/green 8×8 board        │   │ Friend + invite  │                │
│                  │   │ no active-game player names │   │ Join by code     │                │
│                  │   │                              │   │ Rules link       │                │
└──────────────────┴───┴──────────────────────────────┴───┴───────────────────┴────────────────┘
```

The board is a static, labelled initial position (`role=img`), excluded from Tab navigation. The rail is composed of three open mode rows separated by rules, not three oversized nested cards. Each action does one named thing; selecting bot difficulty is immediately beside bot `Play`; invitation creation and code entry are visibly separate actions. The screenshot reference anchors the board at x≈188 and rail at x≈948 at this viewport; proportion matters more than copying its brand assets.

**Play launcher — tablet/mobile:** below1200px, no board preview; the viewport begins with `Играть в русские шашки` → online row + `Find opponent` → bot row + difficulty and `Play` → friend row + `Invite` → join code form → rules link. First main Tab stop is the online action. At ≤900px standard mobile nav remains available; rows and forms are full width and scroll naturally.

**Active game:**

```text
┌── 56px game header: own mark/name ─── [← Lobby] [Menu: all sections · locale · account] ──────┐
│                                       │                                                       │
│  opponent + turn                      │ game status + quiet Draw / Resign actions             │
│  square 8×8 board                     │ move notation / offer response / connection state   │
│  local player + turn                  │                                                       │
└───────────────────────────────────────┴───────────────────────────────────────────────────────┘
```

Desktop board uses available width up to728px; rail is 300–340px. At narrow width, header is followed by players and board, then actions and notation. The drawer, not a hidden sidebar, owns complete application navigation. Leaving for Lobby is not an implicit resignation.

**Daily task:** heading + date/steps → short side-to-move and objective text → board (desktop max560px) beside a 300–320px response/actions region. Wrong/correct announcement uses a stable live region and does not move the board. Mobile retains that exact reading order and puts feedback/actions after the board.

**Personal progress:** title/short explanation → one open four-value strip (total, wins, losses, draws) and one clearly labelled win-rate when it is defined → one concise formula/source explanation. Guest and new-user states take the same bounded reading width, show a real login/play next step, and never leave metric placeholders in the interface.

**Game archive:** title/explanation → one readable central content region. Guest: explanatory text and login/register actions, no sample table. Registered: compact real table with date/mode/players/result/replay. Empty and error/retry messages occupy this same region and are visually distinct. Mobile rows wrap into labelled fields and keep the replay action visible.

**Rules & FAQ:** 680–760px reading measure, page title/short introduction, search up to520px, then open native details rows divided by separators. **Authentication:** standalone 400–440px form under the same two-disc mark and language switch; it does not inherit the game/app navigation.

### Shell, component and source contracts

- Shared fragments own the existing two-disc identity, name, language control, account state, skip-link and navigation labels. Play uses one Lucide icon mapping for each section at desktop and mobile (`Play=gamepad-2`, `Tasks=target`, `Progress=chart`, `Rules=book-open`, `History=history`); color is supplemental. Draughts pieces keep the existing two-ring and crown treatment. No Chess.com pawn/logo, chess glyphs, bot faces, emoji tile artwork, third-party images or imported brand assets.
- Keep current `lobby.js` and its socket/API behavior guarded by the `data-*` elements on Home and Play; home fast-start reuses `[data-find-random]`. The active list, recent list, task preview and personal summary initialize only when their own region exists. The home CTA must not require navigation to `/play` first. Don't duplicate matchmaking or game creation logic.
- The focused-game header uses the existing `Overlay` manager and a complete navigation fragment. The drawer button's `aria-controls` names the element that actually opens. The standard shell remains on Tasks. Preserve current `data-board`, `data-task-board`, queue, invite and game data hooks unless a clearly scoped refactor maps them without duplicate IDs.
- Token migration updates the existing design-system variables in place; no later appended override theme. Replace every low-contrast interactive use of `#68645f` with the current `--line-strong` value; retain `--line` for decorative dividers.
- `/home` integration checklist: explicit public `SecurityConfig` matcher; `PageController` alias; `common.js` locale-preserving path list; home active navigation and links; home page SEO title/description, canonical back to `/`, alternates from `/`, no separate sitemap entry; `?lang=en|ru|hy` and language switch remain. Any `id`/`code` game query continues to route only to `play`.

### Data, focus and announced-state matrix

| Region/flow | State shown to sighted users | Keyboard / screen reader contract |
|---|---|---|
| Home active games | loading text, active row, confirmed none, or error+Retry; never equate error with none | one stable `role=status`, retry button stays in same region; result arrival does not steal focus |
| Home recent games | loading, real rows, real empty, or error+Retry; absent to guest | region has heading/name; row action says “Open game vs {opponent}”; no fake rows |
| Home daily task | loading, real date+position, or error+Retry | board miniature is one labelled non-interactive image, no square tab stops |
| Online matchmaking | idle → connecting/searching → matched/cancelled/failed | `aria-busy`, concise polite live status; cancel remains enabled/reachable except during cancellation; connection failure exposes retry |
| Bot / Friend / Join | idle → creating/starting/joining → game or field/action error | disable only the active submit, retain difficulty/invite value, connect error to code field, return focus to row/action on completion |
| Active game | loading/ready, own turn/opponent turn, mandatory capture, draw offer, resignation/result, connection loss | retain existing board roving keyboard focus and row/cell labels; one concise polite status with move coordinates/turn, never reread full notation or move board focus; all dialogs trap/restore focus |
| Daily puzzle | loading/ready/wrong/correct/reset/error | instruction precedes board and is `aria-describedby`; existing live message announces feedback and progress once; errors offer retry where available |
| Progress and history | guest, new-user empty, data, loading, API error+Retry | values have visible and accessible labels; no `—` metric grid; errors announced in stable live status |
| FAQ and auth | search/no results/open answer; form idle/busy/invalid/server error | native details, visible open/focus states; auth label, `aria-invalid`/`aria-describedby`, first invalid field focused, values retained |

The game header's nav menu is fully operable with keyboard: open by button, initial focus moves into menu, Escape closes and restores focus, background becomes inert, every section/locale/account item remains reachable. `aria-current="page"` identifies current navigation. Main landmarks and skip link stay present in all shells. No fixed mobile nav overlaps focused/last controls; include safe-area insets.

### Responsive and visual acceptance gates

- Compare screenshots of **all eight significant page types** (`/home`, `/play`, active `/play?id`, `/tasks`, `/rating`, `/history`, `/faq`, auth) at 1440×1000,1280×720,768×1024,390×844,360×640 plus a short landscape viewport. Include home guest, new account, returning/active, history guest and real populated/error, stats zero/populated/error, puzzle wrong/correct, invalid form, and three locales. Screenshots show the rendered UI; code-only checks do not certify resemblance.
- At 1440×1000 home first view contains no full-size board; primary online action and a real task are visible. Play has the reference-like board+right mode rail and none of the home feed; measure x188±16 for board start, ≤728px board, rail near x948±24, width300–340. At ≤1199px Play has no preview and puts working mode actions first. The visual and DOM order agree.
- Active game shows the 8×8 board and all game controls without horizontal page overflow; no sidebar steals board width. Tasks keeps the objective before the board and success/error actions in view by ordinary scroll. FAQ never exceeds its reading measure. Auth always leaves form and guest/cross-links reachable.
- Also inspect a >1920px wide screen, 200% text scaling/400% browser zoom, short landscape, keyboard-open mobile viewport, open drawer/dialog, long Armenian and Russian strings, initial/error/success/loading states. Do not shrink normal body type below16px or pin over content to force one-screen fit.
- Check automated contrast plus measured adjacent-pair contrast: body ≥4.5:1, large ≥3:1; interactive boundaries and focus ≥3:1. State must be conveyed by text/icon/shape as well as hue. Active king and piece contrast distinguish by ring/crown treatment.
- Visual acceptance compares page silhouette and hierarchy to the matched Chess.com reference page: Home is activity/actions, Play is launcher, active game is workspace, puzzle is a puzzle workspace, Progress is personal numbers, History is an archive, FAQ is a reading surface and auth is a focused form. The brand/marketing page and authenticated `/home` cannot be pixel-matched in private states without authorized access; authenticated home composition remains a documented product hypothesis based on official help.

### Changes since the first review round

- Concrete desktop/mobile page diagrams, launcher proportions and route screenshot criteria now make the content order reviewable before code.
- Guest, zero-history, returning, populated and failed-network states are defined; the immediate mode actions are explicit and the home quick-start does not add a step before online matchmaking.
- `/home` route, security, locale, canonical/hreflang and sitemap handling; shell ownership; game navigation; existing JS/API contracts; board reading order, focus and state announcements; and token mappings are all stated.
- Russian draughts identity and the existing two-disc mark now persist across home, launcher, focused game, mobile navigation and auth; game ruleset is named before Play.
- Only actual local functionality/data is used. Authenticated Chess.com home/archive remain disclosed as unavailable observations, not fabricated evidence.
- `/faq`: отдельная editorial knowledge page; существующий поиск сохраняет instant filtering, раскрывающиеся ответы остаются details/клавиатурными.
- `/login`, `/register`: отдельная standalone auth surface; имена, ошибки, guest action, значения полей и язык остаются.

### Foundation tokens

- Keep existing CSS variables as the only source of truth: `--bg #302e2b`, `--surface #262421`, `--surface-alt #3d3b38`, `--ink #f7f6f5`, `--muted #c3c0bc`, `--accent #81b64c`, `--accent-hover #95c65f`, `--accent-ink #18220e`, `--focus #abd573`, `--line #53504b` (decorative dividers only), `--line-strong #8b8882` (visible controls and error boundary). The current `#68645f` is below3:1 on panel surfaces and must not identify an interactive boundary. Reuse/update aliases in-place; do not add a parallel token layer. Board stays `#ebecd0/#779556`. No gradients, blur, glass, glow or AI image.
- Local Inter / Noto Sans Armenian remains; body 16 px; main title 30–32 px, section titles 20–24 px, secondary content at least 14 px; language-independent line-height and HY/RU wrap.
- Spacing scale 4/8/12/16/24/32/48; standard content max-width1180px; launcher/game use explicit board+rail widths above; FAQ 680–760px, auth400–440px. Board width is min of available width and available dynamic viewport height after player/header/control blocks; document scroll takes precedence over too-small squares or clipped controls. Exact shell breakpoint: Play wide composition≥1200px; standard nav drawer≤900px; game scales to one column≤1200px while its full menu is still available.
- Surfaces use flat solid fills and 1px borders; corners mostly 3–6px for buttons/inputs and 6–8px for panels; no capsule taxonomy, no card around every paragraph. Green primarily identifies main action, active player/turn or true success—not every icon.
- Interactive boundary uses `--line-strong`, tested ≥3:1 against every adjacent enabled surface; normal text ≥4.5:1, large text ≥3:1; visible focus uses `--focus` plus outline, not color alone. Decorative dividers may use `--line`. All states: hover, focus-visible, pressed, disabled, busy, success, error; transition140–180ms; honor reduced motion. Ordinary controls target44×44px; the 8×8 board squares are a spatial-control exception constrained by screen width, with single-tap and fully keyboard-operable selection (never drag-only). Do not disable pinch/browser zoom; verify 200% text and400% zoom reflow.

### Mapping each issue to design

- Duplicate lobby/play → desktop and mobile diagrams below prove different first screens; home contains no large board and Play loses the home feed.
- Guest home/personal states → conditional layouts below omit absent modules; retry independently on active game, recent games and daily puzzle. Sign-in is never required to start an online or bot game.
- Guest rating/history emptiness → guest/new-account/registered/populated/error layouts below use true data only, with no fake rows or empty stat tiles.
- Game navigation reducing board width → named compact header + complete menu; measure board max/available dimensions and verify all locale/account routes from drawer.
- Puzzle board density and reading order → objective precedes board in DOM and sightline; feedback/actions follow the same board without moving it.
- Identical panel language → home open action/list, Play separated launcher rows, puzzle board/control rail, stats typography, archive table/list, FAQ separators and auth form. Shared tokens bind them; no universal card grid.

## Design approval gate

План просмотрен одной и той же шестёркой независимых субагентов в двух раундах. В первом раунде один эксперт одобрил, пять потребовали доработок. После исправлений все шесть отдельно выдали **APPROVED**, unresolved critical нет. Это экспертная оценка плана по исходникам/скриншотам, не пользовательское исследование и не одобрение ещё не реализованного UI.

| Роль | Visual | Usability | Consistency | Accessibility | Originality | Product fit | Итог |
|---|---:|---:|---:|---:|---:|---:|---|
| Senior Product Designer | 8 | 9 | 9 | 9 | 6 | 9 | APPROVED |
| Art Director | 8 | 9 | 9 | 8 | 7 | 9 | APPROVED |
| UI Systems Architect | 8 | 8 | 8 | 8 | 7 | 9 | APPROVED |
| Accessibility & Usability | 8 | 9 | 9 | 9 | 7 | 9 | APPROVED |
| Brand & Product Identity | 8 | 9 | 9 | 8 | 7 | 9 | APPROVED |
| Anti-AI Red Team | 8 | 8 | 8 | 8 | 7 | 9 | APPROVED |

Заключения и полный протокол двух раундов сохраняются в [reviews/page-revision](reviews/page-revision). Ревью одобрило реализацию именно описанной системы; разрешение не распространяется на принятие результата до просмотра реальных пост-implementation скриншотов.

### Обязательные execution notes reviewers

- При старте поиска `Найти соперника` скрывается; фокус переносится на `Отменить поиск`/стабильный статус. После отмены или ошибки он возвращается на `Найти соперника`. Существующие cancellation/matched race защитить.
- Успешно созданное приглашение оставляет реальную ссылку, `Copy` и `Open game`; фокус уходит к созданному actionable region. При ошибке сохранять код/сложность.
- Русские и английские строки Play/Friend якорей сохраняют locale и приводят/фокусируют соответствующую строку без автоматического запуска партии. Ровно900px проверяется как граница drawer, 1200px — как граница board preview.
- При1280×720 и коротком landscape правая панель следует за фактическим меньшим размером доски; нельзя оставлять искусственную пустую полосу ради координаты x948, которая служит ориентиром только для1440×1000.
- Static preview и daily thumbnail не запускают `BoardUI.create`, чтобы не получить grid semantics и 64 keyboard tab stops; рендерятся как одно доступное изображение.
- Подтверждённое zero-account state согласованно показано на Home и Progress: реальные W/L/D zeros плюс отсутствие 0% win-rate и ясное объяснение. Ошибка запроса никогда не выводится как guest/empty.
- На доске сохраняется текущий двухцветный focus outline: зелёный focus-токен сам по себе недостаточно контрастен на тёмной клетке. Проверить selected/dark/light square и оба цвета фигуры.
- При разметке даты истории использовать поля DTO `startedAt`/`finishedAt` (оба существуют в `GameSummaryResponse`); завершённые строки сортировать/подписывать по `finishedAt` и не подменять их временем создания.

Итерация1→2 устранила замечания о точности компоновок/пустых состояний, реальном online-first потоке, отдельной задаче страниц, контрасте интерактивных границ, полном navigation menu, screen-reader порядке/сообщениях, zoom/короткой высоте, интеграции `/home` с security/SEO/i18n, владении shell/JS компонентами и локализованной идентичности продукта. Исполнение и rendered fidelity проверяются заново после кода.
