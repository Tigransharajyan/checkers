package com.checkers.assistant;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Curated English, Russian and Armenian questions and answers shared by the FAQ page and local
 * assistant.
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
                            "Choose “Play bot”, pick Easy / Medium / Hard, then start. You can practise at three difficulty levels. The same movement, capture and promotion rules apply as in online games.",
                            "bot", "computer", "ai", "difficulty", "easy", "hard"),
                    entry("board", "How is the Russian checkers board set up?", "The board has 8×8 squares. Each side begins with 12 men on the dark squares of its first three ranks. White moves first.", "board", "setup", "доска", "դասավորություն"),
                    entry("men", "How do ordinary pieces move and capture?", "Men move one square diagonally forward. They can capture in either direction by jumping over an adjacent opposing piece to an empty square. If another capture is available, continue the same turn with that piece.", "men", "movement", "простая", "պարզ"),
                    entry("promotion", "What happens if a piece becomes a king during a capture?", "A man becomes a king as soon as it reaches the last rank. If another capture is available during that turn, it continues the capture as a flying king.", "promotion capture", "превращение взятие", "վերածում հարված"),
                    entry("king",
                            "What is a king (flying dame)?",
                            "In Russian draughts a man that reaches the last rank becomes a king (дамка). Kings move and capture any distance along a diagonal.",
                            "king", "dame", "damka", "flying", "promote", "promotion"),
                    entry("capture",
                            "Are captures mandatory?",
                            "Yes — if you can capture, you must. You may choose which capture sequence to take; the longest one is not required (unlike international draughts).",
                            "capture", "take", "jump", "mandatory", "must"),
                    entry("draw", "When does a game end in a draw?", "Automatic draws cover threefold repetition, kings-only limits of 15/25 moves each, unchanged-material limits of 5/30/60 moves each, and the special lone-king endings. Captures and promotions reset the appropriate counters. The same rules apply online and against the bot. Players can agree to a draw. A bot may offer one after a stable, balanced king ending; declining continues the game. An approximate evaluation never forces a draw.", "draw", "repetition", "ending", "king"),
                    entry("premoves", "How do I queue several premoves?", "While waiting for your opponent, click or drag several moves. Your pieces show their projected positions, so you can select the same piece again on its future square. Moves run in order after checking the actual legal moves. An impossible move cancels the rest. Right-click or Escape clears the queue. Mandatory multi-captures remain one complete turn.", "premove", "queue", "multiple"),
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
                            "Выберите «Против бота», укажите сложность и начните партию. Доступны три уровня сложности. Правила ходов, взятия и превращения в дамку такие же, как в игре с человеком.",
                            "бот", "компьютер", "сложность", "ии", "лёгкий", "сложный"),
                    entry("board", "Как устроена доска для русских шашек?", "Доска состоит из 8×8 клеток. У каждого игрока по 12 простых шашек на тёмных полях первых трёх горизонталей. Первыми ходят белые.", "board", "setup", "доска", "դասավորություն"),
                    entry("men", "Как ходят и бьют простые шашки?", "Простая шашка ходит на одно поле по диагонали вперёд. Бить можно в обоих направлениях: перепрыгните через соседнюю шашку соперника на свободное поле за ней. Если этой же шашкой можно бить дальше, продолжайте взятие в тот же ход.", "men", "movement", "простая", "պարզ"),
                    entry("promotion", "Что происходит при превращении в дамку во время взятия?", "Шашка становится дамкой сразу при достижении последней горизонтали. Если во время этого хода есть дальнейшее взятие, она продолжает бить уже как дамка.", "promotion capture", "превращение взятие", "վերածում հարված"),
                    entry("king",
                            "Что такое дамка?",
                            "В русских шашках простая шашка, дошедшая до последней горизонтали, становится дамкой. Дамка ходит и бьёт на любое расстояние по диагонали.",
                            "дамка", "ферзь", "превращение", "летающая", "король"),
                    entry("capture",
                            "Обязательно ли бить?",
                            "Да — если взятие возможно, оно обязательно. Какую цепочку взять — выбираете вы; максимальное взятие не требуется (в отличие от международных шашек).",
                            "бить", "взятие", "обязательно", "удар", "прыжок"),
                    entry("draw", "Когда объявляется ничья?", "Автоматически проверяются троекратное повторение, 15/25 ходов каждой стороны только дамками, 5/30/60 ходов без изменения соотношения сил и специальные окончания против одинокой дамки. Взятия и превращения сбрасывают соответствующие счётчики. Правила одинаковы онлайн и с ботом. Игроки могут договориться о ничьей. Бот может предложить её в устойчивом равном дамочном окончании; отказ продолжает партию. Приблизительная оценка не завершает игру автоматически.", "ничья", "повторение", "окончание", "дамки"),
                    entry("premoves", "Как сделать несколько премувов?", "Пока ходит соперник, задайте несколько ходов кликами или перетаскиванием. Шашки показываются на будущих полях, поэтому одну шашку можно выбрать повторно. Очередь выполняется по порядку с проверкой реальных допустимых ходов. Невозможный ход отменяет остаток очереди. Правая кнопка мыши или Escape очищает её. Серия обязательных взятий остаётся одним полным ходом.", "премув", "очередь", "предварительный ход"),
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
                            "Ընտրեք «Բոտի դեմ», նշեք բարդությունը և սկսեք։ Կան բարդության երեք մակարդակներ։ Քայլերի, հարվածների և դամա դառնալու կանոնները նույնն են, ինչ մարդու հետ խաղում։",
                            "բոտ", "համակարգիչ", "բարդություն", "հեշտ", "դժվար"),
                    entry("board", "Ինչպե՞ս է դասավորվում ռուսական շաշկու խաղատախտակը", "Խաղատախտակը բաղկացած է 8×8 վանդակներից։ Յուրաքանչյուր կողմն ունի 12 պարզ խաղաքար՝ առաջին երեք շարքերի մուգ վանդակներում։ Սպիտակներն են սկսում։", "board", "setup", "доска", "դասավորություն"),
                    entry("men", "Ինչպե՞ս են քայլում և հարվածում պարզ խաղաքարերը", "Պարզ խաղաքարը քայլում է մեկ վանդակ անկյունագծով առաջ։ Հարվածել կարելի է երկու ուղղությամբ՝ ցատկելով մրցակցի հարևան խաղաքարի վրայով դեպի դրա հետևի ազատ վանդակը։ Եթե նույն խաղաքարով հնարավոր է նոր հարված, այն պետք է շարունակել նույն քայլի ընթացքում։", "men", "movement", "простая", "պարզ"),
                    entry("promotion", "Ի՞նչ է լինում, եթե խաղաքարը հարվածի ընթացքում դառնում է դամա", "Խաղաքարը դամա է դառնում վերջին շարքին հասնելուն պես։ Եթե նույն քայլի ընթացքում հնարավոր է շարունակել հարվածը, այն շարունակում է արդեն որպես դամա։", "promotion capture", "превращение взятие", "վերածում հարված"),
                    entry("king",
                            "Ի՞նչ է թագուհին (դամկան)",
                            "Ռուսական շաշկիում պարզ շաշկին, հասնելով վերջին հորիզոնական, դառնում է դամկա։ Դամկան քայլում և հարվածում է անկյունագծով ցանկացած հեռավորությամբ։",
                            "դամկա", "թագուհի", "վերածում", "թռչող"),
                    entry("capture",
                            "Հարվածը պարտադիր է՞",
                            "Այո՝ եթե հարված հնարավոր է, այն պարտադիր է։ Որ շղթան ընտրել՝ ձեր որոշումն է. ամենաերկարը պարտադիր չէ։",
                            "հարված", "պարտադիր", "վերցնել", "ցատկ"),
                    entry("draw", "Ե՞րբ է խաղն ավարտվում ոչ-ոքի։", "Ավտոմատ ստուգվում են դիրքի եռակի կրկնությունը, միայն դամերով 15/25 քայլի, ուժերի անփոփոխ հարաբերակցությամբ 5/30/60 քայլի սահմանները և միակ դամի դեմ հատուկ վերջնախաղերը։ Հարվածն ու դամի վերածումը զրոյացնում են համապատասխան հաշվիչները։ Նույն կանոնները գործում են առցանց և բոտի դեմ։ Ոչ-ոքին հնարավոր է նաև համաձայնությամբ։ Բոտի առաջարկը մերժելուց հետո խաղը շարունակվում է. մոտավոր գնահատումը խաղը չի ավարտում։", "ոչ-ոքի", "կրկնություն", "դամ"),
                    entry("premoves", "Ինչպե՞ս նախապես հերթագրել մի քանի քայլ։", "Մրցակցի հերթի ընթացքում ընտրեք մի քանի քայլ՝ սեղմելով կամ քաշելով խաղաքարերը։ Դրանք ցույց են տալիս ապագա դիրքերը, ուստի նույն խաղաքարը կարելի է նորից ընտրել ապագա դաշտում։ Քայլերը կատարվում են հերթով՝ օրինականությունը ստուգելուց հետո։ Անհնար քայլը չեղարկում է մնացած հերթը։ Մկնիկի աջ կոճակը կամ Escape-ը մաքրում է հերթը։ Պարտադիր բազմակի հարվածը մնում է մեկ ամբողջական քայլ։", "նախնական քայլ", "հերթ"),
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

    /**
     * Returns curated questions, answers and search keywords for a supported language. Falls back
     * to English for an unknown locale.
     */
    public List<FaqEntry> entries(String locale) {
        String key = normalizeLocale(locale);
        return byLocale.getOrDefault(key, byLocale.get("en"));
    }

    /**
     * Normalizes supported languages to en, ru or hy, including regional variants and the arm
     * alias. Uses English for blank or unsupported values.
     */
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

    /**
     * Creates an immutable knowledge-base entry containing its identifier, question, answer and
     * keywords. Keeps the multilingual catalogue declarations concise.
     */
    private static FaqEntry entry(String id, String q, String a, String... keywords) {
        return new FaqEntry(id, q, a, List.of(keywords));
    }
}
