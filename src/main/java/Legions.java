import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Legions {
    static final int[]    COST      = {1, 3, 5};
    static final double[] HP        = {15, 30, 45};
    static final double[] SPEED     = {280, 220, 150};
    static final double[] SIZE_W    = {67, 70, 242};
    static final double[] SIZE_H    = {50, 75, 150};
    static final double[] TURN_RATE = {200, 120, 70};   // degrees per second
    static final double[] CONTACT_DAMAGE = {0, 12, 0};
    static final double[] SHOT_DAMAGE = {2,   30,    50};    // obrażenia jednego pocisku
    static final double[] FIRE_DELAY  = {0.3, 0,  2.0};   // sekundy między strzałami
    static final double[] SHOT_SPEED  = {650, 700,  500};   // px/s
    static final double[] SHOT_RANGE  = {800, 2000,  1300};  // max dystans do gracza, z którego strzela
    static final double[] SHOT_W      = {12,  16,   26};    // rozmiar pocisku (hitbox + sprite)
    static final double[] SHOT_H      = {5,   6,    10};
    static final double[] SHOT_SPREAD = {
            Math.toRadians(12),    // ameise: bardzo niecelne
            Math.toRadians(25),     // grauwolf: średnio
            Math.toRadians(0.5)    // lowe: prawie idealnie
    };
    static final double   AIM_TOLERANCE = Math.toRadians(8);// max błąd kąta, przy którym wolno strzelić
    static final double HURT_COOLDOWN = 1;
    static  final double ROCKET_LIFE = 3.5;
    static final int[] AMMO = {Integer.MAX_VALUE, 6, Integer.MAX_VALUE};
    double hurtTimer = 0;

    static final double ROUND_BREAK        = 3.0;       // seconds between rounds
    static final double SPAWN_RADIUS_MIN   = 1000;
    static final double SPAWN_RADIUS_RANGE = 300;
    static final double SPAWN_MARGIN       = 100;       // how far outside the screen enemies appear
    static final int    SPAWN_ATTEMPTS     = 30;

    static class Legion extends Feldres {
        int type;          // 0 = amaise, 1 = grauwolf, 2 = lowe
        double fireTimer;
        int ammo;

        Legion(int type, double x, double y) {
            super(x, y, SIZE_W[type], SIZE_H[type], HP[type],0, SPEED[type], TURN_RATE[type], 0, 0);
            this.type = type;
            this.fireTimer = FIRE_DELAY[type] * (0.3 + Math.random() * 0.7);
            this.ammo = AMMO[type];
        }
    }

    static class Rockets extends FS.Bullets {
        double life;
        double delay;

        Rockets(double x, double y, double w, double h,
                double vx, double vy, double dmg, double life, double delay) {
            super(x, y, w, h, vx, vy, dmg);
            this.life = life;
            this.delay = delay;
        }
    }

    private final List<Legion> legion = new ArrayList<>();
    private final Random random = new Random();
    public final List<FS.Bullets> shots = new ArrayList<>();
    public final List<Rockets> rockets = new ArrayList<>();

    private int round = 0;
    private int difficulty = 5;          // point pool for the next round
    private double breakTimer = 0;

    static final int    FRAMES        = 4;      // liczba klatek w spritesheecie
    static final double BASE_FRAME_TIME = 0.12; // czas klatki dla prędkości odniesienia
    static final double REF_SPEED     = 260;

    public List<Legion> getEnemies() { return legion; }
    public int getRound()                 { return round; }
    public double getBreakTimer()         { return breakTimer; }
    public boolean isEmpty()              { return legion.isEmpty(); }

    public void reset(){
        legion.clear();
        shots.clear();
        rockets.clear();
        round = 0;
        difficulty = 5;
        breakTimer = 0;
    }

    public void update(double dt, Feldres player, Cam cam) {
        updateEnemies(dt, player, cam);
        updateShots(dt, cam);
        updateRockets(dt, cam, player);
        legionDamage(player, dt);
        updateRounds(dt, player, cam);
    }

    public void updateRounds(double dt, Feldres player, Cam cam) {
        if (legion.isEmpty()) {
            if (breakTimer <= 0) {
                breakTimer = ROUND_BREAK;      // round just ended, start the countdown
            }
            breakTimer -= dt;
            if (breakTimer <= 0) {
                newWave(player, cam);
            }
        }
    }

    public void newWave(Feldres player, Cam cam) {
        round++;
        int points = difficulty;

        double playerX = player.x + player.w / 2;
        double playerY = player.y + player.h / 2;

        while (points > 0) {
            // collect the types we can still afford
            List<Integer> available = new ArrayList<>();
            for (int t = 0; t < COST.length; t++) {
                if (COST[t] <= points) available.add(t);
            }

            int type = available.get(random.nextInt(available.size()));
            points -= COST[type];

            spawn(type, playerX, playerY, cam);
        }

        difficulty += 5;
    }

    private void spawn(int type, double playerX, double playerY, Cam cam) {
        double sx = 0, sy = 0;
        double ringScale = 1.0 / Math.min(cam.getZoom(), 1.0);   // oddalenie -> większy pierścień

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = (SPAWN_RADIUS_MIN + random.nextDouble() * SPAWN_RADIUS_RANGE) * ringScale;
            sx = playerX + Math.cos(a) * r;
            sy = playerY + Math.sin(a) * r;
            if (cam.outside(sx, sy, SPAWN_MARGIN)) break;
        }

        Legion e = new Legion(type, sx, sy);
        e.x -= e.w / 2;                        // (sx, sy) was meant as the centre
        e.y -= e.h / 2;
        e.angle = Math.atan2(playerY - sy, playerX - sx);   // start already facing the player
        e.frame = random.nextInt(FRAMES);
        e.animTimer = random.nextDouble() * BASE_FRAME_TIME;
        legion.add(e);
    }

    public void updateEnemies(double dt, Feldres player, Cam cam) {
        double playerX = player.x + player.w / 2;
        double playerY = player.y + player.h / 2;

        for (Legion l : legion) {
            double ex = l.x + l.w / 2;
            double ey = l.y + l.h / 2;
            double dx = playerX - ex;
            double dy = playerY - ey;
            double distance = Math.hypot(dx, dy);
            if (distance < 0.0001) continue;



            double targetAngle = Math.atan2(dy, dx);

            double difference = targetAngle - l.angle;
            difference = Math.atan2(Math.sin(difference), Math.cos(difference));

            double maxStep = Math.toRadians(TURN_RATE[l.type]) * dt;
            double step = Math.max(-maxStep, Math.min(maxStep, difference));
            l.angle += step;

            if (l.type == 2 && distance < 800){
                l.x += (Math.cos(l.angle)) * 1 * dt;
                l.y += (Math.sin(l.angle)) * 1 * dt;
            }else {
                l.x += (Math.cos(l.angle)) * l.speed * dt;
                l.y += (Math.sin(l.angle)) * l.speed * dt;
            }

            l.animTimer += dt;
            double frameTime = BASE_FRAME_TIME * REF_SPEED / l.speed;
            if (l.animTimer >= frameTime) {
                l.frame = (l.frame + 1) % FRAMES;
                l.animTimer -= frameTime;      // -= zamiast = 0, żeby nie gubić nadwyżki czasu
            }

            // ---------- strzelanie ----------
            l.fireTimer = Math.max(0, l.fireTimer - dt);

            // ile jeszcze brakuje do idealnego namierzenia (po tym, jak już się obrócił w tej klatce)
            double remainingError = Math.abs(difference - step);

            double cx = l.x + l.w / 2;
            double cy = l.y + l.h / 2;
            boolean inRange  = distance <= SHOT_RANGE[l.type];
            boolean onScreen = !cam.outside(cx, cy, 0);
            boolean aimed    = remainingError < AIM_TOLERANCE;
            boolean hasAmmo  = l.ammo > 0;

            if (l.fireTimer <= 0 && inRange && onScreen && aimed && hasAmmo) {
                shoot(l);
                l.ammo--;
                l.fireTimer = FIRE_DELAY[l.type];
            }
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
    private void shoot(Legion l) {
        double cx = l.x + l.w / 2;
        double cy = l.y + l.h / 2;

        // lufa zawsze na przodzie sprite'a, czyli zgodnie z obrotem wroga
        double muzzleX = cx + Math.cos(l.angle) * (l.w / 2);
        double muzzleY = cy + Math.sin(l.angle) * (l.w / 2);

        // losowy błąd: liczba z przedziału (-SPREAD, +SPREAD)
        double error = (random.nextDouble() * 2 - 1) * SHOT_SPREAD[l.type];
        double shotAngle = l.angle + error;

        double dirX = Math.cos(shotAngle);
        double dirY = Math.sin(shotAngle);

        double w = SHOT_W[l.type];
        double h = SHOT_H[l.type];        // lufa na przodzie sprite'a (sprite jest zwrócony w prawo)

        if (l.type == 0){
            shots.add(new FS.Bullets(
                    muzzleX - w / 2 -10,   // x,y to lewy górny róg, więc odejmujemy pół rozmiaru
                    muzzleY - h / 2 - 10,
                    w, h,
                    dirX * SHOT_SPEED[l.type],
                    dirY * SHOT_SPEED[l.type],
                    SHOT_DAMAGE[l.type]
            ));

            shots.add(new FS.Bullets(
                    muzzleX - w / 2 + 10,   // x,y to lewy górny róg, więc odejmujemy pół rozmiaru
                    muzzleY - h / 2 - 10,
                    w, h,
                    dirX * SHOT_SPEED[l.type],
                    dirY * SHOT_SPEED[l.type],
                    SHOT_DAMAGE[l.type]
            ));
        } else if (l.type == 1) {
            rockets.add(new Rockets(
                    muzzleX - w / 2,   // x,y to lewy górny róg, więc odejmujemy pół rozmiaru
                    muzzleY - h / 2,
                    w, h,
                    dirX * SHOT_SPEED[l.type],
                    dirY * SHOT_SPEED[l.type],
                    SHOT_DAMAGE[l.type],
                    0,
                    0.2
            ));
        } else {
            shots.add(new FS.Bullets(
                    muzzleX - w / 2,   // x,y to lewy górny róg, więc odejmujemy pół rozmiaru
                    muzzleY - h / 2,
                    w, h,
                    dirX * SHOT_SPEED[l.type],
                    dirY * SHOT_SPEED[l.type],
                    SHOT_DAMAGE[l.type]
            ));
        }
    }

    public void updateShots(double dt, Cam cam) {
        for (int i = shots.size() - 1; i >= 0; i--) {
            FS.Bullets b = shots.get(i);
            b.prevX = b.x;
            b.prevY = b.y;
            b.x += b.vx * dt;
            b.y += b.vy * dt;

            if (cam.outside(b.x, b.y, 1000)) {
                shots.remove(i);
            }
        }
    }

    public void updateRockets(double dt, Cam cam, Feldres player){
        for (int i = rockets.size() - 1; i >= 0; i--) {
            Rockets b = rockets.get(i);

            double playerX = player.x + player.w / 2;
            double playerY = player.y + player.h / 2;
            double rocketX = b.x + b.w / 2;
            double rocketY = b.y + b.h / 2;
            double dx = playerX - rocketX;
            double dy = playerY - rocketY;
            double targetAngle = Math.atan2(dy, dx);
            double currentAngle = Math.atan2(b.vy, b.vx);
            double speed = Math.hypot(b.vx, b.vy);
            double difference = targetAngle - Math.atan2(b.vy, b.vx);
            difference = Math.atan2(Math.sin(difference), Math.cos(difference));
            double maxStep = Math.toRadians(70) * dt;
            double step = Math.max(-maxStep, Math.min(maxStep, difference));
            double newAngle = currentAngle + step;

            if (b.delay > 0) {
                b.prevX = b.x;
                b.prevY = b.y;
                b.x += b.vx * dt;
                b.y += b.vy * dt;
            }else {
                b.vx = Math.cos(newAngle) * speed;
                b.vy = Math.sin(newAngle) * speed;
                b.prevX = b.x;
                b.prevY = b.y;
                b.x += b.vx * dt;
                b.y += b.vy * dt;
            }

            if (cam.outside(b.x, b.y, 1000) || b.life >= ROCKET_LIFE) {
                rockets.remove(i);
            }
            b.life += dt;
            b.delay -= dt;
        }
    }

    public void removeDead() {
        legion.removeIf(l -> l.hp <= 0);
    }

    /** Returns damage dealt to the player by contact this frame, 0 if none. */
    public double contactDamage(Feldres player) {
        for (Legion l : legion) {
            if (l.koliduje(player)) return CONTACT_DAMAGE[l.type];
        }
        return 0;
    }

     private void legionDamage(Feldres player,double dt){
        for (Legion l : legion) {
            if (l.koliduje(player) && hurtTimer <= 0) {
                l.hp -= 15;
                hurtTimer = HURT_COOLDOWN;
            } else {
                hurtTimer -= dt;
            }
        }
    }

    public double shotsDamage(Feldres player) {
        double total = 0;
        for (int i = shots.size() - 1; i >= 0; i--) {
            FS.Bullets b = shots.get(i);

            // prostokąt obejmujący całą drogę pocisku w tej klatce (tak samo jak w FS.hitList)
            double left   = Math.min(b.prevX, b.x);
            double right  = Math.max(b.prevX, b.x) + b.w;
            double top    = Math.min(b.prevY, b.y);
            double bottom = Math.max(b.prevY, b.y) + b.h;

            boolean hit = left < player.x + player.w && right > player.x
                    && top < player.y + player.h && bottom > player.y;

            if (hit) {
                total += b.dmg;
                shots.remove(i);
            }
        }
        return total;
    }

    public double rocketsDamage(Feldres player) {
        double total = 0;
        for (int i = rockets.size() - 1; i >= 0; i--) {
            FS.Bullets b = rockets.get(i);

            // prostokąt obejmujący całą drogę pocisku w tej klatce (tak samo jak w FS.hitList)
            double left   = Math.min(b.prevX, b.x);
            double right  = Math.max(b.prevX, b.x) + b.w;
            double top    = Math.min(b.prevY, b.y);
            double bottom = Math.max(b.prevY, b.y) + b.h;

            boolean hit = left < player.x + player.w && right > player.x
                    && top < player.y + player.h && bottom > player.y;

            if (hit) {
                total += b.dmg;
                rockets.remove(i);
            }
        }
        return total;
    }
}
