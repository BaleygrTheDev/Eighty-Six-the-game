import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.*;

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
    private final Cam cam = new Cam(SZEROKOSC, WYSOKOSC);

    private Feldres gracz;
    private Feldres fido;
    private double czas;
    private double myszX = SZEROKOSC / 2;
    private double myszY = WYSOKOSC / 2;
    private boolean myszStrzela = false;
    private double dashCzas = 0.10;
    private double dashPredkosc = 1200;
    private double dashTimer = 0;
    private double dashCooldown = 0;
    private double dashX = 0;
    private double dashY = 0;
    private boolean qPoprzednio = false;
    private boolean ePoprzednio = false;
    private double czasDoStrzalu = 0;
    private final double szybkostrzelnosc = .01;
    private final double predkoscPocisku = 1200;
    private final int magCappacity = 14;
    private int  ammo = magCappacity;
    private  final double wymiana = 5;
    private double doWymiany = 0;
    static final double playerSpeed = 500;
    static final double fidoSpeed = 450;
    static final double dashWait = 0.6;
    private int seed;
    static final int[] WAGI = {65, 20, 10, 5};
    private double katGracza = 0;
    private  double fido_kat;

    private Image crosshair = load("/jugger crosshair.png");
    private Image teksturaGracza = load("/jugger walker.png");
    private Image teksturaFido = load("/fido walk.png");
    private double czasAnimacji = 0;
    private double czasAnimacjiFido = 0;
    private int klatka = 0;
    private int klatkaFido = 0;
    private final double czasKlatki = 0.12;
    private Image bullet = load("/HEAT bulet.png");
    private Image[] tiles = {
            load("/tile1.png"),
            load("/tile4.png"),
            load("/tile3.png"),
            load("/tile2.png")
    };

    private Image load(String file){
        return(new Image(Objects.requireNonNull(getClass().getResourceAsStream(file))));
    }

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
        seed = random.nextInt();
        cam.drag(gracz.x + gracz.w / 2, gracz.y + gracz.h / 2, myszX, myszY);
        aktualizujKatGracza();
    }

    private int hash(int x, int y) {
        int h = x * 374761393 + y * 668265263 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private int chooseTile(int tx, int ty) {
        int r = Math.floorMod(hash(tx, ty), 100);     // floorMod, bo hash bywa ujemny
        int suma = 0;
        for (int i = 0; i < WAGI.length; i++) {
            suma += WAGI[i];
            if (r < suma) return i;
        }
        return 0;
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
        int dir = 0;

        if (!fido.koliduje(gracz)) {
            if (ammo == 0) {
                dir = 1;
            } else if (odleglosc < 2000) {
                dir = -1;
            } else {
                dir = 1;
            }
        }else if(doWymiany <= 0 && ammo == magCappacity){
            dir = -1;
        }
        if (dir != 0) {
            fido.x += (dx / odleglosc) * dir * fidoSpeed * dt;
            fido.y += (dy / odleglosc) * dir * fidoSpeed * dt;
            fido_kat = Math.atan2(dir * (dy / odleglosc), dir * (dx / odleglosc));

            czasAnimacjiFido += dt;
            if (czasAnimacjiFido >= czasKlatki) {
                klatkaFido = (klatkaFido + 1) % 4;
                czasAnimacjiFido = 0;
            }
        }else {
            klatkaFido = 0;           // stoi, więc klatka stojąca, bez animacji chodzenia
            czasAnimacjiFido = 0;
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
            klatka = 1;
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
            if (cam.outside(b.x, b.y, 200)) {
                shot.remove(i);
            }
        }
    }

    private void aktualizujKatGracza() {
        double graczX = gracz.x + gracz.w / 2;
        double graczY = gracz.y + gracz.h / 2;

        double celX = cam.worldX(myszX);
        double celY = cam.worldY(myszY);

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

        cam.updateCam(gracz.x + gracz.w / 2, gracz.y + gracz.h / 2, myszX, myszY, dt);
        aktualizujKatGracza();
        shooting(dt);
        fidoUpdate(dt);
        aktualizujAnimacje(dt, dlugosc);

    }

    private void render(GraphicsContext gc) {
        double size = 130;

        double camX = cam.renderX();
        double camY = cam.renderY();

        int T = (int) ROZMIAR_KAFELKA;
        int x0 = (int) Math.floor(camX / T);               // indeks pierwszego widocznego kafelka
        int y0 = (int) Math.floor(camY / T);
        int nx = (int) Math.ceil(SZEROKOSC / T) + 1;       // ile kafelków w poziomie (z zapasem)
        int ny = (int) Math.ceil(WYSOKOSC / T) + 1;

        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                int tx = x0 + i;
                int ty = y0 + j;

                int h = hash(tx, ty);
                int obrot = (h >>> 8) & 3;
                Image tile = tiles[chooseTile(tx, ty)];

                double sx = tx * T - camX;
                double sy = ty * T - camY;

                gc.save();
                gc.translate(sx + T / 2.0, sy + T / 2.0);
                gc.rotate(obrot * 90);
                gc.drawImage(tile, -T / 2.0, -T / 2.0, T, T);
                gc.restore();
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

        double szerokoscFido = teksturaFido.getWidth() / 4;
        double wysokoscFido = teksturaFido.getHeight();

        gc.save();
        gc.translate(dx, dy);
        gc.rotate(Math.toDegrees(fido_kat));
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

        gc.setFont(Font.font("Arial", 20));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(String.format("Czas: %.1f s", czas), 10, 25);
        gc.setFont(Font.font("Arial", 18));
        gc.setFill(Color.RED);
        gc.fillText("ammo", myszX - 65, myszY - 70);
        gc.fillText(String.format("%d/%d" , ammo, magCappacity), myszX - 50, myszY - 50);
        gc.fillText("reload", myszX - 65, myszY + 80);
        if (ammo == 0){
            gc.fillText("empty", myszX - 50, myszY + 60);
        }else if(czasDoStrzalu == 0) {
            gc.fillText("ready", myszX - 50, myszY + 60);
        } else {
            gc.fillText(String.format("%.1f s", czasDoStrzalu), myszX - 50, myszY + 60);
        }
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("swap", myszX + 65, myszY + 80);
        if (doWymiany == 0){
            gc.fillText("ready", myszX + 50, myszY + 60);
        }else {
            gc.fillText(String.format("%.1f s", doWymiany), myszX + 50, myszY + 60);
        }
        if (gracz.koliduje(fido) && doWymiany == 0 && ammo == 0){
            gc.fillText("'R' to swap mag", myszX + 65, myszY + 110);
        }

        gc.drawImage(
                crosshair,
                myszX - size / 2,
                myszY - size / 2,
                size,
                size
        );
    }
    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(SZEROKOSC, WYSOKOSC);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Scene scene = new Scene(new StackPane(canvas));

        scene.setCursor(Cursor.NONE);
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
            public void handle(long now) {
                if (poprzedni == 0) { poprzedni = now; return; }
                double dt = Math.min((now - poprzedni) / 1_000_000_000.0, 0.05);
                poprzedni = now;
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
