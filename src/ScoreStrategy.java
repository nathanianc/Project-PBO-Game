// Interface Utama
public interface ScoreStrategy {
    int calculateScore();
}

// Strategy Opsi Terbaik (+10)
class BestOptionStrategy implements ScoreStrategy {
    @Override
    public int calculateScore() {
        return 10;
    }
}

// Strategy Opsi Berisiko (-5)
class RiskyOptionStrategy implements ScoreStrategy {
    @Override
    public int calculateScore() {
        return -5;
    }
}

// Strategy Opsi Buruk (-10)
class BadOptionStrategy implements ScoreStrategy {
    @Override
    public int calculateScore() {
        return -10;
    }
}

// Strategy Mini Game Berhasil (+20)
class MiniGameSuccessStrategy implements ScoreStrategy {
    @Override
    public int calculateScore() {
        return 20;
    }
}

// Strategy Mini Game Gagal (-10)
class MiniGameFailStrategy implements ScoreStrategy {
    @Override
    public int calculateScore() {
        return -10;
    }
}