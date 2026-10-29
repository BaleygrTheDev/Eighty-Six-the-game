import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.*;

public class Obstacles {
    static final double[] SIZE_W = Legions.SIZE_W;
    static final double[] SIZE_H = Legions.SIZE_H;
    static int round;
    static class Obstacle{
        double x, y;
        double w, h;
        double angle;
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

    public List<Obstacles.Obstacle> getCarcas() { return carcas; }

    public void delete(){
        carcas.clear();
    }

    public int setRound(int round){ return this.round = round; }

    public void newCarcas(double x, double y, double angle, int type, int spawnRound){
        Obstacle c = new Obstacle(x, y, angle, type, spawnRound);
        carcas.add(c);
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
    public void expiration(){
        for(Obstacle c : carcas){
            if (c.spawnRound == round - 2) { carcas.remove(c); }
        }
    }
}
