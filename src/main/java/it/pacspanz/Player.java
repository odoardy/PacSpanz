package it.pacspanz;

import java.awt.Point;

public class Player {
    public static final int SPEED = 4;

    private int x;
    private int y;
    private Direction direction = Direction.NONE;
    private Direction requestedDirection = Direction.NONE;

    public Player(Point spawn) {
        reset(spawn);
    }

    public void reset(Point spawn) {
        x = spawn.x * GameMap.TILE_SIZE;
        y = spawn.y * GameMap.TILE_SIZE;
        direction = Direction.NONE;
        requestedDirection = Direction.NONE;
    }

    public void requestDirection(Direction newDirection) {
        if (newDirection != Direction.NONE) {
            requestedDirection = newDirection;
        }
    }

    public void update(GameMap map) {
        if (isAlignedToGrid()) {
            int tileX = getAlignedTileX();
            int tileY = getAlignedTileY();

            if (map.canMoveFrom(tileX, tileY, requestedDirection)) {
                direction = requestedDirection;
            }

            if (!map.canMoveFrom(tileX, tileY, direction)) {
                direction = Direction.NONE;
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

    public int getCenterTileX() {
        return getCenterX() / GameMap.TILE_SIZE;
    }

    public int getCenterTileY() {
        return getCenterY() / GameMap.TILE_SIZE;
    }

    public Direction getDirection() {
        return direction;
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
