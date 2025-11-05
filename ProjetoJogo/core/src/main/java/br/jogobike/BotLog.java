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
    private final float FADE_SPEED = 3.0f;

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

        if (isFadingOut) {
            alpha -= FADE_SPEED * delta;
            if (alpha <= 0) {
                isFadingOut = false;
                alpha = 1.0f;
                return true;
            }
            return false; 
        }

        float scaledBaseSpeed = BASE_SPEED * difficultyScalar;
        float speed = scaledBaseSpeed + speedOffset + (Math.abs(playerSpeedX) * 0.7f);

        float currentX = getX();
        float newX = currentX - speed * delta;

        updatePosition(newX, getY());

        if (newX + DEFAULT_WIDTH < avalancheWidth) {
            isFadingOut = true; 
        }
        return false;
    }

    public void updatePosition(float x, float y) {
        rect.setPosition(x + collisionOffsetX, y + collisionOffsetY);
    }


    public void respawn(float virtualWidth, float minSpawnDistance, float maxSpawnDistance, float trackBottom, float trackTop) {
        float newX = virtualWidth + MathUtils.random(minSpawnDistance, maxSpawnDistance);
        float newY = getRandomPositionInTrack(trackBottom, trackTop);
        this.speedOffset = MathUtils.random(MIN_SPEED_OFFSET, MAX_SPEED_OFFSET);
        updatePosition(newX, newY);
    }

    private float getRandomPositionInTrack(float trackBottom, float trackTop) {
        float effectiveTrackTop = trackTop - DEFAULT_HEIGHT;

        if (effectiveTrackTop <= trackBottom) {
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

    public float getAlpha() {
        return Math.max(0, alpha); 
    }

    private Texture createPlaceholderTexture() {
        if (MainGame.placeholderTexture != null) {
            return MainGame.placeholderTexture;
        }

        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(
            (int)DEFAULT_WIDTH, (int)DEFAULT_HEIGHT,
            com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888
        );
        pixmap.setColor(0.65f, 0.45f, 0.25f, 1f); 
        pixmap.fill();
        pixmap.setColor(0.55f, 0.35f, 0.15f, 1f); 
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
            Gdx.app.log("BotLog", "Textura disposada");
        }
    }
}

