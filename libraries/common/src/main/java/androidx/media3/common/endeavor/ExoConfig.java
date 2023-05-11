package androidx.media3.common.endeavor;

public class ExoConfig {

  private static class SingletonHolder {
    private static final ExoConfig instance = new ExoConfig();
  }

  // use L3 only if the widevineSecurityLevel set to "L3", otherwise use L1 (default)
  private String widevineSecurityLevel = null;
  private boolean obtainKeyIdsFromManifest = true;

  private ExoConfig() {
  }

  public static final ExoConfig getInstance() {
    return SingletonHolder.instance;
  }

  public String getWidevineSecurityLevel() {
    return widevineSecurityLevel;
  }

  public void setWidevineSecurityLevel(String widevineSecurityLevel) {
    this.widevineSecurityLevel = widevineSecurityLevel;
  }

  public boolean isObtainKeyIdsFromManifest() {
    return obtainKeyIdsFromManifest;
  }

  public void setObtainKeyIdsFromManifest(boolean obtainKeyIdsFromManifest) {
    this.obtainKeyIdsFromManifest = obtainKeyIdsFromManifest;
  }
}
