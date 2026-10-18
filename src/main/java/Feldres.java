public class Feldres {
    double x, y, w, h, hp, angle, speed, turnRate, animTimer;
    int frame;

    Feldres(double x, double y, double w, double h, double hp, double angle,
            double speed, double turnRate, double animTimer, int frame) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.hp = hp; this.angle = angle;
        this.speed = speed; this.turnRate = turnRate; this.animTimer = animTimer; this.frame = frame;
    }

    boolean koliduje(Feldres b) {
        return x < b.x + b.w && x + w > b.x && y < b.y + b.h && y + h > b.y;
    }
}