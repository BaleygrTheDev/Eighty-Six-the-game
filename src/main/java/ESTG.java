import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
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

    private final Set<KeyCode> klawisze = new HashSet<>();
    private final Random random = new Random();
    private final Cam cam = new Cam(WIDTH, HEIGHT);
    private final Radar radar = new Radar(WIDTH, HEIGHT);
    private RNG rng = new RNG();
    private final Legions legion = new Legions();
    private FS fs = new FS();

    private Feldres player;
    private Feldres fido;
    private double czas;
    private double myszX = WIDTH / 2;
    private double myszY = HEIGHT / 2;

    private double dashCzas = 0.10;
    private double dashPredkosc = 1500;
    private double dashTimer = 0;
    private double dashCooldown = 0;
    private double dashX = 0;
    private double dashY = 0;
    private  final double wymiana = 5;
    private double doWymiany = 0;
    static final double dashWait = 0.6;;
    private double targetAngle;
    private double aimX;
    private double aimY;
    static final double PLAYER_MAX_HP = 100;
    static final double HURT_COOLDOWN = 0.5;
    private double hurtTimer = 0;

    private final Image crosshair = load("/jugger crosshair.png");
    private final Image aim = load("/jugger aim.png");
    private final Image teksturaGracza = load("/jugger walker.png");
    private final Image teksturaFido = load("/fido walk.png");
    private final double czasKlatki = 0.12;
    private final Image bullet = load("/HEAT bulet.png");
    private final Image[] tiles = {
            load("/tile1.png"),
            load("/tile4.png"),
            load("/tile3.png"),
            load("/tile2.png")
    };
    private final Image[] legionTexture = {
            load( "/ameise walk.png"),
            load( "/grauwolf walk.png"),
            load( "/lowe walk.png")
    };

    private Image load(String file){
        return(new Image(Objects.requireNonNull(getClass().getResourceAsStream(file))));
    }

    private void newGame() {
        player = new Feldres(WIDTH / 2 - 25, HEIGHT / 2 - 50, 121, 75, PLAYER_MAX_HP, 0, 600, 120, 0, 0);
        fido = new Feldres(WIDTH / 2 - 25, HEIGHT / 2 - 300, 60, 50, 0, 0, 550, 120, 0, 0);
        fs.clearB();
        legion.reset();
        player.hp = PLAYER_MAX_HP;
        hurtTimer = 0;
        czas = 0;
        doWymiany = 0;
        dashCooldown = 0;
        dashTimer = 0;
        player.frame = 0;
        fido.frame = 0;
        aktualizujKatGracza(1);
        rng.newSeed();
        cam.drag(player.x + player.w / 2, player.y + player.h / 2, myszX, myszY);
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
            } else if (odleglosc < 3000) {
                dir = -1;
            } else {
                dir = 1;
            }
        }else if(doWymiany <= 0 && fs.isFull()){
            dir = -1;
        }
        if (dir != 0) {
            fido.x += (dx / odleglosc) * dir * fido.speed * dt;
            fido.y += (dy / odleglosc) * dir * fido.speed * dt;
            fido.angle = Math.atan2(dir * (dy / odleglosc), dir * (dx / odleglosc));

           fido.animTimer += dt;
            if (fido.animTimer >= czasKlatki) {
                fido.frame = (fido.frame + 1) % 4;
                fido.animTimer = 0;
            }
        }else {
            fido.frame = 0;           // stoi, więc klatka stojąca, bez animacji chodzenia
            fido.animTimer = 0;
        }
    }

    private void aktualizujAnimacje(double dt, double dlugosc) {
        boolean chodzi = (dlugosc > 0 && doWymiany <= 0);

        if (chodzi) {
            player.animTimer += dt;

            if (player.animTimer >= czasKlatki) {
                player.frame = (player.frame + 1) % 4;
                player.animTimer = 0;
            }
        } else {
            player.frame = 1;
            player.animTimer = 0;
        }
    }

    private void aktualizujKatGracza(double dt) {
        double graczX = player.x + player.w / 2;
        double graczY = player.y + player.h / 2;

        double celX = cam.worldX(myszX);
        double celY = cam.worldY(myszY);

        targetAngle = Math.atan2(celY - graczY, celX - graczX);
        double diference = targetAngle  - player.angle;
        diference = Math.atan2(Math.sin(diference), Math.cos(diference));

        double dx = celX - graczX;
        double dy = celY - graczY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance * cam.getZoom() > 35){
            double maxStep = Math.toRadians(player.turnRate) * dt;
            player.angle += Math.max(-maxStep, Math.min(maxStep, diference));
        }

        aimX = graczX + Math.cos(player.angle) * distance;
        aimY = graczY + Math.sin(player.angle) * distance;
    }

    private void update(double dt) {
        czas += dt;

        aktualizujKatGracza(dt);

        // ruch gracza
        double kierunekX = 0;
        double kierunekY = 0;

        if (klawisze.contains(KeyCode.LEFT) || klawisze.contains(KeyCode.A)) kierunekX -= 1;
        if (klawisze.contains(KeyCode.RIGHT) || klawisze.contains(KeyCode.D)) kierunekX += 1;
        if (klawisze.contains(KeyCode.UP) || klawisze.contains(KeyCode.W)) kierunekY += 1;
        if (klawisze.contains(KeyCode.DOWN) || klawisze.contains(KeyCode.S)) kierunekY -= 1;

        // Ruch względem obrotu gracza
        double ruchX = Math.cos(player.angle) * kierunekY - Math.sin(player.angle) * kierunekX;
        double ruchY = Math.sin(player.angle) * kierunekY + Math.cos(player.angle) * kierunekX;

        // Normalizacja ruchu po przekątnej
        double dlugosc = Math.sqrt(ruchX * ruchX + ruchY * ruchY);
        if (dlugosc > 1) {
            ruchX /= dlugosc;
            ruchY /= dlugosc;
        }
        if (doWymiany <= 0) {
            player.x += ruchX * player.speed * dt;
            player.y += ruchY * player.speed * dt;

            if (dashTimer > 0) {
                double krok = Math.min(dt, dashTimer);
                player.x += ruchX * dashPredkosc * krok;
                player.y += ruchY * dashPredkosc * krok;
                dashTimer -= krok;
            }

            boolean dash = klawisze.contains(KeyCode.SPACE);

            if (dashCooldown == 0 && dash) {
                dashTimer = dashCzas;
                dashCooldown = dashWait;
            }
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

        fs.shooting(dt, player, player.angle, cam, klawisze, doWymiany > 0);
        fs.hitEnemies(legion.getEnemies());
        legion.removeDead();

        hurtTimer = Math.max(0, hurtTimer - dt);
        if (hurtTimer <= 0) {
            double damage = legion.contactDamage(player) ;
            if (damage > 0) {
                player.hp -= damage;
                hurtTimer = HURT_COOLDOWN;
            }
        }
        player.hp -= legion.shotsDamage(player);
        player.hp -= legion.rocketsDamage(player);
        if (player.hp <= 0) {
            newGame();
            return;
        }

        cam.updateCam(player.x + player.w / 2, player.y + player.h / 2, myszX, myszY, dt);
        aktualizujKatGracza(dt);
        fidoUpdate(dt);
        aktualizujAnimacje(dt, dlugosc);
        legion.update(dt, player, cam);
    }

    private void render(GraphicsContext gc) {
        double size = 130;
        double zoom = cam.getZoom();

        double camX = cam.renderX();
        double camY = cam.renderY();

        // =====================================================================
        //  ŚWIAT  (jednostki świata, skalowane przez zoom)
        // =====================================================================
        gc.save();
        gc.scale(zoom, zoom);

        // ---------- kafelki tła ----------
        int T = (int) TILE_SIZE;
        int x0 = (int) Math.floor(camX / T);                    // indeks pierwszego widocznego kafelka
        int y0 = (int) Math.floor(camY / T);
        int nx = (int) Math.ceil(WIDTH / zoom / T) + 1;         // widoczny obszar świata to WIDTH / zoom
        int ny = (int) Math.ceil(HEIGHT / zoom / T) + 1;

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
                // +1 i -0.5: lekka zakładka, żeby przy skalowaniu nie było szwów między kafelkami
                gc.drawImage(tile, -T / 2.0 - 0.5, -T / 2.0 - 0.5, T + 1, T + 1);
                gc.restore();
            }
        }

        // ---------- gracz ----------
        double cx = player.x - camX + player.w / 2;
        double cy = player.y - camY + player.h / 2;
        double kat = Math.toDegrees(player.angle);

        double szerokoscKlatki = teksturaGracza.getWidth() / 4;
        double wysokoscKlatki = teksturaGracza.getHeight();

        gc.save();
        gc.translate(cx, cy);
        gc.rotate(kat);
        gc.drawImage(
                teksturaGracza,
                player.frame * szerokoscKlatki, 0,
                szerokoscKlatki, wysokoscKlatki,
                -player.w / 2, -player.h / 2,
                player.w, player.h
        );
        gc.restore();

        // ---------- Fido ----------
        double dx = fido.x - camX + fido.w / 2;
        double dy = fido.y - camY + fido.h / 2;

        double szerokoscFido = teksturaFido.getWidth() / 4;
        double wysokoscFido = teksturaFido.getHeight();

        gc.save();
        gc.translate(dx, dy);
        gc.rotate(Math.toDegrees(fido.angle));
        gc.drawImage(
                teksturaFido,
                fido.frame * szerokoscFido, 0,
                szerokoscFido, wysokoscFido,
                -fido.w / 2, -fido.h / 2,
                fido.w, fido.h
        );
        gc.restore();

        // ---------- pociski gracza ----------
        fs.drawBullets(gc, fs.shotHEAT, bullet, camX, camY);
        fs.drawBullets(gc, fs.shotAPFSDS, bullet, camX, camY);

        // ---------- wrogowie ----------
        for (Legions.Legion l : legion.getEnemies()) {
            double ex = l.x - camX + l.w / 2;
            double ey = l.y - camY + l.h / 2;

            gc.save();
            gc.translate(ex, ey);
            gc.rotate(Math.toDegrees(l.angle));
            Image tex = legionTexture[l.type];
            double frameW = tex.getWidth() / Legions.FRAMES;
            double frameH = tex.getHeight();
            gc.drawImage(
                    tex,
                    l.frame * frameW, 0,        // wycinek ze spritesheetu
                    frameW, frameH,
                    -l.w / 2, -l.h / 2,         // miejsce docelowe, wyśrodkowane
                    l.w, l.h
            );
            gc.restore();
        }

        // ---------- pociski i rakiety wrogów (nad wrogami, żeby były widoczne przy lufie) ----------
        fs.drawBullets(gc, legion.shots, bullet, camX, camY);
        fs.drawRockets(gc, legion.rockets, bullet, camX, camY);

        gc.restore();   // koniec skali; od tej chwili rysujemy w pikselach EKRANU

        // =====================================================================
        //  HUD  (piksele ekranu, bez zoomu)
        // =====================================================================

        // ---------- licznik rundy i czasu ----------
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 18));
        gc.setTextAlign(TextAlignment.LEFT);        // jawnie, bo poprzednia klatka zostawiała CENTER
        gc.fillText("Round: " + legion.getRound(), 50, 50);
        gc.fillText(String.format("Czas: %.1f s", czas), 50, 25);

        // ---------- odliczanie do następnej rundy ----------
        if (legion.isEmpty() && legion.getBreakTimer() > 0) {
            gc.setFill(Color.RED);
            gc.setFont(Font.font("Courier", 50));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(String.format("Round %d in %.1f s",
                    legion.getRound() + 1, legion.getBreakTimer()), WIDTH / 2, 150);
        }

        // ---------- napisy przy kursorze ----------
        gc.setFill(Color.TOMATO);
        gc.setFont(Font.font("Arial", 18));

        // lewa strona: HEAT
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("HEAT", myszX - 65, myszY - 70);
        gc.fillText(String.format("%d/%d", fs.getHeat(), FS.MAG), myszX - 50, myszY - 50);
        gc.fillText("reload", myszX - 65, myszY + 80);
        if (fs.isEmpty()) {
            gc.fillText("empty", myszX - 50, myszY + 60);
        } else if (fs.getUntilFire() == 0) {
            gc.fillText("ready", myszX - 50, myszY + 60);
        } else {
            gc.fillText(String.format("%.1f s", fs.getUntilFire()), myszX - 50, myszY + 60);
        }

        // prawa strona: APFSDS
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("APFSDS", myszX + 65, myszY - 70);
        gc.fillText(String.format("%d/%d", fs.getApfsds(), FS.MAG), myszX + 50, myszY - 50);
        gc.fillText("swap", myszX + 65, myszY + 80);
        if (doWymiany == 0) {
            gc.fillText("ready", myszX + 50, myszY + 60);
        } else {
            gc.fillText(String.format("%.1f s", doWymiany), myszX + 50, myszY + 60);
        }

        // podpowiedź przy Fido
        gc.setTextAlign(TextAlignment.CENTER);
        if (player.koliduje(fido) && doWymiany == 0 && fs.isEmpty()) {
            gc.fillText("'R' to swap mag", myszX, myszY + 110);
        }

        // ---------- pasek HP ----------
        gc.setFill(Color.DARKGRAY);
        gc.fillRect(myszX + 75, myszY - 75, 20, 150);
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(myszX + 75, myszY - 75, 20, 150 * Math.max(0, player.hp) / PLAYER_MAX_HP);

        radar.render(gc, player, fido, legion, cam);

        // ---------- celownik (ekran) i punkt celowania (świat -> ekran) ----------
        gc.drawImage(
                crosshair,
                myszX - size / 2,
                myszY - size / 2,
                size,
                size
        );
        gc.drawImage(
                aim,
                (aimX - camX) * zoom - 13,
                (aimY - camY) * zoom - 14,
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
        scene.setOnScroll(e -> cam.zoomBy(e.getDeltaY()));

        scene.setOnMousePressed(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                fs.shootsHeat = true;
            }
            if (e.getButton() == MouseButton.SECONDARY) {
                fs.shootsApfsds = true;
            }
        });

        scene.setOnMouseReleased(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                fs.shootsHeat = false;
            }
            if (e.getButton() == MouseButton.SECONDARY) {
                fs.shootsApfsds = false;
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
