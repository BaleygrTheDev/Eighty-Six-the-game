import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;

import java.lang.annotation.Target;
import java.util.*;

public class FS {
    private final Random random = new Random();
    public static final int MAG = 7;                 // pojemność magazynka każdej broni

    static final int HEAT = 0, APFSDS = 1, ENEMY = 2;

    private static final double FIRE_DELAY = 1;   // przerwa między strzałami
    private static final double[] BULLET_SPEED = {1200, 2000};
    private static final double[] BULLET_DMG   = {0, 45};  // indeks = typ pocisku
    private static final double BULLET_W = 10, BULLET_H = 5;
    private static final double MUZZLE = 45;
    private static final double SPEED_FALLOFF = 1000;          // utrata obrażeń na SEKUNDĘ lotu
    private static final double MIN_SPEED = 500;               // poniżej tego obrażenia nie spadają
    private static final double CULL_MARGIN = 100;         // dalej od ekranu niż tyle pocisk jest usuwany
    static final double[] CONCUSSION_TIME = {0.1, 0.8, 0.3};
    static final double BLAST_RADIUS = 200;     // zasięg obrażeń od wybuchu (używa go też rysowanie fali)
    private static final double BLAST_DAMAGE = 15;

    static class Bullets extends Object{
        double dmg;
        double prevX, prevY;
        final int type;
        private double cos, sin;
        private Set<Object> hitTargets;

        Bullets(double x, double y, double w, double h,
                double angle, double speed, double dmg, int type) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.speed = speed;
            this.dmg = dmg;
            this.prevX = x;
            this.prevY = y;
            this.type = type;
            setAngle(angle);
        }

        double getAngle() {return angle;}

        void setAngle(double angle){
            this.angle = angle;
            this.cos = Math.cos(angle);
            this.sin = Math.sin(angle);
        }

        void move(double dt){
            prevX = x;
            prevY = y;
            x += cos * speed * dt;
            y += sin * speed * dt;
        }

        boolean hits(Feldres t) {
            double left   = Math.min(prevX, x);
            double right  = Math.max(prevX, x) + w;
            double top    = Math.min(prevY, y);
            double bottom = Math.max(prevY, y) + h;
            return left < t.x + t.w && right > t.x && top < t.y + t.h && bottom > t.y;
        }

        boolean alreadyHit(Object target){
            return hitTargets != null && hitTargets.contains(target);
        }

