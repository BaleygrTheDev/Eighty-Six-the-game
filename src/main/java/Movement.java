import javafx.scene.input.KeyCode;

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

    public Movement(double mouseX, double mouseY, Set<KeyCode> klawisze){
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.klawisze = klawisze;
    }

    public double getLength(){ return this.length; }
    public double getAimX(){ return this.aimX; }
    public double getAimY(){ return this.aimY; }

    public void setDoWymiany(double doWymiany){ this.doWymiany = doWymiany; }
    public void setMouse(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public void update(double dt, Feldres player, Cam cam){
        updatePlayerAngle(dt, player, cam);
        updatePlayerPosition(dt, player);
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
    public void updatePlayerPosition(double dt, Feldres player){
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
        length = Math.sqrt(ruchX * ruchX + ruchY * ruchY);
        if (length > 1) {
            ruchX /= length;
            ruchY /= length;
        }
        if (doWymiany <= 0) {
            player.x += ruchX * player.speed * dt;
            player.y += ruchY * player.speed * dt;

            if (dashTimer > 0) {
                double krok = Math.min(dt, dashTimer);
                player.x += ruchX * dashSpeed * krok;
                player.y += ruchY * dashSpeed * krok;
                dashTimer -= krok;
            }

            boolean dash = klawisze.contains(KeyCode.SPACE);

            if (dashCooldown == 0 && dash && !dashHolding) {
                dashTimer = dashTime;
                dashCooldown = dashWait;
            }
            dashHolding = dash;
        }

        dashCooldown = Math.max(0, dashCooldown - dt);
    }
}
