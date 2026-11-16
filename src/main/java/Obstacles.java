import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.*;

public class Obstacles {
    static final double TRENCH_CELL = 300;      // rozmiar komórki siatki ŚWIATA
    static final int TRENCH_CHANCE = 3;         // % komórek, w których jest okop
    static final double TRENCH_W = 200, TRENCH_H = 100;

    static final double[] SIZE_W = Legions.SIZE_W;
    static final double[] SIZE_H = Legions.SIZE_H;
    static final double SEPARATION_RATE = 6.3;
    static final double SLOW = 0.3;
    static int round;

    private final RNG rng;

    public Obstacles(RNG rng) {
        this.rng = rng;
    }

    static class Obstacle extends Body {
        int type;
        int spawnRound;

        Obstacle(double x, double y, double angle, int type, int spawnRound){
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.type = type;
            this.w = SIZE_W[type];
            this.h = SIZE_H[type];
            this.spawnRound = spawnRound;
        }
    }
    private List<Obstacle> carcas = new ArrayList<>();
    private List<Obstacle> trench = new ArrayList<>();

    public List<Obstacle> getCarcas() { return carcas; }

    public void delete(){
        carcas.clear();
    }

    public int setRound(int round){ return this.round = round; }

    public void newCarcas(double x, double y, double angle, int type, int spawnRound){
        Obstacle c = new Obstacle(x, y, angle, type, spawnRound);
        carcas.add(c);
    }
    public boolean hasTrench(int cx, int cy) {
        return Math.floorMod(rng.hash(cx, cy), 100) < TRENCH_CHANCE;
    }
    public boolean inTrench(Body b) {
        // okop z komórki cx sięga o TRENCH_W w prawo, więc szukamy też komórek na lewo od ciała
        int cx0 = (int) Math.floor((b.x - TRENCH_W) / TRENCH_CELL);
        int cx1 = (int) Math.floor((b.x + b.w) / TRENCH_CELL);
        int cy0 = (int) Math.floor((b.y - TRENCH_H) / TRENCH_CELL);
        int cy1 = (int) Math.floor((b.y + b.h) / TRENCH_CELL);

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cy = cy0; cy <= cy1; cy++) {
                if (!hasTrench(cx, cy)) continue;

                double rx = cx * TRENCH_CELL;
                double ry = cy * TRENCH_CELL;
                if (b.x < rx + TRENCH_W && b.x + b.w > rx
                        && b.y < ry + TRENCH_H && b.y + b.h > ry) {
                    return true;
                }
            }
        }
        return false;
    }
    public void drawCarcasses(GraphicsContext gc, List<Obstacle> carcas, Image[] obraz, double camX, double camY){
        for (Obstacle c : carcas){
            gc.save();
            gc.translate(c.x - camX + c.w / 2, c.y - camY + c.h / 2);
            gc.rotate(Math.toDegrees(c.angle));
            gc.drawImage(obraz[c.type], -c.w / 2, -c.h / 2, c.w, c.h);
            gc.restore();
        }
    }
    public void drawTrenches(GraphicsContext gc, Cam cam, double width, double height, Image trench) {
        double camX = cam.renderX();
        double camY = cam.renderY();
        int x0 = (int) Math.floor(camX / TRENCH_CELL);
        int y0 = (int) Math.floor(camY / TRENCH_CELL);
        int nx = (int) Math.ceil(width / cam.getZoom() / TRENCH_CELL) + 1;
        int ny = (int) Math.ceil(height / cam.getZoom() / TRENCH_CELL) + 1;

        gc.setFill(Color.rgb(255, 255, 255));
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                int cx = x0 + i;
                int cy = y0 + j;
                if (hasTrench(cx, cy)) {
                    //gc.fillRect(cx * TRENCH_CELL - camX, cy * TRENCH_CELL - camY, TRENCH_W, TRENCH_H);
                    gc.drawImage(trench, cx * TRENCH_CELL - camX, cy * TRENCH_CELL - camY, TRENCH_W, TRENCH_H);
                }
            }
        }
    }
    public void expiration(){
        carcas.removeIf(c -> c.spawnRound <= round - 2);
    }
}