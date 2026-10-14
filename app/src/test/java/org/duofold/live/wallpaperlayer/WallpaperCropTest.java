package org.duofold.live.wallpaperlayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class WallpaperCropTest {
 @Test public void legacyCenterCropIsPreserved(){assertArrayEquals(new int[]{500,0,1500,2000},WallpaperCrop.rect(2000,2000,1000,2000,1,.5f,.5f));}
 @Test public void positioningReachesBothEdgesWithoutBlankPixels(){assertArrayEquals(new int[]{0,0,500,1000},WallpaperCrop.rect(2000,2000,1000,2000,2,0,0));assertArrayEquals(new int[]{1500,1000,2000,2000},WallpaperCrop.rect(2000,2000,1000,2000,2,1,1));}
 @Test public void corruptTransformsRemainSafe(){int[] r=WallpaperCrop.rect(2000,2000,1000,2000,Float.NaN,Float.POSITIVE_INFINITY,-8);assertTrue(r[0]>=0&&r[1]>=0&&r[2]<=2000&&r[3]<=2000);assertTrue(r[2]>r[0]&&r[3]>r[1]);}
 @Test public void rotationDoesNotChangePanelKind(){assertTrue(WallpaperCrop.cover(1248,1972));assertTrue(WallpaperCrop.cover(1972,1248));assertFalse(WallpaperCrop.cover(2448,1848));assertFalse(WallpaperCrop.cover(1848,2448));}
 @Test public void extremeSourceStillHasNonemptyCrop(){int[] r=WallpaperCrop.rect(1,4096,2448,1848,4,1,1);assertEquals(1,r[2]-r[0]);assertTrue(r[3]>r[1]);}
}
