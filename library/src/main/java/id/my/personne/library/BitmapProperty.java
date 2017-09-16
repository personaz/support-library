package id.my.personne.library;

import android.graphics.Bitmap;
import android.webkit.MimeTypeMap;

import java.util.Locale;

/**
 * Created by surya on 9/16/17.
 */

public class BitmapProperty {
    private String filename;
    private Bitmap bitmap;
    private String extension;
    private static final String MIME_IMAGE_JPEG = "image/jpeg";
    private static final String EXT_JPEG = ".jpeg";

    public BitmapProperty(String filename, Bitmap bitmap) {
        this.filename = filename;
        this.bitmap = bitmap;
    }

    public String getFilename() {
        return filename;
    }

    /**
     * filename with extension if null, default is jpeg
     * @param filename String filename
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    /**
     * extension need dot at first extension, see StringLibrary for sample
     * @param stringLibraryExt extension(.jpeg | .png)
     */
    public void setExtension(String stringLibraryExt) {
        this.extension = stringLibraryExt;
    }

    public String getMimeType() {
        try {
            String fileExt = extension;
            if (extension == null && filename != null && filename.contains(".")) {
                fileExt = filename.substring(filename.lastIndexOf("."));
            }
            if (fileExt == null || fileExt.trim().isEmpty()) {
                fileExt = EXT_JPEG;
            }
            String mimeTypeMap = MimeTypeMap.getFileExtensionFromUrl(fileExt.trim());
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(mimeTypeMap.toLowerCase(Locale.US));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return MIME_IMAGE_JPEG;
    }

    public Bitmap.CompressFormat getCompressFormat() {
        Bitmap.CompressFormat compress = Bitmap.CompressFormat.PNG;
        switch (getMimeType()) {
            case MIME_IMAGE_JPEG:
                compress = Bitmap.CompressFormat.JPEG;
                break;
        }
        return compress;
    }
}
