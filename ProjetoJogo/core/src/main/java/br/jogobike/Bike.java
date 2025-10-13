package br.jogobike;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;

public class Bike {
    public Texture texture;
    public Rectangle rect;

    public Bike(String texturePath, float x, float y) {
        texture = new Texture(texturePath);
        rect = new Rectangle(x, y, texture.getWidth(), texture.getHeight());
    }

    public void updatePosition(float x, float y) {
        rect.setPosition(x, y);
    }

    public void dispose() {
        texture.dispose();
    }
}
