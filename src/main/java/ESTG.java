import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javafx.scene.image.Image;

public class ESTG extends Application {
    static final double SZEROKOSC = 1900;
    static final double WYSOKOSC = 1000;
    static final double ROZMIAR_KAFELKA = 300;

    static class Feldres {
        double x, y, w, h, vy;

        Feldres(double x, double y, double w, double h, double vy) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.vy = vy;
        }

        boolean koliduje(Feldres b) {
            return x < b.x + b.w && x + w > b.x && y < b.y + b.h && y + h > b.y;
        }
    }
    private final Set<KeyCode> klawisze = new HashSet<>();
    private final List<Feldres> legion = new ArrayList<>();
    private final Random random = new Random();

    private Feldres gracz;
    private double czas;
    private double myszX;
    private double myszY;
    private double kameraX;
    private double kameraY;
    private double dashCzas = 0.15;
    private double dashPredkosc = 1000;
    private double dashTimer = 0;
    private double dashCooldown = 0;
    private double dashX = 0;
    private double dashY = 0;
    private boolean qPoprzednio = false;
    private boolean ePoprzednio = false;

    private Image teksturaGracza =
            new Image(getClass().getResourceAsStream("/juggernaut_temu.png"));
    private Image tlo =
            new Image(getClass().getResourceAsStream("/kawalek_ziemi.png"));

    private void nowaGra() {
        gracz = new Feldres(SZEROKOSC / 2 - 25, WYSOKOSC / 2 - 50, 121, 50, 0);
        legion.clear();
        czas = 0;
        aktualizujKamere();
    }

    private void aktualizujKamere() {
        double graczX = gracz.x + gracz.w / 2;
        double graczY = gracz.y + gracz.h / 2;

        double kat = Math.atan2(
                myszY - WYSOKOSC / 2,
                myszX - SZEROKOSC / 2
        );

        double przesuniecieX = Math.cos(kat) * SZEROKOSC * 0.25;
        double przesuniecieY = Math.sin(kat) * WYSOKOSC * 0.25;

        kameraX = graczX - SZEROKOSC / 2 + przesuniecieX;
        kameraY = graczY - WYSOKOSC / 2 + przesuniecieY;
    }

    private void update(double dt) {
        czas += dt;

        // ruch gracza
        double kierunekX = 0;
        double kierunekY = 0;

        if (klawisze.contains(KeyCode.LEFT) || klawisze.contains(KeyCode.A)) kierunekX -= 1;
        if (klawisze.contains(KeyCode.RIGHT) || klawisze.contains(KeyCode.D)) kierunekX += 1;
        if (klawisze.contains(KeyCode.UP) || klawisze.contains(KeyCode.W)) kierunekY += 1;
        if (klawisze.contains(KeyCode.DOWN) || klawisze.contains(KeyCode.S)) kierunekY -= 1;

        // Kąt w stronę kursora
        double celX = myszX + kameraX;
        double celY = myszY + kameraY;

        double kat = Math.atan2(
                celY - (gracz.y + gracz.h / 2),
                celX - (gracz.x + gracz.w / 2)
        );

        // Ruch względem obrotu gracza
        double ruchX = Math.cos(kat) * kierunekY - Math.sin(kat) * kierunekX;
        double ruchY = Math.sin(kat) * kierunekY + Math.cos(kat) * kierunekX;

        // Normalizacja ruchu po przekątnej
        double dlugosc = Math.sqrt(ruchX * ruchX + ruchY * ruchY);
        if (dlugosc > 1) {
            ruchX /= dlugosc;
            ruchY /= dlugosc;
        }

        gracz.x += ruchX * 350 * dt;
        gracz.y += ruchY * 350 * dt;

        dashCooldown = Math.max(0, dashCooldown - dt);

        boolean q = klawisze.contains(KeyCode.Q);
        boolean e = klawisze.contains(KeyCode.E);

        if (dashCooldown == 0) {
            if (q && !qPoprzednio) {
                dashX = Math.sin(kat);
                dashY = -Math.cos(kat);
                dashTimer = dashCzas;
                dashCooldown = 0.6;
            } else if (e && !ePoprzednio) {
                dashX = -Math.sin(kat);
                dashY = Math.cos(kat);
                dashTimer = dashCzas;
                dashCooldown = 0.6;
            }
        }

        qPoprzednio = q;
        ePoprzednio = e;

        if (dashTimer > 0) {
            double krok = Math.min(dt, dashTimer);
            gracz.x += dashX * dashPredkosc * krok;
            gracz.y += dashY * dashPredkosc * krok;
            dashTimer -= krok;
        } else {
            gracz.x += ruchX * 350 * dt;
            gracz.y += ruchY * 350 * dt;
        }

        aktualizujKamere();
    }

    private void render(GraphicsContext gc) {
        gc.setFill(Color.web("#181a20"));
        double tileW = ROZMIAR_KAFELKA;
        double tileH = ROZMIAR_KAFELKA;

        // Pierwszy kafelek widoczny w lewym górnym rogu
        double startX = Math.floor(kameraX / tileW) * tileW;
        double startY = Math.floor(kameraY / tileH) * tileH;

        // Rysowanie kafelków na całym ekranie
        for (double x = startX; x < kameraX + SZEROKOSC + tileW; x += tileW) {
            for (double y = startY; y < kameraY + WYSOKOSC + tileH; y += tileH) {
                gc.drawImage(
                        tlo,
                        x - kameraX,
                        y - kameraY,
                        tileW,
                        tileH
                );
            }
        }
        //gc.fillRect(0, 0, SZEROKOSC, WYSOKOSC);

        gc.setFill(Color.DODGERBLUE);
        double cx = gracz.x - kameraX + gracz.w / 2;
        double cy = gracz.y - kameraY + gracz.h / 2;

        double kat = Math.toDegrees(
                Math.atan2(myszY - cy, myszX - cx)
        );

        gc.save();
        gc.translate(cx, cy);
        gc.rotate(kat);
        gc.drawImage(
                teksturaGracza,
                -gracz.w / 2,
                -gracz.h / 2,
                gracz.w,
                gracz.h
        );

        gc.restore();

        gc.setFill(Color.TOMATO);
        for (Feldres b : legion) {
            gc.fillRect(b.x, b.y, b.w, b.h);
        }

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 20));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(String.format("Czas: %.1f s", czas), 10, 25);
    }
    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(SZEROKOSC, WYSOKOSC);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Scene scene = new Scene(new StackPane(canvas));

        scene.setOnMouseMoved(e -> {
            myszX = e.getX();
            myszY = e.getY();
        });

        scene.setOnKeyPressed(e -> {
            klawisze.add(e.getCode());
            if (e.getCode() == KeyCode.R) nowaGra();
        });
        scene.setOnKeyReleased(e -> klawisze.remove(e.getCode()));

        nowaGra();

        new AnimationTimer() {
            long poprzedni = 0;

            @Override
            public void handle(long teraz) {
                if (poprzedni == 0) { poprzedni = teraz; return; }
                double dt = Math.min((teraz - poprzedni) / 1_000_000_000.0, 0.05);
                poprzedni = teraz;
                update(dt);
                render(gc);
            }
        }.start();

        stage.setTitle("pa tera rusza sie");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
