package net.gini.android.capture;

import android.content.Context;

import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.internal.camera.photo.PhotoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;

/**
 * <p>
 * This class allows you to enable and disable debugging for the Gini Capture SDK.
 * </p>
 * <p>
 * Debugging is disabled by default.
 * </p>
 * <p>
 * <b>Warning:</b> Don't forget to disable debugging before releasing.
 * </p>
 * <p>
 *     If debug is enabled:
 *     <ul>
 *         <li>
 *          The reviewed jpegs are written to a folder called {@code ginicapturesdk} in your app's external directory.
 *         </li>
 *     </ul>
 * </p>
 */
public final class GiniCaptureDebug {

    private static final Logger LOG = LoggerFactory.getLogger(GiniCaptureDebug.class);

    private static boolean sEnabled;

    /**
     * <p>
     *     Enables debugging for the Gini Capture SDK.
     * </p>
     */
    public static void enable() {
        sEnabled = true;
    }

    /**
     * <p>
     *     Disables debugging for the Gini Capture SDK.
     * </p>
     */
    public static void disable() {
        sEnabled = false;
    }

    /**
     * <p>
     *     Helper for writing a document to file. Has no effect if debugging is disabled.
     * </p>
     * <p>
     *     The filename consists of a timestamp concatenated with the suffix. Ex.: if suffix is "_original" then {@code 1469541253_original.jpeg}
     * </p>
     * <p>
     *     Destination directory is {@code ginicapturesdk} inside your apps external files directory: {@code /sdcard/Android/data/your.app.id/files/ginicapturesdk/}
     * </p>
     */
    public static void writeDocumentToFile(
            final Context context, final Document document, final String suffix) {
        if (!sEnabled) {
            return;
        }
        if (document instanceof ImageDocument) {
            writeImageDocumentToFile(context, (ImageDocument) document, suffix);
        }
    }

    /**
     * <p>
     *     Helper for writing a document to file. Has no effect if debugging is disabled.
     * </p>
     * <p>
     *     The filename consists of a timestamp concatenated with the suffix. Ex.: if suffix is "_original" then {@code 1469541253_original.jpeg}
     * </p>
     * <p>
     *     Destination directory is {@code ginicapturesdk} inside your apps external files directory: {@code /sdcard/Android/data/your.app.id/files/ginicapturesdk/}
     * </p>
     */
    private static void writeImageDocumentToFile(final Context context,
            final ImageDocument document, final String suffix) {
        if (!sEnabled) {
            return;
        }
        final long time = new Date().getTime();
        final String jpegFilename = time + suffix + ".jpeg";
        final File giniCaptureDir = createGiniCaptureDir(context);
        if (giniCaptureDir == null) {
            LOG.error("Could not write document to file {}", jpegFilename);
            return;
        }
        final File jpegFile = new File(giniCaptureDir, jpegFilename);
        PhotoFactory.newPhotoFromDocument((ImageDocument) document).saveToFile(jpegFile);
        LOG.debug("Document written to {}", jpegFile.getAbsolutePath());
    }

    private static File createGiniCaptureDir(final Context context) {
        final File externalFilesDir = context.getExternalFilesDir(null);
        final File ginicaptureDir = new File(externalFilesDir, "ginicapturesdk");
        if (ginicaptureDir.exists() || ginicaptureDir.mkdir()) {
            return ginicaptureDir;
        }
        return null;
    }

    private GiniCaptureDebug() {

    }
}
