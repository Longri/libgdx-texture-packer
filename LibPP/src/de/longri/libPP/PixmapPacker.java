/*
 * Copyright (C) 2018 team-cachebox.de
 *
 * Licensed under the : GNU General Public License (GPL);
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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;

import java.io.IOException;
import java.io.Writer;
import java.nio.IntBuffer;

/**
 * Created by Longri on 14.02.2018.
 */
public class PixmapPacker {

    private final static boolean ALLOW_FLIP = false;
    private final static boolean WRITE_DEBUG = false;

    private final boolean FORCE_POT;
    private final int MAX_TEXTURE_SIZE, PADDING, PADDING2X;
    private final Array<objStruct> list = new Array<>();
    private Pixmap[] pages;
    Texture.TextureFilter minFilter;
    Texture.TextureFilter magFilter;

    private int count;

    public PixmapPacker(boolean force_pot, int maxTextureSize, int padding) {
        FORCE_POT = force_pot;
        MAX_TEXTURE_SIZE = maxTextureSize;
        PADDING = padding;
        PADDING2X = PADDING * 2;
    }

    public void pack(String name, Pixmap pixmap) {
        list.add(new objStruct(count++, name, pixmap));
    }

    public TextureAtlas generateTextureAtlas(Texture.TextureFilter minFilter, Texture.TextureFilter magFilter, boolean useMipMaps) {

        this.minFilter = minFilter;
        this.magFilter = magFilter;

        //create short array for call native
        int recCount = list.size;
        short[] valueArray = new short[recCount * 7];

        int index = 0;
        for (int i = 0; i < recCount; i++) {
            objStruct obj = list.get(i);
            valueArray[index + 0] = (short) obj.index; // index
            valueArray[index + 1] = 0; // x
            valueArray[index + 2] = 0; // y
            valueArray[index + 3] = (short) (obj.pixmap.getWidth() + PADDING2X); // width
            valueArray[index + 4] = (short) (obj.pixmap.getHeight() + PADDING2X); // height
            valueArray[index + 5] = 0; // flipped
            valueArray[index + 6] = 0; // texture index
            index += 7;
        }


        int[] pages = NativePacker.packNative(valueArray, valueArray.length / 7, MAX_TEXTURE_SIZE, ALLOW_FLIP, WRITE_DEBUG);

        int pageCount = pages[0];


        this.pages = new Pixmap[pageCount];
        int idx = 1;
        for (int i = 0; i < pageCount; i++) {
            int pageWidth = pages[idx++];
            int pageHeight = pages[idx++];
            if (FORCE_POT) {
                pageWidth = MathUtils.nextPowerOfTwo(pageWidth);
                pageHeight = MathUtils.nextPowerOfTwo(pageHeight);
            }
            this.pages[i] = new Pixmap(pageWidth, pageHeight, Pixmap.Format.RGBA8888);
        }

        //draw textures to pixmap pages
        index = 0;
        for (int i = 0; i < recCount; i++) {
            objStruct obj = list.get(i);
//            int textureIndex = valueArray[index + 0]; // index
            obj.x = valueArray[index + 1] + PADDING; // x
            obj.y = valueArray[index + 2] + PADDING; // y
//            int width = valueArray[index + 3]; // width
//            int height = valueArray[index + 4]; // height
//            boolean flipped = valueArray[index + 5] > 0; // flipped
            int pageIndex = valueArray[index + 6]; // page index
            this.pages[pageIndex].drawPixmap(obj.pixmap, obj.x, obj.y);
            obj.setTexturePageIndex(pageIndex);
            index += 7;
        }

        Texture[] textures = new Texture[pageCount];
        for (int i = 0; i < pageCount; i++) {
            textures[i] = new Texture(this.pages[i], Pixmap.Format.RGBA8888, useMipMaps);
            textures[i].setFilter(minFilter, magFilter);
        }

        TextureAtlas atlas = new TextureAtlas();

        for (int i = 0; i < recCount; i++) {
            objStruct obj = list.get(i);
            atlas.addRegion(obj.name, textures[obj.texturePageIndex], obj.x, obj.y, obj.pixmap.getWidth(), obj.pixmap.getHeight());
        }
        return atlas;
    }

    public void save(FileHandle file) throws IOException {

        if (pages == null) throw new RuntimeException("Atlas not created! Call create first");

        Writer writer = file.writer(false);
        int index = -1;
        for (Pixmap page : pages) {
            {
                FileHandle pageFile = file.sibling(file.nameWithoutExtension() + "_" + (++index) + ".png");
                PixmapIO.writePNG(pageFile, page);
                writer.write("\n");
                writer.write(pageFile.name() + "\n");
                writer.write("size: " + page.getWidth() + "," + page.getHeight() + "\n");
                writer.write("format: " + Pixmap.Format.RGBA8888.name() + "\n");
                writer.write("filter: " + minFilter.name() + "," + magFilter.name() + "\n");
                writer.write("repeat: none" + "\n");
                for (int i = 0, n = list.size; i < n; i++) {
                    objStruct obj = list.get(i);
                    if (obj.texturePageIndex == index) {
                        writer.write(obj.name + "\n");
                        writer.write("rotate: false" + "\n");
                        writer.write("xy: " + obj.x + "," + obj.y + "\n");
                        writer.write("size: " + obj.pixmap.getWidth() + "," + obj.pixmap.getHeight() + "\n");
                        writer.write("orig: " + obj.pixmap.getWidth() + "," + obj.pixmap.getHeight() + "\n");
                        writer.write("offset: 0, 0" + "\n");
                        writer.write("index: -1" + "\n");
                    }
                }
            }
        }
        writer.close();
    }

    private static class objStruct {
        final int index;
        final String name;
        final Pixmap pixmap;
        private int texturePageIndex;
        int x, y;

        private objStruct(int index, String name, Pixmap pixmap) {
            this.index = index;
            this.name = name;
            this.pixmap = pixmap;
        }

        public void setTexturePageIndex(int texturePageIndex) {
            this.texturePageIndex = texturePageIndex;
        }
    }


    static int maxTextureSize = -1;

    public static int getDeviceMaxGlTextureSize() {
        if (maxTextureSize > -1) {
            return maxTextureSize;
        }

        IntBuffer max = BufferUtils.newIntBuffer(16);
        Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, max);

        maxTextureSize = max.get(0);
        return maxTextureSize;
    }
}
