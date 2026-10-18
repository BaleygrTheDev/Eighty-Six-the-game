import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FS {
    public static final int MAG = 7;                 // pojemność magazynka każdej broni
    private static final double FIRE_DELAY = 1;   // przerwa między strzałami
    private static final double BULLET_SPEED = 1200;
    private static final double MUZZLE = 45;

    static class Bullets {
        double x, y, w, h;
        double vx, vy;
        double dmg;
        double prevX, prevY;

        Bullets(double x, double y, double w, double h,
                double vx, double vy, double dmg) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.vx = vx;
            this.vy = vy;
            this.dmg = dmg;
            this.prevX = x;
            this.prevY = y;
        }
    }

    public final List<Bullets> shotHEAT = new ArrayList<>();
    public final List<Bullets> shotAPFSDS = new ArrayList<>();

    public boolean shootsHeat = false;      // left mouse button held
    public boolean shootsApfsds = false;

    // stan należy do FS i tylko tutaj się zmienia
    private double untilFire = 0;
    private int heat = MAG;
    private int apfsds = MAG;


    // ---------- odczyt dla HUD-u i Fido ----------
    public int getHeat()          { return heat; }
    public int getApfsds()        { return apfsds; }
    public double getUntilFire()  { return untilFire; }
    public boolean isEmpty()      { return heat == 0 && apfsds == 0; }
    public boolean isFull()       { return heat == MAG && apfsds == MAG; };

    public void refill() {
        heat = MAG;
        apfsds = MAG;
    }

    public void clearB(){
        shotHEAT.clear();
        shotAPFSDS.clear();
        untilFire = 0;
        refill();
    }

    /**
     * Wołaj raz na klatkę, PO aktualizacji kamery i kąta gracza.
     * @param swapping czy trwa wymiana magazynków (wtedy nie wolno strzelać)
     */

    public void shooting(double dt, Feldres player, double angle,
                         Cam cam, Set<KeyCode> keys, boolean swapping) {
        untilFire = Math.max(0, untilFire - dt);

        if (untilFire <= 0 && !swapping) {
            double playerX = player.x + player.w / 2;
            double playerY = player.y + player.h / 2;

            // Tworzenie pocisków podczas trzymania spacji
            if (shootsHeat && heat > 0) {
                shotHEAT.add(newBullet(playerX, playerY, angle, 30));

                untilFire = FIRE_DELAY;
                heat -= 1;
            }else if (shootsApfsds && apfsds > 0){

                shotAPFSDS.add(newBullet(playerX, playerY, angle, 15));

                untilFire = FIRE_DELAY;
                apfsds -= 1;
            }
        }
        moveBullet(shotHEAT, dt, cam);
        moveBullet(shotAPFSDS, dt, cam);
    }

    public void moveBullet(List<Bullets> list, double dt, Cam cam){
        for (int i = list.size() - 1; i >= 0; i--) {
            Bullets b = list.get(i);

            b.prevX = b.x;
            b.prevY = b.y;
            b.x += b.vx * dt;
            b.y += b.vy * dt;

            // Usuwanie pocisków daleko poza widocznym ekranem
            if (cam.outside(b.x, b.y, 200)) {
                list.remove(i);
            }
        }
    }

    public Bullets newBullet(double playerX, double playerY, double angle, double dmg){
        double dirX = Math.cos(angle);
        double dirY = Math.sin(angle);

        return new Bullets(
                playerX + dirX * MUZZLE - 5,
                playerY + dirY * MUZZLE - 2.5,
                10,
                5,
                dirX * BULLET_SPEED,
                dirY* BULLET_SPEED,
                dmg
        );
    }
    public void drawBullets(GraphicsContext gc, List<Bullets> lista, Image obraz,
                              double camX, double camY) {
        for (Bullets b : lista) {
            double katPocisku = Math.toDegrees(Math.atan2(b.vy, b.vx));

            gc.save();
            gc.translate(b.x - camX + b.w / 2, b.y - camY + b.h / 2);
            gc.rotate(katPocisku);
            gc.drawImage(obraz, -b.w / 2, -b.h / 2, b.w, b.h);
            gc.restore();
        }
    }

    public void drawRockets(GraphicsContext gc, List<Legions.Rockets> lista, Image obraz,
                            double camX, double camY) {
        for (Legions.Rockets b : lista) {
            double katPocisku = Math.toDegrees(Math.atan2(b.vy, b.vx));

            gc.save();
            gc.translate(b.x - camX + b.w / 2, b.y - camY + b.h / 2);
            gc.rotate(katPocisku);
            gc.drawImage(obraz, -b.w / 2, -b.h / 2, b.w, b.h);
            gc.restore();
        }
    }

    public void hitEnemies(List<Legions.Legion> legion) {
        hitList(shotHEAT, legion);
        hitList(shotAPFSDS, legion);
    }

    private void hitList(List<Bullets> list, List<Legions.Legion> legion) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Bullets b = list.get(i);

            // rectangle covering the whole path travelled this frame
            double left   = Math.min(b.prevX, b.x);
            double right  = Math.max(b.prevX, b.x) + b.w;
            double top    = Math.min(b.prevY, b.y);
            double bottom = Math.max(b.prevY, b.y) + b.h;

            for (Legions.Legion l : legion) {
                if (l.hp <= 0) continue;

                boolean hit = left < l.x + l.w && right > l.x
                        && top < l.y + l.h && bottom > l.y;

                if (hit) {
                    l.hp -= b.dmg;
                    list.remove(i);
                    break;                // this bullet is used up, go to the next one
                }
            }
        }
    }
}
