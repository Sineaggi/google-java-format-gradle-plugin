package com.github.sherter.googlejavaformatgradleplugin;

import com.github.sherter.googlejavaformatgradleplugin.format.Formatter;
import com.github.sherter.googlejavaformatgradleplugin.format.FormatterException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

class FormatFileCallable implements Callable<FileInfo> {

  private static final Logger logger = Logging.getLogger(FormatFileCallable.class);

  private final Formatter formatter;
  private final Path file;

  FormatFileCallable(Formatter formatter, Path file) {
    this.file = Objects.requireNonNull(file);
    this.formatter = Objects.requireNonNull(formatter);
  }

  /**
   * Returns a {@link FileInfo} object that describes the state of the file after trying to format
   * it (in place). The {@link FileInfo#state() state} will either be {@link FileState#FORMATTED} or
   * {@link FileState#INVALID}, if Java syntax errors where found.
   *
   * @throws Exception if and only if file system access fails at some point
   */
  @Override
  public FileInfo call() throws PathException {
    try {
      byte[] content = Files.readAllBytes(file);
      String utf8Decoded = new String(content, StandardCharsets.UTF_8.name());
      String formatted;
      try {
        formatted = formatter.format(utf8Decoded);
      } catch (FormatterException e) {
        String error = e.getMessage();
        return FileInfo.create(
            file, Files.getLastModifiedTime(file), content.length, FileState.INVALID, error);
      }
      if (utf8Decoded.equals(formatted)) {
        logger.info("{}: UP-TO-DATE", file);
        return FileInfo.create(
            file, Files.getLastModifiedTime(file), content.length, FileState.FORMATTED);
      }
      byte[] utf8Encoded = formatted.getBytes(StandardCharsets.UTF_8.name());
      Files.write(file, utf8Encoded);
      logger.lifecycle("{}: formatted successfully", file);
      return FileInfo.create(
          file, Files.getLastModifiedTime(file), utf8Encoded.length, FileState.FORMATTED);
    } catch (Throwable t) {
      throw new PathException(file, t);
    }
  }
}
