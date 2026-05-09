package it.pacspanz;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class GameFrame extends JFrame {
    private final GraphicsDevice graphicsDevice;
    private Rectangle windowedBounds;
    private boolean fullscreen;

    public GameFrame() {
        super("PacSpanz");
        graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        GamePanel gamePanel = new GamePanel();
        setContentPane(gamePanel);
        pack();
        setMinimumSize(new Dimension(gamePanel.getLogicalWidth(), gamePanel.getLogicalHeight()));
        setLocationRelativeTo(null);
    }

    public void showGame() {
        setVisible(true);
        SwingUtilities.invokeLater(this::enterFullscreen);
    }

    public void toggleFullscreen() {
        if (fullscreen) {
            exitFullscreen();
        } else {
            enterFullscreen();
        }
    }

    private void enterFullscreen() {
        if (fullscreen) {
            return;
        }

        windowedBounds = getBounds();
        fullscreen = true;

        try {
            dispose();
            setUndecorated(true);
            setResizable(false);
            setExtendedState(JFrame.NORMAL);

            if (graphicsDevice.isFullScreenSupported()) {
                setVisible(true);
                graphicsDevice.setFullScreenWindow(this);
            } else {
                setBounds(graphicsDevice.getDefaultConfiguration().getBounds());
                setVisible(true);
            }
        } catch (RuntimeException ex) {
            fullscreen = false;
            dispose();
            setUndecorated(false);
            setResizable(true);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setVisible(true);
        }

        requestGameFocus();
    }

    private void exitFullscreen() {
        if (!fullscreen) {
            return;
        }

        if (graphicsDevice.getFullScreenWindow() == this) {
            graphicsDevice.setFullScreenWindow(null);
        }

        fullscreen = false;
        dispose();
        setUndecorated(false);
        setResizable(true);

        if (windowedBounds != null) {
            setBounds(windowedBounds);
        } else {
            pack();
            setLocationRelativeTo(null);
        }

        setVisible(true);
        requestGameFocus();
    }

    private void requestGameFocus() {
        SwingUtilities.invokeLater(() -> getContentPane().requestFocusInWindow());
    }
}
