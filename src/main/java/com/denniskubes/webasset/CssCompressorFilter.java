package com.denniskubes.webasset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.platform.yui.compressor.CssCompressor;

public class CssCompressorFilter
  implements WebAssetFilter {

  private final static Logger LOG = LoggerFactory
    .getLogger(CssCompressorFilter.class);

  private String charset = "UTF-8";
  private int lineBreakPos = -1;

  @Override
  public File filterAsset(File input, Map<String, String> fieldMap) {

    String inputPath = input.getPath();
    String basename = FilenameUtils.getBaseName(inputPath);
    File output = new File(input.getParent(), basename + ".min.css");

    // don't minify files that are named something.min.(js|css), anything
    // with the min extension is assumed to already be minified, don't
    // want to do it twice
    boolean alreadyMinified = StringUtils.contains(basename, "min.")
      || StringUtils.contains(basename, "min-");

    // if we are minifiying try to compress the file, otherwise we are
    // just copying the original file
    if (!alreadyMinified) {

      try {

        FileInputStream fis = new FileInputStream(input);
        Reader reader = new InputStreamReader(fis, charset);
        CssCompressor compressor = new CssCompressor(reader);
        reader.close();
        fis.close();

        FileOutputStream fos = new FileOutputStream(output);
        Writer writer = new OutputStreamWriter(fos, charset);
        compressor.compress(writer, lineBreakPos);
        writer.flush();
        writer.close();
        fos.close();

        return output;
      }
      catch (Exception e) {
        // log the error and return the original input file
        LOG.error("Error compressing stylesheet:" + inputPath, e);
      }
    }

    return input;
  }

  public String getCharset() {
    return charset;
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }

  public int getLineBreakPos() {
    return lineBreakPos;
  }

  public void setLineBreakPos(int lineBreakPos) {
    this.lineBreakPos = lineBreakPos;
  }
}
