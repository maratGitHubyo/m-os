package com.mos.seed;

import com.mos.item.enums.ItemRarity;

import java.util.List;

public final class PartyItemCatalog {

    public record SeedItem(
            String codeSuffix,
            String name,
            String description,
            ItemRarity rarity,
            String imageFile
    ) {
        public String imageUrl() {
            return "/items/" + rarityFolder() + "/" + imageFile;
        }

        public String qrCode() {
            return "ITEM-" + codeSuffix;
        }

        private String rarityFolder() {
            return switch (rarity) {
                case COMMON -> "common";
                case RARE -> "rare";
                case EPIC -> "epic";
                case LEGENDARY -> "legendary";
            };
        }
    }

    public static final List<SeedItem> ITEMS = List.of(
            // LEGENDARY
            new SeedItem("BMW", "Чёрный BMW", "Легендарный чёрный BMW — символ статуса и праздника.", ItemRarity.LEGENDARY, "bmw.jpg"),
            new SeedItem("FAM", "Семья", "Самое важное — близкие рядом на большой арене жизни.", ItemRarity.LEGENDARY, "fam.jpg"),
            new SeedItem("GRANTA", "Гранта", "Белая Гранта с характером — верный спутник дорог.", ItemRarity.LEGENDARY, "granta.jpg"),
            new SeedItem("HOME", "Дом", "Твой дом: уют, экран и место, куда всегда хочется вернуться.", ItemRarity.LEGENDARY, "home.jpg"),
            new SeedItem("SNOWBOARD", "Сноуборд Escape", "Легендарная доска Escape для ночных склонов.", ItemRarity.LEGENDARY, "snowboard.jpg"),
            new SeedItem("VILLAGE", "Деревня", "Родной двор, бассейн и лето, которое не забывается.", ItemRarity.LEGENDARY, "village.jpg"),

            // EPIC
            new SeedItem("VERSHINA", "Вершина мира", "Руки в стороны на горном гребне — ты наверху.", ItemRarity.EPIC, "vershina-mira.jpg"),
            new SeedItem("SITI", "Огни Сити", "Ночные огни Москва-Сити у реки.", ItemRarity.EPIC, "ogni-siti.jpg"),
            new SeedItem("KREPOST", "Скамейка крепости", "Скамейка у каменных стен и зелёных гор.", ItemRarity.EPIC, "skamejka-kreposti.jpg"),
            new SeedItem("RAJDER", "Горчичный райдер", "Склон, доска и горчичная куртка под синим небом.", ItemRarity.EPIC, "gorchichnyj-rajder.jpg"),
            new SeedItem("VIRAZH", "Снежный вираж", "Резкий вираж мимо подъёмника и ёлок.", ItemRarity.EPIC, "snezhnyj-virazh.jpg"),
            new SeedItem("LEONARDO", "Посох Леонардо", "Детский ритуал передачи посоха черепашке-ниндзя.", ItemRarity.EPIC, "posoh-leonardo.jpg"),
            new SeedItem("HAOS", "Костюмный хаос", "Банда в безумных костюмах на закате.", ItemRarity.EPIC, "kostyumnyj-haos.jpg"),
            new SeedItem("RODINA", "Родина-мать", "Трое у монумента «Родина-мать зовёт!» в Волгограде.", ItemRarity.EPIC, "rodina-mat.jpg"),
            new SeedItem("TUR", "Красный тур", "Game Boy Tour: стань лучше себя.", ItemRarity.EPIC, "krasnyj-tur.jpg"),
            new SeedItem("KOVYOR", "Каньонный ковёр", "Красный ковёр над ущельем среди кувшинов.", ItemRarity.EPIC, "kanonnyj-kovyor.jpg"),
            new SeedItem("KARV", "Ночной карв", "Оранжевая куртка режет ночной склон.", ItemRarity.EPIC, "nochnoj-karv.jpg"),

            // RARE
            new SeedItem("BUNTAR", "Фишай-бунтарь", "Капюшон, сигарета и жест «рога» во дворе.", ItemRarity.RARE, "fishaj-buntar.jpg"),
            new SeedItem("SNIZU", "Совет снизу", "Трое друзей нависают над камерой.", ItemRarity.RARE, "sovet-snizu.jpg"),
            new SeedItem("KOLONIZATORY", "Колонизаторы ночи", "Партия в «Колонизаторы» на полу пустой квартиры.", ItemRarity.RARE, "kolonizatory-nochi.jpg"),
            new SeedItem("DIPLOM", "Братский диплом", "Аттестат и объятие у учебного корпуса.", ItemRarity.RARE, "bratskij-diplom.jpg"),
            new SeedItem("PREMIERA", "Грамота Премьеры", "Грамота школы танца «Премьера».", ItemRarity.RARE, "gramota-premery.jpg"),
            new SeedItem("SHARF", "Шарф болельщика", "Трибуна и шарф цветов Ак Барса.", ItemRarity.RARE, "sharf-bolelschika.jpg"),
            new SeedItem("STVOL", "Дымный ствол", "Синий свет, дым и дерзкий кадр.", ItemRarity.RARE, "dymnyj-stvol.jpg"),
            new SeedItem("ARKADA", "Зеркало аркады", "Зеркальное селфи среди игровых автоматов.", ItemRarity.RARE, "zerkalo-arkady.jpg"),
            new SeedItem("SMEHA", "Бочка смеха", "Двое хохочут в ржавой бочке на даче.", ItemRarity.RARE, "bochka-smeha.jpg"),
            new SeedItem("BASSEJN", "Дачный бассейн", "Забраться в бочку с водой под солнцем.", ItemRarity.RARE, "dachnyj-bassejn.jpg"),
            new SeedItem("TRIUMF", "Туалетный триумф", "Диплом и костюм среди голубых кабинок.", ItemRarity.RARE, "tualetnyj-triumf.jpg"),

            // COMMON
            new SeedItem("FLEKS", "Двойной флекс", "Друзья на дорожке: костюм и два телефона.", ItemRarity.COMMON, "dvojnoj-fleks.jpg"),
            new SeedItem("STENS", "Красный стэнс", "Заниженный красный спорткар на ночной парковке.", ItemRarity.COMMON, "krasnyj-stens.jpg"),
            new SeedItem("LENTY", "Алые ленты", "Выпускники с красными лентами под шарами.", ItemRarity.COMMON, "alye-lenty.jpg"),
            new SeedItem("ROKER", "Багажный рокер", "На открытом багажнике белой Лады зимой.", ItemRarity.COMMON, "bagazhnyj-roker.jpg"),
            new SeedItem("TATU", "Фейк-тату", "Красный пуховик, «тату» и жест у зеркала.", ItemRarity.COMMON, "fejk-tatu.jpg"),
            new SeedItem("HAKATON", "Кубок хакатона", "Первое место хакатона H2O.", ItemRarity.COMMON, "kubok-hakatona.jpg"),
            new SeedItem("KOLCO", "Шкатулка жениха", "Открытая шкатулка с кольцом в костюме.", ItemRarity.COMMON, "shkatulka-zheniha.jpg"),
            new SeedItem("BITMEJKER", "Битмейкер-магнат", "FL Studio и пачки купюр на клавиатуре.", ItemRarity.COMMON, "bitmejker-magnat.jpg"),
            new SeedItem("CODE", "Указатель судьбы", "Жест к логотипу CODE INSIDE.", ItemRarity.COMMON, "ukazatel-sudby.jpg"),
            new SeedItem("RAKETKA", "Красная ракетка", "Подача за зелёным теннисным столом.", ItemRarity.COMMON, "krasnaya-raketka.jpg"),
            new SeedItem("POCELUJ", "Цифровой поцелуй", "Поцелуй через рамки видеозвонка.", ItemRarity.COMMON, "cifrovoj-poceluj.jpg"),
            new SeedItem("ZVONOK", "Денежный звонок", "Пачка купюр у уха в жёлтой худи.", ItemRarity.COMMON, "denezhnyj-zvonok.jpg"),
            new SeedItem("OLIMPIJKA", "Ночная олимпийка", "Светлая олимпийка на ночной парковке.", ItemRarity.COMMON, "nochnaya-olimpijka.jpg")
    );

    private PartyItemCatalog() {
    }
}