        void markHit(Object target){
            if (hitTargets == null) hitTargets = new HashSet<>();
            hitTargets.add(target);
        }
    }

    public final List<Bullets> shot = new ArrayList<>();

    public boolean shootsHeat = false;      // left mouse button held
    public boolean shootsApfsds = false;

    // stan należy do FS i tylko tutaj się zmienia
    private double untilFire = 0;
    private int heat = MAG;
    private int apfsds = MAG;


    // ---------- odczyt dla HUD-u i Fido ----------
    public int getHeat() {
        return heat;
    }

    public int getApfsds() {
        return apfsds;
    }

    public double getUntilFire() {
        return untilFire;
    }

    public boolean isEmpty() {
        return heat == 0 && apfsds == 0;
    }

    public boolean isFull() {
        return heat == MAG && apfsds == MAG;
    }

    public void refill() {
        heat = MAG;
        apfsds = MAG;
    }

    public void clearB() {
        shot.clear();
        untilFire = 0;
        refill();
    }

    /**
     * Wołaj raz na klatkę, PO aktualizacji kamery i kąta gracza.
     *
     * @param swapping czy trwa wymiana magazynków (wtedy nie wolno strzelać)
     */

    public void shooting(double dt, Feldres player, double angle, Cam cam, boolean swapping) {
        untilFire = Math.max(0, untilFire - dt);

        if (untilFire <= 0 && !swapping) {
            // Tworzenie pocisków podczas trzymania spacji
            if (shootsHeat && heat > 0) {
                fire(player, angle, HEAT);
                heat -= 1;
            } else if (shootsApfsds && apfsds > 0) {
                fire(player, angle, APFSDS);
                apfsds -= 1;
            }
        }
        moveBullets(dt, cam);
    }

    public void fire(Feldres player, double angle, int type){
        double cx = player.x + player.w / 2;
        double cy = player.y + player.h / 2;

        shot.add(new Bullets(
                cx + Math.cos(angle) * MUZZLE - BULLET_W / 2,    // x,y to lewy górny róg
                cy + Math.sin(angle) * MUZZLE - BULLET_H / 2,
                BULLET_W, BULLET_H,
                angle, BULLET_SPEED[type], BULLET_DMG[type], type
        ));
        untilFire = FIRE_DELAY;
    }

    public void moveBullets(double dt, Cam cam) {
        shot.removeIf(b -> {
            b.move(dt);
            if (b.type == APFSDS){
                b.speed = Math.max(MIN_SPEED, b.speed - SPEED_FALLOFF * dt);
                b.dmg = BULLET_DMG[1] * b.speed / BULLET_SPEED[1];
            }
            return cam.outside(b.x, b.y, CULL_MARGIN);
        });
    }

    public void drawBullets(GraphicsContext gc, List<? extends Bullets> lista, Image obraz,
                            double camX, double camY) {
        for (Bullets b : lista) {
            gc.save();
            gc.translate(b.x - camX + b.w / 2, b.y - camY + b.h / 2);
            gc.rotate(Math.toDegrees(b.getAngle()));
            gc.drawImage(obraz, -b.w / 2, -b.h / 2, b.w, b.h);
            gc.restore();
        }
    }

    public void hitEnemies(List<Legions.Legion> legion) {
        shot.removeIf(b -> resolveHit(b, legion));
    }

    public void hitCarcas(double dt, Obstacles obstacle) { shot.removeIf(b -> obstacle.colision(dt, b, false, BULLET_SPEED[b.type])); }

    /** @return true, jeśli pocisk został zużyty i trzeba go usunąć. */

    private boolean resolveHit(Bullets b, List<Legions.Legion> legion) {
        for (Legions.Legion l : legion) {
            if (l.hp <= 0 || b.alreadyHit(l) || !b.hits(l)) continue;

            switch (l.type) {
                case Legions.LOWE:
                    if (b.type == APFSDS) damage(l, b.dmg, b.type);
                    return true;
                case Legions.GRAUWOLF:
                    if (b.type == HEAT) {
                        if(random.nextInt(3) == 0) {
                            kill(l, HEAT);
                            explosion(l, legion);
                        }
                    } else {
                        damage(l, b.dmg, b.type);
                    }
                    return true;
                default:
                    if (b.type == HEAT) {
                        kill(l, HEAT);
                        explosion(l, legion);
                        return true;
                    }
                    damage(l, b.dmg / 4, b.type);
                    b.markHit(l);
                    return false;
            }
        }
        return false;
    }

    private void explosion(Legions.Legion o, List<Legions.Legion> legion){
        double cx = o.x;
        double cy = o.y;

        for(Legions.Legion l : legion){
            double targetX = l.x + l.w / 2;
            double targetY = l.y + l.h / 2;
            double dx = targetX - cx;
            double dy = targetY - cy;
            double distance = Math.hypot(dx, dy);

            if (distance < BLAST_RADIUS){
                damage(l, BLAST_DAMAGE, l.type);
            }
        }
    }

    private void damage(Legions.Legion l, double amount, int killer){
        l.hp -= amount;
        l.concussionTimer = CONCUSSION_TIME[l.type];
        if (l.hp <= 0 && l.killedBy == ENEMY) l.killedBy = killer;
    }

    private void kill(Legions.Legion l, int killer){
        l.hp = 0;
        if (l.killedBy == ENEMY) l.killedBy = killer;
    }
}