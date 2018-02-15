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

/**
 * Created by Longri on 15.02.2018.
 */
public class NativePacker {

    public static native int[] packNative(short[] valueArray, int maxTextureSize, boolean allowFlip, boolean writeDebug); /*
    jintArray result;
    int size =3;
    result = (env)->NewIntArray( size);
    if (result == NULL) {
        return NULL;
    }
    int i;
    // fill a temp structure to use to populate the java int array
    jint fill[size];
        for (i = 0; i < size; i++) {
        fill[i] = i; // put whatever logic you want to populate the values here.
        }
        // move from the temp structure to the java structure
        (env)->SetIntArrayRegion(result, 0, size, fill);
        return result;

    */

}
