public class Feldres extends Object{
    double hp, turnRate, animTimer;
    int frame;

    Feldres(double x, double y, double w, double h, double hp, double angle,
            double speed, double turnRate, double animTimer, int frame) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.hp = hp; this.angle = angle;
        this.speed = speed; this.turnRate = turnRate; this.animTimer = animTimer; this.frame = frame;
    }

    boolean koliduje(Object b) {
        return x < b.x + b.w && x + w > b.x && y < b.y + b.h && y + h > b.y;
    }
}