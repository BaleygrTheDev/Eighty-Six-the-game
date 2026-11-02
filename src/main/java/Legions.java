import java.util.*;

public class Legions {
    static final int AMEISE = 0, GRAUWOLF = 1, LOWE = 2;
    static final int[]    COST      = {1, 3, 5};
    static final double[] HP        = {15, 30, 45};
    static final double[] SPEED     = {400, 500, 250};
    static final double[] SIZE_W    = {90, 121, 242};
    static final double[] SIZE_H    = {67, 135, 165};
    static final double[] TURN_RATE = {270, 400, 100};   // degrees per second
    static final double[] CONTACT_DAMAGE = {0, 25, 0};
    static final double[] SHOT_DAMAGE = {2,   30,    50};    // obrażenia jednego pocisku
    static final double[] FIRE_DELAY  = {0.3, 0.02,  2.0};   // sekundy między strzałami
    static final double[] SHOT_SPEED  = {650, 700,  500};   // px/s
    static final double[] SHOT_RANGE  = {800, 2200,  1300};  // max dystans do gracza, z którego strzela
    static final double[] SHOT_W      = {12,  16,   26};    // rozmiar pocisku (hitbox + sprite)
    static final double[] SHOT_H      = {5,   6,    10};
    static final double[] SHOT_SPREAD = {
            Math.toRadians(12),    // ameise: bardzo niecelne
            Math.toRadians(20),    // grauwolf: średnio
            Math.toRadians(0.5)    // lowe: prawie idealnie
    };
    static final double AIM_TOLERANCE = Math.toRadians(8);// max błąd kąta, przy którym wolno strzelić
    static  final double ROCKET_LIFE = 5;
    static final double TWIN_GUN_OFFSET = 10;               // Ameise: odstęp każdej z dwóch luf od osi
    static final double HOLD_SPEED = 1;                     // "prawie stoi" (Löwe/Grauwolf w pozycji strzeleckiej)
    static final double LOWE_HOLD_DISTANCE = 800;
    static final double GRAUWOLF_HOLD_DISTANCE = 2137;
    static final double ROCKET_DELAY = 0.2;                 // sekundy lotu na wprost, zanim rakieta zacznie sterować
    static final double ROCKET_TURN_RATE = 70;              // stopnie na sekundę
    static final double SHOT_CULL_MARGIN = 1000;            // pocisk dalej od ekranu niż tyle jest usuwany
    static final double RAM_DAMAGE = 15;                    // obrażenia wroga przy zderzeniu z graczem
    static final double RAM_COOLDOWN = 1;
    static final int[] AMMO = {Integer.MAX_VALUE, 6, Integer.MAX_VALUE};
    private double ramTimer = 0;

    static final double AVOID_RANGE = 250;    // od jakiej odległości od powierzchni przeszkody wróg zaczyna ją omijać
    static final double AVOID_SIDE  = 2.5;    // siła skrętu bokiem (wzdłuż przeszkody)
    static final double AVOID_BACK  = 0.5;    // siła odpychania wprost od przeszkody
    static final int    GROUP_MIN = 3;                   // ile Ameise musi być razem, żeby zaatakować
    static final double GROUP_RADIUS = 250;              // "razem" = w tym promieniu od siebie
    static final double SEEK_RADIUS = 700;               // z jakiej odległości uśredniają położenie reszty
    static final double GATHER_ARRIVE_DIST = 200;        // tyle przed punktem zbiórki zaczynają zwalniać
    static final double GATHER_TIMEOUT = 10;             // po tylu sekundach bez grupy i tak atakują
    static final double GATHER_PLAYER_CLEARANCE = 800;   // w trybie zbierania omijają gracza z takiej odległości
    static final double ROUND_BREAK        = 3.0;       // seconds between rounds
    static final double SPAWN_RADIUS_MIN   = 1000;
    static final double SPAWN_RADIUS_RANGE = 300;
    static final double SPAWN_MARGIN       = 100;       // how far outside the screen enemies appear
    static final int    SPAWN_ATTEMPTS     = 30;

    static class Legion extends Feldres {
        int type;          // 0 = amaise, 1 = grauwolf, 2 = lowe
        double fireTimer;
        double concussionTimer;
        int ammo;
        boolean concussed;
        boolean touching;
        int killedBy = FS.ENEMY;
        boolean attacking;        // <-- nowe (Ameise): true = biegnie na gracza i strzela
        double gatherTimer;       // <-- nowe: ile sekund już zbiera
        double gatherX, gatherY;  // <-- nowe: punkt zbiórki (środek reszty grupy)

