import java.util.Random;

public class RNG {
    private int seed;
    static final int[] CHANCE = {65, 20, 10, 5};

    private final Random random = new Random();

    public RNG(){
        seed = random.nextInt();
    }

    public void newSeed(){
        seed = random.nextInt();
    }

    public int hash(int x, int y) {
        int h = x * 374761393 + y * 668265263 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    public int chooseTile(int tx, int ty) {
        int r = Math.floorMod(hash(tx, ty), 100);     // floorMod, bo hash bywa ujemny
        int suma = 0;
        for (int i = 0; i < CHANCE.length; i++) {
            suma += CHANCE[i];
            if (r < suma) return i;
        }
        return 0;
    }
}
