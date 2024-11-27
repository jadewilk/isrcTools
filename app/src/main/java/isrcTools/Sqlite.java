package isrcTools;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.UUID;

public class Sqlite {
  public static Connection conn;

  public Sqlite(Connection conn) {
    this.conn = conn;
  }

  private void ensureTrackDb() throws SQLException {
    String sql = """
      create table if not exists tracks (
        trackId text primary key,
        friendlyName text,
        isrc text
      );
    """;
    conn.createStatement().execute(sql);
  }

  private void ensureCacheDb() throws SQLException {
    String sql = """
      create table if not exists cache (
        trackId text,
        providerSlug text,
        providerId text,
        url text,
        primary key (providerId, trackId, providerSlug)
        foreign key (trackId) references tracks(rackId),
        unique (providerId, trackId, providerSlug)
      );
    """;
    conn.createStatement().execute(sql);
  }

  public void insertTrackProviderLocation(Track track, String providerSlug, String providerId, String url) {
    String sql = """
      insert into cache(trackId, providerSlug, providerId, url) values(?, ?, ?, ?)
    """;
    try {
      PreparedStatement pstmt = conn.prepareStatement(sql);
      pstmt.setString(1, track.trackId.toString());
      pstmt.setString(2, providerSlug);
      pstmt.setString(3, providerId);
      pstmt.setString(4, url);
      pstmt.executeUpdate();
    }
    catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  public String fetchTrackProviderId(Track track, String providerSlug) {
    String sql = """
      select providerId from cache where trackId = ? and providerSlug = ?;
    """;
    try {
      PreparedStatement pstmt = conn.prepareStatement(sql);
      pstmt.setString(1, track.trackId.toString());
      pstmt.setString(2, providerSlug);
      ResultSet rs = pstmt.executeQuery();
      while (rs.next()) {
        return rs.getString("providerId");
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    } 
    return null;
  }
  
  public String fetchTrackProviderUrl(Track track, String providerSlug) {
    String sql = """
      select url from cache where trackId = ? and providerSlug = ?;
    """;
    try {
      PreparedStatement pstmt = conn.prepareStatement(sql);
      pstmt.setString(1, track.trackId.toString());
      pstmt.setString(2, providerSlug);
      ResultSet rs = pstmt.executeQuery();
      while (rs.next()) {
        return rs.getString("url");
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    } 
    return null;
  }

  private void ensurePlaylistDb() throws SQLException {
    String sql = """
      create table if not exists playlists (
        playlistId text primary key,
        createDate text
      );
    """;
    conn.createStatement().execute(sql);
  }

  private void ensurePlaylistLocationsDb() throws SQLException {
    String sql = """
      create table if not exists playlistLocations (
        playlistId text primary key,
        providerSlug text,
        providerUrl text,
        providerId text,
        playlistName text,
        foreign key (playlistId) references playlists(playlistId)
      );
    """;
    conn.createStatement().execute(sql);
  }

  public String getPlaylistProviderId(String playlistId, String providerSlug) {
    String sql = """
      select providerId from playlistLocations where playlistId = ? and providerSlug = ?;
    """;
    try {
      PreparedStatement pstmt = conn.prepareStatement(sql);
      pstmt.setString(1, playlistId);
      pstmt.setString(2, providerSlug);
      ResultSet rs = pstmt.executeQuery();
      while (rs.next()) {
        return rs.getString("providerId");
      }
      return null;
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      return null;
    }
  }

  private void ensurePlaylistEntriesDb() throws SQLException {
    String sql = """
      create table if not exists playlistEntries (
        playlistId text,
        trackId text,
        addDate text,
        primary key (playlistId, trackId),
        foreign key (playlistId) references playlists(playlistId),
        foreign key (trackId) references tracks(trackId),
        unique (trackId, playlistId)
      );
    """;
    conn.createStatement().execute(sql);
  }

  public void insertPlaylistEntry(String playlistId, String trackId, String addDate) {
    String sql = """
      insert into playlistEntries(playlistId, trackId, addDate) values(?, ?, ?)
    """;
    try {
      PreparedStatement pstmt = conn.prepareStatement(sql);
      pstmt.setString(1, playlistId);
      pstmt.setString(2, trackId);
      pstmt.setString(3, addDate);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public ArrayList<Track> fetchPlaylistEntries(Playlist playlist) {
    String sql = """
      select * from tracks where trackId in (select trackId from playlistEntries where playlistId = ?)
    """;
    ArrayList<Track> tracks = new ArrayList<Track>();
    try {
      PreparedStatement pstmt = conn.prepareStatement(sql);
      pstmt.setString(1, playlist.playlistId.toString());
      ResultSet rs = pstmt.executeQuery();
      while (rs.next()) {
        tracks.add(new Track.TrackBuilder(conn)
          .friendlyName(rs.getString("friendlyName"))
          .isrc(rs.getString("isrc"))
          .uuid(UUID.fromString(rs.getString("trackId")))
          .build()
        );
      }
      return tracks;
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }

    return new ArrayList<Track>();
  }
  
  public void ensureDbs() {
    try {
      ensureTrackDb();
      ensureCacheDb();
      ensurePlaylistDb();
      ensurePlaylistLocationsDb();
      ensurePlaylistEntriesDb();
    }
    catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public void debugListTracks() throws SQLException {
    String sql = "select * from tracks";
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery(sql);
    while (rs.next()) {
      System.out.println("" + rs.getString("trackId") + " " + rs.getString("isrc") + " " + rs.getString("friendlyName"));
    }
  }
}
