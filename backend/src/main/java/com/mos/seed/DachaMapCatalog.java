package com.mos.seed;

import java.util.List;

/**
 * Default dacha map spots for lore promo placement (physical printouts).
 * Coordinates are percent of the map image (0–100).
 */
public final class DachaMapCatalog {

    public record MapSpot(
            String name,
            String description,
            double x,
            double y,
            String zone,
            boolean hidden
    ) {
        public String loreCode() {
            int index = Integer.parseInt(name.replaceAll("\\D+", "")) - 1;
            return LoreCatalog.FRAGMENTS.get(index).code();
        }
    }

    /**
     * Official start-of-game positions (captured from admin map layout).
     */
    public static final List<MapSpot> SPOTS = List.of(
            new MapSpot("Lor1", "Вкуснота", 21.0, 19.8, "участок", false),
            new MapSpot("Lor2", "Банька парилка", 42.5, 13.6, "ух красиво", false),
            new MapSpot("Lor3", "Не обязательно в", 81.0, 82.2, "Дом", false),
            new MapSpot("Lor4", "Теплоооо", 63.6, 20.3, "Беседка", false),
            new MapSpot("Lor5", "Угол", 22.0, 52.0, "Забор", false),
            new MapSpot("Lor6", "Тропинка", 48.0, 55.0, "Сад", false),
            new MapSpot("Lor7", "Тень", 63.4, 63.9, "Деревья", false),
            new MapSpot("Lor8", "Край", 62.0, 80.1, "Задний двор", false)
    );

    private DachaMapCatalog() {
    }
}
