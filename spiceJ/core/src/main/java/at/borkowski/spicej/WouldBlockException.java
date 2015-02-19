package at.borkowski.spicej;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Thrown when an operation of an {@link InputStream} or {@link OutputStream}
 * that is in non-blocking mode would block.
 */
public class WouldBlockException extends RuntimeException {
   private static final long serialVersionUID = 4591580308060967901L;
}
