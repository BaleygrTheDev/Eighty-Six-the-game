import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Radar w lewym dolnym rogu ekranu: pokazuje wrogów, rakiety i Fido względem gracza.
 * Północ jest zawsze do góry. Obiekty poza zasięgiem przyklejają się do brzegu radaru.
 */
public class Radar {
    static final double RADIUS = 130;      // promień panelu w pikselach
    static final double RANGE  = 5000;     // ile jednostek świata sięga radar
    static final double MARGIN = 30;       // odstęp panelu od krawędzi ekranu

    // indeks = typ wroga (0 ameise, 1 grauwolf, 2 lowe)
    static final Color[]  ENEMY_COLORS = {Color.RED, Color.RED, Color.RED};
    static final double[] ENEMY_DOT    = {3, 4, 5};

    static final Color  FIDO_COLOR    = Color.ORANGE;
    static final double FIDO_DOT      = 5;
    static final Color  ROCKET_COLOR  = Color.RED;
    static final double ROCKET_DOT    = 2;
    static final boolean SHOW_ROCKETS = false;

    private final double screenW;
    private final double screenH;
    private final double centerX;
    private final double centerY;

    public Radar(double screenW, double screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
        this.centerX = screenW - RADIUS - MARGIN;                 // lewy dolny róg
        this.centerY = RADIUS + MARGIN;
    }

    public void render(GraphicsContext gc, Feldres player, Feldres fido,
                       Legions legion, Cam cam) {

        double scale = RADIUS / RANGE;                  // jednostki świata -> piksele radaru
        double px = player.x + player.w / 2;
        double py = player.y + player.h / 2;

        drawBackground(gc);
        drawGridAndView(gc, cam, px, py, scale);

        // wrogowie
        for (Legions.Legion l : legion.getEnemies()) {
            drawBlip(gc, l.x + l.w / 2, l.y + l.h / 2, px, py, scale,
                    ENEMY_DOT[l.type], ENEMY_COLORS[l.type], true);
        }

        // rakiety wrogów
        if (SHOW_ROCKETS) {
            for (Legions.Rockets r : legion.rockets) {
                drawBlip(gc, r.x + r.w / 2, r.y + r.h / 2, px, py, scale,
                        ROCKET_DOT, ROCKET_COLOR, true);
            }
        }

        // Fido na wierzchu, zawsze w pełnym kolorze
        drawBlip(gc, fido.x + fido.w / 2, fido.y + fido.h / 2, px, py, scale,
                FIDO_DOT, FIDO_COLOR, false);

        drawPlayer(gc, player);
        drawBorder(gc);
    }

    private void drawBackground(GraphicsContext gc) {
        gc.setFill(Color.rgb(0, 0, 0, 0.45));
        gc.fillRect(centerX - RADIUS, centerY - RADIUS, RADIUS * 2, RADIUS * 2);
    }

    /** Siatka i prostokąt widoku kamery, przycięte do okręgu. */
    private void drawGridAndView(GraphicsContext gc, Cam cam, double px, double py, double scale) {
        gc.save();
        gc.beginPath();
        gc.arc(centerX, centerY, RADIUS, RADIUS, 0, 360);
        gc.clip();

        gc.setLineWidth(1);
        gc.setStroke(Color.rgb(255, 255, 255, 0.2));
        gc.strokeRect(centerX - RADIUS / 2, centerY - RADIUS / 2, RADIUS, RADIUS);   // połowa zasięgu
        gc.strokeLine(centerX - RADIUS, centerY, centerX + RADIUS, centerY);
        gc.strokeLine(centerX, centerY - RADIUS, centerX, centerY + RADIUS);
        gc.restore();
    }

    /** Biały trójkąt pośrodku, zwrócony zgodnie z lufą gracza. */
    private void drawPlayer(GraphicsContext gc, Feldres player) {
        gc.save();
        gc.translate(centerX, centerY);
        gc.rotate(Math.toDegrees(player.angle));
        gc.setFill(Color.CORNFLOWERBLUE);
        gc.fillOval(-5, -5, 10, 10);
        gc.restore();
    }

    private void drawBorder(GraphicsContext gc) {
        gc.setStroke(Color.rgb(255, 255, 255, 0.8));
        gc.setLineWidth(2);
        gc.strokeRect(centerX - RADIUS, centerY - RADIUS, RADIUS * 2, RADIUS * 2);
        gc.setLineWidth(1);
    }

    /**
     * Jedna kropka. Obiekt dalej niż zasięg radaru jest przyklejany do brzegu
     * w tym samym kierunku (przy dimIfFar dodatkowo przyciemniony).
     */
    private void drawBlip(GraphicsContext gc, double worldX, double worldY,
                          double px, double py, double scale,
                          double dot, Color color, boolean dimIfFar) {
        double rx = (worldX - px) * scale;
        double ry = (worldY - py) * scale;

        double d = Math.hypot(rx, ry);
        double edge = RADIUS - dot - 2;                 // kropka ma się zmieścić w całości w panelu
        boolean tooFar = d > edge;

        if (tooFar && d > 0.0001) {
            rx *= edge / d;
            ry *= edge / d;
        }

        gc.setFill(tooFar && dimIfFar ? color.deriveColor(0, 1, 1, 0.45) : color);
        gc.fillOval(centerX + rx - dot, centerY + ry - dot, dot * 2, dot * 2);
    }
}
