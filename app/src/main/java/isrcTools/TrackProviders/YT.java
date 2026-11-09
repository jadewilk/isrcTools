package isrcTools.TrackProviders;

import isrcTools.TrackProvider;
import isrcTools.Track;
import isrcTools.Playlist;
import isrcTools.Sqlite;

import java.util.ArrayList;
import java.sql.Connection;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.HashMap;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.services.youtube.YouTube;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.common.collect.Lists;

public class YT extends TrackProvider {
  public final String providerSlug = "youtube";
  public final String friendlyName = "Youtube Music";

  private static Sqlite dbOps;
  private static Connection conn;
  private static YouTube youtube;

  public YT(Sqlite dbOps) {
    this.dbOps = dbOps;
    this.conn = dbOps.conn;
    auth();
  }

  public void removeFromPlaylist(String url, ArrayList<Track> tracks) {
  }

  public void addToPlaylist(String url, ArrayList<Track> tracks) {
  }

  // true = found, false = not found
  private boolean cacheTrackProviderId(Track track) {
    // try get from db first
    String providerId = dbOps.fetchTrackProviderId(track, providerSlug);
    // Else fetch from remote
    if (providerId == null) {
      System.out.println("Did not find " + track.friendlyName + " for " + providerSlug + " in cache");
      try {
        SearchListResponse res = youtube.search()
            .list(List.of("snippet"))
            .setQ(track.isrc)
            .setType(List.of("video"))
            .setMaxResults(1L)
            .execute();
        List<SearchResult> items = res.getItems();
        if (!items.isEmpty()) {
          SearchResult item = items.get(0);
          providerId = item.getId().getVideoId();
          dbOps.insertTrackProviderLocation(track, this.providerSlug,
              providerId, "https://www.youtube.com/watch?v=" + providerId);
          return true;
        } else {
          System.out.println("Did not find " + track.friendlyName + " on " + providerSlug);
          return false;
        }
      } catch (IOException e) {
        return false;
      }
    } else {
      System.out.println("Found " + track.friendlyName + " for " + providerSlug + " in cache");
      return true;
    }
  }

  public ArrayList<String> getPlaylistTrackUrls(Playlist playlist) {
    // missing tracks: <isrc, title>
    HashMap<String, String> missingTracks = new HashMap<String, String>();
    // fetch all tracks in playlist
    ArrayList<Track> tracks = dbOps.fetchPlaylistEntries(playlist);

    ArrayList<String> urls = new ArrayList<String>();

    for (Track track : tracks) {
      if (!cacheTrackProviderId(track)) {
        missingTracks.put(track.isrc, track.friendlyName);
      } else {
        //String trackProviderId = dbOps.fetchTrackProviderId(track, providerSlug);
        String url = dbOps.fetchTrackProviderUrl(track, providerSlug);
        urls.add(url);
      }
    }
    System.out.println("Missing tracks:\n" + missingTracks.entrySet());
    return urls;
  }

  public String createPlaylist(Playlist playlist) {
    // missing tracks: <isrc, title>
    HashMap<String, String> missingTracks = new HashMap<String, String>();
    // fetch all tracks in playlist
    ArrayList<Track> tracks = dbOps.fetchPlaylistEntries(playlist);

    /*
    com.google.api.services.youtube.model.Playlist created;
    String playlistProviderId  = dbOps.getPlaylistProviderId(playlist.playlistId.toString(), this.providerSlug);
    String url = "https://www.youtube.com/playlist?list=";

    if (playlistProviderId != null) {
      url += playlistProviderId;
      // modify playlist in-place
    }
    else { // create new playlist
      com.google.api.services.youtube.model.Playlist ytPlaylist = new com.google.api.services.youtube.model.Playlist()
        .setSnippet(new PlaylistSnippet()
          .setTitle(playlist.friendlyName)
        )
        .setStatus(new PlaylistStatus().setPrivacyStatus("private"));

      try {
        created = youtube.playlists().insert(List.of("snippet", "status"), ytPlaylist).execute();
        playlistProviderId = created.getId();
        url += playlistProviderId;
        Playlist ytList = new Playlist.PlaylistBuilder(conn, playlistProviderId, this.providerSlug, url)
          .friendlyName(playlist.friendlyName)
          .uuid(playlist.playlistId)
          .createDate(playlist.createDate)
          .build();
      } catch (IOException e) {
        System.out.println(e.getMessage());
      }

    }*/

    for (Track track : tracks) {
      if (!cacheTrackProviderId(track)) {
        missingTracks.put(track.isrc, track.friendlyName);
      } else {
        String trackProviderId = dbOps.fetchTrackProviderId(track, providerSlug);
      }
    }
        /*
        ResourceId resourceId = new ResourceId();
        resourceId.setKind("youtube#video");
        resourceId.setVideoId(trackProviderId);

        PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
        playlistItemSnippet.setPlaylistId(playlistProviderId);
        playlistItemSnippet.setResourceId(resourceId);

        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.setSnippet(playlistItemSnippet);

        try {
          PlaylistItem returnedPlaylistItem = youtube.playlistItems().insert(List.of("snippet", "contentDetails"), playlistItem).execute();
        }
        catch (IOException e) {
          System.out.println(e.getMessage());
        }
      }
    }*/

    System.out.println("Missing tracks:\n" + missingTracks.entrySet());


    return "";
  }

  public ArrayList<Track> fetchPlaylist(Playlist playlist) {

    return new ArrayList<Track>();
  }

  public String fetchTrackUrl(Track track) {
    return "";
  }

  public ArrayList<Playlist> fetchPlaylistList() {
    return new ArrayList<Playlist>();
  }

  private void auth() {
    try {
      GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
      //List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube");
      //Credential credential =  Auth.authorize(scopes, "playlistupdates");
      HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
      NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

      youtube = new YouTube.Builder(httpTransport, jsonFactory, requestInitializer)
          .setApplicationName("isrcstreamer")
          .build();
    } catch (IOException | GeneralSecurityException e) {
      System.out.println(e.getMessage());
    }
  }
}