        Legion(int type, double x, double y) {
            super(x, y, SIZE_W[type], SIZE_H[type], HP[type],0, SPEED[type], TURN_RATE[type], 0, 0);
            this.type = type;
            this.fireTimer = FIRE_DELAY[type] * (0.3 + Math.random() * 0.7);
            this.ammo = AMMO[type];
            this.concussed = false;
        }
    }

    static class Rockets extends FS.Bullets {
        double age = 0;
        double delay;

        Rockets(double x, double y, double w, double h,
                double angle, double speed, double dmg, double delay) {
            super(x, y, w, h, angle, speed, dmg, FS.ENEMY);
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
        ramTimer = 0;
    }

    public void update(double dt, Feldres player, Cam cam, Obstacles obstacles) {
        updateEnemies(dt, player, cam, obstacles);
        updateShots(dt, cam, obstacles);
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

        difficulty += 2;
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

    private void updateSwarm(double dt) {
        int total = 0;
        for (Legion l : legion) {
            if (l.type == AMEISE) total++;
        }
        int need = Math.min(GROUP_MIN, total);        // gdy zostało mniej, wystarczy tyle, ile jest

        for (Legion a : legion) {
            if (a.type != AMEISE || a.attacking) continue;

            double ax = a.x + a.w / 2;
            double ay = a.y + a.h / 2;

            int near = 1;                              // liczymy też siebie
            double sumX = 0, sumY = 0;
            int seekCount = 0;
            Legion nearest = null;
            double nearestDist = Double.MAX_VALUE;

            for (Legion b : legion) {
                if (b == a || b.type != AMEISE) continue;
                double bx = b.x + b.w / 2;
                double by = b.y + b.h / 2;
                double d = Math.hypot(bx - ax, by - ay);

                if (d < GROUP_RADIUS) near++;
                if (d < SEEK_RADIUS) {
                    sumX += bx;
                    sumY += by;
                    seekCount++;
                }
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = b;
                }
            }

            a.gatherTimer += dt;

            if (near >= need || nearest == null || a.gatherTimer >= GATHER_TIMEOUT) {
                // grupa gotowa (albo koniec czekania): ta Ameise i wszyscy w jej pobliżu atakują razem
                a.attacking = true;
                for (Legion b : legion) {
                    if (b.type != AMEISE) continue;
                    double d = Math.hypot(b.x + b.w / 2 - ax, b.y + b.h / 2 - ay);
                    if (d < GROUP_RADIUS) b.attacking = true;
                }
                continue;
            }

            // jeszcze nie ma grupy: punkt zbiórki = środek Ameise w pobliżu, a gdy ich nie ma, najbliższa Ameise
            if (seekCount > 0) {
                a.gatherX = sumX / seekCount;
                a.gatherY = sumY / seekCount;
            } else {
                a.gatherX = nearest.x + nearest.w / 2;
                a.gatherY = nearest.y + nearest.h / 2;
            }
        }
    }

    private double steerAngle(Legion l, double dirX, double dirY, Obstacles obstacles) {
        double ex = l.x + l.w / 2;
        double ey = l.y + l.h / 2;
        double headingX = Math.cos(l.angle);
        double headingY = Math.sin(l.angle);
        double radius = (l.w + l.h) / 4;               // taki sam promień jak w Obstacles.colision

        double sumX = dirX;                            // zaczynamy od kierunku na gracza
        double sumY = dirY;

        for (Obstacles.Obstacle o : obstacles.getCarcas()) {
            double ox = o.x + o.w / 2 - ex;
            double oy = o.y + o.h / 2 - ey;
            double od = Math.hypot(ox, oy);
            if (od < 0.0001) continue;

            double gap = od - radius - (o.w + o.h) / 4;      // odległość między powierzchniami
            if (gap >= AVOID_RANGE) continue;                // za daleko, ignoruj

            double nx = ox / od;                             // kierunek do przeszkody
            double ny = oy / od;
            if (nx * headingX + ny * headingY <= 0) continue;   // przeszkoda za plecami

            double weight = 1 - Math.max(0, gap) / AVOID_RANGE; // 0 daleko -> 1 przy styku

            // po której stronie przeszkody jest gracz (znak iloczynu wektorowego)
            double side = (nx * dirY - ny * dirX) >= 0 ? 1 : -1;

            // bok (prostopadle do kierunku na przeszkodę) + odpychanie wprost od niej
            sumX += weight * (-ny * side * AVOID_SIDE - nx * AVOID_BACK);
            sumY += weight * ( nx * side * AVOID_SIDE - ny * AVOID_BACK);
        }
        return Math.atan2(sumY, sumX);
    }

