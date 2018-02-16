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

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * Created by Longri on 14.02.2018.
 */
public class PixmapPacker {

    private final static boolean ALLOW_FLIP = false;
    private final static boolean WRITE_DEBUG = false;

    private final boolean FORCE_POT;
    private final int MAX_TEXTURE_SIZE;
    private final Array<objStruct> list = new Array<>();

    private int count;

    public PixmapPacker(boolean force_pot, int maxTextureSize) {
        FORCE_POT = force_pot;
        this.MAX_TEXTURE_SIZE = maxTextureSize;
    }

    public void pack(String name, Pixmap pixmap) {
        list.add(new objStruct(count++, name, pixmap));
    }

    public TextureAtlas generateTextureAtlas(Texture.TextureFilter minFilter, Texture.TextureFilter magFilter, boolean useMipMaps) {

        //create short array for call native
        int recCount = list.size;
        short[] valueArray = new short[recCount * 7];

        int index = 0;
        for (int i = 0; i < recCount; i++) {
            objStruct obj = list.get(i);
            valueArray[index + 0] = (short) obj.index; // index
            valueArray[index + 1] = 0; // x
            valueArray[index + 2] = 0; // y
            valueArray[index + 3] = (short) obj.pixmap.getWidth(); // width
            valueArray[index + 4] = (short) obj.pixmap.getHeight(); // height
            valueArray[index + 5] = 0; // flipped
            valueArray[index + 6] = 0; // texture index
            index += 7;
        }



        int[] pages = NativePacker.packNative(valueArray, valueArray.length / 7, MAX_TEXTURE_SIZE, ALLOW_FLIP, WRITE_DEBUG);

        int pageCount = pages[0];


        Pixmap[] pixmaps = new Pixmap[pageCount];
        int idx = 1;
        for (int i = 0; i < pageCount; i++) {
            int pageWidth = pages[idx++];
            int pageHeight= pages[idx++];
            if (FORCE_POT) {
                pageWidth = MathUtils.nextPowerOfTwo(pageWidth);
                pageHeight = MathUtils.nextPowerOfTwo(pageHeight);
            }
            pixmaps[i] = new Pixmap(pageWidth, pageHeight, Pixmap.Format.RGBA8888);
        }

        //draw textures to pixmap pages
        index = 0;
        for (int i = 0; i < recCount; i++) {
            objStruct obj = list.get(i);
//            int textureIndex = valueArray[index + 0]; // index
            obj.x = valueArray[index + 1]; // x
            obj.y = valueArray[index + 2]; // y
//            int width = valueArray[index + 3]; // width
//            int height = valueArray[index + 4]; // height
//            boolean flipped = valueArray[index + 5] > 0; // flipped
            int pageIndex = valueArray[index + 6] = 0; // page index
            pixmaps[pageIndex].drawPixmap(obj.pixmap, obj.x, obj.y);
            obj.setTexturePageIndex(pageIndex);
            index += 7;
        }

        Texture[] textures = new Texture[pageCount];
        for (int i = 0; i < pageCount; i++) {
            textures[i] = new Texture(pixmaps[i], Pixmap.Format.RGBA8888,useMipMaps);
        }

        TextureAtlas atlas = new TextureAtlas();

        for (int i = 0; i < recCount; i++) {
            objStruct obj = list.get(i);
            atlas.addRegion(obj.name, textures[obj.texturePageIndex], obj.x, obj.y, obj.pixmap.getWidth(), obj.pixmap.getHeight());
        }
        return atlas;
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
}
