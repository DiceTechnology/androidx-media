package androidx.media3.common.endeavor.cmcd;

import androidx.media3.common.endeavor.cmcd.CMCDType.CMCDKey;
import java.util.EnumMap;

public class CMCDConfig {

  private final EnumMap<CMCDKey, Boolean> activationMap;

  public CMCDConfig() {
    this.activationMap = new EnumMap<>(CMCDKey.class);
  }

  public void setActivation(CMCDKey key, boolean activation) {
    activationMap.put(key, activation);
  }

  public boolean isActive(CMCDKey key) {
    Boolean activation = activationMap.get(key);
    return activation != null && activation;
  }
}
