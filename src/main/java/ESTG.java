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
import javafx.scene.image.Image;

import java.util.*;

public class ESTG extends Application {
    static final double WIDTH = 1900;
    static final double HEIGHT = 1000;
    static final double TILE_SIZE = 300;

    static class Feldres {
        double x, y, w, h, vy;

        Feldres(double x, double y, double w, double h, double vy) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.vy = vy;
        }

        boolean koliduje(Feldres b) {
            return x < b.x + b.w && x + w > b.x && y < b.y + b.h && y + h > b.y;
        }
    }

    static class Legion extends Feldres {
        int type;          // 0 = amaise, 1 = grauwolf, 2 = lowe
        double hp;
        double speed;
        double angle;

        Legion(int type, double x, double y) {
            super(x, y, SIZE_W[type], SIZE_H[type], 0);
            this.type = type;
            this.hp = HP[type];
            this.speed = SPEED[type];
        }
    }

    private final Set<KeyCode> klawisze = new HashSet<>();
    private final List<Legion> legion = new ArrayList<>();
    private final Random random = new Random();
    private final Cam cam = new Cam(WIDTH, HEIGHT);
    private RNG rng = new RNG();

    private Feldres player;
    private Feldres fido;
    private double czas;
    private double myszX = WIDTH / 2;
    private double myszY = HEIGHT / 2;

    private double dashCzas = 0.10;
    private double dashPredkosc = 1200;
    private double dashTimer = 0;
    private double dashCooldown = 0;
    private double dashX = 0;
    private double dashY = 0;
    private boolean qPoprzednio = false;
    private boolean ePoprzednio = false;
    private  final double wymiana = 5;
    private double doWymiany = 0;
    static final double playerSpeed = 500;
    static final double fidoSpeed = 450;
    static final double dashWait = 0.6;
    private double katGracza = 0;
    private double fido_kat;
    private double turnRate = 300;
    private double targetAngle;
    private double aimX;
    private double aimY;
    private int round = 0;
    private int difficulty = 5;          // point pool for the next round
    private double breakTimer = 0;
    static final int[]    COST    = {1, 3, 5};
    static final double[] HP      = {15, 30, 45};
    static final double[] SPEED   = {260, 200, 140};
    static final double[] SIZE_W  = {40, 70, 110};
    static final double[] SIZE_H  = {30, 50, 80};
    static final double ROUND_BREAK = 3.0;     // seconds between rounds
    static final double SPAWN_RADIUS_MIN = 1200;
    static final double SPAWN_RADIUS_RANGE = 300;
    static final double PLAYER_MAX_HP = 100;
    static final double[] CONTACT_DAMAGE = {5, 12, 25};   // per enemy type
    static final double HURT_COOLDOWN = 0.5;
    private double playerHp = PLAYER_MAX_HP;
    private double hurtTimer = 0;
    private int seed;

    private FS fs = new FS();

    private Image crosshair = load("/jugger crosshair.png");
    private Image aim = load("/jugger aim.png");
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

    private void newGame() {
        player = new Feldres(WIDTH / 2 - 25, HEIGHT / 2 - 50, 121, 75, 0);
        fido = new Feldres(WIDTH / 2 - 25, HEIGHT / 2 - 300, 60, 50, 0);
        legion.clear();
        fs.clearB();
        round = 0;
        difficulty = 5;
        breakTimer = 0;
        playerHp = PLAYER_MAX_HP;
        hurtTimer = 0;
        czas = 0;
        doWymiany = 0;
        dashCooldown = 0;
        dashTimer = 0;
        qPoprzednio = false;
        ePoprzednio = false;
        klatka = 0;
        klatkaFido = 0;
        rng.newSeed();
        seed = random.nextInt();
        cam.drag(player.x + player.w / 2, player.y + player.h / 2, myszX, myszY);
        aktualizujKatGracza();
    }

    private void newWave() {
        round++;
        int points = difficulty;

        while (points > 0) {
            // collect the types we can still afford
            List<Integer> available = new ArrayList<>();
            for (int t = 0; t < COST.length; t++) {
                if (COST[t] <= points) available.add(t);
            }

            int type = available.get(random.nextInt(available.size()));
            points -= COST[type];

            // spawn position on a circle around the player
            double playerX = player.x + player.w / 2;
            double playerY = player.y + player.h / 2;
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = SPAWN_RADIUS_MIN + random.nextDouble() * SPAWN_RADIUS_RANGE;

            legion.add(new Legion(type,
                    playerX + Math.cos(angle) * radius,
                    playerY + Math.sin(angle) * radius));
        }

        difficulty += 5;
    }

    private void updateEnemies(double dt) {
        double playerX = player.x + player.w / 2;
        double playerY = player.y + player.h / 2;

        for (Legion l : legion) {
            double ex = l.x + l.w / 2;
            double ey = l.y + l.h / 2;
            double dx = playerX - ex;
            double dy = playerY - ey;
            double distance = Math.hypot(dx, dy);
            if (distance < 0.0001) continue;

            l.angle = Math.atan2(dy, dx);
            l.x += (dx / distance) * l.speed * dt;
            l.y += (dy / distance) * l.speed * dt;
        }

        // separation so enemies don't stack on one point
        for (int i = 0; i < legion.size(); i++) {
            for (int j = i + 1; j < legion.size(); j++) {
                Legion a = legion.get(i);
                Legion b = legion.get(j);
                double ax = a.x + a.w / 2, ay = a.y + a.h / 2;
                double bx = b.x + b.w / 2, by = b.y + b.h / 2;
                double dx = bx - ax, dy = by - ay;
                double distance = Math.hypot(dx, dy);
                double minDistance = (a.w + b.w) / 3;
                if (distance < minDistance && distance > 0.0001) {
                    double push = (minDistance - distance) / 2;
                    a.x -= dx / distance * push;
                    a.y -= dy / distance * push;
                    b.x += dx / distance * push;
                    b.y += dy / distance * push;
                }
            }
        }
    }

    private void updateRounds(double dt) {
        if (legion.isEmpty()) {
            if (breakTimer <= 0) {
                breakTimer = ROUND_BREAK;      // round just ended, start the countdown
            }
            breakTimer -= dt;
            if (breakTimer <= 0) {
                newWave();
            }
        }
    }

    private void fidoUpdate(double dt) {
        double graczX = player.x + player.w / 2;
        double graczy = player.y + player.h / 2;

        double fidoX = fido.x + fido.w / 2;
        double fidoy = fido.y + fido.h / 2;

        double dx = graczX - fidoX;
        double dy = graczy - fidoy;
        double odleglosc = Math.sqrt(dx * dx + dy * dy);
        if (odleglosc < 0.0001) return;
        int dir = 0;

        if (!fido.koliduje(player)) {
            if (fs.isEmpty()) {
                dir = 1;
            } else if (odleglosc < 2000) {
                dir = -1;
            } else {
                dir = 1;
            }
        }else if(doWymiany <= 0 && fs.isFull()){
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

    private void aktualizujKatGracza() {
        double graczX = player.x + player.w / 2;
        double graczY = player.y + player.h / 2;

        double celX = cam.worldX(myszX);
        double celY = cam.worldY(myszY);

        targetAngle = Math.atan2(celY - graczY, celX - graczX);
        double diference = targetAngle  - katGracza;
        diference = Math.atan2(Math.sin(diference), Math.cos(diference));

        double dx = celX - graczX;
        double dy = celY - graczY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 35) katGracza += diference * 0.1;

        aimX = graczX + Math.cos(katGracza) * distance;
        aimY = graczY + Math.sin(katGracza) * distance;
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
            player.x += ruchX * playerSpeed * dt;
            player.y += ruchY * playerSpeed * dt;

            if (dashTimer > 0) {
                double krok = Math.min(dt, dashTimer);
                player.x += dashX * dashPredkosc * krok;
                player.y += dashY * dashPredkosc * krok;
                dashTimer -= krok;
            }

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
        }

        dashCooldown = Math.max(0, dashCooldown - dt);

        if (klawisze.contains(KeyCode.R) && player.koliduje(fido) && doWymiany <= 0 && !fs.isFull()){
            doWymiany = wymiana;
        }

        if (doWymiany > 0) {
            doWymiany -= dt;

            if (doWymiany <= 0) {
                doWymiany = 0;
                fs.refill();
            }
        }

        if(klawisze.contains(KeyCode.BACK_SPACE)){
            newGame();
        }

        fs.shooting(dt, player, katGracza, cam, klawisze, doWymiany > 0);
        fs.hitEnemies(legion);
        legion.removeIf(l -> l.hp <= 0);
        hurtTimer = Math.max(0, hurtTimer - dt);
        if (hurtTimer <= 0) {
            for (Legion l : legion) {
                if (l.koliduje(player)) {
                    playerHp -= CONTACT_DAMAGE[l.type];
                    hurtTimer = HURT_COOLDOWN;
                    break;                      // one hit per cooldown
                }
            }
        }
        if (playerHp <= 0) {
            newGame();
            return;                             // do not process the rest of this frame with the old state
        }

        cam.updateCam(player.x + player.w / 2, player.y + player.h / 2, myszX, myszY, dt);
        aktualizujKatGracza();
        fidoUpdate(dt);
        aktualizujAnimacje(dt, dlugosc);
        updateEnemies(dt);
        updateRounds(dt);
    }

    private void render(GraphicsContext gc) {
        double size = 130;

        double camX = cam.renderX();
        double camY = cam.renderY();

        int T = (int) TILE_SIZE;
        int x0 = (int) Math.floor(camX / T);               // indeks pierwszego widocznego kafelka
        int y0 = (int) Math.floor(camY / T);
        int nx = (int) Math.ceil(WIDTH / T) + 1;       // ile kafelków w poziomie (z zapasem)
        int ny = (int) Math.ceil(HEIGHT / T) + 1;

        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                int tx = x0 + i;
                int ty = y0 + j;

                int h = rng.hash(tx, ty);
                int obrot = (h >>> 8) & 3;
                Image tile = tiles[rng.chooseTile(tx, ty)];

                double sx = tx * T - camX;
                double sy = ty * T - camY;

                gc.save();
                gc.translate(sx + T / 2.0, sy + T / 2.0);
                gc.rotate(obrot * 90);
                gc.drawImage(tile, -T / 2.0, -T / 2.0, T, T);
                gc.restore();
            }
        }

        double cx = player.x - camX + player.w / 2;
        double cy = player.y - camY + player.h / 2;

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
                -player.w / 2, -player.h / 2,
                player.w, player.h
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

        fs.drawBullets(gc, fs.shotHEAT, bullet, camX, camY);
        fs.drawBullets(gc, fs.shotAPFSDS, bullet, camX, camY);

        for (Legion l : legion) {
            double ex = l.x - camX + l.w / 2;
            double ey = l.y - camY + l.h / 2;

            gc.save();
            gc.translate(ex, ey);
            gc.rotate(Math.toDegrees(l.angle));
            gc.setFill(l.type == 0 ? Color.TOMATO : l.type == 1 ? Color.ORANGE : Color.DARKRED);
            gc.fillRect(-l.w / 2, -l.h / 2, l.w, l.h);
            gc.restore();
        }

        gc.setFill(Color.BLACK);
        // if your text color was changed by the fill above
        gc.fillText("Round: " + round, 50, 50);
        gc.fillText(String.format("Czas: %.1f s", czas), 50, 25);
        gc.setFont(Font.font("Courier", 50));
        gc.setFill(Color.RED);
        if (legion.isEmpty() && breakTimer > 0) {
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(String.format("Round %d in %.1f s", round + 1, breakTimer), WIDTH / 2, 150);
            gc.setTextAlign(TextAlignment.LEFT);
        }

        gc.setFill(Color.TOMATO);
        gc.setFont(Font.font("Arial", 20));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Arial", 18));

        gc.fillText("HEAT", myszX - 65, myszY - 70);
        gc.fillText(String.format("%d/%d" , fs.getHeat(), FS.MAG), myszX - 50, myszY - 50);
        gc.fillText("reload", myszX - 65, myszY + 80);
        if (fs.isEmpty()){
            gc.fillText("empty", myszX - 50, myszY + 60);
        }else if(fs.getUntilFire() == 0) {
            gc.fillText("ready", myszX - 50, myszY + 60);
        } else {
            gc.fillText(String.format("%.1f s", fs.getUntilFire()), myszX - 50, myszY + 60);
        }
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("APFSDS", myszX + 65, myszY - 70);
        gc.fillText(String.format("%d/%d" , fs.getApfsds(), FS.MAG), myszX + 50, myszY - 50);
        gc.fillText("swap", myszX + 65, myszY + 80);
        if (doWymiany == 0){
            gc.fillText("ready", myszX + 50, myszY + 60);
        }else {
            gc.fillText(String.format("%.1f s", doWymiany), myszX + 50, myszY + 60);
        }
        gc.setTextAlign(TextAlignment.CENTER);
        if(fs.getChosen() == 0){
            gc.fillText("<      ", myszX, myszY - 50);
        }else {
            gc.fillText("      >", myszX, myszY - 50);
        }
        if (player.koliduje(fido) && doWymiany == 0 && fs.isEmpty()){
            gc.fillText("'R' to swap mag", myszX, myszY + 110);
        }


        gc.setFill(Color.DARKGRAY);
        gc.fillRect(myszX + 75, myszY - 75, 20, 150);
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(myszX + 75, myszY - 75, 20, 150  * Math.max(0, playerHp) / PLAYER_MAX_HP);

        gc.drawImage(
                crosshair,
                myszX - size / 2,
                myszY - size / 2,
                size,
                size
        );
        gc.drawImage(
                aim,
                aimX - camX - 13,
                aimY - camY - 14,
                25,
                25
        );
    }
    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Scene scene = new Scene(new StackPane(canvas));

        scene.setCursor(Cursor.NONE);
        scene.setOnMouseMoved(e -> { myszX = e.getX(); myszY = e.getY(); });
        scene.setOnMouseDragged(e -> { myszX = e.getX(); myszY = e.getY(); });

        scene.setOnMousePressed(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                fs.mouseShoots = true;
            }
        });

        scene.setOnMouseReleased(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                fs.mouseShoots = false;
            }
        });

        scene.setOnKeyPressed(e -> klawisze.add(e.getCode()));
        scene.setOnKeyReleased(e -> klawisze.remove(e.getCode()));

        newGame();

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
