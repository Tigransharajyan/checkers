# Переработка по Chess.com — текущая версия

Пользователь отклонил предыдущий вариант и прямо попросил скопировать дизайн Chess.com. Эта версия заменяет ранее выбранный graphite/olive dashboard.

## Что изучено
Реально открыты публичные [Chess.com Play](https://www.chess.com/play), [Computer](https://www.chess.com/play/computer) и [главная](https://www.chess.com/). Исходные настоящие browser screenshots: [Play](screenshots/play.png), [Computer](screenshots/computer.png). Предыдущий аудит всех маршрутов и7 других референсов остаётся в [общем отчёте](../REPORT.md).

Основное отличие от предыдущего результата — композиция, а не палитра: большая доска слева, игроки над/под доской, компактная боковая навигация и узкое меню справа. Выбран именно игровой /play интерфейс, а не маркетинговая главная.

## Реализация
- Sidebar170px, нейтральные тёмные поверхности #302e2b/#262421, зелёный акцент #81b64c, крупные подписанные режимы справа. Для армянского sidebar200px, чтобы длинные слова читались целиком при том же16px шрифте.
- Большая доска #ebecd0/#779556 с собственными шашками. На главной это явно подписанный неинтерактивный предварительный просмотр, а не вымышленная активная партия.
- В настоящей партии игроки перенесены к доске, статус/действия/нотация — вправо. Сохраняются двигатели,API,auth,drag,keyboard,premove,аннотации и live-status.
- На мобильном действия первыми и в DOM, и визуально. Десктопная компоновка задаётся grid, без перестановки Tab через CSS order и без положительных tabindex.
- Общий выбор режима используется на главной и /play; задачи, приглашения, история и статистика продолжают использовать настоящие данные.
- Никаких Chess.com логотипов, чужих персонажей, фиктивных турниров или отсутствующих функций. Градиенты,AI-картинки,glow/blur не добавлены. Шрифты/иконки остаются локальными, новых runtime-зависимостей нет.

## Независимое ревью
Шесть настоящих субагентов: Product, Art, Systems, Accessibility, Brand, Red Team.1раунд плана до изменения кода и2раунда фактического UI. Первый post-review выявил два дефекта: orphan letters вHY-sidebar и неверный mobile Tab-order. Оба исправлены и лично повторно проверены экспертами. Итог:6 APPROVED, критических нерешённых замечаний нет. [Все заключения](reviews).

## Проверка
-54 Java-теста, успешный mvnw package. Последняя пересборка после удаления пробела выполнена с-DskipTests; функциональные тесты не менялись.
-8/8 browser regressions: реальные партии/бот/друг/ничья, задачи, анимации, premove, keyboard, auth/errors,drawer/focus,invitations,matchmaking cancellation. Добавлена регрессия первого Tab из main.
-384 состояния страниц EN/RU/HY,320–1920px включая границы breakpoint: HTTP200,overflow0,missing translations0,duplicate IDs0,rendered gradients0,JSerrors0.
-axe58 состояний: violations0. Остались incomplete contrast у auth-home-link и assistant text; это ограничение автоматической оценки. Основные сплошные цвета читаемы, белый текст на ярко-зелёной CTA намеренно заменён тёмным для AA.
-Реальный дополнительный сценарный проход registration→bot→resign→history/progress,invite,FAQ,helper,wrong/solved puzzle,network errors,matchmaking двух сессий: runtime errors0.
-Не проводились native screen-reader с речью, тесты физических телефонов и production performance/deployment. Не выдаём экспертные оценки за пользовательское исследование.

Логи и JSON лежат рядом с этим отчётом. Полные screenshots: `/home/tigran/.codex/visualizations/2026/11/06/01a1b1ab-c07f-7bf1-ad89-4050299961b4/chesscom-revision/after/`.

## Визуальный результат
[Ранее отклонённый экран](screenshots/before-home.png), [новый desktop](screenshots/final-desktop.png), [mobile](screenshots/home-390.png), [HY](screenshots/home-hy-1440.png), [настоящая партия](screenshots/game-registered-1440.png).

Изменения остаются в существующей ветке codex/redesign-checkers. Предшествующие незакоммиченные изменения сохранены; дополнительный архив до этой правки находится в /tmp/checkers-chesscom/pre-revision-resources.tar.gz. Локальный сайт оставлен работающим по явному запросу пользователя.
