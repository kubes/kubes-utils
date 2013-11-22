package com.denniskubes.webasset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class WebAssetConfig
  implements Serializable, Cloneable {

  // ids for config
  private List<String> ids = new ArrayList<String>();

  // used by global configs
  private boolean globalConfig = false;
  private Map<String, String> aliases = new LinkedHashMap<String, String>();

  // used by all configs
  private String title;
  private List<Map<String, String>> metas = new ArrayList<Map<String, String>>();
  private List<Map<String, String>> scripts = new ArrayList<Map<String, String>>();
  private List<Map<String, String>> links = new ArrayList<Map<String, String>>();

  public WebAssetConfig() {

  }

  public List<String> getIds() {
    return ids;
  }

  public void setIds(List<String> ids) {
    this.ids = ids;
  }

  public boolean isGlobalConfig() {
    return globalConfig;
  }

  public void setGlobalConfig(boolean globalConfig) {
    this.globalConfig = globalConfig;
  }

  public Map<String, String> getAliases() {
    return aliases;
  }

  public void setAliases(Map<String, String> aliases) {
    this.aliases = aliases;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<Map<String, String>> getMetas() {
    return metas;
  }

  public void setMetas(List<Map<String, String>> metas) {
    this.metas = metas;
  }

  public List<Map<String, String>> getScripts() {
    return scripts;
  }

  public void setScripts(List<Map<String, String>> scripts) {
    this.scripts = scripts;
  }

  public List<Map<String, String>> getLinks() {
    return links;
  }

  public void setLinks(List<Map<String, String>> links) {
    this.links = links;
  }

  public Object clone()
    throws CloneNotSupportedException {
    return super.clone();
  }

  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public boolean equals(Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj);
  }

  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }
}
