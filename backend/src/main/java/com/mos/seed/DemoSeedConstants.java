package com.mos.seed;

import java.util.List;
import java.util.UUID;

public final class DemoSeedConstants {

    public static final UUID SESSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final String DEMO_SESSION_NAME = "M-OS Demo Game";

    public record SeedAccount(String username, String password, String nickname) {
    }

    public static final SeedAccount HOST_ADMIN = new SeedAccount("marat", "Kv7nR2xP", "Марат");

    public static final List<SeedAccount> PARTY_PLAYERS = List.of(
            new SeedAccount("amina", "Tg4mW9qL", "Амина"),
            new SeedAccount("leha", "Hb8cN3vR", "Леха"),
            new SeedAccount("nastya", "Px2kF7sD", "Настя"),
            new SeedAccount("sasha_sh", "Jm6yQ1wE", "Саша Школьников"),
            new SeedAccount("rita", "Za9tL4nC", "Маргарита"),
            new SeedAccount("nikita", "Ru5bV8mK", "Никита"),
            new SeedAccount("sasha_k", "We3hP6xS", "Саша Костюшин"),
            new SeedAccount("arina", "Yq7dN2fG", "Арина"),
            new SeedAccount("daniil", "Ck4sM9jT", "Даниил"),
            new SeedAccount("anton", "Fn8wR5pL", "Антон"),
            new SeedAccount("yana", "Dx1vH6qB", "Яна"),
            new SeedAccount("egor", "Sp3kT8nW", "Егор"),
            new SeedAccount("lera", "Mj7cY2rF", "Лера"),
            new SeedAccount("ruslan", "Bq5xL9vH", "Руслан"),
            new SeedAccount("lilya", "Gw2nP4sK", "Лиля"),
            new SeedAccount("pasha", "Vt6mC8dR", "Паша"),
            new SeedAccount("nadya", "Hk9fW3qE", "Надя"),
            new SeedAccount("katya", "Ln4jS7xA", "Катя")
    );

    private DemoSeedConstants() {
    }
}
