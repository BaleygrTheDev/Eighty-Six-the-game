import java.util.List;

public class Body {
    double x, y, w, h;
    double angle, speed;

    boolean colision(double dt, boolean dash, double objSpeed, List<? extends Body> bodys){
        boolean colided = false;

        for (Body b : bodys) {
            if (b == this) continue;
            double px = this.x + this.w / 2, py = this.y + this.h / 2;
            double ox = b.x + b.w / 2, oy = b.y + b.h / 2;
            double dx = ox - px, dy = oy - py;
            double distance = Math.hypot(dx, dy);
            double minDistance = ((this.w + this.h) / 2 + (b.w + b.h) / 2) / 2;
            if (distance < minDistance && distance > 0.0001 && !dash) {
                colided = true;
                double overLap = minDistance - distance;
                double separation = overLap * (1 - Math.exp(-6 * dt));
                double shift = separation / 2;
                this.x -= dx / distance * shift ;
                this.y -= dy / distance * shift ;
                b.x += dx / distance * shift ;
                b.y += dy / distance * shift ;
            }
        }
        if(colided){
            this.speed = objSpeed * 0.3;
        }else if(objSpeed != 0){
            this.speed = objSpeed;
        }
        return colided;
    }
    boolean colision(List<? extends Body> bodys){
        boolean colided = false;
        for (Body b : bodys) {
            if (b == this) continue;
            double px = this.x + this.w / 2, py = this.y + this.h / 2;
            double ox = b.x + b.w / 2, oy = b.y + b.h / 2;
            double dx = ox - px, dy = oy - py;
            double distance = Math.hypot(dx, dy);
            double minDistance = ((this.w + this.h) / 2 + (b.w + b.h) / 2) / 2;
            if (distance < minDistance && distance > 0.00001) colided = true;
        }
        return colided;
    }
}
