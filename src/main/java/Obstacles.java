import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.*;

public class Obstacles {
    static final double[] SIZE_W = Legions.SIZE_W;
    static final double[] SIZE_H = Legions.SIZE_H;
    static final double SEPARATION_RATE = 6.3;
    static final double SLOW_SPEED = 200;
    static int round;
    static class Obstacle extends Object{
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

    public List<Obstacle> getCarcas() { return carcas; }

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
    public boolean colision(double dt, Object object, boolean dash, double objSpeed){
        boolean slowed = false;
        boolean colided = false;

        for (Obstacle o : carcas) {
            double px = object.x + object.w / 2, py = object.y + object.h / 2;
            double ox = o.x + o.w / 2, oy = o.y + o.h / 2;
            double dx = ox - px, dy = oy - py;
            double distance = Math.hypot(dx, dy);
            double minDistance = ((object.w + object.h) / 2 + (o.w + o.h) / 2) / 2;
            if (distance < minDistance && distance > 0.0001 && !dash) {
                slowed = true;
                colided = true;
                double overLap = minDistance - distance;
                double separation = overLap * (1 - Math.exp(-SEPARATION_RATE * dt));
                double shift = separation / 2;
                object.x -= dx / distance * shift ;
                object.y -= dy / distance * shift ;
                o.x += dx / distance * shift ;
                o.y += dy / distance * shift ;
            }
        }
        if(slowed){
            object.speed = SLOW_SPEED;
        }else if(objSpeed != 0){
            object.speed = objSpeed;
        }
        return colided;
    }
    public void expiration(){
        carcas.removeIf(c -> c.spawnRound == round - 2);
    }
}