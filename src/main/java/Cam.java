public class Cam {
    static final double CAM_SMOOTHING = 10;
    static final double MARGIN_X = 140;
    static final double MARGIN_Y = 100;
    static final double DEAD_X = 0;      // połowa szerokości pasa martwego (px od środka)
    static final double SPAN_X = 500;   // ile px za strefą martwą gracz dojeżdża do pełnego wysunięcia
    static final double DEAD_Y = 0;        // pion: 0 = brak strefy martwej
    static final double SPAN_Y = 250;
    static final double ZOOM_MIN = 0.7;           // maksymalne oddalenie
    static final double ZOOM_MAX = 1.3;           // maksymalne przybliżenie
    static final double ZOOM_SENSITIVITY = 0.002; // jak mocno reaguje na scroll
    static final double ZOOM_SMOOTHING = 12;      // jak szybko zoom dąży do celu

    private final double HEIGHT;
    private final double WIDTH;

    private double x, y;
    private double targetX, targetY;
    private double zoom = 1;
    private double targetZoom = 1;

    public Cam(double WIDTH, double HEIGHT){
        this.HEIGHT = HEIGHT;
        this.WIDTH = WIDTH;
    }

    public double getX(){return x;}
    public double getY(){return y;}
    public double getZoom(){return zoom;}

    public double renderX(){return Math.round(x * zoom) / zoom;}
    public double renderY(){return Math.round(y * zoom) / zoom;}

    public double worldX(double screenX){return screenX / zoom + x;}
    public double worldY(double screenY){return screenY / zoom + y;}

    public void zoomBy(double scrollDelta){
        targetZoom *= Math.exp(scrollDelta * ZOOM_SENSITIVITY);
        targetZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, targetZoom));
    }

    public void resetZoom(){ targetZoom = 1; }

    public boolean outside(double worldX, double worldY, double margin){
        double areaScale = Math.min(zoom, 1.0);
        double areaW = WIDTH / areaScale;
        double areaH = HEIGHT / areaScale;

        // środek aktualnego widoku (x,y to lewy górny róg widoku o rozmiarze WIDTH/zoom)
        double cx = x + WIDTH / zoom / 2;
        double cy = y + HEIGHT / zoom / 2;

        return worldX < cx - areaW / 2 - margin ||
                worldX > cx + areaW / 2 + margin ||
                worldY < cy - areaH / 2 - margin ||
                worldY > cy + areaH / 2 + margin;
    }

    /** Płynny ruch kamery do celu, niezależny od FPS. Wywołuj co klatkę. */
    public void updateCam(double playerX, double playerY,
                          double mouseX, double mouseY, double dt) {
        // najpierw zoom, bo cel kamery zależy od aktualnej skali
        double az = 1 - Math.exp(-ZOOM_SMOOTHING * dt);
        zoom += (targetZoom - zoom) * az;

        countTarget(playerX, playerY, mouseX, mouseY);

        double a = 1 - Math.exp(-CAM_SMOOTHING * dt);
        x += (targetX - x) * a;
        y += (targetY - y) * a;
    }

    /** Natychmiastowe ustawienie kamery w docelowym miejscu (start gry, respawn). */
    public void drag(double graczX, double graczY,
                            double myszX, double myszY) {
        countTarget(graczX, graczY, myszX, myszY);
        x = targetX;
        y = targetY;
    }

    private void countTarget(double playerX, double playerY, double mouseX, double mouseY) {
        double dx = mouseX - WIDTH / 2;
        double dy = mouseY - HEIGHT / 2;

        double sx = smooth((Math.abs(dx) - DEAD_X) / SPAN_X);
        double sy = smooth((Math.abs(dy) - DEAD_Y) / SPAN_Y);

        double rangeX = WIDTH / 2 - MARGIN_X;
        double rangeY = HEIGHT / 2 - MARGIN_Y;

        // gdzie NA EKRANIE (w pikselach) ma stać gracz
        double docelowyEkranX = WIDTH / 2 - Math.signum(dx) * rangeX * sx;
        double docelowyEkranY = HEIGHT / 2 - Math.signum(dy) * rangeY * sy;

        // piksele ekranu -> jednostki świata: dzielimy przez zoom
        targetX = playerX - docelowyEkranX / zoom;
        targetY = playerY - docelowyEkranY / zoom;
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
