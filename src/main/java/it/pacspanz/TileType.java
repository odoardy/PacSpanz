package it.pacspanz;

public enum TileType {
    WALL,
    PELLET,
    POWER_PELLET,
    TUNNEL,
    EMPTY,
    PLAYER_SPAWN,
    GHOST_SPAWN;

    public static TileType fromSymbol(char symbol) {
        return switch (symbol) {
            case '#' -> WALL;
            case '.' -> PELLET;
            case 'o' -> POWER_PELLET;
            case 'T' -> TUNNEL;
            case 'P' -> PLAYER_SPAWN;
            case 'G' -> GHOST_SPAWN;
            default -> EMPTY;
        };
    }

    public boolean isConsumable() {
        return this == PELLET || this == POWER_PELLET;
    }
}
