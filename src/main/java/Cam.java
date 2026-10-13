public class Cam {
    static final double CAM_SMOOTHING = 10;
    static final double MARGIN_X = 140;
    static final double MARGIN_Y = 100;
    static final double DEAD_X = 0;      // połowa szerokości pasa martwego (px od środka)
    static final double SPAN_X = 500;   // ile px za strefą martwą gracz dojeżdża do pełnego wysunięcia
    static final double DEAD_Y = 0;        // pion: 0 = brak strefy martwej
    static final double SPAN_Y = 250;

    private final double HEIGHT;
    private final double WIDTH;

    private double x, y;
    private double targetX, targetY;

    public Cam(double WIDTH, double HEIGHT){
        this.HEIGHT = HEIGHT;
        this.WIDTH = WIDTH;
    }

    public double getX(){return x;}
    public double getY(){return y;}

    public double renderX(){return Math.round(x);}
    public double renderY(){return Math.round(y);}

    public double worldX(double screenX){return screenX + x;}
    public double worldY(double screenY){return screenY + y;}

    public boolean outside(double worldX, double worldY, double margin){
        return worldX < x - margin ||
                worldX > x + WIDTH + margin ||
                worldY < y - margin ||
                worldY > y + HEIGHT + margin;
    }

    /** Płynny ruch kamery do celu, niezależny od FPS. Wywołuj co klatkę. */
    public void updateCam(double playerX, double playerY,
                           double mouseX, double mouseY, double dt) {
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
        // położenie kursora względem ŚRODKA EKRANU (nie zależy od kamery)
        double dx = mouseX - WIDTH / 2;
        double dy = mouseY - HEIGHT / 2;

        // siła wysunięcia: 0 w strefie martwej, rośnie płynnie do 1
        double sx = smooth((Math.abs(dx) -DEAD_X) / SPAN_X);
        double sy = smooth((Math.abs(dy) - DEAD_Y) / SPAN_Y);

        double rangeX = WIDTH / 2 - MARGIN_X;
        double rangeY = HEIGHT / 2 - MARGIN_Y;

        // gracz stoi po przeciwnej stronie niż kursor
        double docelowyEkranX = WIDTH / 2 - Math.signum(dx) * rangeX * sx;
        double docelowyEkranY = HEIGHT / 2 - Math.signum(dy) * rangeY * sy;

        targetX = playerX - docelowyEkranX;
        targetY = playerY - docelowyEkranY;
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
