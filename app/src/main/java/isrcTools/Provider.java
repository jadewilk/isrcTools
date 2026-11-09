package isrcTools;

import java.util.ArrayList;

public interface Provider {
  void removeFromPlaylist(String url, ArrayList<Track> tracks);

  void addToPlaylist(String url, ArrayList<Track> tracks);

  String createPlaylist(Playlist playlist);

  ArrayList<Track> fetchPlaylist(Playlist playlist);

  String fetchTrackUrl(Track track);

  ArrayList<Playlist> fetchPlaylistList();
}
