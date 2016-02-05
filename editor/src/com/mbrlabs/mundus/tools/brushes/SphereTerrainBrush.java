/*
 * Copyright (c) 2015. See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mbrlabs.mundus.tools.brushes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.mbrlabs.mundus.commons.terrain.SplatMap;
import com.mbrlabs.mundus.commons.utils.MathUtils;
import com.mbrlabs.mundus.core.project.ProjectContext;
import com.mbrlabs.mundus.utils.Fa;

/**
 * @author Marcus Brummer
 * @version 25-12-2015
 */
public class SphereTerrainBrush extends TerrainBrush {

    private static final String NAME = "Sphere Brush";

    private Model sphereModel;
    private ModelInstance sphereModelInstance;
    private BoundingBox boundingBox = new BoundingBox();
    private int lastMousePosIndicator = 0;

    private Pixmap brushPixmap;
    private Color c0 = new Color();

    private Vector2 c = new Vector2();
    private Vector2 p = new Vector2();
    private Vector2 v = new Vector2();


    protected Vector3 tVec0 = new Vector3();

    private ModelInstance mi;
    private Model mod;

    public SphereTerrainBrush(ProjectContext projectContext, Shader shader, ModelBatch modelBatch) {
        super(projectContext, shader, modelBatch);
        ModelBuilder modelBuilder = new ModelBuilder();
        sphereModel = modelBuilder.createSphere(1, 1, 1, 30, 30, new Material(), VertexAttributes.Usage.Position);
        sphereModelInstance = new ModelInstance(sphereModel);
        sphereModelInstance.calculateBoundingBox(boundingBox);
        scale(15);

        //brushPixmap = new Pixmap(Gdx.files.internal("brushes/brush_circle_smooth.png"));
        brushPixmap = new Pixmap(Gdx.files.internal("brushes/star.png"));

        mod = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(Color.GREEN)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        mi = new ModelInstance(mod);

    }

    @Override
    public boolean supportsMode(BrushMode mode) {
        switch (mode) {
            case RAISE_LOWER:
            case FLATTEN:
            case PAINT: return true;
        }

        return false;
    }

    @Override
    public void render() {
        sphereModelInstance.transform.getTranslation(tVec0);
        mi.transform.setToTranslation(tVec0);

        if(terrain.isOnTerrain(brushPos.x, brushPos.z)) {
            batch.begin(projectContext.currScene.cam);
            batch.render(sphereModelInstance, shader);
            batch.render(mi);
            batch.end();
        }
    }

    @Override
    public void scale(float amount) {
        sphereModelInstance.transform.scl(amount);
        radius = (boundingBox.getWidth()*sphereModelInstance.transform.getScaleX()) / 2f;
    }

    @Override
    public void act() {
        BrushAction action = getAction();
        if(action == null) return;
        if(terrain == null) return;

        // sample height
        if(action == BrushAction.SECONDARY && mode == BrushMode.FLATTEN) {
            heightSample = brushPos.y;
            return;
        }

        // only act if mouse has been moved
        if(lastMousePosIndicator == Gdx.input.getX() + Gdx.input.getY()) return;

        // Paint
        if(mode == BrushMode.PAINT) {
            SplatMap sm = terrain.getTerrainTexture().getSplatmap();
            if(sm != null) {
                final float splatX = ((brushPos.x - terrain.getPosition().x) / (float) terrain.terrainWidth) * sm.getWidth();
                final float splatY = ((brushPos.z - terrain.getPosition().z) / (float) terrain.terrainDepth) * sm.getHeight();
                final float splatRad = (radius / terrain.terrainWidth) * sm.getWidth();
                sm.drawCircle((int) splatX, (int) splatY, (int) splatRad, paintStrength, paintChannel);
                sm.updateTexture();
            }
            return;
        }

        final Vector3 terPos = terrain.getPosition();
        float dir = (action == BrushAction.PRIMARY) ? 1 : -1;

        for (int x = 0; x < terrain.vertexResolution; x++) {
            for (int z = 0; z <  terrain.vertexResolution; z++) {
                final Vector3 vertexPos = terrain.getVertexPosition(tVec0, x, z);
                vertexPos.x += terPos.x;
                vertexPos.z += terPos.z;
                float distance = vertexPos.dst(brushPos);

                if(distance <= radius) {
                    final int heightIndex = z * terrain.vertexResolution + x;
                    // Raise/Lower
                    if(mode == BrushMode.RAISE_LOWER) {

                        float elevation = getValueOfBrushPixmap(brushPos, vertexPos.x, vertexPos.z);
                        terrain.heightData[heightIndex] += elevation;
                        // Flatten
                    } else if(mode == BrushMode.FLATTEN) {
                        terrain.heightData[heightIndex] = heightSample;
                    }
                }
            }
        }

        if(mode == BrushMode.RAISE_LOWER || mode == BrushMode.FLATTEN || mode == BrushMode.SMOOTH) {
            terrain.update();
        }
    }

    private float getValueOfBrushPixmap(Vector3 brushPosition, float vertexX, float vertexZ) {
        int pixmapCenter = brushPixmap.getWidth() / 2;

        c.set(brushPosition.x, brushPosition.z);
        p.set(vertexX, vertexZ);
        v = p.sub(c);

        float len = v.len();
        float progress = len / radius;
        v.nor().scl(pixmapCenter * progress);

        float mapX = pixmapCenter + (int) v.x;
        float mapY = pixmapCenter + (int) v.y;

        c0.set(brushPixmap.getPixel((int)mapX, (int)mapY));

        return c0.r;
    }

    @Override
    public void dispose() {
        sphereModel.dispose();
    }

    @Override
    public boolean scrolled(int amount) {
        if(amount < 0) {
            scale(0.9f);
        } else {
            scale(1.1f);
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        super.mouseMoved(screenX, screenY);
        lastMousePosIndicator = screenX + screenY;

        sphereModelInstance.transform.setTranslation(brushPos);
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return mouseMoved(screenX, screenY);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Drawable getIcon() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getIconFont() {
        return Fa.CIRCLE_O;
    }

    @Override
    public void reset() {

    }

}