    public void updateEnemies(double dt, Feldres player, Cam cam, Obstacles obstacles) {
        updateSwarm(dt);

        double playerX = player.x + player.w / 2;
        double playerY = player.y + player.h / 2;

        for (Legion l : legion) {
            double ex = l.x + l.w / 2;
            double ey = l.y + l.h / 2;
            double dx = playerX - ex;
            double dy = playerY - ey;
            double distance = Math.hypot(dx, dy);
            if (distance < 0.0001) continue;

            l.concussed = l.concussionTimer > 0;
            l.concussionTimer = Math.max(0, l.concussionTimer - dt);

            double dirX = dx / distance;
            double dirY = dy / distance;
            double speedFactor = 1;

            if (l.type == AMEISE && !l.attacking) {
                // tryb zbierania: do punktu zbiórki zamiast na gracza
                double gx = l.gatherX - ex;
                double gy = l.gatherY - ey;
                double gd = Math.hypot(gx, gy);
                if (gd > 0.0001) {
                    dirX = gx / gd;
                    dirY = gy / gd;
                } else {
                    dirX = Math.cos(l.angle);              // jest w punkcie zbiórki: nie zmieniaj kierunku
                    dirY = Math.sin(l.angle);
                }
                speedFactor = Math.min(1, gd / GATHER_ARRIVE_DIST);    // zwalnia przy dojściu do grupy

                // nie biegnij przez gracza, żeby dołączyć do grupy po jego drugiej stronie
                if (distance < GATHER_PLAYER_CLEARANCE) {
                    double pxn = dx / distance;            // kierunek do gracza
                    double pyn = dy / distance;
                    if (pxn * dirX + pyn * dirY > 0) {     // gracz leży mniej więcej na drodze
                        double w = 1 - distance / GATHER_PLAYER_CLEARANCE;
                        double side = (pxn * dirY - pyn * dirX) >= 0 ? 1 : -1;
                        dirX += w * (-pyn * side * 2 - pxn);
                        dirY += w * ( pxn * side * 2 - pyn);
                    }
                }
            }


            double targetAngle = steerAngle(l, dirX, dirY, obstacles);

            double difference = targetAngle - l.angle;
            difference = Math.atan2(Math.sin(difference), Math.cos(difference));

            double maxStep = Math.toRadians(TURN_RATE[l.type]) * dt;
            double step = Math.max(-maxStep, Math.min(maxStep, difference));
            l.angle += step;

            if (!l.concussed){
                double moveSpeed = l.speed;
                if (l.type == LOWE && distance <= LOWE_HOLD_DISTANCE && distance > 400){
                    moveSpeed = HOLD_SPEED;
                } else if (l.type == LOWE && distance <= 400) {
                    moveSpeed = -moveSpeed;
                } else if (l.type == GRAUWOLF && distance < GRAUWOLF_HOLD_DISTANCE && l.ammo > 0){
                    moveSpeed = HOLD_SPEED;
                }
                moveSpeed *= speedFactor;
                l.x += (Math.cos(l.angle)) * moveSpeed * dt;
                l.y += (Math.sin(l.angle)) * moveSpeed * dt;
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

            double aimDiff = Math.atan2(dy, dx) - l.angle;                         // l.angle jest już po obrocie
            aimDiff = Math.atan2(Math.sin(aimDiff), Math.cos(aimDiff));
            boolean aimed = Math.abs(aimDiff) < AIM_TOLERANCE;
            boolean inRange = distance <= SHOT_RANGE[l.type];
            boolean visible = l.type == GRAUWOLF || !cam.outside(l.x + l.w / 2, l.y + l.h / 2, 0);

            boolean mayShoot = l.type != AMEISE || l.attacking;

            if (l.fireTimer <= 0 && !l.concussed && l.ammo > 0 && inRange && aimed && visible && mayShoot) {
                shoot(l);
                l.ammo--;
                l.fireTimer = FIRE_DELAY[l.type];
            }

        }

        // separation so enemies don't stack on one point
        for (int i = 0; i < legion.size(); i++) {
            Legion a = legion.get(i);
            for (int j = i + 1; j < legion.size(); j++) {
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
            obstacles.colision(dt, a, false, SPEED[a.type]);
        }
    }
    private void shoot(Legion l) {
        double cx = l.x + l.w / 2;
        double cy = l.y + l.h / 2;

        // lufa zawsze na przodzie sprite'a, czyli zgodnie z obrotem wroga
        double muzzleX = cx + Math.cos(l.angle) * (l.w / 2);
        double muzzleY = cy + Math.sin(l.angle) * (l.w / 2);

        // losowy błąd: liczba z przedziału (-SPREAD, +SPREAD)
        double shotAngle = l.angle + (random.nextDouble() * 2 - 1) * SHOT_SPREAD[l.type];

        switch (l.type){
            case AMEISE : {
               double sideX = - Math.sin(l.angle) * (TWIN_GUN_OFFSET);
               double sideY = - Math.cos(l.angle) * (TWIN_GUN_OFFSET);
               addShot(muzzleX + sideX, muzzleY + sideY, shotAngle, l.type);
               addShot(muzzleX - sideX, muzzleY - sideY, shotAngle, l.type);
               break;
            }
            case GRAUWOLF : {
                double w = SHOT_W[l.type];
                double h = SHOT_H[l.type];
                rockets.add(new Rockets(
                        muzzleX - w / 2, muzzleY - h / 2,
                        w, h,
                        shotAngle,
                        SHOT_SPEED[l.type],
                        SHOT_DAMAGE[l.type],
                        ROCKET_DELAY
                ));
                break;
            } default: {
                addShot(muzzleX, muzzleY, shotAngle, l.type);
            }
        }
    }

    public void addShot(double muzzleX, double muzzleY, double shotAngle, int type){
        double w = SHOT_W[type];
        double h = SHOT_H[type];

        shots.add(new FS.Bullets(
                muzzleX - w / 2,   // x,y to lewy górny róg, więc odejmujemy pół rozmiaru
                muzzleY - h / 2,
                w, h,
                shotAngle,
                SHOT_SPEED[type],
                SHOT_DAMAGE[type],
                FS.ENEMY
        ));
    }

    public void updateShots(double dt, Cam cam, Obstacles obstacle) {
        shots.removeIf(b -> {
           b.move(dt);
           return cam.outside(b.x, b.y, SHOT_CULL_MARGIN);
        });
        shots.removeIf(b -> obstacle.colision(dt, b, false, 0));
    }

    public void updateRockets(double dt, Cam cam, Feldres player){
        double playerX = player.x + player.w / 2;
        double playerY = player.y + player.h / 2;
        double maxStep = Math.toRadians(ROCKET_TURN_RATE) * dt;

        rockets.removeIf(r ->{
            if(r.delay <= 0) {
                double targetAngle = Math.atan2(playerY - (r.y + r.h / 2), playerX - (r.x + r.w / 2));
                double difference = targetAngle - r.getAngle();
                difference = Math.atan2(Math.sin(difference), Math.cos(difference));
                double step = Math.max(-maxStep, Math.min(maxStep, difference));
                r.setAngle(r.getAngle() + step);
            }
            r.move(dt);
            r.age += dt;
            r.delay -= dt;

            return r.age >= ROCKET_LIFE || cam.outside(r.x, r.y, SHOT_CULL_MARGIN);
        });
    }

    public void removeDead(Explosions fx, Obstacles o) {
        legion.removeIf(l -> {
            if (l.hp > 0) return false;
            fx.spawn(l.x + l.w / 2, l.y + l.h / 2,l.killedBy, Math.max(l.w, l.h));
            o.newCarcas(l.x, l.y , l.angle, l.type, getRound());
            return true;
        });
    }

    /** Returns damage dealt to the player by contact this frame, 0 if none. */
    public double contactDamage(Feldres player) {
        for (Legion l : legion) {
            if (l.koliduje(player) && !l.concussed) return CONTACT_DAMAGE[l.type];
        }
        return 0;
    }

     private void legionDamage(Feldres player,double dt){
        ramTimer = Math.max(0, ramTimer - dt);

        for (Legion l : legion) {
            boolean touching = l.koliduje(player);
            if (touching && !l.touching && ramTimer <= 0) {
                l.hp -= RAM_DAMAGE;
                ramTimer = RAM_COOLDOWN;
            }
            l.touching = touching;
        }

    }

    public double shotsDamage(Feldres player) { return collectDamage(shots, player); }
    public double rocketsDamage(Feldres player) { return collectDamage(rockets, player); }

    public double collectDamage(List<? extends FS.Bullets> list, Feldres player) {
        double total = 0;
        for (Iterator<? extends FS.Bullets> it = list.iterator(); it.hasNext();) {
            FS.Bullets b = it.next();

            if (b.hits(player)) {
                total += b.dmg;
                it.remove();
            }
        }
        return total;
    }
}
