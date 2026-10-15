import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Legions {
    static final int[]    COST      = {1, 3, 5};
    static final double[] HP        = {15, 30, 45};
    static final double[] SPEED     = {260, 200, 140};
    static final double[] SIZE_W    = {67, 70, 242};
    static final double[] SIZE_H    = {50, 75, 150};
    static final double[] TURN_RATE = {200, 120, 70};   // degrees per second
    static final double[] CONTACT_DAMAGE = {5, 12, 25};

    static final double ROUND_BREAK        = 3.0;       // seconds between rounds
    static final double SPAWN_RADIUS_MIN   = 1000;
    static final double SPAWN_RADIUS_RANGE = 300;
    static final double SPAWN_MARGIN       = 100;       // how far outside the screen enemies appear
    static final int    SPAWN_ATTEMPTS     = 30;

    static class Legion extends Feldres {
        int type;          // 0 = amaise, 1 = grauwolf, 2 = lowe
        double hp;
        double speed;
        double angle;
        int frame = 0;           // aktualna klatka animacji
        double animTimer = 0;

        Legion(int type, double x, double y) {
            super(x, y, SIZE_W[type], SIZE_H[type], 0);
            this.type = type;
            this.hp = HP[type];
            this.speed = SPEED[type];
        }
    }

    private final List<Legion> legion = new ArrayList<>();
    private final Random random = new Random();

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
        round = 0;
        difficulty = 5;
        breakTimer = 0;
    }

    public void update(double dt, Feldres player, Cam cam) {
        updateEnemies(dt, player);
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

        // draw random points on a ring until one lies outside the visible screen
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = SPAWN_RADIUS_MIN + random.nextDouble() * SPAWN_RADIUS_RANGE;
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

    public void updateEnemies(double dt, Feldres player) {
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

            l.x += (Math.cos(l.angle)) * l.speed * dt;
            l.y += (Math.sin(l.angle)) * l.speed * dt;

            l.animTimer += dt;
            double frameTime = BASE_FRAME_TIME * REF_SPEED / l.speed;
            if (l.animTimer >= frameTime) {
                l.frame = (l.frame + 1) % FRAMES;
                l.animTimer -= frameTime;      // -= zamiast = 0, żeby nie gubić nadwyżki czasu
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
}
