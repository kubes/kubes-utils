package com.denniskubes.ecstatic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.NoSuchMessageException;

import com.denniskubes.utils.FileIOUtils;

/**
 * A web asset manager that handles transparent caching and reloading of web 
 * assets and their configurations.
 */
public class WebAssetManager
  implements MessageSourceAware {

  private final static Logger LOG =
    LoggerFactory.getLogger(WebAssetManager.class);
  public final static String GLOBAL = "_global_";

  private MessageSource messageSource;

  private AtomicBoolean active = new AtomicBoolean(false);
  private WebAssetParser webAssetParser = new WebAssetParser();
  private Map<String, WebAssetConfig> idToConfig =
    new HashMap<String, WebAssetConfig>();
  private String configDirectory;
  private String rootDirectory;
  private String cacheDirectory = "_ecstatic_cache_";
  private String configFileSuffix = ".ecf";
  private Pattern tagPattern = Pattern.compile("(\\$\\{.*?\\})");
  private boolean removeExistingCacheFiles = false;
  private boolean removeTempResources = true;
  private boolean removeCacheDirOnShutdown = true;

  // config file change monitoring
  private long reloadCheckInterval = 10000;
  private ConfigFileMonitor configFileMonitor;
  private Map<String, Long> configLastModTimes =
    new ConcurrentHashMap<String, Long>();

  // filters
  private Map<String, WebAssetFilter> filters =
    new HashMap<String, WebAssetFilter>();
  private Map<String, String[]> typeToFilters = new HashMap<String, String[]>();

  // caches
  private boolean caching = false;
  private Map<String, String> aliasesCache =
    new ConcurrentHashMap<String, String>();
  private Map<String, List> scriptsCache =
    new ConcurrentHashMap<String, List>();
  private Map<String, List> metaCache = new ConcurrentHashMap<String, List>();
  private Map<String, List> linksCache = new ConcurrentHashMap<String, List>();
  private Map<String, String> titleCache =
    new ConcurrentHashMap<String, String>();
  private Map<String, Long> assetLastModTimes =
    new ConcurrentHashMap<String, Long>();
  private Map<String, String> pathsCache =
    new ConcurrentHashMap<String, String>();

  /**
   * A continuous looping thread that polls the file system for changes to all
   * configuration files and reloads if changes are found.
   */
  private class ConfigFileMonitor
    extends Thread {

    @Override
    public void run() {

      // holder for any files that were monitored and have not been removed
      // from the filesystem and need to be removed from monitoring
      List<String> removed = new ArrayList<String>();

      while (active.get()) {

        // loop through the files checking for updated modified times
        for (Entry<String, Long> fileModEntry : configLastModTimes.entrySet()) {

          // get the filename and last modified time
          String filePath = fileModEntry.getKey();
          long fileLastMod = fileModEntry.getValue();

          // if the file exists on the file system and it has been modified
          File configFile = new File(filePath);
          if (configFile.exists() && (configFile.lastModified() > fileLastMod)) {
            loadAssetConfigFile(configFile);
          }
          else if (!configFile.exists()) {
            // the file existed previously but has now been removed
            removed.add(filePath);
          }
        }

        // remove any assets from monitoring that have been removed from the
        // filesystem, no need to keep polling for files that aren't there
        // TODO: more fine grained removal from caches
        for (String assetOrConfig : removed) {
          configLastModTimes.remove(assetOrConfig);
        }
        removed.clear();

        // sleep and then do it all over again
        try {
          Thread.sleep(reloadCheckInterval);
        }
        catch (InterruptedException e) {
          // continue if interrupted
        }
      }
    }
  }

  /**
   * Parses and loads a single web asset configuration file.
   * 
   * @param configFile The web asset configuration file to load.
   */
  private void loadAssetConfigFile(File configFile) {

    try {

      WebAssetConfig assetConfig = webAssetParser.parseConfig(configFile);
      if (assetConfig != null) {
        if (assetConfig.isGlobalConfig()) {

          // if global load the aliases, used for resolution at request time
          Map<String, String> aliases = assetConfig.getAliases();
          if (aliases.size() > 0) {
            aliasesCache.clear();
            aliasesCache.putAll(aliases);
          }
          idToConfig.put(GLOBAL, assetConfig);
        }
        else {
          // connect id to config
          for (String id : assetConfig.getIds()) {
            idToConfig.put(id, assetConfig);
          }
        }

        // set the last modified time to allow change detection
        configLastModTimes.put(configFile.getPath(), configFile.lastModified());
      }
    }
    catch (IOException e) {
      // bad parsing, log and ignore file
      LOG.warn("Error loading web asset config: " + configFile.getPath(), e);
    }
  }

  /**
   * Replaces any aliases or messages in the input with their replacement values.
   * 
   * @param input The input to check for aliases and messages.
   * @param Locale The current locale.
   * 
   * @return The interpolated alias value.
   */
  private String resolve(String input, Locale locale) {

    if (StringUtils.isNotBlank(input) && StringUtils.contains(input, "${")
      && StringUtils.contains(input, "}")) {

      StringBuffer buffer = new StringBuffer();
      Matcher tagMatcher = tagPattern.matcher(input);
      while (tagMatcher.find()) {
        String tag = tagMatcher.group();
        String code = StringUtils.removeStart(tag, "${");
        code = StringUtils.removeEnd(code, "}");
        if (aliasesCache.containsKey(code)) {
          tagMatcher.appendReplacement(buffer, aliasesCache.get(code));
        }
        else {
          String message;
          try {
            message = messageSource.getMessage(code, null, locale);
            tagMatcher.appendReplacement(buffer, message);
          }
          catch (NoSuchMessageException e) {
            tagMatcher.appendReplacement(buffer, Matcher.quoteReplacement(tag));
          }
        }
      }
      tagMatcher.appendTail(buffer);
      return buffer.toString();
    }

    // no replacement tags return the original input
    return input;
  }

  /**
   * Replaces all aliases for values in the input Map and returns a new Map with
   * the key to interpolated alias values or original values if no alias exists.
   * 
   * @param keyVals The Map of keys and values to check and replace aliases.
   * @param Locale The current locale.
   * 
   * @return A new Map of keys to interpolated alias values or original values
   * if the value didn't contain an alias.
   */
  private Map<String, String> resolveAll(Map<String, String> keyVals,
    Locale locale) {

    // loop through the key value map checking for aliases, replacing any
    // aliases in values if they are found
    Map<String, String> replaced = new LinkedHashMap<String, String>();
    for (Entry<String, String> keyVal : keyVals.entrySet()) {
      String value = resolve(keyVal.getValue(), locale);
      replaced.put(keyVal.getKey(), value);
    }
    return replaced;
  }

  /**
   * Filter and cache asset source files. Allows scripts and style sheets to be
   * changed on the fly and new versions to have new names and be loaded
   * immediately by users.  Any number of filters can be run on scripts and 
   * style sheets to produce the final output.
   * 
   * @param fieldMap The asset key values Map.
   */
  private boolean filterAndCache(Map<String, String> attributes) {

    // setup the cache directory
    File cacheRoot = new File(rootDirectory, cacheDirectory);

    // get the asset path, for example /WEB-INF/js/script.js, ignore if no path
    String assetPath = attributes.get("path");
    if (assetPath == null) {
      return false;
    }

    // get the raw file from the asset path, ignore if the file doesn't exist
    File assetFile = new File(rootDirectory, assetPath);
    if (assetFile.exists()) {

      // if the file hasn't been changed since the last time it was filtered
      // and cached, just return the cached path, don't reprocess. This also
      // lets it keep any other attributes that may be different across configs
      // using the same file
      long assetLastModified = assetFile.lastModified();
      if (assetLastModTimes.containsKey(assetPath)
        && assetLastModified == assetLastModTimes.get(assetPath)) {
        attributes.put("path", getCachedPath(assetPath));
        return true;
      }

      try {

        LOG.info("Filtering and caching {}", assetFile.getPath());

        // get the file path for the original file. If we are storing files
        // under WEB-INF, that part of the path is removed. Allows hiding the
        // scripts and stylesheets and making them available only from cache
        String pathPrefix = FilenameUtils.getPath(assetPath);
        pathPrefix = StringUtils.removeStart(pathPrefix, "WEB-INF");
        String assetName = FilenameUtils.getName(assetFile.getPath());
        String tempFilename = FilenameUtils.concat(pathPrefix, assetName);

        // copy the input file to the temp directory
        String randomKey = "_ecstatic_working_" + System.currentTimeMillis();
        File workingDir = new File(FileUtils.getTempDirectory(), randomKey);
        File workingFile = new File(workingDir, tempFilename);
        FileUtils.copyFile(assetFile, workingFile);

        // run through the filter chain for the filetype
        String[] filterNames = typeToFilters.get(attributes.get("filtertype"));
        if (filterNames != null) {
          for (String filterName : filterNames) {
            WebAssetFilter filter = filters.get(filterName);
            if (filter != null) {
              workingFile = filter.filterAsset(workingFile, attributes);
            }
          }
        }

        // get the raw bytes of the asset source and create a crc value to
        // identify unique contents
        byte[] filteredBytes = FileUtils.readFileToByteArray(workingFile);
        byte[] cachedBytes = filteredBytes;
        CRC32 crc32 = new CRC32();
        crc32.update(filteredBytes);
        long crcVal = crc32.getValue();

        // get the cached name for the filtered file
        String filteredPath = workingFile.getPath();
        String filteredExt = FilenameUtils.getExtension(filteredPath);
        String filteredBase = FilenameUtils.getBaseName(filteredPath);
        String cachedName = filteredBase + "." + crcVal + "." + filteredExt;
        String cachedPath = FilenameUtils.concat(pathPrefix, cachedName);

        // write the file out to the cache, the parent directories of the
        // file will be created in the cache dir if they don't already exist
        File cacheFile = new File(cacheRoot, cachedPath);
        boolean copyToCache =
          !cacheFile.exists()
            || (removeExistingCacheFiles && FileUtils.deleteQuietly(cacheFile));
        if (copyToCache) {
          FileUtils.writeByteArrayToFile(cacheFile, cachedBytes);
          LOG.info("Added {} to cache as {}", assetFile.getPath(),
            cacheFile.getPath());
        }
        else {
          LOG.info("Existing file {} in cache, no copy", cacheFile.getPath());
        }

        // cache to prevent filtering of files that haven't changed and have
        // pointer from raw asset to the cached path
        assetLastModTimes.put(assetPath, assetLastModified);
        pathsCache.put(assetPath, cachedPath);
        attributes.put("path", getCachedPath(assetPath));

        // quietly remove the working directory used for filtering, any files
        // created during filtering are removed
        if (removeTempResources) {
          FileUtils.deleteQuietly(workingDir);
        }

        // successfully filtered and cached
        return true;
      }
      catch (Exception e) {
        // errors during filtering and caching that are not caught, we can't
        // use the output, ignore file and log error
        LOG.error("Error filtering and caching, ignoring: " + assetPath, e);
      }
    }

    // raw file doesn't exist or an error occurred during filtering
    return false;
  }

  /**
   * Initialize the web asset manager. Loads all of the configuration resources.
   * Starts up file change monitoring.
   */
  public synchronized void startup()
    throws IOException {

    // setup the cache directory
    File tempCache = new File(rootDirectory, cacheDirectory);
    boolean cacheExists = tempCache.exists();
    if (!cacheExists) {
      cacheExists = tempCache.mkdirs();
      if (!cacheExists) {
        throw new IOException("Couldn't create cache directory for assets");
      }
      LOG.info("Created web asset cache directory: " + tempCache.getPath());
    }

    // get the root asset directory from the classpath
    File configRoot = new File(rootDirectory, configDirectory);
    if (!configRoot.exists() || !configRoot.canRead()) {
      throw new IOException("No configuration root directory found");
    }

    // collect all matching config files under the root asset path
    List<File> configFiles = new ArrayList<File>();
    SuffixFileFilter suffixFilter = new SuffixFileFilter(configFileSuffix);
    FileIOUtils.collectFiles(configFiles, configRoot, suffixFilter, false);

    // load all config files
    if (configFiles.size() == 0) {
      LOG.warn("No web asset config files to load.");
    }
    else {
      for (File configFile : configFiles) {
        loadAssetConfigFile(configFile);
      }
    }

    // activate the service
    active.set(true);

    // start the config file monitoring thread if we have reload interval
    if (reloadCheckInterval > 0) {
      configFileMonitor = new ConfigFileMonitor();
      configFileMonitor.setDaemon(true);
      configFileMonitor.start();
    }
  }

  /**
   * Shutdown the web asset manager. Clears all assets and configs. Clears all
   * caches. Delete the cache directory from the file system.
   */
  public synchronized void shutdown() {

    // set active to false to stop the config file monitoring thread
    active.set(false);

    // clear the caches
    aliasesCache.clear();
    scriptsCache.clear();
    metaCache.clear();
    linksCache.clear();
    titleCache.clear();
    assetLastModTimes.clear();

    // quietly remove the cache directory
    if (removeCacheDirOnShutdown) {
      File tempCache = new File(rootDirectory, cacheDirectory);
      FileUtils.deleteQuietly(tempCache);
      LOG.info("Removed web asset cache directory: " + tempCache.getPath());
    }
  }

  /**
   * Reinitializes by shutting down and restarting.
   */
  public synchronized void restart()
    throws IOException {
    shutdown();
    startup();
  }

  public Set<String> getConfigIds() {
    return idToConfig.keySet();
  }

  public WebAssetConfig getConfigForId(String id) {
    return idToConfig.get(id);
  }

  public String getCachedPath(String assetPath) {

    String cachedAsset = pathsCache.get(assetPath);
    if (StringUtils.isBlank(cachedAsset)) {
      return null;
    }

    String fullCachedPath = cacheDirectory;
    if (!StringUtils.startsWith(fullCachedPath, "/")) {
      fullCachedPath = "/" + fullCachedPath;
    }
    if (!StringUtils.endsWith(fullCachedPath, "/")
      && !StringUtils.startsWith(cachedAsset, "/")) {
      fullCachedPath += "/";
    }
    fullCachedPath += cachedAsset;

    return fullCachedPath;
  }

  public List<Map<String, String>> getGlobalScripts(Locale locale) {
    return getScriptsForId(GLOBAL, locale);
  }

  public List<Map<String, String>> getScriptsForId(String id, Locale locale) {

    // check the cache first
    String cacheKey = id + "_" + StringUtils.lowerCase(locale.toString());
    if (caching && scriptsCache.containsKey(cacheKey)) {
      return scriptsCache.get(cacheKey);
    }

    // get the id script attributes
    List<Map<String, String>> scripts = new ArrayList<Map<String, String>>();
    WebAssetConfig assetConfig = idToConfig.get(id);

    if (assetConfig != null) {

      List<Map<String, String>> scriptConfigs = assetConfig.getScripts();
      for (Map<String, String> scriptConfig : scriptConfigs) {

        // resolving the aliases creates a copy of the script config
        Map<String, String> scriptAttrs = resolveAll(scriptConfig, locale);
        if (!scriptAttrs.containsKey("filter")) {
          scriptAttrs.put("filtertype", "javascript");
        }

        // script was successfully filtered and cached
        if (filterAndCache(scriptAttrs)) {
          scripts.add(scriptAttrs);
        }
      }

      if (caching) {
        scriptsCache.put(cacheKey, scripts);
      }
    }

    return scripts;
  }

  public List<Map<String, String>> getGlobalLinks(Locale locale) {
    return getLinksForId(GLOBAL, locale);
  }

  public List<Map<String, String>> getLinksForId(String id, Locale locale) {

    // check the cache first
    String cacheKey = id + "_" + StringUtils.lowerCase(locale.toString());
    if (caching && linksCache.containsKey(cacheKey)) {
      return linksCache.get(cacheKey);
    }

    // get the id link attributes
    List<Map<String, String>> links = new ArrayList<Map<String, String>>();
    WebAssetConfig assetConfig = idToConfig.get(id);

    if (assetConfig != null) {

      List<Map<String, String>> linkConfigs = assetConfig.getLinks();
      for (Map<String, String> linkConfig : linkConfigs) {

        // resolving the aliases creates a copy of the script config
        Map<String, String> linkAttrs = resolveAll(linkConfig, locale);
        if (!linkAttrs.containsKey("filter")) {
          linkAttrs.put("filtertype", "stylesheet");
        }

        // stylesheet was successfully filtered and cached
        if (filterAndCache(linkAttrs)) {
          links.add(linkAttrs);
        }
      }

      if (caching) {
        linksCache.put(cacheKey, links);
      }
    }

    return links;
  }

  public List<Map<String, String>> getGlobalMetas(Locale locale) {
    return getMetasForId(GLOBAL, locale);
  }

  public List<Map<String, String>> getMetasForId(String id, Locale locale) {

    // check the cache first
    String cacheKey = id + "_" + StringUtils.lowerCase(locale.toString());
    if (caching && metaCache.containsKey(cacheKey)) {
      return metaCache.get(cacheKey);
    }

    // get the id meta attributes
    List<Map<String, String>> metas = new ArrayList<Map<String, String>>();
    WebAssetConfig assetConfig = idToConfig.get(id);

    if (assetConfig != null) {

      // resolve any aliases and cache
      List<Map<String, String>> metaConfigs = assetConfig.getMetas();
      for (Map<String, String> metaConfig : metaConfigs) {
        Map<String, String> metaAttrs = resolveAll(metaConfig, locale);
        metas.add(metaAttrs);
      }

      if (caching) {
        metaCache.put(cacheKey, metas);
      }
    }

    return metas;
  }

  public String getGlobalTitle(Locale locale) {
    return getTitleForId(GLOBAL, locale);
  }

  public String getTitleForId(String id, Locale locale) {

    // check the cache first
    String cacheKey = id + "_" + StringUtils.lowerCase(locale.toString());
    if (caching && titleCache.containsKey(cacheKey)) {
      return titleCache.get(cacheKey);
    }

    // get the global and local meta tag attributes
    String title = null;
    WebAssetConfig assetConfig = idToConfig.get(id);
    if (assetConfig != null) {

      title = assetConfig.getTitle();
      if (StringUtils.isNotBlank(title)) {
        title = resolve(title, locale);
      }

      if (caching) {
        titleCache.put(cacheKey, title);
      }
    }

    return title;
  }

  @Override
  public void setMessageSource(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public boolean isRemoveExistingCacheFiles() {
    return removeExistingCacheFiles;
  }

  public void setRemoveExistingCacheFiles(boolean removeExistingCacheFiles) {
    this.removeExistingCacheFiles = removeExistingCacheFiles;
  }

  public boolean isRemoveTempResources() {
    return removeTempResources;
  }

  public void setRemoveTempResources(boolean removeTempResources) {
    this.removeTempResources = removeTempResources;
  }

  public boolean isRemoveCacheDirOnShutdown() {
    return removeCacheDirOnShutdown;
  }

  public void setRemoveCacheDirOnShutdown(boolean removeCacheDirOnShutdown) {
    this.removeCacheDirOnShutdown = removeCacheDirOnShutdown;
  }

  public long getReloadCheckInterval() {
    return reloadCheckInterval;
  }

  public void setReloadCheckInterval(long reloadCheckInterval) {
    this.reloadCheckInterval = reloadCheckInterval;
  }

  public boolean isCaching() {
    return caching;
  }

  public void setCaching(boolean caching) {
    this.caching = caching;
  }

  public Map<String, WebAssetFilter> getFilters() {
    return filters;
  }

  public void setFilters(Map<String, WebAssetFilter> filters) {
    this.filters = filters;
  }

  public Map<String, String[]> getTypeToFilters() {
    return typeToFilters;
  }

  public void setTypeToFilters(Map<String, String[]> typeToFilters) {
    this.typeToFilters = typeToFilters;
  }

  public String getConfigDirectory() {
    return configDirectory;
  }

  public void setConfigDirectory(String configDirectory) {
    this.configDirectory = configDirectory;
  }

  public String getRootDirectory() {
    return rootDirectory;
  }

  public void setRootDirectory(String rootDirectory) {
    this.rootDirectory = rootDirectory;
  }

  public String getCacheDirectory() {
    return cacheDirectory;
  }

  public void setCacheDirectory(String cacheDirectory) {
    this.cacheDirectory = cacheDirectory;
  }

  public String getConfigFileSuffix() {
    return configFileSuffix;
  }

  public void setConfigFileSuffix(String configFileSuffix) {
    this.configFileSuffix = configFileSuffix;
  }

}
