package br.jogobike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameWorld {
    private MainGame game;
    private Random random;

    // Estado do Jogo
    private int pontos;
    private int vidas = 3;
    private boolean isPaused = false;
    private boolean isGameOver = false;

    // Estado do Jogador
    private float playerX, playerY;
    private float playerSpeedX = 0;
    private float playerSpeedY = 0;
    private Rectangle playerRect;

    // Estado do Bot
    private BotLog bot;

    // Invencibilidade
    private boolean podeLevarDano = true;
    private float tempoInvencivel = 0f;
    private final float TEMPO_INVENCIBILIDADE = 1.5f;

    // Dano Avalanche
    private float tempoUltimoDanoAvalanche = 0f;
    private final float INTERVALO_DANO_AVALANCHE = 0.3f;

    // Constantes do Jogador
    private final float PLAYER_WIDTH = 70f;
    private final float PLAYER_HEIGHT = 90f;
    private final float PLAYER_COLLISION_WIDTH = 40f;
    private final float PLAYER_COLLISION_HEIGHT = 40f;
    private final float PLAYER_COLLISION_OFFSET_X = (PLAYER_WIDTH - PLAYER_COLLISION_WIDTH) / 2f;
    private final float PLAYER_COLLISION_OFFSET_Y = (PLAYER_HEIGHT - PLAYER_COLLISION_HEIGHT) / 2f;

    // Constantes de Física Base
    private final float BASE_MAX_SPEED = 400f;
    private final float BASE_ACCELERATION = 280f;
    private final float BASE_FRICTION = 180f;
    // --- MUDANÇA (Request 3): Drift (ré) um pouco mais rápido ---
    private final float BASE_DRIFT_SPEED = 190f; // Era 160f

    // Variáveis de Física (que mudam com a dificuldade)
    private float maxSpeed;
    private float acceleration;
    private float friction;
    private float driftSpeed;
    private float currentDifficulty; // Para passar ao Bot

    // Constantes da Pista
    private final float PISTA_BAIXO = 275f;
    private final float PISTA_CIMA = 500f;
    public static final float LARGURA_AVALANCHE = 120f;
    public static final float ALTURA_AVALANCHE = 520f;

    // Efeitos Visuais (Lógica)
    private List<SnowEffect> snowEffects;
    private List<WindLine> windLines;
    private List<PlayerDashLine> dashLines;
    private float avalancheTimer = 0f;
    private float avalancheIntensity = 0f;
    private float windTimer = 0f;
    private float backgroundOffsetX = 0f;
    private float parallaxSpeed = 300f;

    public GameWorld(MainGame game) {
        this.game = game;
        this.random = new Random();
        this.pontos = 0;

        // Posições iniciais
        playerX = MainGame.VIRTUAL_WIDTH / 2f - PLAYER_WIDTH / 2f;
        playerY = MainGame.VIRTUAL_HEIGHT / 2f - PLAYER_HEIGHT / 2f;

        // Inicializar PlayerRect
        playerRect = new Rectangle(
            playerX + PLAYER_COLLISION_OFFSET_X,
            playerY + PLAYER_COLLISION_OFFSET_Y,
            PLAYER_COLLISION_WIDTH,
            PLAYER_COLLISION_HEIGHT
        );

        // Carregar BotLog
        try {
            Texture troncoTexture = new Texture(Gdx.files.internal("tronco.png"));
            bot = new BotLog(troncoTexture, MainGame.VIRTUAL_WIDTH + 50, getRandomPositionInTrack());
        } catch (Exception e) {
            Gdx.app.error("GameWorld", "Erro ao carregar tronco.png, usando fallback");
            bot = new BotLog("tronco.png", MainGame.VIRTUAL_WIDTH + 50, getRandomPositionInTrack());
        }

        // Efeitos
        snowEffects = new ArrayList<>();
        windLines = new ArrayList<>();
        dashLines = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            windLines.add(new WindLine());
        }

        // Inicializa variáveis de física
        updateDifficulty();
    }

    /**
     * O loop principal de lógica do jogo.
     */
    public void update(float delta) {
        if (isGameOver) return;

        updateDifficulty();

        if (!podeLevarDano) {
            tempoInvencivel -= delta;
            if (tempoInvencivel <= 0) {
                podeLevarDano = true;
            }
        }
        tempoUltimoDanoAvalanche += delta;

        // Atualiza lógica dos efeitos visuais
        updateAvalanche(delta);
        updateWindEffect(delta);
        updateParallax(delta);
        updateDashLines(delta);

        // Física e Movimento
        playerX += playerSpeedX * delta;
        playerY += playerSpeedY * delta;

        // Limites da tela
        playerX = MathUtils.clamp(playerX, LARGURA_AVALANCHE, MainGame.VIRTUAL_WIDTH - PLAYER_WIDTH);
        playerY = MathUtils.clamp(playerY, PISTA_BAIXO, PISTA_CIMA - PLAYER_HEIGHT);

        // Atualizar retângulo de colisão
        updatePlayerRect();

        // Checar dano da avalanche
        if (playerX <= LARGURA_AVALANCHE + 5 && podeLevarDano && tempoUltimoDanoAvalanche >= INTERVALO_DANO_AVALANCHE) {
            aplicarDanoAvalanche();
            tempoUltimoDanoAvalanche = 0f;
        }

        // Atualizar BotLog
        if (bot != null) {
            boolean foiDesviado = bot.update(delta, playerSpeedX, LARGURA_AVALANCHE, MainGame.VIRTUAL_WIDTH, currentDifficulty);
            if (foiDesviado) {
                pontos += 10;
                Gdx.app.log("GameWorld", "Desviou! Pontos: " + pontos);
            }
        }

        // Colisões com bot
        if (bot != null && playerRect.overlaps(bot.rect) && podeLevarDano) {
            aplicarDano();
            bot.respawn(MainGame.VIRTUAL_WIDTH, 50, 600);
        }

        // Checar Game Over
        if (vidas <= 0) {
            isGameOver = true;
        }
    }

    private void updateDifficulty() {
        float scalar = 1.0f + (pontos / 100f) * 0.05f;
        currentDifficulty = Math.min(2.5f, scalar);

        maxSpeed = BASE_MAX_SPEED * currentDifficulty;
        acceleration = BASE_ACCELERATION * currentDifficulty;
        friction = BASE_FRICTION * currentDifficulty;
        driftSpeed = BASE_DRIFT_SPEED * currentDifficulty;
    }

    /**
     * Processa o input de movimento do jogador e atualiza a física.
     */
    public void handlePlayerInput(boolean isMovingUp, boolean isMovingDown, boolean isAccelerating, boolean isBraking) {
        // MOVIMENTO HORIZONTAL
        if (isAccelerating) {
            playerSpeedX += acceleration * Gdx.graphics.getDeltaTime();
            playerSpeedX = Math.min(maxSpeed, playerSpeedX);

            if (playerSpeedX > maxSpeed * 0.5f && random.nextFloat() > 0.3f) {
                dashLines.add(new PlayerDashLine(
                    playerX - 10f,
                    playerY + PLAYER_HEIGHT / 2f
                ));
            }

        } else if (isBraking) {
            playerSpeedX -= acceleration * 1.5f * Gdx.graphics.getDeltaTime();
            playerSpeedX = Math.max(-maxSpeed * 0.5f, playerSpeedX);
        } else {
            // --- MUDANÇA (Request 3): Aqui usa o BASE_DRIFT_SPEED (agora 190f) ---
            playerSpeedX -= driftSpeed * Gdx.graphics.getDeltaTime();
        }

        if (playerSpeedX > 0) {
            playerSpeedX = Math.max(0, playerSpeedX - friction * Gdx.graphics.getDeltaTime() * 0.5f);
        } else if (playerSpeedX < 0) {
            playerSpeedX = Math.min(0, playerSpeedX + friction * Gdx.graphics.getDeltaTime() * 0.5f);
        }

        // MOVIMENTO VERTICAL
        if (isMovingUp) {
            playerSpeedY = maxSpeed * 0.7f;
        } else if (isMovingDown) {
            playerSpeedY = -maxSpeed * 0.7f;
        } else {
            if (playerSpeedY > 0) {
                playerSpeedY = Math.max(0, playerSpeedY - friction * Gdx.graphics.getDeltaTime());
            } else if (playerSpeedY < 0) {
                playerSpeedY = Math.min(0, playerSpeedY + friction * Gdx.graphics.getDeltaTime());
            }
        }
    }

    private void updatePlayerRect() {
        playerRect.setPosition(
            playerX + PLAYER_COLLISION_OFFSET_X,
            playerY + PLAYER_COLLISION_OFFSET_Y
        );
    }

    // --- Lógica de Dano ---

    private void aplicarDano() {
        if (podeLevarDano) {
            vidas--;
            pontos = Math.max(0, pontos - 20);
            podeLevarDano = false;
            tempoInvencivel = TEMPO_INVENCIBILIDADE;
            playerX = Math.min(MainGame.VIRTUAL_WIDTH - PLAYER_WIDTH - 100, playerX + 80);
            playerSpeedX = 100;
            Gdx.app.log("GameWorld", "Dano! Vidas restantes: " + vidas);
        }
    }

    private void aplicarDanoAvalanche() {
        if (podeLevarDano) {
            vidas--;
            pontos = Math.max(0, pontos - 15);
            podeLevarDano = false;
            tempoInvencivel = TEMPO_INVENCIBILIDADE;
            playerX = LARGURA_AVALANCHE + 30;
            playerSpeedX = 200;
            Gdx.app.log("GameWorld", "Avalanche! Vidas restantes: " + vidas);
        }
    }

    // --- Lógica de Efeitos ---

    private void updateAvalanche(float delta) {
        avalancheTimer += delta;
        avalancheIntensity = Math.min(1.0f, avalancheTimer * 0.1f);

        // --- MUDANÇA (Request 1): Aumentar spawn de partículas
        if (random.nextFloat() < 0.9f + avalancheIntensity * 0.1f) { // Mais partículas base
            snowEffects.add(new SnowEffect());
        }

        for (int i = snowEffects.size() - 1; i >= 0; i--) {
            SnowEffect effect = snowEffects.get(i);
            effect.update(delta);
            if (effect.isDead()) {
                snowEffects.remove(i);
            }
        }
    }

    private void updateWindEffect(float delta) {
        windTimer += delta;
        for (WindLine line : windLines) {
            line.update(delta, playerSpeedX);
        }
    }

    private void updateDashLines(float delta) {
        for (int i = dashLines.size() - 1; i >= 0; i--) {
            PlayerDashLine line = dashLines.get(i);
            line.update(delta);
            if (line.isDead()) {
                dashLines.remove(i);
            }
        }
    }

    private void updateParallax(float delta) {
        backgroundOffsetX -= parallaxSpeed * delta;
        if (backgroundOffsetX < -MainGame.VIRTUAL_WIDTH) {
            backgroundOffsetX += MainGame.VIRTUAL_WIDTH;
        }
    }

    private float getRandomPositionInTrack() {
        return PISTA_BAIXO + (float) (random.nextFloat() * (PISTA_CIMA - PISTA_BAIXO - 75f));
    }

    // --- Controle de Pausa ---

    public void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            game.pauseBackgroundMusic();
        } else {
            game.playBackgroundMusic();
        }
        Gdx.app.log("GameWorld", "Pausado: " + isPaused);
    }

    public void dispose() {
        if (bot != null) bot.dispose();
    }

    // --- Getters (para o GameRenderer) ---

    public int getPontos() { return pontos; }
    public int getVidas() { return vidas; }
    public float getPlayerX() { return playerX; }
    public float getPlayerY() { return playerY; }
    public float getPlayerSpeedX() { return playerSpeedX; }
    public BotLog getBot() { return bot; }
    public boolean isPaused() { return isPaused; }
    public boolean isGameOver() { return isGameOver; }
    public boolean podeLevarDano() { return podeLevarDano; }
    public float getTempoInvencivel() { return tempoInvencivel; }
    public float getBackgroundOffsetX() { return backgroundOffsetX; }
    public List<SnowEffect> getSnowEffects() { return snowEffects; }
    public List<WindLine> getWindLines() { return windLines; }
    public List<PlayerDashLine> getDashLines() { return dashLines; }
    public float getAvalancheTimer() { return avalancheTimer; }
    public float getWindTimer() { return windTimer; }
    public float getPlayerWidth() { return PLAYER_WIDTH; }
    public float getPlayerHeight() { return PLAYER_HEIGHT; }
    public void setPaused(boolean paused) {
        isPaused = paused;
        if (isPaused) game.pauseBackgroundMusic();
    }
    public float getDifficulty() { return currentDifficulty; }
    public float getMaxSpeed() { return maxSpeed; }
    public float getBaseMaxSpeed() { return BASE_MAX_SPEED; }


    // --- Inner classes para Efeitos (só a lógica) ---

    class PlayerDashLine {
        float x, y, length, alpha;
        float speed = 1000f;

        public PlayerDashLine(float startX, float startY) {
            this.x = startX;
            this.y = startY + MathUtils.random(-PLAYER_HEIGHT / 4f, PLAYER_HEIGHT / 4f);
            this.length = MathUtils.random(20f, 40f);
            this.alpha = 0.7f;
        }

        public void update(float delta) {
            x -= speed * delta;
            alpha -= 3.0f * delta;
        }

        public boolean isDead() { return alpha <= 0; }
    }


    class WindLine {
        float x, y;
        float length;
        float speed;
        float thickness;
        float alpha;
        boolean isActive;

        public WindLine() {
            reset();
        }

        public void reset() {
            this.x = -50;
            this.y = (float) (Math.random() * MainGame.VIRTUAL_HEIGHT);
            this.length = (float) (Math.random() * 100 + 50);
            this.speed = (float) (Math.random() * 200 + 250);
            this.thickness = (float) (Math.random() * 2 + 1);
            this.alpha = (float) (Math.random() * 0.2 + 0.1);
            this.isActive = true;
        }

        public void update(float delta, float playerSpeed) {
            float scaledSpeed = speed * (1 + (currentDifficulty - 1) * 0.8f);
            x += scaledSpeed * delta;
            alpha -= 0.5f * delta;
            if (x > MainGame.VIRTUAL_WIDTH + 100 || alpha <= 0) {
                reset();
            }
        }
    }

    class SnowEffect {
        float x, y;
        float size;
        float speed;
        float life;
        float maxLife;
        float rotation;
        float rotationSpeed;
        boolean isSnowball;
        Color color;

        public SnowEffect() {
            this.isSnowball = random.nextBoolean();
            // --- MUDANÇA (Request 1): Aumentar área de spawn
            this.x = (float) (Math.random() * (LARGURA_AVALANCHE + 20f)); // Um pouco mais largo

            if (isSnowball) {
                this.size = (float) (Math.random() * 12 + 8);
                this.y = (float) (Math.random() * ALTURA_AVALANCHE);
                this.speed = (float) (Math.random() * 80 + 40) + avalancheIntensity * 80;
                this.maxLife = (float) (Math.random() * 4 + 3);
                this.color = new Color(1, 1, 1, 0.9f);
            } else {
                this.size = (float) (Math.random() * 40 + 20);
                this.y = (float) (Math.random() * ALTURA_AVALANCHE);
                this.speed = (float) (Math.random() * 60 + 30) + avalancheIntensity * 60;
                this.maxLife = (float) (Math.random() * 1.5f + 1.0f);
                this.color = new Color(1, 1, 1, 0.6f);
            }

            this.life = maxLife;
            this.rotation = (float) (Math.random() * 360);
            this.rotationSpeed = (float) (Math.random() * 100 - 50);
        }

        public void update(float delta) {
            float scaledSpeed = speed * currentDifficulty;
            x += scaledSpeed * delta;
            rotation += rotationSpeed * delta;
            life -= delta;

            if (isSnowball) {
                size += 8f * delta;
                color.a = life / maxLife * 0.9f;
            } else {
                size += 40f * delta;
                color.a = (life / maxLife) * 0.4f;
                y += (float) (Math.sin(avalancheTimer * 3 + x * 0.01f) * 20 * delta);
            }
        }

        public boolean isDead() {
            // Permite que as partículas passem um pouco da borda visual
            return life <= 0 || x > LARGURA_AVALANCHE + 100 || (isSnowball && size > 50) || (!isSnowball && size > 80);
        }
    }
}
