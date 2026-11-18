public class FidoMovement {
    final private double WIDTH;
    final private double HEIGHT;
    final private double FRAME_TIME = 0.12;
    final private FS fs;

    private double tilSwap;

    public FidoMovement(double WIDTH, double HEIGHT, FS fs) {
        this.WIDTH = WIDTH;
        this.HEIGHT = HEIGHT;
        this.fs = fs;
    }

    private Feldres fido;

    public void setTilSwap(double tilSwap) {
        this.tilSwap = tilSwap;
    }
    public Feldres getFido() { return fido; }

    public void spawnFido(){
        fido = new Feldres(WIDTH / 2 - 25, HEIGHT / 2 - 300, 60, 50, 0, 0, 550, 120, 0, 0);
    }

    public void fidoUpdate(double dt, Feldres player) {
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
        }else if(tilSwap <= 0 && fs.isFull()){
            dir = -1;
        }
        if (dir != 0) {
            fido.x += (dx / odleglosc) * dir * fido.speed * dt;
            fido.y += (dy / odleglosc) * dir * fido.speed * dt;
            fido.angle = Math.atan2(dir * (dy / odleglosc), dir * (dx / odleglosc));

            fido.animTimer += dt;
            if (fido.animTimer >= FRAME_TIME) {
                fido.frame = (fido.frame + 1) % 4;
                fido.animTimer = 0;
            }
        }else {
            fido.frame = 0;           // stoi, więc klatka stojąca, bez animacji chodzenia
            fido.animTimer = 0;
        }
    }
}
