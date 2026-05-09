package it.pacspanz;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameMap {
    public static final int TILE_SIZE = 40;

    private static final String[] LAYOUT = {
            "#########T#########",
            "#o...............o#",
            "#.###.###.###.###.#",
            "#.................#",
            "#.#.#.#####.#.#.#.#",
            "#....#...#...#....#",
            "####.#.#.#.#.#.####",
            "T....#G G G G#....T",
            "####.#.#.#.#.#.####",
            "#....#...#...#....#",
            "#.#.#.#####.#.#.#.#",
            "#........P........#",
            "#.###.###.###.###.#",
            "#o...............o#",
            "#########T#########"
    };

    private TileType[][] tiles;
    private Point playerSpawn;
    private final List<Point> ghostSpawns = new ArrayList<>();
    private int remainingPellets;

    public GameMap() {
        reset();
    }

    public final void reset() {
        int height = LAYOUT.length;
        int width = LAYOUT[0].length();
        tiles = new TileType[height][width];
        playerSpawn = null;
        ghostSpawns.clear();
        remainingPellets = 0;

        for (int y = 0; y < height; y++) {
            if (LAYOUT[y].length() != width) {
                throw new IllegalStateException("Tutte le righe della mappa devono avere la stessa larghezza.");
            }

            for (int x = 0; x < width; x++) {
                TileType tileType = TileType.fromSymbol(LAYOUT[y].charAt(x));
                if (tileType == TileType.PLAYER_SPAWN) {
                    playerSpawn = new Point(x, y);
                    tiles[y][x] = TileType.EMPTY;
                } else if (tileType == TileType.GHOST_SPAWN) {
                    ghostSpawns.add(new Point(x, y));
                    tiles[y][x] = TileType.EMPTY;
                } else {
                    tiles[y][x] = tileType;
                    if (tileType.isConsumable()) {
                        remainingPellets++;
                    }
                }
            }
        }

        if (playerSpawn == null) {
            throw new IllegalStateException("La mappa deve contenere uno spawn giocatore indicato con P.");
        }
        if (ghostSpawns.isEmpty()) {
            throw new IllegalStateException("La mappa deve contenere almeno uno spawn fantasma indicato con G.");
        }
    }

    public int getWidthInTiles() {
        return tiles[0].length;
    }

    public int getHeightInTiles() {
        return tiles.length;
    }

    public int getWidthInPixels() {
        return getWidthInTiles() * TILE_SIZE;
    }

    public int getHeightInPixels() {
        return getHeightInTiles() * TILE_SIZE;
    }

    public TileType getTile(int tileX, int tileY) {
        if (!isInside(tileX, tileY)) {
            return TileType.WALL;
        }
        return tiles[tileY][tileX];
    }

    public boolean isWall(int tileX, int tileY) {
        return getTile(tileX, tileY) == TileType.WALL;
    }

    public boolean isTunnel(int tileX, int tileY) {
        return getTile(tileX, tileY) == TileType.TUNNEL;
    }

    public boolean canMoveFrom(int tileX, int tileY, Direction direction) {
        if (direction == Direction.NONE) {
            return false;
        }

        int nextX = tileX + direction.dx();
        int nextY = tileY + direction.dy();
        if (isInside(nextX, nextY)) {
            return !isWall(nextX, nextY);
        }

        return canWrapFrom(tileX, tileY, direction);
    }

    public List<Direction> getAvailableDirections(int tileX, int tileY) {
        List<Direction> directions = new ArrayList<>();
        for (Direction direction : new Direction[]{Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT}) {
            if (canMoveFrom(tileX, tileY, direction)) {
                directions.add(direction);
            }
        }
        return directions;
    }

    public TileType consumeAt(int tileX, int tileY) {
        if (!isInside(tileX, tileY)) {
            return TileType.EMPTY;
        }

        TileType tileType = tiles[tileY][tileX];
        if (tileType.isConsumable()) {
            tiles[tileY][tileX] = TileType.EMPTY;
            remainingPellets--;
            return tileType;
        }
        return TileType.EMPTY;
    }

    public int getRemainingPellets() {
        return remainingPellets;
    }

    public Point getPlayerSpawn() {
        return new Point(playerSpawn);
    }

    public List<Point> getGhostSpawns() {
        List<Point> copies = new ArrayList<>();
        for (Point spawn : ghostSpawns) {
            copies.add(new Point(spawn));
        }
        return Collections.unmodifiableList(copies);
    }

    public int clampTileX(int tileX) {
        return Math.max(1, Math.min(getWidthInTiles() - 2, tileX));
    }

    public int clampTileY(int tileY) {
        return Math.max(1, Math.min(getHeightInTiles() - 2, tileY));
    }

    public Point getNeighborTile(int tileX, int tileY, Direction direction) {
        int nextX = tileX + direction.dx();
        int nextY = tileY + direction.dy();
        if (isInside(nextX, nextY)) {
            return new Point(nextX, nextY);
        }

        if (canWrapFrom(tileX, tileY, direction)) {
            return switch (direction) {
                case LEFT -> new Point(getWidthInTiles() - 1, tileY);
                case RIGHT -> new Point(0, tileY);
                case UP -> new Point(tileX, getHeightInTiles() - 1);
                case DOWN -> new Point(tileX, 0);
                case NONE -> new Point(tileX, tileY);
            };
        }

        return new Point(tileX, tileY);
    }

    public Point wrapEntityPosition(int pixelX, int pixelY) {
        int wrappedX = pixelX;
        int wrappedY = pixelY;
        int centerX = pixelX + TILE_SIZE / 2;
        int centerY = pixelY + TILE_SIZE / 2;

        int tileY = Math.floorDiv(Math.max(0, Math.min(getHeightInPixels() - 1, centerY)), TILE_SIZE);
        if (centerX < 0 && hasHorizontalTunnel(tileY)) {
            wrappedX = (getWidthInTiles() - 1) * TILE_SIZE;
        } else if (centerX >= getWidthInPixels() && hasHorizontalTunnel(tileY)) {
            wrappedX = 0;
        }

        int tileX = Math.floorDiv(Math.max(0, Math.min(getWidthInPixels() - 1, centerX)), TILE_SIZE);
        if (centerY < 0 && hasVerticalTunnel(tileX)) {
            wrappedY = (getHeightInTiles() - 1) * TILE_SIZE;
        } else if (centerY >= getHeightInPixels() && hasVerticalTunnel(tileX)) {
            wrappedY = 0;
        }

        return new Point(wrappedX, wrappedY);
    }

    private boolean canWrapFrom(int tileX, int tileY, Direction direction) {
        if (!isInside(tileX, tileY) || !isTunnel(tileX, tileY)) {
            return false;
        }

        return switch (direction) {
            case LEFT -> tileX == 0 && hasHorizontalTunnel(tileY);
            case RIGHT -> tileX == getWidthInTiles() - 1 && hasHorizontalTunnel(tileY);
            case UP -> tileY == 0 && hasVerticalTunnel(tileX);
            case DOWN -> tileY == getHeightInTiles() - 1 && hasVerticalTunnel(tileX);
            case NONE -> false;
        };
    }

    private boolean hasHorizontalTunnel(int tileY) {
        return isInside(0, tileY)
                && isTunnel(0, tileY)
                && isTunnel(getWidthInTiles() - 1, tileY);
    }

    private boolean hasVerticalTunnel(int tileX) {
        return isInside(tileX, 0)
                && isTunnel(tileX, 0)
                && isTunnel(tileX, getHeightInTiles() - 1);
    }

    private boolean isInside(int tileX, int tileY) {
        return tileY >= 0 && tileY < tiles.length && tileX >= 0 && tileX < tiles[0].length;
    }
}
