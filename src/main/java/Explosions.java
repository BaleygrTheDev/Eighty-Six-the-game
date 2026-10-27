import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Efekty wybuchów po zniszczeniu wroga; rodzaj wynika z tego, czym wróg został zabity. */
public class Explosions {
    // czas trwania; indeks = rodzaj (FS.HEAT, FS.APFSDS, FS.ENEMY = inna przyczyna)
    private static final double[] DURATION = {0.7, 0.4, 0.5};

    private static class Blast {
        final double x, y;        // środek, współrzędne ŚWIATA
        final int kind;
        final double scale;       // skala względem wroga o wielkości 100 px
        final double rotation;    // losowy obrót iskier, żeby każdy wybuch wyglądał inaczej
        double age = 0;

        Blast(double x, double y, int kind, double size, double rotation) {
            this.x = x;
            this.y = y;
            this.kind = kind;
            this.scale = size / 100.0;
            this.rotation = rotation;
        }
    }

    private final List<Blast> blasts = new ArrayList<>();
    private final Random random = new Random();

    public void spawn(double x, double y, int kind, double size) {
        blasts.add(new Blast(x, y, kind, size, random.nextDouble() * Math.PI * 2));
    }

    public void update(double dt) {
        blasts.removeIf(b -> {
            b.age += dt;
            return b.age >= DURATION[b.kind];
        });
    }

    public void clear() {
        blasts.clear();
    }

    /** Wołaj w sekcji ŚWIAT (przed gc.restore() kończącym zoom), po wrogach i pociskach. */
    public void draw(GraphicsContext gc, double camX, double camY) {
        for (Blast b : blasts) {
            double t = b.age / DURATION[b.kind];         // postęp: 0 -> 1
            double sx = b.x - camX;
            double sy = b.y - camY;

            switch (b.kind) {
                case FS.HEAT:
                    drawHeat(gc, sx, sy, b.scale, t);
                    break;
                case FS.APFSDS:
                    drawApfsds(gc, sx, sy, b.scale, t, b.rotation);
                    break;
                default:
                    drawSmoke(gc, sx, sy, b.scale, t);
            }
        }
        gc.setLineWidth(1);       // nie zostawiaj grubej linii dla reszty rysowania
    }

    /** Szybki start, potem zwalnia (wybuch "wystrzeliwuje" i się uspokaja). */
    private static double easeOut(double t) {
        return 1 - (1 - t) * (1 - t);
    }

    // ---------- HEAT: kula ognia + fala uderzeniowa o zasięgu obrażeń ----------
    private void drawHeat(GraphicsContext gc, double x, double y, double scale, double t) {
        double e = easeOut(t);
        double fade = 1 - t;

        // zewnętrzna pomarańczowa kula
        double r = 70 * scale * e;
        gc.setFill(Color.color(1.0, 0.45, 0.05, 0.85 * fade));
        gc.fillOval(x - r, y - r, r * 2, r * 2);

        // jasny, żółtawy rdzeń (mniejszy i szybciej gasnący)
        double rc = r * 0.55;
        gc.setFill(Color.color(1.0, 0.9, 0.4, fade * fade));
        gc.fillOval(x - rc, y - rc, rc * 2, rc * 2);

        // fala uderzeniowa: dochodzi dokładnie do promienia obrażeń
        double rw = FS.BLAST_RADIUS * e;
        gc.setStroke(Color.color(1.0, 0.8, 0.4, 0.5 * fade));
        gc.setLineWidth(2 + 6 * fade);
        gc.strokeOval(x - rw, y - rw, rw * 2, rw * 2);
    }

    // ---------- APFSDS: krótki biały błysk i iskry ----------
    private void drawApfsds(GraphicsContext gc, double x, double y, double scale,
                            double t, double rotation) {
        double e = easeOut(t);
        double fade = 1 - t;

        // błysk, który szybko gaśnie
        double rf = 5 + 35 * scale * fade;
        gc.setFill(Color.color(1.0, 1.0, 1.0, 0.9 * fade * fade));
        gc.fillOval(x - rf, y - rf, rf * 2, rf * 2);

        // iskry rozchodzące się promieniście
        int sparks = 10;
        double inner = 10 * scale + 40 * scale * e;
        double outer = inner + 40 * scale * fade;
        gc.setStroke(Color.color(0.7, 0.9, 1.0, fade));
        gc.setLineWidth(1 + 3 * fade);
        for (int i = 0; i < sparks; i++) {
            double a = rotation + i * (Math.PI * 2 / sparks);
            gc.strokeLine(x + Math.cos(a) * inner, y + Math.sin(a) * inner,
                    x + Math.cos(a) * outer, y + Math.sin(a) * outer);
        }
    }

    // ---------- inne (np. taranowanie): obłok dymu z krótkim błyskiem ----------
    private void drawSmoke(GraphicsContext gc, double x, double y, double scale, double t) {
        double e = easeOut(t);
        double fade = 1 - t;

        double r = 45 * scale * e;
        gc.setFill(Color.color(0.25, 0.25, 0.25, 0.6 * fade));
        gc.fillOval(x - r, y - r, r * 2, r * 2);

        double rs = r * 0.6;
        gc.setFill(Color.color(0.5, 0.5, 0.5, 0.5 * fade));
        gc.fillOval(x - rs, y - rs, rs * 2, rs * 2);

        if (t < 0.25) {                                   // błysk tylko na początku
            double rb = 30 * scale * (1 - t * 4);
            gc.setFill(Color.color(1.0, 0.6, 0.1, 0.8));
            gc.fillOval(x - rb, y - rb, rb * 2, rb * 2);
        }
    }
}