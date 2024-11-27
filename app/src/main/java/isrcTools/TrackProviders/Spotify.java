package isrcTools.TrackProviders;
import isrcTools.TrackProvider;
import isrcTools.Track;
import isrcTools.Playlist;
import isrcTools.Sqlite;

import java.util.ArrayList;
import java.util.Base64;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.net.URI;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.awt.Desktop;
import java.lang.Runtime;
import java.sql.Connection;
import java.sql.SQLException;

// using https://github.com/spotify-web-api-java/spotify-web-api-java 
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERequest;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERefreshRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchTracksRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistsItemsRequest;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;

public class Spotify extends TrackProvider {
  public static String providerSlug = "spotify";
  public static String friendlyName = "Spotify"; 
  private static String clientId = "57b2453d652b4a75b68457106252f58e";
  private static int port = 9001;
  private static final URI redirectUri = SpotifyHttpManager.makeUri("http://localhost:" + port);
  private static String codeChallenge;
  private static String codeChallengeVerifier;

  private static SpotifyApi spotifyApi;
  private static AuthorizationCodePKCERequest authorizationCodePKCERequest;
  private static AuthorizationCodePKCERefreshRequest authorizationCodePKCERefreshRequest;
  private static Connection conn;
  private static Sqlite dbOps;
  
  public Spotify(Sqlite dbOps) {
    this.dbOps = dbOps;
    this.conn = dbOps.conn;
    auth();
  }

  public void removeFromPlaylist(String url, ArrayList<Track> tracks) {}

  public void addToPlaylist(String url, ArrayList<Track> tracks) {}

  public String createPlaylist(Playlist playlist) {
    return "";
  }

  public ArrayList<Track> fetchPlaylist(Playlist playlist) {
    if (playlist.providerSlug != this.providerSlug) return null;
    int offset = 0;
    GetPlaylistsItemsRequest getPlaylistsItemsRequest;
    ArrayList<Track> tracks = new ArrayList<Track>();

    try {
      while (true) {
        getPlaylistsItemsRequest = spotifyApi.getPlaylistsItems(playlist.providerId)
          .limit(50)
          .offset(offset)
          .build();
        Paging<PlaylistTrack> playlistTrackPaging = getPlaylistsItemsRequest.execute();

        if (tracks.size() == playlistTrackPaging.getTotal()) return tracks;
        else {
          offset += 50;
          for (PlaylistTrack trackP: playlistTrackPaging.getItems()) {
            se.michaelthelin.spotify.model_objects.specification.Track track = (se.michaelthelin.spotify.model_objects.specification.Track) trackP.getTrack();
            Track t = new Track.TrackBuilder(conn)
              .friendlyName(track.getName())
              .isrc(track.getExternalIds().getExternalIds().get("isrc"))
              .build();

            dbOps.insertTrackProviderLocation(t, providerSlug, track.getId(), track.getExternalUrls().get("spotify"));
            tracks.add(t);
            // create virtual playlist
            dbOps.insertPlaylistEntry(playlist.playlistId.toString(), t.trackId.toString(), trackP.getAddedAt().toString());
          }
        }
      }
    } catch (IOException | SpotifyWebApiException | ParseException e) {return null;}
  }

  public String fetchTrackUrl(Track track) {
    if (track.isrc == null) return null;
    SearchTracksRequest searchTracksRequest = spotifyApi.searchTracks("isrc:" + track.isrc)
    .limit(1)
    .build();
    try {
      Paging<se.michaelthelin.spotify.model_objects.specification.Track> trackPaging = searchTracksRequest.execute();
      if (trackPaging.getTotal() == 1) {
        return trackPaging.getItems()[1].getExternalUrls().get("spotify");
      }
    } catch (IOException | SpotifyWebApiException | ParseException e) {
      System.out.println("Error: " + e.getMessage());
    }
    return null;
  }
  
