package isrcTools;


public abstract class TrackProvider implements Provider {
  public static String providerSlug;
  public static String friendlyName;
}

/*

// Template for implementing providers
 
public class ProviderSlug extends TrackProvider {
  public String providerSlug = "providerSlug";
  public String friendlyName = "Provider Name";

  public providerSlug() {}

  public void removeFromPlaylist(String url, ArrayList<track> tracks) {}

  public void addToPlaylist(String url, ArrayList<track> tracks) {}

  public String createPlaylist(String name, ArrayList<track> tracks) {
    return "";
  }

  public ArrayList<track> fetchPlaylist(String urls) {
    return new ArrayList<String>();
  }

  public String fetchTrackUrl(track track) {
    return "";
  }
}
*/
