package com.checkers.assistant;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Curated FAQ pairs per locale (same content as the public /faq page).
 */
@Component
public class FaqKnowledgeBase {

    private final Map<String, List<FaqEntry>> byLocale = Map.of(
            "en", List.of(
                    entry("start",
                            "How do I start a game?",
                            "Open the lobby, sign in (or continue as guest), then pick a mode: friend, random opponent, or bot. Press the green action button for that mode.",
                            "start", "begin", "play", "game", "how", "lobby"),
                    entry("friend",
                            "How do I play with a friend?",
                            "Choose “Play a friend”, create an invite, copy the link or code, and send it to your friend. They open the link or enter the code on the lobby.",
                            "friend", "invite", "code", "link", "share", "private"),
                    entry("matchmaking",
                            "How does matchmaking work?",
                            "Choose “Random opponent” and wait in the queue. When another player is available, you are paired automatically. You can cancel the search anytime.",
                            "random", "matchmaking", "queue", "opponent", "find", "search"),
                    entry("bot",
                            "How do I play against the bot?",
                            "Choose “Play bot”, pick Easy / Medium / Hard, then start. The bot thinks asynchronously and sends moves on the same live channel as a human.",
                            "bot", "computer", "ai", "difficulty", "easy", "hard"),
                    entry("king",
                            "What is a king (flying dame)?",
                            "In Russian draughts a man that reaches the last rank becomes a king (дамка). Kings move and capture any distance along a diagonal.",
                            "king", "dame", "damka", "flying", "promote", "promotion"),
                    entry("capture",
                            "Are captures mandatory?",
                            "Yes — if you can capture, you must. You may choose which capture sequence to take; the longest one is not required (unlike international draughts).",
                            "capture", "take", "jump", "mandatory", "must"),
                    entry("language",
                            "How do I change the language?",
                            "Use EN / RU / HY in the site header. Registered users also save the choice to their profile so it persists across sessions.",
                            "language", "locale", "english", "russian", "armenian", "translate"),
                    entry("guest",
                            "What can guests do?",
                            "Guests can play all modes, but game history and win/loss stats are not saved. Create an account to keep your record.",
                            "guest", "anonymous", "history", "stats", "account")
            ),
            "ru", List.of(
                    entry("start",
                            "Как начать игру?",
                            "Откройте лобби, войдите (или продолжите как гость) и выберите режим: с другом, случайный соперник или бот. Нажмите зелёную кнопку действия.",
                            "начать", "старт", "игра", "играть", "лобби", "как"),
                    entry("friend",
                            "Как играть с другом?",
                            "Выберите «С другом», создайте приглашение, скопируйте ссылку или код и отправьте другу. Друг открывает ссылку или вводит код в лобби.",
                            "друг", "другом", "друга", "друзьями", "приглашение", "код", "ссылка", "комната"),
                    entry("matchmaking",
                            "Как работает подбор соперника?",
                            "Выберите «Случайный соперник» и ждите в очереди. Когда появится второй игрок, вас соединят автоматически. Поиск можно отменить.",
                            "случайный", "подбор", "очередь", "соперник", "матчмейкинг", "поиск"),
                    entry("bot",
                            "Как играть с ботом?",
                            "Выберите «Против бота», укажите сложность и начните партию. Бот думает асинхронно и шлёт ход по тому же WebSocket-каналу.",
                            "бот", "компьютер", "сложность", "ии", "лёгкий", "сложный"),
                    entry("king",
                            "Что такое дамка?",
                            "В русских шашках простая шашка, дошедшая до последней горизонтали, становится дамкой. Дамка ходит и бьёт на любое расстояние по диагонали.",
                            "дамка", "ферзь", "превращение", "летающая", "король"),
                    entry("capture",
                            "Обязательно ли бить?",
                            "Да — если взятие возможно, оно обязательно. Какую цепочку взять — выбираете вы; максимальное взятие не требуется (в отличие от международных шашек).",
                            "бить", "взятие", "обязательно", "удар", "прыжок"),
                    entry("language",
                            "Как сменить язык?",
                            "Переключатель EN / RU / HY в шапке сайта. У зарегистрированных пользователей выбор также сохраняется в профиле.",
                            "язык", "локаль", "русский", "английский", "армянский"),
                    entry("guest",
                            "Что может гость?",
                            "Гость играет во всех режимах, но история и статистика не сохраняются. Зарегистрируйтесь, чтобы копить результат.",
                            "гость", "история", "статистика", "аккаунт", "регистрация")
            ),
            "hy", List.of(
                    entry("start",
                            "Ինչպե՞ս սկսել խաղը",
                            "Բացեք լոբբին, մտեք (կամ շարունակեք որպես հյուր) և ընտրեք ռեժիմ՝ ընկեր, պատահական մրցակից կամ բոտ։ Սեղմեք կանաչ կոճակը։",
                            "սկսել", "խաղ", "լոբբի", "ինչպես", "սկիզբ"),
                    entry("friend",
                            "Ինչպե՞ս խաղալ ընկերոջ հետ",
                            "Ընտրեք «Ընկերոջ հետ», ստեղծեք հրավեր, պատճենեք հղումը կամ կոդը և ուղարկեք ընկերոջը։ Նա բացում է հղումը կամ մուտքագրում կոդը։",
                            "ընկեր", "հրավեր", "կոդ", "հղում", "սենյակ"),
                    entry("matchmaking",
                            "Ինչպե՞ս է աշխատում մրցակցի ընտրությունը",
                            "Ընտրեք «Պատահական մրցակից» և սպասեք հերթում։ Երբ հայտնվի մեկ այլ խաղացող, ձեզ կմիացնեն ավտոմատ։ Որոնումը կարելի է չեղարկել։",
                            "պատահական", "հերթ", "մրցակից", "որոնում", "զույգ"),
                    entry("bot",
                            "Ինչպե՞ս խաղալ բոտի դեմ",
                            "Ընտրեք «Բոտի դեմ», նշեք բարդությունը և սկսեք։ Բոտը մտածում է ասինխրոն և քայլն ուղարկում է նույն ալիքով։",
                            "բոտ", "համակարգիչ", "բարդություն", "հեշտ", "դժվար"),
                    entry("king",
                            "Ի՞նչ է թագուհին (դամկան)",
                            "Ռուսական շաշկիում պարզ շաշկին, հասնելով վերջին հորիզոնական, դառնում է դամկա։ Դամկան քայլում և հարվածում է անկյունագծով ցանկացած հեռավորությամբ։",
                            "դամկա", "թագուհի", "վերածում", "թռչող"),
                    entry("capture",
                            "Հարվածը պարտադիր է՞",
                            "Այո՝ եթե հարված հնարավոր է, այն պարտադիր է։ Որ շղթան ընտրել՝ ձեր որոշումն է. ամենաերկարը պարտադիր չէ։",
                            "հարված", "պարտադիր", "վերցնել", "ցատկ"),
                    entry("language",
                            "Ինչպե՞ս փոխել լեզուն",
                            "Օգտագործեք EN / RU / HY վերնագրում։ Գրանցված օգտատերերի համար ընտրությունը պահվում է նաև պրոֆիլում։",
                            "լեզու", "հայերեն", "անգլերեն", "ռուսերեն"),
                    entry("guest",
                            "Ի՞նչ կարող է հյուրը",
                            "Հյուրը խաղում է բոլոր ռեժիմներում, բայց պատմությունն ու վիճակագրությունը չեն պահվում։ Գրանցվեք՝ արդյունքը պահելու համար։",
                            "հյուր", "պատմություն", "վիճակագրություն", "հաշիվ")
            )
    );

    public List<FaqEntry> entries(String locale) {
        String key = normalizeLocale(locale);
        return byLocale.getOrDefault(key, byLocale.get("en"));
    }

    public String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "en";
        }
        String lang = locale.toLowerCase(Locale.ROOT);
        if (lang.startsWith("ru")) {
            return "ru";
        }
        if (lang.startsWith("hy") || lang.startsWith("arm")) {
            return "hy";
        }
        return "en";
    }

    private static FaqEntry entry(String id, String q, String a, String... keywords) {
        return new FaqEntry(id, q, a, List.of(keywords));
    }
}
