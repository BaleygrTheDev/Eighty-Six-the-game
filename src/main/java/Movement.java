import javafx.scene.input.KeyCode;

import java.util.List;
import java.util.Set;

public class Movement {
    private double mouseX;
    private double mouseY;
    private Set<KeyCode> klawisze;

    private double doWymiany;
    private double targetAngle;
    private double aimX;
    private double aimY;
    private boolean dashHolding;

    private double length;
    private double dashTime = 0.10;
    private double dashSpeed = 1500;
    private double dashTimer = 0;
    private double dashCooldown = 0;
    private double dashWait = 0.5;
    private double boostSpeed = 1;
    private final double MAX_ENERGY = 100;
    private double energy = MAX_ENERGY;

    public Movement(double mouseX, double mouseY, Set<KeyCode> klawisze){
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.klawisze = klawisze;
    }

    public double getLength(){ return this.length; }
    public double getAimX(){ return this.aimX; }
    public double getAimY(){ return this.aimY; }

    public double getDashCooldown() {
        return dashCooldown;
    }
    public double getEnergy(){ return this.energy; }
    public double getMAX_ENERGY(){ return this.MAX_ENERGY; }

    public void setDoWymiany(double doWymiany){ this.doWymiany = doWymiany; }
    public void setMouse(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public void update(double dt, Feldres player, Cam cam, Obstacles obstacles){
        updatePlayerAngle(dt, player, cam);
        updatePlayerPosition(dt, player, obstacles);
    }

    public void reset() {
        dashTimer = 0;
        dashCooldown = 0;
        length = 0;
    }

    public void updatePlayerAngle(double dt, Feldres player, Cam cam) {
        double graczX = player.x + player.w / 2;
        double graczY = player.y + player.h / 2;

        double celX = cam.worldX(mouseX);
        double celY = cam.worldY(mouseY);

        targetAngle = Math.atan2(celY - graczY, celX - graczX);
        double diference = targetAngle - player.angle;
        diference = Math.atan2(Math.sin(diference), Math.cos(diference));

        double dx = celX - graczX;
        double dy = celY - graczY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance * cam.getZoom() > 35) {
            double maxStep = Math.toRadians(player.turnRate) * dt;
            player.angle += Math.max(-maxStep, Math.min(maxStep, diference));
        }
        aimX = graczX + Math.cos(player.angle) * distance;
        aimY = graczY + Math.sin(player.angle) * distance;
    }
    public void updatePlayerPosition(double dt, Feldres player, Obstacles obstacles){
        double kierunekX = 0;
        double kierunekY = 0;

        if (energy > MAX_ENERGY) energy = MAX_ENERGY;

        if (klawisze.contains(KeyCode.LEFT) || klawisze.contains(KeyCode.A)) kierunekX -= 1;
        if (klawisze.contains(KeyCode.RIGHT) || klawisze.contains(KeyCode.D)) kierunekX += 1;
        if (klawisze.contains(KeyCode.UP) || klawisze.contains(KeyCode.W)) kierunekY += 1;
        if (klawisze.contains(KeyCode.DOWN) || klawisze.contains(KeyCode.S)) kierunekY -= 1;
        if (klawisze.contains(KeyCode.SHIFT) && energy > 0) boostSpeed = 1.3;

        // Ruch względem obrotu gracza
        double ruchX = Math.cos(player.angle) * kierunekY - Math.sin(player.angle) * kierunekX;
        double ruchY = Math.sin(player.angle) * kierunekY + Math.cos(player.angle) * kierunekX;
        boolean dash = false;

        // Normalizacja ruchu po przekątnej
        length = Math.sqrt(ruchX * ruchX + ruchY * ruchY);
        if (length > 1) {
            ruchX /= length;
            ruchY /= length;
        }
        if (doWymiany <= 0) {
            player.x += ruchX * player.speed * boostSpeed * dt;
            player.y += ruchY * player.speed * boostSpeed * dt;

            if (dashTimer > 0) {
                double krok = Math.min(dt, dashTimer);
                player.x += ruchX * dashSpeed * boostSpeed * krok;
                player.y += ruchY * dashSpeed * boostSpeed * krok;
                dashTimer -= krok;
            }

            dash = klawisze.contains(KeyCode.SPACE);

            if (dashCooldown == 0 && dash && !dashHolding && energy >= 20) {
                dashTimer = dashTime;
                dashCooldown = dashWait;
                energy -= 20;
            }
            dashHolding = dash;
        }
        if (boostSpeed > 1){
            energy -= 20 * dt;
        }else {
            energy += 20 * dt;
        }
        boostSpeed = 1;
        dashCooldown = Math.max(0, dashCooldown - dt);
        obstacles.colision(dt, player, dash, ESTG.PLAYER_SPEED);
    }
}
