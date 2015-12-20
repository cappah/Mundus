package com.mbrlabs.mundus.terrain;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.utils.FloatArray;
import com.mbrlabs.mundus.utils.Log;
import com.mbrlabs.mundus.utils.TextureUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Exports the terrain height.
 * Everything else (e.g. normals) can be calculated at runtime.
 *
 * @author Marcus Brummer
 * @version 04-12-2015
 */
public class TerrainIO {

    public static final String FILE_EXTENSION = "terra";

    /**
     * Binary gziped format.
     *
     * @param terrain
     * @param path
     */
    public static void exportBinary(Terrain terrain, String path) {
        float[] data = terrain.heightData;
        long start = System.currentTimeMillis();
        try {
            File file = new File(path);
            FileUtils.touch(file);
            DataOutputStream outputStream = new DataOutputStream(
                    new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file))));
            for(float f : data) {
                outputStream.writeFloat(f);
            }
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.debug("Terrain export execution time (" + data.length + " floats): "
                + (System.currentTimeMillis() - start) + " ms");
    }

//    public static Terrain importTerrain(String path) {
//        FloatArray floatArray = new FloatArray();
//        try {
//            DataInputStream is = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(path))));
//            while (is.available() > 0) {
//                floatArray.add(is.readFloat());
//            }
//            is.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Log.debug("Terrain import. floats: " + floatArray.size);
//        Terrain terrain = new Terrain((int)Math.sqrt(floatArray.size));
//        terrain.heightData = floatArray.toArray();
//        terrain.update();
//        Texture tex = TextureUtils.loadMipmapTexture(Gdx.files.internal("textures/stone_hr.jpg"));
//        terrain.setTexture(tex);
//
//        return terrain;
//    }

    public static Terrain importTerrain(Terrain terrain, String path) {
        FloatArray floatArray = new FloatArray();
        try {
            DataInputStream is = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(path))));
            while (is.available() > 0) {
                floatArray.add(is.readFloat());
            }
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.debug("Terrain import. floats: " + floatArray.size);

        terrain.init();
        terrain.heightData = floatArray.toArray();
        terrain.update();
        Texture tex = TextureUtils.loadMipmapTexture(Gdx.files.internal("textures/stone_hr.jpg"));
        terrain.setTexture(tex);

        return terrain;
    }

}