  public ArrayList<Playlist> fetchPlaylistList() {
    GetListOfCurrentUsersPlaylistsRequest getListOfCurrentUsersPlaylistsRequest = spotifyApi
    .getListOfCurrentUsersPlaylists()
    .offset(0)
    .build();

    ArrayList<PlaylistSimplified> providerPlaylists = new ArrayList<PlaylistSimplified>();

    ArrayList<Playlist> playlists = new ArrayList<Playlist>();
    
    try {
      Paging<PlaylistSimplified> playlistSimplifiedPaging = getListOfCurrentUsersPlaylistsRequest.execute();
      int total = playlistSimplifiedPaging.getTotal();
      for (PlaylistSimplified p: playlistSimplifiedPaging.getItems()) providerPlaylists.add(p);
      for (int offset = 50; offset < total; offset += 50) {
        playlistSimplifiedPaging = spotifyApi.getListOfCurrentUsersPlaylists()
          .offset(offset)
          .build()
          .execute();
        for (PlaylistSimplified p: playlistSimplifiedPaging.getItems()) providerPlaylists.add(p);
      }

      

      for (PlaylistSimplified p: providerPlaylists) {
        playlists.add(
          new Playlist.PlaylistBuilder(conn, p.getId(), providerSlug, p.getExternalUrls().get("spotify"))
            .friendlyName(p.getName())
            .build()
        );
      }
    } catch (IOException | SpotifyWebApiException | ParseException e) {
      System.out.println("Error: " + e.getMessage());
    }

    return playlists;
  }




  // Auth boilerplate
  private void authCodeRefresh() {
    try {
      final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodePKCERefreshRequest.execute();

      // Set access and refresh token for further "spotifyApi" object usage
      spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
      spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());

      System.out.println("Expires in: " + authorizationCodeCredentials.getExpiresIn());
    } catch (IOException | SpotifyWebApiException | ParseException e) {
     System.out.println("Error: " + e.getMessage());
    }
  }
  
  private void auth() {
    try {
      // 'borrowed' from https://www.appsdeveloperblog.com/pkce-code-verifier-and-code-challenge-in-java/ there is no way i am implementing this myself bruh
      SecureRandom secureRandom = new SecureRandom();
      byte[] codeVerifier = new byte[32];
      secureRandom.nextBytes(codeVerifier);
      codeChallengeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);

      byte[] bytes = codeChallengeVerifier.getBytes("US-ASCII");
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(bytes, 0, bytes.length);
      byte[] digest = messageDigest.digest();
      codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {}
    
    this.spotifyApi = new SpotifyApi.Builder()
      .setClientId(clientId)
      .setRedirectUri(redirectUri)
      .build();
    AuthorizationCodeUriRequest authCodeUriReq = spotifyApi.authorizationCodePKCEUri(codeChallenge)
      .scope("user-library-read,playlist-read-private,playlist-modify-private,user-library-modify")
      .show_dialog(true)
     .build();

    URI uri = authCodeUriReq.execute();
    Desktop desktop = Desktop.getDesktop();
    try {
      if (desktop.isSupported(Desktop.Action.BROWSE)) desktop.browse(uri);
      else {
        Runtime.getRuntime().exec(new String[] {"xdg-open", uri.toString()});
      }
    } catch (IOException _) {
      System.out.println("Error opening uri; navigate manually: " + uri.toString());
    }
  
    try {
      ServerSocket serverSocket = new ServerSocket(port);
      Socket clientSocket = serverSocket.accept();
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String authCode = in.readLine().split("code=| ")[2];
    this.authorizationCodePKCERequest = spotifyApi
      .authorizationCodePKCE(authCode, codeChallengeVerifier)
      .build();
    } catch (IOException e) {}

    try {
      final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodePKCERequest.execute();

      // Set access and refresh token for further "spotifyApi" object usage
      spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
      spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
    } catch (IOException | SpotifyWebApiException | ParseException e) {
      System.out.println("Error: " + e.getMessage());
    }

    this.authorizationCodePKCERefreshRequest = spotifyApi.authorizationCodePKCERefresh()
    .build();
  }
}
