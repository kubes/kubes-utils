package com.denniskubes.webasset;

import java.util.List;
import java.util.Map;

public interface WebAssetConfigParser {
  
  public boolean isGlobal();
  
  public String getTitle();
  
  public Map<String, String> getAliases();
  
  public List<String> getIds();
  
  public List<Map<String, String>> getScripts();
  
  public List<Map<String, String>> getLinks();
  
  public List<Map<String, String>> getMetas();

}
