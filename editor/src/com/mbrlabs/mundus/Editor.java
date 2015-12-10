package com.mbrlabs.mundus;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.mbrlabs.mundus.core.BrushManager;
import com.mbrlabs.mundus.core.Inject;
import com.mbrlabs.mundus.core.Mundus;
import com.mbrlabs.mundus.core.Shaders;
import com.mbrlabs.mundus.core.data.ProjectContext;
import com.mbrlabs.mundus.input.InputManager;
import com.mbrlabs.mundus.input.navigation.FreeCamController;
import com.mbrlabs.mundus.terrain.Terrain;
import com.mbrlabs.mundus.ui.Ui;
import com.mbrlabs.mundus.utils.*;

public class Editor implements ApplicationListener {

    // axes
    private RenderContext renderContext;

    private ModelInstance axesInstance;

    private Ui ui;
    @Inject
    private InputManager inputManager;
    @Inject
    private PerspectiveCamera cam;
    @Inject
    private ModelBatch batch;
    @Inject
    private BrushManager brushManager;
    @Inject
    private ProjectContext projectContext;
    @Inject
    private Compass compass;
    @Inject
    private Shaders shaders;


    private FreeCamController camController;

	@Override
	public void create () {
        Mundus.init();
        Mundus.inject(this);
        ui = Ui.getInstance();
        inputManager.addProcessor(ui);
        camController = new FreeCamController(cam);
        inputManager.addProcessor(camController);


        Model axesModel = UsefulMeshs.createAxes();
        axesInstance = new ModelInstance(axesModel);
        Mundus.testModels.add(axesModel);

        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.WEIGHTED, 1));

        createTestModels();
    }

	@Override
	public void render () {
        GlUtils.clearScreen(Colors.GRAY_222);

        ui.act();
        camController.update();
        brushManager.act();

        // update status bar
        ui.getStatusBar().setFps(Gdx.graphics.getFramesPerSecond());
        ui.getStatusBar().setVertexCount(0);

        // render model instances
       batch.begin(cam);
       batch.render(axesInstance);
       batch.render(projectContext.entities,
                projectContext.environment, shaders.entityShader);
        batch.render(Mundus.testInstances,
                projectContext.environment, shaders.entityShader);
        batch.end();

        // render terrains
        shaders.terrainShader.begin(cam, renderContext);
        for(Terrain terrain : projectContext.terrains) {
            terrain.renderable.environment = projectContext.environment;
            shaders.terrainShader.render(terrain.renderable);
        }
        shaders.terrainShader.end();

        // render active brush
        if(brushManager.getActiveBrush() != null) {
            brushManager.getActiveBrush().render(cam, batch);
        }

        // render compass
        compass.render(batch);

        // render UI
        ui.draw();
	}

    @Deprecated
    private void createTestModels() {
        // boxes to test terrain height
        if(projectContext.terrains.first() != null) {
            float boxSize = 0.5f;
            Model boxModel = new ModelBuilder().createBox(boxSize, boxSize,boxSize,
                    new Material(ColorAttribute.createDiffuse(Color.RED)),
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            Mundus.testModels.add(boxModel);
            Mundus.testInstances.addAll(TestUtils.createABunchOfModelsOnTheTerrain(1000,
                    boxModel, projectContext.terrains.first()));
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void resize(int width, int height) {
        ui.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        Mundus.dispose();
    }

}
