/*
 * Copyright (c) 2016. See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mbrlabs.mundus.commons.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marcus Brummer
 * @version 06-10-2016
 */
public class AssetManager implements Disposable {

    protected FileHandle rootFolder;

    protected Array<Asset> assets;
    protected Map<String, Asset> assetIndex;

    /**
     * Asset manager constructor.
     * @param assetsFolder
     */
    public AssetManager(FileHandle assetsFolder) {
        this.rootFolder = assetsFolder;
        this.assets = new Array<Asset>();
        this.assetIndex = new HashMap<String, Asset>();
    }

    public Asset findAssetByID(String id) {
        return assetIndex.get(id);
    }

    public Array<Asset> getAssets() {
        return assets;
    }

    public Array<ModelAsset> getModelAssets() {
        Array<ModelAsset> models = new Array<ModelAsset>();
        for(Asset asset : assets) {
            if(asset instanceof ModelAsset) {
                models.add((ModelAsset) asset);
            }
        }

        return models;
    }

    /**
     * Loads all imported assets in the project's asset folder.
     */
    public void loadAssets() throws AssetNotFoundException, MetaFileParseException {
        // create meta file filter
        FileFilter metaFileFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(MetaFile.META_EXTENSION);
            }
        };

        // load assets
        for(FileHandle meta : rootFolder.list(metaFileFilter)) {
            loadAsset(new MetaFile(meta));
        }

        // resolve dependencies
        for(Asset asset : assets) {
            // model asset
            if(asset instanceof ModelAsset) {
                String diffuseTexture = asset.getMeta().getDiffuseTexture();
                if(diffuseTexture != null) {
                    TextureAsset tex = (TextureAsset) findAssetByID(diffuseTexture);
                    if(tex != null) {
                        // Log.error(TAG, diffuseTexture);
                        ((ModelAsset) asset).setDiffuseTexture(tex);
                    }
                }
            }
        }
    }

    public Asset loadAsset(MetaFile meta) throws MetaFileParseException, AssetNotFoundException {
        // get handle to asset
        String assetPath = meta.getFile().pathWithoutExtension();
        FileHandle assetFile = new FileHandle(assetPath);

        // check if asset exists
        if(!assetFile.exists()) {
            throw new AssetNotFoundException("Meta file found, but asset does not exist: " + meta.getFile().path());
        }

        meta.load();

        // load actual asset
        Asset asset = null;
        switch (meta.getType()) {
            case TEXTURE:
                asset = loadTextureAsset(meta, assetFile);
                break;
            case PIXMAP_TEXTURE:
                asset = loadPixmapTextureAsset(meta, assetFile);
                break;
            case TERRA:
                asset = loadTerraAsset(meta, assetFile);
                break;
            case MODEL:
                asset = loadModelAsset(meta, assetFile);
                break;
            default:
                return null;
        }

        // add to list
        if(asset != null) {
            assets.add(asset);
            assetIndex.put(asset.getUUID(), asset);
        }

        return asset;
    }

    private TextureAsset loadTextureAsset(MetaFile meta, FileHandle assetFile) {
        TextureAsset asset = new TextureAsset(meta, assetFile);
        asset.setTileable(true);
        asset.generateMipmaps(true);
        asset.load();
        return asset;
    }

    private TerraAsset loadTerraAsset(MetaFile meta, FileHandle assetFile) {
        TerraAsset asset = new TerraAsset(meta, assetFile);
        asset.load();
        return asset;
    }

    private PixmapTextureAsset loadPixmapTextureAsset(MetaFile meta, FileHandle assetFile) {
        PixmapTextureAsset asset = new PixmapTextureAsset(meta, assetFile);
        asset.load();
        return asset;
    }

    private ModelAsset loadModelAsset(MetaFile meta, FileHandle assetFile) {
        ModelAsset asset = new ModelAsset(meta, assetFile);
        asset.load();
        return asset;
    }

    @Override
    public void dispose() {
        for(Asset asset : assets) {
            asset.dispose();
        }
    }

}