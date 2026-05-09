package it.pacspanz;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Ghost {
    public enum Behavior {
        AGGRESSIVE,
        RANDOM,
        INTERCEPT,
        SWITCHER
    }

    public static final int SPEED = 4;
    private static final int INTERCEPT_DISTANCE = 4;
    private static final int SWITCH_INTERVAL_TICKS = 180;

    private final Point spawn;
    private final Color baseColor;
    private final Behavior behavior;
    private final Random random = new Random();

    private int x;
    private int y;
    private Direction direction = Direction.NONE;

    public Ghost(Point spawn, Color baseColor, Behavior behavior) {
        this.spawn = new Point(spawn);
        this.baseColor = baseColor;
        this.behavior = behavior;
        resetToSpawn();
    }

    public void resetToSpawn() {
        x = spawn.x * GameMap.TILE_SIZE;
        y = spawn.y * GameMap.TILE_SIZE;
        direction = Direction.NONE;
    }

    public void update(GameMap map, Player player, boolean vulnerable, long tick) {
        if (isAlignedToGrid()) {
            int tileX = getAlignedTileX();
            int tileY = getAlignedTileY();
            List<Direction> options = map.getAvailableDirections(tileX, tileY);

            boolean currentBlocked = !map.canMoveFrom(tileX, tileY, direction);
            boolean atIntersection = options.size() >= 3;
            boolean atCorner = options.size() == 2 && !options.contains(direction);

            if (direction == Direction.NONE || currentBlocked || atIntersection || atCorner) {
                direction = chooseDirection(map, player, vulnerable, tick, tileX, tileY, options);
            }
        }

        if (direction.isMoving()) {
            x += direction.dx() * SPEED;
            y += direction.dy() * SPEED;
            Point wrappedPosition = map.wrapEntityPosition(x, y);
            x = wrappedPosition.x;
            y = wrappedPosition.y;
        }
    }

    private Direction chooseDirection(
            GameMap map,
            Player player,
            boolean vulnerable,
            long tick,
            int tileX,
            int tileY,
            List<Direction> options
    ) {
        if (options.isEmpty()) {
            return Direction.NONE;
        }

        List<Direction> candidates = new ArrayList<>(options);
        if (direction != Direction.NONE && candidates.size() > 1) {
            candidates.remove(direction.opposite());
            if (candidates.isEmpty()) {
                candidates = new ArrayList<>(options);
            }
        }

        if (vulnerable) {
            return chooseFarthestFromPlayer(map, player, tileX, tileY, candidates);
        }

        return switch (behavior) {
            case AGGRESSIVE -> chooseClosestToTarget(map, player.getCenterTileX(), player.getCenterTileY(), tileX, tileY, candidates);
            case RANDOM -> chooseRandom(candidates);
            case INTERCEPT -> chooseIntercepting(map, player, tileX, tileY, candidates);
            case SWITCHER -> {
                boolean chaseMode = (tick / SWITCH_INTERVAL_TICKS) % 2 == 0;
                yield chaseMode
                        ? chooseClosestToTarget(map, player.getCenterTileX(), player.getCenterTileY(), tileX, tileY, candidates)
                        : chooseRandom(candidates);
            }
        };
    }

    private Direction chooseIntercepting(GameMap map, Player player, int tileX, int tileY, List<Direction> candidates) {
        Direction playerDirection = player.getDirection();
        int targetX = player.getCenterTileX() + playerDirection.dx() * INTERCEPT_DISTANCE;
        int targetY = player.getCenterTileY() + playerDirection.dy() * INTERCEPT_DISTANCE;
        return chooseClosestToTarget(map, map.clampTileX(targetX), map.clampTileY(targetY), tileX, tileY, candidates);
    }

    private Direction chooseClosestToTarget(
            GameMap map,
            int targetX,
            int targetY,
            int tileX,
            int tileY,
            List<Direction> candidates
    ) {
        Collections.shuffle(candidates, random);
        return candidates.stream()
                .min(Comparator.comparingInt(direction -> distanceAfterMove(map, tileX, tileY, direction, targetX, targetY)))
                .orElse(Direction.NONE);
    }

    private Direction chooseFarthestFromPlayer(GameMap map, Player player, int tileX, int tileY, List<Direction> candidates) {
        Collections.shuffle(candidates, random);
        return candidates.stream()
                .max(Comparator.comparingInt(direction -> distanceAfterMove(
                        map,
                        tileX,
                        tileY,
                        direction,
                        player.getCenterTileX(),
                        player.getCenterTileY()
                )))
                .orElse(Direction.NONE);
    }

    private Direction chooseRandom(List<Direction> candidates) {
        return candidates.get(random.nextInt(candidates.size()));
    }

    private int distanceAfterMove(GameMap map, int tileX, int tileY, Direction direction, int targetX, int targetY) {
        Point nextTile = map.getNeighborTile(tileX, tileY, direction);
        int nextX = nextTile.x;
        int nextY = nextTile.y;
        int deltaX = nextX - targetX;
        int deltaY = nextY - targetY;
        return deltaX * deltaX + deltaY * deltaY;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getCenterX() {
        return x + GameMap.TILE_SIZE / 2;
    }

    public int getCenterY() {
        return y + GameMap.TILE_SIZE / 2;
    }

    public Color getBaseColor() {
        return baseColor;
    }

    private boolean isAlignedToGrid() {
        return x % GameMap.TILE_SIZE == 0 && y % GameMap.TILE_SIZE == 0;
    }

    private int getAlignedTileX() {
        return x / GameMap.TILE_SIZE;
    }

    private int getAlignedTileY() {
        return y / GameMap.TILE_SIZE;
    }
}
