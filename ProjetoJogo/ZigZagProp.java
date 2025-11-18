package br.jogobike;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.MathUtils;

public class ZigZagProp {

    public float x, y;
    public float width, height;
    private float speed = 200f;
    private float amplitude = 10f;
    private float frequency = 2.7f;
    private float initialX;

    public Rectangle bounds;
    public Texture texture;

    public boolean isLava = false;

    private float time = 0;

    public ZigZagProp(Texture texture, float startX, float startY) {
        this.texture = texture;
        this.x = startX;
        this.y = startY;
        this.width = 75;
        this.height = 77;
        this.initialX = startX;

        bounds = new Rectangle(x, y, width, height);
    }

    public void update(float delta) {
        time += delta;

        // Movimento em zigue-zag
        x = initialX + MathUtils.sin(time * frequency) * amplitude;

        // Avanço pra frente da avalanche
        y -= speed * delta;

        // Atualiza hitbox
        bounds.set(x, y, width, height);
    }
}
