package com.denniskubes.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import org.apache.commons.io.filefilter.DelegateFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;

public class FileIOUtils {

  /**
   * <p>Recursively collects files into the collector list starting at rootDir
   * and working files first and then directories in a depth first fashion.</p>
   * 
   * @param collector The List into which the collected files are stored.
   * @param rootDir The starting or child directory.
   * @param filter An optional filter used to match files.  If no filter is
   * specified all files.
   * @param collectDirs Should directories be collected along with files.
   */
  public static void collectFiles(List<File> collector, File rootDir,
    FileFilter filter, boolean collectDirs) {

    if (rootDir.exists() && rootDir.isDirectory() && rootDir.canRead()) {

      // list either files by filter or all the files
      File[] children = null;
      if (filter != null) {
        IOFileFilter dirFilter = DirectoryFileFilter.DIRECTORY;
        DelegateFileFilter delFilter = new DelegateFileFilter(filter);
        OrFileFilter dirOrDel = new OrFileFilter(dirFilter, delFilter);
        children = rootDir.listFiles((FileFilter)dirOrDel);
      }
      else {
        children = rootDir.listFiles();
      }

      if (children != null) {
        for (File child : children) {
          if (child.isFile() && child.canRead()) {
            collector.add(child);
          }
          else {
            if (collectDirs) {
              collector.add(child);
            }
            // recurse for directories
            collectFiles(collector, child, filter, collectDirs);
          }
        } // end child files
      }
    }
  }
  
  
}
