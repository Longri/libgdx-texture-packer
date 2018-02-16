/*
 * Copyright (C) 2017 team-cachebox.de
 *
 * Licensed under the : GNU General  License (GPL);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.longri.libPP;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import com.badlogic.gdx.utils.async.AsyncTask;

/**
 * Created by Longri on 18.12.2017.
 */
public class TestClass extends ApplicationAdapter {
    SpriteBatch batch;
    Texture img;
    TextureAtlas atlas;
    Array<TextureAtlas.AtlasRegion> regionArray;
    Texture texture;

    int state = -1;
    Color actBackColor = Color.BLUE;
    private static final AsyncExecutor asyncExecutor = new AsyncExecutor(50);


    @Override
    public void create() {
        batch = new SpriteBatch();
        img = new Texture("badlogic.jpg");


        postAsync(new Runnable() {
            @Override
            public void run() {

                //Load asset images
                final Array<Pixmap> assetPixmaps = new Array<>();
                final Array<String> assetNames = new Array<>();

                FileHandle assetPath = Gdx.files.internal("test");
                FileHandle files[] = assetPath.list("png");
                for (FileHandle file : files) {
                    Pixmap pix = new Pixmap(file);
                    String name = file.nameWithoutExtension();
                    assetPixmaps.add(pix);
                    assetNames.add(name);
                }


                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        PixmapPacker pixmapPacker = new PixmapPacker(true, 512,2);

                        for (int i = 0; i < assetPixmaps.size; i++) {
                            pixmapPacker.pack(assetNames.get(i), assetPixmaps.get(i));
                        }
                        atlas = pixmapPacker.generateTextureAtlas(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest, true);
                        regionArray = atlas.getRegions();
                        drawMax = regionArray.size;
                        texture = atlas.getTextures().first();
                    }
                });
            }
        });

    }


    int drawCount = 0;
    int drawMax;

    @Override
    public void render() {
        switch (state) {
            case 0:
                actBackColor = Color.YELLOW;
                break;
            case 1:
                actBackColor = Color.GREEN;
                break;
            case 2:
                actBackColor = Color.RED;
                break;
        }
        Gdx.gl.glClearColor(actBackColor.r, actBackColor.g, actBackColor.b, actBackColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();


        if (regionArray != null) {
            if (drawCount < drawMax) {
                TextureAtlas.AtlasRegion region = regionArray.get(drawCount++);
                batch.draw(region, 200, 200, region.getRegionWidth(), region.getRegionHeight());
            } else {
                float s = Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                batch.draw(texture, 0, 0, s, s);
            }
        } else {
            batch.draw(img, 0, 0);
        }

        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        img.dispose();
    }

    public static void postAsync(final Runnable runnable) {
        asyncExecutor.submit(new AsyncTask<Void>() {
            @Override
            public Void call() {
                try {
                    runnable.run();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }
}
