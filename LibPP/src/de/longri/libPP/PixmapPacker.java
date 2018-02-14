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
import com.badlogic.gdx.utils.Array;

/**
 * Created by Longri on 14.02.2018.
 */
public class PixmapPacker {

    private int count;
    private Array<objStruct> list = new Array<>();

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

        boolean allowFlip = false;
        boolean writeDebug = false;
        int maxtextureSize = 1024;

        int[] pages = packNative(valueArray, maxtextureSize, allowFlip, writeDebug);


        return null;
    }

    private native int[] packNative(short[] valueArray, int maxTextureSize, boolean allowFlip, boolean writeDebug);/*

            int[] ret= int[]{1,2,3};

            return ret;
    */


    private static class objStruct {
        final int index;
        final String name;
        final Pixmap pixmap;

        private objStruct(int index, String name, Pixmap pixmap) {
            this.index = index;
            this.name = name;
            this.pixmap = pixmap;
        }
    }
}
