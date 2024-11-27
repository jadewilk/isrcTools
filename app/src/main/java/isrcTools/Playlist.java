package isrcTools;

import java.util.UUID;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class Playlist {
  public String friendlyName;
  public UUID playlistId;
  public String providerId;
  public String url;
  public String providerSlug;
  private static Connection conn;
  public String createDate;
  

  private Playlist(PlaylistBuilder builder) {
    this.friendlyName = builder.friendlyName;
    this.playlistId = builder.playlistId;
    this.providerId = builder.providerId;
    this.url = builder.url;
    this.providerSlug = builder.providerSlug;
    this.conn = builder.conn;
    this.createDate = builder.createDate;
  }

  public static class PlaylistBuilder {
    public String friendlyName;
    public UUID playlistId;
    public String providerId;
    public String url;
    public String providerSlug;
    private static Connection conn;
    public String createDate;

    public PlaylistBuilder(Connection conn, String providerId, String providerSlug, String url) {
      this.conn = conn;
      this.providerId = providerId;
      this.providerSlug = providerSlug;
      this.url = url;
    }

    public PlaylistBuilder friendlyName(String friendlyName) {
      this.friendlyName = friendlyName;
      return this;
    }

    public PlaylistBuilder uuid(UUID playlistId) {
      this.playlistId = playlistId;
      return this;
    }

    public PlaylistBuilder createDate(String createDate) {
      this.createDate = createDate;
      return this;
    }

    public Playlist build() {
      try {
        if (this.playlistId == null) {
          String sql = "select playlistId from playlistLocations where providerSlug = ? and providerId = ?";
          PreparedStatement pstmt = conn.prepareStatement(sql);
          pstmt.setString(1, this.providerSlug);
          pstmt.setString(2, this.providerId);
          ResultSet rs = pstmt.executeQuery();
          while (rs.next()) {
            this.playlistId = UUID.fromString(rs.getString("playlistId"));
          }
        }
        if (this.playlistId == null) {
          // create virtual playlist
          this.playlistId = UUID.randomUUID();
          String sql = "insert into playlists(playlistId, createDate) values(?, ?)";
          PreparedStatement pstmt = conn.prepareStatement(sql);
          if (this.createDate == null) this.createDate = LocalDateTime.now().toString();
          pstmt.setString(1, this.playlistId.toString());
          pstmt.setString(2, this.createDate);
          pstmt.executeUpdate();
          // create playlist provider location
          sql = "insert into playlistLocations(playlistId, providerSlug, providerUrl, playlistName, providerId) values(?, ?, ?, ?, ?)";
          pstmt = conn.prepareStatement(sql);
          if (this.friendlyName == null) this.friendlyName = "New Playlist from " + this.createDate ;
          pstmt.setString(1, this.playlistId.toString());
          pstmt.setString(2, this.providerSlug);
          pstmt.setString(3, this.url);
          pstmt.setString(4, this.friendlyName);
          pstmt.setString(5, this.providerId);
          pstmt.executeUpdate();
        }
      }
      catch (SQLException e) {
        System.out.println(e.getMessage());
      }
      return new Playlist(this);
    }
  }

}
