package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.maps.objects.RectangleMapObject;

public class PlatformTiled {
    private RectangleMapObject platform1;
    private RectangleMapObject platform2;


    public PlatformTiled(RectangleMapObject platform1, RectangleMapObject platform2) {
        this.platform1 = platform1;
        this.platform2 = platform2;
    }

}
