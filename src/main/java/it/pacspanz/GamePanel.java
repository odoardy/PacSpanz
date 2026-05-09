package it.pacspanz;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class GamePanel extends JPanel {
    private static final int HUD_HEIGHT = 72;
    private static final int TIMER_DELAY_MS = 16;
    private static final int STARTING_LIVES = 3;
    private static final int NORMAL_PELLET_SCORE = 10;
    private static final int POWER_PELLET_SCORE = 50;
    private static final int VULNERABLE_GHOST_SCORE = 200;
    private static final int POWER_DURATION_TICKS = 420;
    private static final int LIFE_LOST_PAUSE_TICKS = 90;
    private static final int COLLISION_DISTANCE = 28;
    private static final double PLAYER_SPRITE_SCALE = 1.18;
    private static final int WHITE_TRANSPARENCY_THRESHOLD = 240;

    private static final Color BACKGROUND = Color.BLACK;
    private static final Color WALL_COLOR = new Color(12, 45, 132);
    private static final Color WALL_EDGE_COLOR = new Color(55, 112, 228);
    private static final Color WALL_HIGHLIGHT_COLOR = new Color(20, 66, 168);
    private static final Color PELLET_COLOR = new Color(255, 244, 204);
    private static final Color PELLET_GLOW_COLOR = new Color(255, 236, 152, 88);
    private static final Color POWER_COLOR = new Color(255, 214, 88);
    private static final Color TEXT_COLOR = new Color(242, 245, 255);
    private static final Color MUTED_TEXT_COLOR = new Color(178, 188, 212);
    private static final Color PLAYER_FALLBACK_COLOR = new Color(255, 218, 49);
    private static final Color VULNERABLE_GHOST_COLOR = new Color(155, 214, 255);
    private static final Color PANEL_COLOR = new Color(10, 14, 27);
    private static final Color PANEL_EDGE_COLOR = new Color(45, 62, 105);
    private static final Color TUNNEL_COLOR = new Color(50, 135, 255);
    private static final Color TUNNEL_GLOW_COLOR = new Color(70, 170, 255, 78);

    private static final String[] PLAYER_AVATAR_PATHS = {
            "/assets/player_avatar.png",
            "/assets/player_avatar.jpg",
            "/assets/player_avatar.jpeg",
            "/assets/player_full.png",
            "/assets/player_full.jpg",
            "/assets/player_full.jpeg"
    };

    private static final String[] PLAYER_FULL_PATHS = {
            "/assets/player_full.png",
            "/assets/player_full.jpg",
            "/assets/player_full.jpeg",
            "/assets/player_avatar.png",
            "/assets/player_avatar.jpg",
            "/assets/player_avatar.jpeg"
    };

    private final GameMap map = new GameMap();
    private final Player player = new Player(map.getPlayerSpawn());
    private final List<Ghost> ghosts = new ArrayList<>();
    private final Timer timer;
    private final BufferedImage playerAvatar;
    private final BufferedImage playerFull;
    private final Font arcadeFont;

    private GameState state = GameState.MENU;
    private int score;
    private int lives = STARTING_LIVES;
    private int powerTicksRemaining;
    private int lifePauseTicks;
    private long tick;
    private boolean pauseMenuButtonHovered;

    public GamePanel() {
        AssetLoader.LoadedImage loadedAvatar = AssetLoader.loadFirstAvailable(
                "avatar giocatore",
                "Avatar giocatore",
                PLAYER_AVATAR_PATHS
        );
        AssetLoader.LoadedImage loadedFull = AssetLoader.loadFirstAvailable(
                "immagine menu giocatore",
                "Immagine menu giocatore",
                PLAYER_FULL_PATHS
        );
        playerAvatar = loadedAvatar == null
                ? null
                : AssetLoader.makeNearWhiteTransparent(loadedAvatar.image(), WHITE_TRANSPARENCY_THRESHOLD);
        playerFull = loadedFull == null ? null : loadedFull.image();
        arcadeFont = loadArcadeFont();

        setBackground(BACKGROUND);
        setFocusable(true);
        setPreferredSize(new Dimension(getLogicalWidth(), getLogicalHeight()));

        createGhosts();
        bindKeys();
        bindMouse();

        timer = new Timer(TIMER_DELAY_MS, this::onTimer);
        timer.start();
    }

    public int getLogicalWidth() {
        return map.getWidthInPixels();
    }

    public int getLogicalHeight() {
        return map.getHeightInPixels() + HUD_HEIGHT;
    }

    private Font loadArcadeFont() {
        Font loadedFont = AssetLoader.loadFont("/assets/fonts/arcade.ttf");
        return loadedFont == null ? new Font(Font.MONOSPACED, Font.BOLD, 18) : loadedFont;
    }

    private void createGhosts() {
        List<java.awt.Point> spawns = map.getGhostSpawns();
        Color[] colors = {
                new Color(231, 70, 70),
                new Color(66, 202, 240),
                new Color(240, 142, 64),
                new Color(205, 93, 232)
        };
        Ghost.Behavior[] behaviors = {
                Ghost.Behavior.AGGRESSIVE,
                Ghost.Behavior.RANDOM,
                Ghost.Behavior.INTERCEPT,
                Ghost.Behavior.SWITCHER
        };

        for (int i = 0; i < 4; i++) {
            java.awt.Point spawn = spawns.get(i % spawns.size());
            ghosts.add(new Ghost(spawn, colors[i], behaviors[i]));
        }
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        bindDirection(inputMap, actionMap, KeyEvent.VK_UP, Direction.UP);
        bindDirection(inputMap, actionMap, KeyEvent.VK_W, Direction.UP);
        bindDirection(inputMap, actionMap, KeyEvent.VK_DOWN, Direction.DOWN);
        bindDirection(inputMap, actionMap, KeyEvent.VK_S, Direction.DOWN);
        bindDirection(inputMap, actionMap, KeyEvent.VK_LEFT, Direction.LEFT);
        bindDirection(inputMap, actionMap, KeyEvent.VK_A, Direction.LEFT);
        bindDirection(inputMap, actionMap, KeyEvent.VK_RIGHT, Direction.RIGHT);
        bindDirection(inputMap, actionMap, KeyEvent.VK_D, Direction.RIGHT);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "start");
        actionMap.put("start", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (state == GameState.MENU) {
                    startNewGame();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "pause");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "pause");
        actionMap.put("pause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                togglePause();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "return-to-menu");
        actionMap.put("return-to-menu", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (state == GameState.PAUSED) {
                    returnToMenu();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "restart");
        actionMap.put("restart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (state == GameState.GAME_OVER || state == GameState.VICTORY) {
                    startNewGame();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "fullscreen");
        actionMap.put("fullscreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Window window = SwingUtilities.getWindowAncestor(GamePanel.this);
                if (window instanceof GameFrame frame) {
                    frame.toggleFullscreen();
                }
            }
        });
    }

    private void bindMouse() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                Point logicalPoint = toLogicalPoint(e);
                if (state == GameState.PAUSED
                        && logicalPoint != null
                        && getPauseMenuButtonBounds().contains(logicalPoint)) {
                    returnToMenu();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updatePauseMenuButtonHover(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setPauseMenuButtonHovered(false);
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    private void bindDirection(InputMap inputMap, ActionMap actionMap, int keyCode, Direction direction) {
        String name = "move-" + direction.name() + "-" + keyCode;
        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0), name);
        actionMap.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (state == GameState.PLAYING) {
                    player.requestDirection(direction);
                }
            }
        });
    }

    private void onTimer(ActionEvent event) {
        if (state == GameState.PLAYING) {
            updateGame();
        } else if (state == GameState.LIFE_LOST) {
            updateLifeLostPause();
        }
        repaint();
    }

    private void startNewGame() {
        resetGameState();
        state = GameState.PLAYING;
        setPauseMenuButtonHovered(false);
    }

    private void returnToMenu() {
        resetGameState();
        state = GameState.MENU;
        setPauseMenuButtonHovered(false);
        repaint();
    }

    private void resetGameState() {
        map.reset();
        score = 0;
        lives = STARTING_LIVES;
        powerTicksRemaining = 0;
        lifePauseTicks = 0;
        tick = 0;
        resetActors();
    }

    private void resetActors() {
        player.reset(map.getPlayerSpawn());
        for (Ghost ghost : ghosts) {
            ghost.resetToSpawn();
        }
    }

    private void togglePause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
            setPauseMenuButtonHovered(false);
        }
    }

    private void updatePauseMenuButtonHover(MouseEvent e) {
        Point logicalPoint = toLogicalPoint(e);
        boolean hovered = state == GameState.PAUSED
                && logicalPoint != null
                && getPauseMenuButtonBounds().contains(logicalPoint);
        setPauseMenuButtonHovered(hovered);
    }

    private void setPauseMenuButtonHovered(boolean hovered) {
        if (pauseMenuButtonHovered != hovered) {
            pauseMenuButtonHovered = hovered;
            repaint();
        }
        setCursor(Cursor.getPredefinedCursor(hovered ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private Point toLogicalPoint(MouseEvent e) {
        double scale = Math.min(
                getWidth() / (double) getLogicalWidth(),
                getHeight() / (double) getLogicalHeight()
        );
        if (scale <= 0.0) {
            return null;
        }

        int scaledWidth = (int) Math.round(getLogicalWidth() * scale);
        int scaledHeight = (int) Math.round(getLogicalHeight() * scale);
        int offsetX = (getWidth() - scaledWidth) / 2;
        int offsetY = (getHeight() - scaledHeight) / 2;
        double logicalX = (e.getX() - offsetX) / scale;
        double logicalY = (e.getY() - offsetY) / scale;

        if (logicalX < 0
                || logicalY < 0
                || logicalX >= getLogicalWidth()
                || logicalY >= getLogicalHeight()) {
            return null;
        }
        return new Point((int) Math.floor(logicalX), (int) Math.floor(logicalY));
    }

    private void updateGame() {
        tick++;
        player.update(map);
        collectCurrentTile();

        if (map.getRemainingPellets() == 0) {
            state = GameState.VICTORY;
            return;
        }

        boolean vulnerable = powerTicksRemaining > 0;
        for (Ghost ghost : ghosts) {
            ghost.update(map, player, vulnerable, tick);
        }

        handleCollisions();

        if (powerTicksRemaining > 0) {
            powerTicksRemaining--;
        }
    }

    private void updateLifeLostPause() {
        lifePauseTicks--;
        if (lifePauseTicks <= 0) {
            resetActors();
            state = GameState.PLAYING;
        }
    }

    private void collectCurrentTile() {
        TileType consumed = map.consumeAt(player.getCenterTileX(), player.getCenterTileY());
        if (consumed == TileType.PELLET) {
            score += NORMAL_PELLET_SCORE;
        } else if (consumed == TileType.POWER_PELLET) {
            score += POWER_PELLET_SCORE;
            powerTicksRemaining = POWER_DURATION_TICKS;
        }
    }

    private void handleCollisions() {
        for (Ghost ghost : ghosts) {
            int deltaX = player.getCenterX() - ghost.getCenterX();
            int deltaY = player.getCenterY() - ghost.getCenterY();
            double distance = Math.hypot(deltaX, deltaY);

            if (distance <= COLLISION_DISTANCE) {
                if (powerTicksRemaining > 0) {
                    score += VULNERABLE_GHOST_SCORE;
                    ghost.resetToSpawn();
                } else {
                    loseLife();
                    return;
                }
            }
        }
    }

    private void loseLife() {
        lives--;
        powerTicksRemaining = 0;
        if (lives <= 0) {
            state = GameState.GAME_OVER;
        } else {
            lifePauseTicks = LIFE_LOST_PAUSE_TICKS;
            state = GameState.LIFE_LOST;
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        configureRendering(g);
        drawWindowBackdrop(g);

        double scale = Math.min(
                getWidth() / (double) getLogicalWidth(),
                getHeight() / (double) getLogicalHeight()
        );
        int scaledWidth = (int) Math.round(getLogicalWidth() * scale);
        int scaledHeight = (int) Math.round(getLogicalHeight() * scale);
        int offsetX = (getWidth() - scaledWidth) / 2;
        int offsetY = (getHeight() - scaledHeight) / 2;

        Graphics2D logicalGraphics = (Graphics2D) g.create();
        logicalGraphics.translate(offsetX, offsetY);
        logicalGraphics.scale(scale, scale);
        logicalGraphics.setClip(0, 0, getLogicalWidth(), getLogicalHeight());
        configureRendering(logicalGraphics);

        if (state == GameState.MENU) {
            drawMenu(logicalGraphics);
        } else {
            drawGame(logicalGraphics);
        }

        logicalGraphics.dispose();
        g.dispose();
    }

    private void configureRendering(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }

    private void drawWindowBackdrop(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(2, 4, 10), 0, getHeight(), new Color(4, 10, 26)));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawGame(Graphics2D g) {
        drawHud(g);

        Graphics2D world = (Graphics2D) g.create();
        world.translate(0, HUD_HEIGHT);
        drawMap(world);
        drawPlayer(world);
        drawGhosts(world);
        world.dispose();

        if (state == GameState.PAUSED) {
            drawPauseOverlay(g);
        } else if (state == GameState.LIFE_LOST) {
            drawCenteredOverlay(g, "VITA PERSA", "Preparati a ripartire");
        } else if (state == GameState.GAME_OVER) {
            drawCenteredOverlay(g, "PARTITA TERMINATA", "Premi R per ricominciare");
        } else if (state == GameState.VICTORY) {
            drawCenteredOverlay(g, "VITTORIA", "Premi R per ricominciare");
        }
    }

    private void drawHud(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(7, 10, 20), 0, HUD_HEIGHT, new Color(13, 18, 35)));
        g.fillRect(0, 0, getLogicalWidth(), HUD_HEIGHT);
        g.setColor(new Color(37, 50, 86));
        g.drawLine(0, HUD_HEIGHT - 1, getLogicalWidth(), HUD_HEIGHT - 1);

        int gap = 16;
        int scoreWidth = 210;
        int livesWidth = 118;
        int pauseWidth = 156;
        int groupWidth = scoreWidth + livesWidth + pauseWidth + gap * 2;
        int x = (getLogicalWidth() - groupWidth) / 2;
        int y = (HUD_HEIGHT - 22) / 2;

        drawHudPill(g, x, y, scoreWidth, "PUNTEGGIO", String.valueOf(score));
        x += scoreWidth + gap;
        drawHudPill(g, x, y, livesWidth, "VITE", String.valueOf(lives));
        x += livesWidth + gap;
        drawHudPill(g, x, y, pauseWidth, "PAUSA", "P/ESC");
    }

    private void drawHudPill(Graphics2D g, int x, int y, int width, String label, String value) {
        int height = 22;
        g.setColor(new Color(0, 0, 0, 95));
        g.fillRoundRect(x + 1, y + 2, width, height, 10, 10);
        g.setColor(new Color(18, 24, 44));
        g.fillRoundRect(x, y, width, height, 10, 10);
        g.setColor(new Color(45, 63, 112));
        g.drawRoundRect(x, y, width, height, 10, 10);

        Font labelFont = fitFont(g, arcade(Font.BOLD, 8), label, Math.max(24, width / 2), 6f);
        g.setFont(labelFont);
        g.setColor(MUTED_TEXT_COLOR);
        g.drawString(label, x + 9, y + 15);

        int labelWidth = g.getFontMetrics().stringWidth(label);
        int maxValueWidth = Math.max(24, width - labelWidth - 29);
        g.setFont(fitFont(g, arcade(Font.BOLD, 12), value, maxValueWidth, 7f));
        g.setColor(TEXT_COLOR);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(value, x + width - metrics.stringWidth(value) - 9, y + 16);
    }

    private void drawMap(Graphics2D g) {
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, map.getWidthInPixels(), map.getHeightInPixels());

        for (int y = 0; y < map.getHeightInTiles(); y++) {
            for (int x = 0; x < map.getWidthInTiles(); x++) {
                int px = x * GameMap.TILE_SIZE;
                int py = y * GameMap.TILE_SIZE;
                TileType tile = map.getTile(x, y);

                if (tile == TileType.WALL) {
                    drawWall(g, px, py);
                } else if (tile == TileType.TUNNEL) {
                    drawTunnel(g, px, py, x, y);
                } else if (tile == TileType.PELLET) {
                    drawPellet(g, px, py, 6, PELLET_COLOR, false);
                } else if (tile == TileType.POWER_PELLET) {
                    int pulse = (int) Math.round(Math.sin(tick * 0.16) * 3.0);
                    drawPellet(g, px, py, 17 + pulse, POWER_COLOR, true);
                }
            }
        }
    }

    private void drawWall(Graphics2D g, int x, int y) {
        int inset = 2;
        int size = GameMap.TILE_SIZE - inset * 2;
        RoundRectangle2D wall = new RoundRectangle2D.Double(x + inset, y + inset, size, size, 9, 9);
        g.setColor(WALL_COLOR);
        g.fill(wall);
        g.setColor(WALL_HIGHLIGHT_COLOR);
        g.drawLine(x + 8, y + 6, x + GameMap.TILE_SIZE - 9, y + 6);
        g.setColor(WALL_EDGE_COLOR);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(wall);
    }

    private void drawTunnel(Graphics2D g, int x, int y, int tileX, int tileY) {
        g.setColor(TUNNEL_GLOW_COLOR);
        g.fillRoundRect(x + 4, y + 4, GameMap.TILE_SIZE - 8, GameMap.TILE_SIZE - 8, 14, 14);

        g.setColor(new Color(6, 18, 42));
        g.fillRoundRect(x + 8, y + 8, GameMap.TILE_SIZE - 16, GameMap.TILE_SIZE - 16, 12, 12);

        g.setColor(TUNNEL_COLOR);
        g.setStroke(new BasicStroke(2f));
        if (tileX == 0 || tileX == map.getWidthInTiles() - 1) {
            int centerY = y + GameMap.TILE_SIZE / 2;
            g.drawLine(x + 4, centerY, x + GameMap.TILE_SIZE - 4, centerY);
            g.drawArc(x + 8, y + 8, GameMap.TILE_SIZE - 16, GameMap.TILE_SIZE - 16, 0, 360);
        }
        if (tileY == 0 || tileY == map.getHeightInTiles() - 1) {
            int centerX = x + GameMap.TILE_SIZE / 2;
            g.drawLine(centerX, y + 4, centerX, y + GameMap.TILE_SIZE - 4);
            g.drawArc(x + 8, y + 8, GameMap.TILE_SIZE - 16, GameMap.TILE_SIZE - 16, 0, 360);
        }
    }

    private void drawPellet(Graphics2D g, int tileX, int tileY, int size, Color color, boolean power) {
        int x = tileX + (GameMap.TILE_SIZE - size) / 2;
        int y = tileY + (GameMap.TILE_SIZE - size) / 2;
        if (power) {
            int glow = size + 10;
            int glowX = tileX + (GameMap.TILE_SIZE - glow) / 2;
            int glowY = tileY + (GameMap.TILE_SIZE - glow) / 2;
            g.setColor(new Color(255, 206, 80, 58));
            g.fillOval(glowX, glowY, glow, glow);
        } else {
            g.setColor(PELLET_GLOW_COLOR);
            g.fillOval(x - 2, y - 2, size + 4, size + 4);
        }

        g.setColor(color);
        g.fillOval(x, y, size, size);
        if (power) {
            g.setColor(new Color(255, 247, 194));
            g.setStroke(new BasicStroke(1.2f));
            g.drawOval(x + 2, y + 2, size - 4, size - 4);
        }
    }

    private void drawPlayer(Graphics2D g) {
        if (playerAvatar != null) {
            int spriteBox = (int) Math.round(GameMap.TILE_SIZE * PLAYER_SPRITE_SCALE);
            int x = player.getCenterX() - spriteBox / 2;
            int y = player.getCenterY() - spriteBox / 2;

            g.setColor(new Color(0, 0, 0, 120));
            g.fillOval(x + 6, y + spriteBox - 11, spriteBox - 12, 9);
            drawImageContain(g, playerAvatar, x, y, spriteBox, spriteBox);
        } else {
            int size = GameMap.TILE_SIZE - 5;
            int x = player.getX() + (GameMap.TILE_SIZE - size) / 2;
            int y = player.getY() + (GameMap.TILE_SIZE - size) / 2;
            g.setColor(new Color(0, 0, 0, 150));
            g.fillOval(x + 2, y + 3, size, size);
            g.setColor(PLAYER_FALLBACK_COLOR);
            g.fillOval(x, y, size, size);
        }
    }

    private void drawGhosts(Graphics2D g) {
        boolean vulnerable = powerTicksRemaining > 0;
        boolean blink = vulnerable && powerTicksRemaining < 120 && (powerTicksRemaining / 12) % 2 == 0;

        for (Ghost ghost : ghosts) {
            drawGhost(g, ghost, vulnerable, blink);
        }
    }

    private void drawGhost(Graphics2D g, Ghost ghost, boolean vulnerable, boolean blink) {
        int margin = 5;
        int x = ghost.getX() + margin;
        int y = ghost.getY() + margin;
        int size = GameMap.TILE_SIZE - margin * 2;
        Color bodyColor = vulnerable ? (blink ? Color.WHITE : VULNERABLE_GHOST_COLOR) : ghost.getBaseColor();

        g.setColor(new Color(0, 0, 0, 125));
        g.fillOval(x + 2, y + 4, size, size);

        Path2D body = new Path2D.Double();
        body.moveTo(x, y + size);
        body.lineTo(x, y + size / 2.5);
        body.curveTo(x, y + 6, x + 6, y, x + size / 2.0, y);
        body.curveTo(x + size - 6, y, x + size, y + 6, x + size, y + size / 2.5);
        body.lineTo(x + size, y + size);
        body.quadTo(x + size - 5, y + size - 7, x + size - 10, y + size);
        body.quadTo(x + size - 15, y + size - 7, x + size - 20, y + size);
        body.quadTo(x + size - 25, y + size - 7, x, y + size);
        body.closePath();

        g.setColor(bodyColor);
        g.fill(body);

        g.setColor(new Color(18, 22, 34));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(body);

        g.setColor(Color.WHITE);
        g.fillOval(x + 8, y + 10, 7, 9);
        g.fillOval(x + size - 15, y + 10, 7, 9);
        g.setColor(vulnerable ? new Color(20, 70, 130) : new Color(20, 35, 78));
        g.fillOval(x + 10, y + 13, 3, 4);
        g.fillOval(x + size - 13, y + 13, 3, 4);
    }

    private void drawMenu(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(4, 5, 10), 0, getLogicalHeight(), new Color(9, 18, 43)));
        g.fillRect(0, 0, getLogicalWidth(), getLogicalHeight());

        int contentY = 128;
        String title = "PacSpanz";
        Font titleFont = fitFont(g, arcade(Font.BOLD, 70), title, getLogicalWidth() - 40, 8f);
        Rectangle2D titleBounds = titleFont.createGlyphVector(g.getFontRenderContext(), title).getVisualBounds();
        int imageFrameTop = playerFull == null ? contentY : contentY - 4;
        int titleY = (int) Math.round((imageFrameTop - titleBounds.getHeight()) / 2.0 - titleBounds.getY());
        g.setFont(titleFont);
        drawCenteredString(g, title, titleY, TEXT_COLOR);

        if (playerFull != null) {
            int imageWidth = Math.min(300, getLogicalWidth() - 190);
            int imageHeight = 300;
            int imageX = (getLogicalWidth() - imageWidth) / 2;
            g.setColor(new Color(0, 0, 0, 105));
            g.fillRoundRect(imageX + 4, contentY + 6, imageWidth, imageHeight, 18, 18);
            g.setColor(PANEL_COLOR);
            g.fillRoundRect(imageX, contentY, imageWidth, imageHeight, 18, 18);
            g.setColor(new Color(255, 214, 88, 105));
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(imageX - 4, contentY - 4, imageWidth + 8, imageHeight + 8, 22, 22);
            g.setColor(PANEL_EDGE_COLOR);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(imageX, contentY, imageWidth, imageHeight, 18, 18);
            g.setColor(new Color(115, 159, 255, 130));
            g.drawRoundRect(imageX + 5, contentY + 5, imageWidth - 10, imageHeight - 10, 14, 14);
            drawImageContain(g, playerFull, imageX + 10, contentY + 10, imageWidth - 20, imageHeight - 20);
            contentY += imageHeight + 62;
        } else {
            contentY += 188;
        }

        drawCallToAction(g, contentY);

        g.setFont(arcade(Font.BOLD, 12));
        drawCenteredString(g, "WASD / Frecce: movimento", contentY + 48, MUTED_TEXT_COLOR);
        drawCenteredString(g, "P o ESC: pausa", contentY + 72, MUTED_TEXT_COLOR);
        drawCenteredString(g, "F11: schermo intero", contentY + 96, MUTED_TEXT_COLOR);
        drawCenteredString(g, "R: ricomincia dopo sconfitta o vittoria", contentY + 120, MUTED_TEXT_COLOR);
    }

    private void drawCallToAction(Graphics2D g, int baselineY) {
        String text = "Premi SPAZIO per giocare";
        int width = 380;
        int height = 38;
        int x = (getLogicalWidth() - width) / 2;
        int y = baselineY - 28;

        g.setColor(new Color(255, 214, 88, 44));
        g.fillRoundRect(x - 8, y - 5, width + 16, height + 10, 18, 18);
        g.setColor(new Color(24, 28, 47));
        g.fillRoundRect(x, y, width, height, 14, 14);
        g.setColor(new Color(255, 214, 88, 170));
        g.setStroke(new BasicStroke(1.8f));
        g.drawRoundRect(x, y, width, height, 14, 14);

        g.setFont(arcade(Font.BOLD, 17));
        drawCenteredString(g, text, baselineY, POWER_COLOR, width - 24);
    }

    private void drawPauseOverlay(Graphics2D g) {
        Rectangle overlay = getPauseOverlayBounds();

        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, HUD_HEIGHT, getLogicalWidth(), map.getHeightInPixels());
        g.setColor(new Color(0, 0, 0, 215));
        g.fillRoundRect(overlay.x, overlay.y, overlay.width, overlay.height, 10, 10);
        g.setColor(new Color(255, 214, 88, 80));
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(overlay.x - 4, overlay.y - 4, overlay.width + 8, overlay.height + 8, 14, 14);
        g.setColor(new Color(80, 112, 220));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(overlay.x, overlay.y, overlay.width, overlay.height, 10, 10);

        g.setFont(arcade(Font.BOLD, 31));
        drawCenteredString(g, "PAUSA", overlay.y + 62, TEXT_COLOR);
        g.setFont(arcade(Font.BOLD, 13));
        drawCenteredString(g, "Premi P o ESC per riprendere", overlay.y + 106, MUTED_TEXT_COLOR);
        drawCenteredString(g, "Premi M per tornare al menu", overlay.y + 132, MUTED_TEXT_COLOR);

        drawPauseMenuButton(g);
    }

    private Rectangle getPauseOverlayBounds() {
        int width = 560;
        int height = 242;
        int x = (getLogicalWidth() - width) / 2;
        int y = HUD_HEIGHT + (map.getHeightInPixels() - height) / 2;
        return new Rectangle(x, y, width, height);
    }

    private Rectangle getPauseMenuButtonBounds() {
        Rectangle overlay = getPauseOverlayBounds();
        int width = 270;
        int height = 42;
        int x = overlay.x + (overlay.width - width) / 2;
        int y = overlay.y + 157;
        return new Rectangle(x, y, width, height);
    }

    private void drawPauseMenuButton(Graphics2D g) {
        Rectangle button = getPauseMenuButtonBounds();

        g.setColor(pauseMenuButtonHovered
                ? new Color(255, 236, 130, 76)
                : new Color(255, 214, 88, 44));
        g.fillRoundRect(button.x - 8, button.y - 5, button.width + 16, button.height + 10, 18, 18);
        g.setColor(pauseMenuButtonHovered
                ? new Color(31, 36, 60)
                : new Color(18, 23, 42));
        g.fillRoundRect(button.x, button.y, button.width, button.height, 14, 14);
        g.setColor(pauseMenuButtonHovered
                ? new Color(255, 237, 148, 220)
                : new Color(255, 214, 88, 170));
        g.setStroke(new BasicStroke(1.8f));
        g.drawRoundRect(button.x, button.y, button.width, button.height, 14, 14);
        g.setColor(new Color(80, 112, 220, 150));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(button.x + 5, button.y + 5, button.width - 10, button.height - 10, 10, 10);

        drawCenteredStringInRect(
                g,
                "Torna al menu",
                button,
                pauseMenuButtonHovered ? new Color(255, 247, 194) : POWER_COLOR,
                button.width - 24
        );
    }

    private void drawCenteredOverlay(Graphics2D g, String title, String subtitle) {
        int width = 560;
        int height = 170;
        int x = (getLogicalWidth() - width) / 2;
        int y = HUD_HEIGHT + (map.getHeightInPixels() - height) / 2;

        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, HUD_HEIGHT, getLogicalWidth(), map.getHeightInPixels());
        g.setColor(new Color(0, 0, 0, 215));
        g.fillRoundRect(x, y, width, height, 10, 10);
        g.setColor(new Color(255, 214, 88, 80));
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(x - 4, y - 4, width + 8, height + 8, 14, 14);
        g.setColor(new Color(80, 112, 220));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, width, height, 10, 10);

        g.setFont(arcade(Font.BOLD, 31));
        drawCenteredString(g, title, y + 68, TEXT_COLOR);
        g.setFont(arcade(Font.BOLD, 13));
        drawCenteredString(g, subtitle, y + 112, MUTED_TEXT_COLOR);
    }

    private void drawCenteredStringInRect(Graphics2D g, String text, Rectangle bounds, Color color, int maxWidth) {
        Font oldFont = g.getFont();
        g.setFont(fitFont(g, oldFont, text, maxWidth, 8f));
        FontMetrics metrics = g.getFontMetrics();
        int x = bounds.x + (bounds.width - metrics.stringWidth(text)) / 2;
        int y = bounds.y + (bounds.height - metrics.getHeight()) / 2 + metrics.getAscent();
        g.setColor(color);
        g.drawString(text, x, y);
        g.setFont(oldFont);
    }

    private void drawCenteredString(Graphics2D g, String text, int baselineY, Color color) {
        drawCenteredString(g, text, baselineY, color, getLogicalWidth() - 40);
    }

    private void drawCenteredString(Graphics2D g, String text, int baselineY, Color color, int maxWidth) {
        Font oldFont = g.getFont();
        g.setFont(fitFont(g, oldFont, text, maxWidth, 8f));
        FontMetrics metrics = g.getFontMetrics();
        int x = (getLogicalWidth() - metrics.stringWidth(text)) / 2;
        g.setColor(color);
        g.drawString(text, x, baselineY);
        g.setFont(oldFont);
    }

    private Font arcade(int style, float size) {
        return arcadeFont.deriveFont(style, size);
    }

    private Font fitFont(Graphics2D g, Font baseFont, String text, int maxWidth, float minimumSize) {
        Font fittedFont = baseFont;
        float size = baseFont.getSize2D();
        while (size > minimumSize && g.getFontMetrics(fittedFont).stringWidth(text) > maxWidth) {
            size -= 1f;
            fittedFont = baseFont.deriveFont(size);
        }
        return fittedFont;
    }

    private void drawImageContain(Graphics2D g, BufferedImage image, int x, int y, int width, int height) {
        double scale = Math.min(width / (double) image.getWidth(), height / (double) image.getHeight());
        int drawWidth = (int) Math.round(image.getWidth() * scale);
        int drawHeight = (int) Math.round(image.getHeight() * scale);
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;
        g.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
    }
}
