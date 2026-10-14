public class Feldres {
    double x, y, w, h, vy;

    Feldres(double x, double y, double w, double h, double vy) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.vy = vy;
    }

    boolean koliduje(Feldres b) {
        return x < b.x + b.w && x + w > b.x && y < b.y + b.h && y + h > b.y;
    }
}