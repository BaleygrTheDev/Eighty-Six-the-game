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

    static class Bulets {
        double x, y, w, h;
        double vx, vy;
        double dmg;

        Bulets(double x, double y, double w, double h,
               double vx, double vy, double dmg) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.vx = vx;
            this.vy = vy;
            this.dmg = dmg;
        }
    }

    private final Set<KeyCode> klawisze = new HashSet<>();
    private final List<Feldres> legion = new ArrayList<>();
    private final List<Bulets> shot = new ArrayList<>();
    private final Random random = new Random();

    private Feldres gracz;
    private Feldres fido;
    private double czas;
    private double myszX = SZEROKOSC / 2;
    private double myszY = WYSOKOSC / 2;
    private boolean myszStrzela = false;
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
    private double czasDoStrzalu = 0;
    private final double szybkostrzelnosc = 2;
    private final double predkoscPocisku = 1200;
    private final double magCappacity = 14;
    private double  ammo = magCappacity;
    private  final double wymiana = 5;
    private double doWymiany = 0;
    static final double playerSpeed = 350;
    static final double fidoSpeed = 300;
    static final double dashWait = 0.6;
    static final double wygladzanieKamery = 20;
    static final double marginX = 140;
    static final double marginY = 100;
    static final double MARTWA_X = 0;      // połowa szerokości pasa martwego (px od środka)
    static final double ROZPETOSC_X = 500;   // ile px za strefą martwą gracz dojeżdża do pełnego wysunięcia
    static final double MARTWA_Y = 0;        // pion: 0 = brak strefy martwej
    static final double ROZPETOSC_Y = 250;

    private double katGracza = 0;
    private double celKameryX, celKameryY;

    private Image teksturaGracza =
            new Image(getClass().getResourceAsStream("/jugger walker.png"));
    private Image teksturaFido =
            new Image(getClass().getResourceAsStream("/fido walk.png"));
    private double czasAnimacji = 0;
    private double czasAnimacjiFido = 0;
    private int klatka = 0;
    private int klatkaFido = 0;
    private final double czasKlatki = 0.12;
    private Image tlo =
            new Image(getClass().getResourceAsStream("/kawalek_ziemi.png"));
    private Image bullet =
            new Image(getClass().getResourceAsStream("/HEAT bulet.png"));

    private void nowaGra() {
        gracz = new Feldres(SZEROKOSC / 2 - 25, WYSOKOSC / 2 - 50, 121, 75, 0);
        fido = new Feldres(SZEROKOSC / 2 - 25, WYSOKOSC / 2 - 300, 60, 50, 0);
        legion.clear();
        shot.clear();
        czas = 0;
        czasDoStrzalu = 0;
        doWymiany = 0;
        ammo = magCappacity;
        dashCooldown = 0;
        dashTimer = 0;
        qPoprzednio = false;
        ePoprzednio = false;
        klatka = 0;
        klatkaFido = 0;
        kameraX = gracz.x + gracz.w / 2 - SZEROKOSC / 2;
        kameraY = gracz.y + gracz.h / 2 - WYSOKOSC / 2;
        for (int i = 0; i < 60; i++) aktualizujKamere(1.0 / 60);
        aktualizujKatGracza();
        aktualizujKamere(0);
    }
    private void fidoUpdate(double dt) {
        double graczX = gracz.x + gracz.w / 2;
        double graczy = gracz.y + gracz.h / 2;

        double fidoX = fido.x + fido.w / 2;
        double fidoy = fido.y + fido.h / 2;

        double dx = graczX - fidoX;
        double dy = graczy - fidoy;
        double odleglosc = Math.sqrt(dx * dx + dy * dy);
        if (odleglosc < 0.0001) return;

        if (!fido.koliduje(gracz)) {
            if (ammo == 0) {
                fido.x += (dx / odleglosc) * fidoSpeed * dt;
                fido.y += (dy / odleglosc) * fidoSpeed * dt;
            } else if (odleglosc < 2000) {
                fido.x -= (dx / odleglosc) * fidoSpeed * dt;
                fido.y -= (dy / odleglosc) * fidoSpeed * dt;
            } else {
                fido.x += (dx / odleglosc) * fidoSpeed * dt;
                fido.y += (dy / odleglosc) * fidoSpeed * dt;
            }
            czasAnimacjiFido += dt;

            if (czasAnimacjiFido >= czasKlatki) {
                klatkaFido = (klatkaFido + 1) % 4;
                czasAnimacjiFido = 0;
            }
        }
    }

    private void aktualizujAnimacje(double dt, double dlugosc) {
        boolean chodzi = (dlugosc > 0 && doWymiany <= 0);

        if (chodzi) {
            czasAnimacji += dt;

            if (czasAnimacji >= czasKlatki) {
                klatka = (klatka + 1) % 4;
                czasAnimacji = 0;
            }
        } else {
            klatka = 0;
            czasAnimacji = 0;
        }
    }

    private void shooting(double dt) {
        czasDoStrzalu -= dt;
        if(czasDoStrzalu < 0) czasDoStrzalu = 0;

        double graczX = gracz.x + gracz.w / 2;
        double graczY = gracz.y + gracz.h / 2;

        // Tworzenie pocisków podczas trzymania spacji
        if (myszStrzela && czasDoStrzalu <= 0 && ammo >0) {
            double kierunekX = Math.cos(katGracza);
            double kierunekY = Math.sin(katGracza);

            double startX = graczX + kierunekX * 45;
            double startY = graczY + kierunekY * 45;

            shot.add(new Bulets(
                    startX - 5,
                    startY - 2.5,
                    10,
                    5,
                    kierunekX * predkoscPocisku,
                    kierunekY * predkoscPocisku,
                    10
            ));

            czasDoStrzalu = szybkostrzelnosc;
            ammo -= 1;
        }

        // Aktualizacja lotu pocisków
        for (int i = shot.size() - 1; i >= 0; i--) {
            Bulets b = shot.get(i);

            b.x += b.vx * dt;
            b.y += b.vy * dt;

            // Usuwanie pocisków daleko poza widocznym ekranem
            if (b.x < kameraX - 200
                    || b.x > kameraX + SZEROKOSC + 200
                    || b.y < kameraY - 200
                    || b.y > kameraY + WYSOKOSC + 200) {
                shot.remove(i);
            }
        }
    }

    private void aktualizujKamere(double dt) {
        liczCelKamery();

        double a = 1 - Math.exp(-wygladzanieKamery * dt);
        kameraX += (celKameryX - kameraX) * a;
        kameraY += (celKameryY - kameraY) * a;
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private void liczCelKamery() {
        double graczX = gracz.x + gracz.w / 2;
        double graczY = gracz.y + gracz.h / 2;

        // położenie kursora względem ŚRODKA EKRANU (nie zależy od kamery)
        double dx = myszX - SZEROKOSC / 2;
        double dy = myszY - WYSOKOSC / 2;

        // siła wysunięcia: 0 w strefie martwej, rośnie płynnie do 1
        double sx = smooth((Math.abs(dx) - MARTWA_X) / ROZPETOSC_X);
        double sy = smooth((Math.abs(dy) - MARTWA_Y) / ROZPETOSC_Y);

        double zasiegX = SZEROKOSC / 2 - marginX;
        double zasiegY = WYSOKOSC / 2 - marginY;

        // gracz stoi po przeciwnej stronie niż kursor
        double docelowyEkranX = SZEROKOSC / 2 - Math.signum(dx) * zasiegX * sx;
        double docelowyEkranY = WYSOKOSC / 2 - Math.signum(dy) * zasiegY * sy;

        celKameryX = graczX - docelowyEkranX;
        celKameryY = graczY - docelowyEkranY;
    }

    private void aktualizujKatGracza() {
        double graczX = gracz.x + gracz.w / 2;
        double graczY = gracz.y + gracz.h / 2;

        double celX = myszX + kameraX;
        double celY = myszY + kameraY;

        katGracza = Math.atan2(celY - graczY, celX - graczX);
    }

    private void update(double dt) {
        czas += dt;

        aktualizujKatGracza();

        // ruch gracza
        double kierunekX = 0;
        double kierunekY = 0;

        if (klawisze.contains(KeyCode.LEFT) || klawisze.contains(KeyCode.A)) kierunekX -= 1;
        if (klawisze.contains(KeyCode.RIGHT) || klawisze.contains(KeyCode.D)) kierunekX += 1;
        if (klawisze.contains(KeyCode.UP) || klawisze.contains(KeyCode.W)) kierunekY += 1;
        if (klawisze.contains(KeyCode.DOWN) || klawisze.contains(KeyCode.S)) kierunekY -= 1;

        // Ruch względem obrotu gracza
        double ruchX = Math.cos(katGracza) * kierunekY - Math.sin(katGracza) * kierunekX;
        double ruchY = Math.sin(katGracza) * kierunekY + Math.cos(katGracza) * kierunekX;

        // Normalizacja ruchu po przekątnej
        double dlugosc = Math.sqrt(ruchX * ruchX + ruchY * ruchY);
        if (dlugosc > 1) {
            ruchX /= dlugosc;
            ruchY /= dlugosc;
        }
        if (doWymiany <= 0) {
            gracz.x += ruchX * playerSpeed * dt;
            gracz.y += ruchY * playerSpeed * dt;
        }

        dashCooldown = Math.max(0, dashCooldown - dt);

        boolean q = klawisze.contains(KeyCode.Q);
        boolean e = klawisze.contains(KeyCode.E);

        if (dashCooldown == 0) {
            if (q && !qPoprzednio) {
                dashX = Math.sin(katGracza);
                dashY = -Math.cos(katGracza);
                dashTimer = dashCzas;
                dashCooldown = dashWait;
            } else if (e && !ePoprzednio) {
                dashX = -Math.sin(katGracza);
                dashY = Math.cos(katGracza);
                dashTimer = dashCzas;
                dashCooldown = dashWait;
            }
        }

        qPoprzednio = q;
        ePoprzednio = e;

        if (dashTimer > 0) {
            double krok = Math.min(dt, dashTimer);
            gracz.x += dashX * dashPredkosc * krok;
            gracz.y += dashY * dashPredkosc * krok;
            dashTimer -= krok;
        }

        if (klawisze.contains(KeyCode.R) && gracz.koliduje(fido) && doWymiany <= 0){
            doWymiany = wymiana;
        }

        if (doWymiany > 0) {
            doWymiany -= dt;

            if (doWymiany <= 0) {
                doWymiany = 0;
                ammo = magCappacity;
            }
        }

        aktualizujKamere(dt);
        aktualizujKatGracza();
        shooting(dt);
        fidoUpdate(dt);
        aktualizujAnimacje(dt, dlugosc);

    }

    private void render(GraphicsContext gc) {
        double camX = Math.round(kameraX);
        double camY = Math.round(kameraY);

        double tileW = ROZMIAR_KAFELKA;
        double tileH = ROZMIAR_KAFELKA;

        // Pierwszy kafelek widoczny w lewym górnym rogu
        double startX = Math.floor(camX / tileW) * tileW;
        double startY = Math.floor(camY / tileH) * tileH;

        // Rysowanie kafelków na całym ekranie
        for (double x = startX; x < camX + SZEROKOSC + tileW; x += tileW) {
            for (double y = startY; y < camY + WYSOKOSC + tileH; y += tileH) {
                gc.drawImage(
                        tlo,
                        x - camX,
                        y - camY,
                        tileW,
                        tileH
                );
            }
        }

        double cx = gracz.x - camX + gracz.w / 2;
        double cy = gracz.y - camY + gracz.h / 2;

        double kat = Math.toDegrees(katGracza);

        double szerokoscKlatki = teksturaGracza.getWidth() / 4;
        double wysokoscKlatki = teksturaGracza.getHeight();

        gc.save();
        gc.translate(cx, cy);
        gc.rotate(kat);

        gc.drawImage(
                teksturaGracza,
                klatka * szerokoscKlatki, 0,
                szerokoscKlatki, wysokoscKlatki,
                -gracz.w / 2, -gracz.h / 2,
                gracz.w, gracz.h
        );

        gc.restore();

        double dx = fido.x - camX + fido.w / 2;
        double dy = fido.y - camY + fido.h / 2;

        double fido_kat = Math.toDegrees(
                Math.atan2(gracz.y + gracz.h / 2 - (fido.y + fido.h / 2),
                        gracz.x + gracz.w / 2 - (fido.x + fido.w / 2))
        );

        double szerokoscFido = teksturaFido.getWidth() / 4;
        double wysokoscFido = teksturaFido.getHeight();

        gc.save();
        gc.translate(dx, dy);
        gc.rotate(fido_kat);

        gc.drawImage(
                teksturaFido,
                klatkaFido * szerokoscFido, 0,
                szerokoscFido, wysokoscFido,
                -fido.w / 2, -fido.h / 2,
                fido.w, fido.h
        );

        gc.restore();

        for (Bulets b : shot) {
            // Kąt lotu pocisku w stopniach
            double katPocisku = Math.toDegrees(
                    Math.atan2(b.vy, b.vx)
            );

            gc.save();

            // Środek pocisku na ekranie
            double cxPocisku = b.x - camX + b.w / 2;
            double cyPocisku = b.y - camY + b.h / 2;

            // Obrót tekstury wokół jej środka
            gc.translate(cxPocisku, cyPocisku);
            gc.rotate(katPocisku);

            gc.drawImage(
                    bullet,
                    -b.w / 2,
                    -b.h / 2,
                    b.w,
                    b.h
            );

            gc.restore();
        }

        gc.setFill(Color.TOMATO);
        for (Feldres b : legion) {
            gc.fillRect(b.x - camX, b.y - camY, b.w, b.h);
        }

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 20));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(String.format("Czas: %.1f s", czas), 10, 25);
        gc.fillText(String.format("Ammo: %.1f / %.1f" , ammo, magCappacity), SZEROKOSC - 200, 25);
        gc.fillText(String.format("Przeładowanie: %.1f s", czasDoStrzalu), SZEROKOSC - 200, 75);
        gc.fillText(String.format("Przeładowanie: %.1f s", doWymiany), SZEROKOSC - 200, 125);
    }
    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(SZEROKOSC, WYSOKOSC);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Scene scene = new Scene(new StackPane(canvas));

        scene.setOnMouseMoved(e -> { myszX = e.getX(); myszY = e.getY(); });
        scene.setOnMouseDragged(e -> { myszX = e.getX(); myszY = e.getY(); });

        scene.setOnMousePressed(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                myszStrzela = true;
            }
        });

        scene.setOnMouseReleased(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                myszStrzela = false;
            }
        });

        scene.setOnKeyPressed(e -> klawisze.add(e.getCode()));
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

        stage.setTitle("8ighty 6ix");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
