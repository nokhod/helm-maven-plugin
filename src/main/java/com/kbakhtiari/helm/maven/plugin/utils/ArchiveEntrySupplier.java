package com.kbakhtiari.helm.maven.plugin.utils;

import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;

import java.io.IOException;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ArchiveEntrySupplier implements Supplier<ArchiveEntry> {

  private final ArchiveInputStream is;
  private final org.apache.maven.plugin.logging.Log log;

  @Override
  public ArchiveEntry get() {
    try {
      final ArchiveEntry nextEntry = is.getNextEntry();
      log.debug("extracting the next entry in the archive: " + nextEntry.getName());
      return nextEntry;
    } catch (IOException e) {
      return null;
    }
  }
}
