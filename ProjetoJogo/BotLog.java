package br.jogobike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.MathUtils;
import java.util.Random;

public class BotLog {
    public Texture texture;
    public Rectangle rect;

    private float speedOffset;
    private Random random;


    private float alpha = 1.0f;
    private boolean isFadingOut = false;
    private final float FADE_SPEED = 3.0f; // ~0.33s para sumir

    // Constantes
    private static final float BASE_SPEED = 200f;
    private static final float MIN_SPEED_OFFSET = -80f;
    private static final float MAX_SPEED_OFFSET = 120f;
    private static final float DEFAULT_WIDTH = 70f;
    private static final float DEFAULT_HEIGHT = 75f;
    private static final float COLLISION_WIDTH = 40f;
    private static final float COLLISION_HEIGHT = 40f;

    // Offset para centralizar a área de colisão
    private final float collisionOffsetX;
    private final float collisionOffsetY;

    // Construtor com Texture direta
    public BotLog(Texture texture, float x, float y) {
        this.random = new Random();
        this.texture = texture;
        this.collisionOffsetX = (DEFAULT_WIDTH - COLLISION_WIDTH) / 2f;
        this.collisionOffsetY = (DEFAULT_HEIGHT - COLLISION_HEIGHT) / 2f;
        this.rect = new Rectangle(x + collisionOffsetX, y + collisionOffsetY, COLLISION_WIDTH, COLLISION_HEIGHT);
        this.speedOffset = MathUtils.random(MIN_SPEED_OFFSET, MAX_SPEED_OFFSET);
        Gdx.app.log("BotLog", "Criado com textura fornecida");
    }

    // Construtor com caminho de arquivo
    public BotLog(String texturePath, float x, float y) {
        this.random = new Random();

        try {
            this.texture = new Texture(Gdx.files.internal(texturePath));
            Gdx.app.log("BotLog", "Textura carregada: " + texturePath);
        } catch (Exception e) {
            Gdx.app.error("BotLog", "Erro ao carregar: " + texturePath + ", usando fallback");
            this.texture = createPlaceholderTexture();
        }

        this.collisionOffsetX = (DEFAULT_WIDTH - COLLISION_WIDTH) / 2f;
        this.collisionOffsetY = (DEFAULT_HEIGHT - COLLISION_HEIGHT) / 2f;
        this.rect = new Rectangle(x + collisionOffsetX, y + collisionOffsetY, COLLISION_WIDTH, COLLISION_HEIGHT);
        this.speedOffset = MathUtils.random(MIN_SPEED_OFFSET, MAX_SPEED_OFFSET);
    }

    // Construtor padrão para fallback
    public BotLog() {
        this.random = new Random();
        this.texture = createPlaceholderTexture();
        this.collisionOffsetX = (DEFAULT_WIDTH - COLLISION_WIDTH) / 2f;
        this.collisionOffsetY = (DEFAULT_HEIGHT - COLLISION_HEIGHT) / 2f;
        this.rect = new Rectangle(0, 0, COLLISION_WIDTH, COLLISION_HEIGHT);
        this.speedOffset = MathUtils.random(MIN_SPEED_OFFSET, MAX_SPEED_OFFSET);
        Gdx.app.log("BotLog", "Criado com placeholder");
    }


    public boolean update(float delta, float playerSpeedX, float avalancheWidth, float virtualWidth, float difficultyScalar) {

        // --- MUDANÇA (Request 1): Lógica de Fade Out ---
        if (isFadingOut) {
            alpha -= FADE_SPEED * delta;
            if (alpha <= 0) {
                // CORREÇÃO: Chamamos o respawn interno, mas com os limites padrão (Fase 1)
                // O respawn só acontece se o GameWorld chamar respawnExterno
                isFadingOut = false;
                alpha = 1.0f;
                return true; // Avisa que foi desviado (respawnou)
            }
            return false; // Ainda sumindo, mas não respawnou
        }
        // ----------------------------------------------

        float scaledBaseSpeed = BASE_SPEED * difficultyScalar;
        float speed = scaledBaseSpeed + speedOffset + (Math.abs(playerSpeedX) * 0.7f);

        float currentX = getX();
        float newX = currentX - speed * delta;

        updatePosition(newX, getY());

        if (newX + DEFAULT_WIDTH < avalancheWidth) {
            isFadingOut = true; // Inicia o fade
        }
        return false; // Ainda não foi desviado
    }

    public void updatePosition(float x, float y) {
        rect.setPosition(x + collisionOffsetX, y + collisionOffsetY);
    }


    public void respawn(float virtualWidth, float minSpawnDistance, float maxSpawnDistance, float trackBottom, float trackTop) {
        float newX = virtualWidth + MathUtils.random(minSpawnDistance, maxSpawnDistance);
        // Agora usa os limites de pista passados como parâmetro
        float newY = getRandomPositionInTrack(trackBottom, trackTop);
        this.speedOffset = MathUtils.random(MIN_SPEED_OFFSET, MAX_SPEED_OFFSET);
        updatePosition(newX, newY);
    }

    private float getRandomPositionInTrack(float trackBottom, float trackTop) {
        // Garantindo que a parte de baixo do bot não vá abaixo de trackBottom
        // e que a parte de cima do bot não vá acima de trackTop.
        // O ponto de respawn (newY) é o canto inferior esquerdo do objeto (getY()).
        float effectiveTrackTop = trackTop - DEFAULT_HEIGHT;

        if (effectiveTrackTop <= trackBottom) {
            // Caso a pista seja muito estreita, apenas centraliza ou usa o limite inferior
            return trackBottom;
        }

        return MathUtils.random(trackBottom, effectiveTrackTop);
    }

    // Getters
    public float getX() {
        return rect.x - collisionOffsetX;
    }

    public float getY() {
        return rect.y - collisionOffsetY;
    }

    public float getWidth() {
        return DEFAULT_WIDTH;
    }

    public float getHeight() {
        return DEFAULT_HEIGHT;
    }

    // --- MUDANÇA (Request 1): Getter para o alpha ---
    public float getAlpha() {
        return Math.max(0, alpha); // Garante que não seja negativo
    }

    private Texture createPlaceholderTexture() {
        if (MainGame.placeholderTexture != null) {
            return MainGame.placeholderTexture;
        }

        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(
            (int)DEFAULT_WIDTH, (int)DEFAULT_HEIGHT,
            com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888
        );
        pixmap.setColor(0.65f, 0.45f, 0.25f, 1f); // Cor marrom
        pixmap.fill();
        pixmap.setColor(0.55f, 0.35f, 0.15f, 1f); // Marrom mais escuro
        for (int i = 0; i < DEFAULT_WIDTH; i += 10) {
            pixmap.drawLine(i, 0, i, (int)DEFAULT_HEIGHT);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        Gdx.app.log("BotLog", "Placeholder criado");
        return texture;
    }

    public void dispose() {
        if (texture != null && texture != MainGame.placeholderTexture) {
            texture.dispose();
            // --- CORREÇÃO APLICADA AQUI ---
            Gdx.app.log("BotLog", "Textura disposada");
        }
    }
}
