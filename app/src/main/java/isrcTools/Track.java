package isrcTools;

import java.util.UUID;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Track {
  public UUID trackId;
  public String isrc;
  public String friendlyName;
  public static Connection conn;

  private Track(TrackBuilder builder) {
    this.isrc = builder.isrc;
    this.friendlyName = builder.friendlyName;
    this.trackId = builder.trackId;
    this.conn = builder.conn;
  }

  public static class TrackBuilder {
    private String isrc;
    private String friendlyName;
    private UUID trackId;
    private static Connection conn;

    public TrackBuilder(Connection conn) {
      this.conn = conn;
    }

    public TrackBuilder isrc(String isrc) {
      this.isrc = isrc;
      return this;
    }

    public TrackBuilder friendlyName(String friendlyName) {
      this.friendlyName = friendlyName;
      return this;
    }

    public TrackBuilder uuid(UUID trackId) {
      this.trackId = trackId;
      return this;
    }

    public Track build() {
      try {
        if (this.trackId == null && this.isrc != null) {
          String sql = "select trackId from tracks where isrc = ?";
          PreparedStatement pstmt = conn.prepareStatement(sql);
          pstmt.setString(1, this.isrc);
          ResultSet rs = pstmt.executeQuery();
          while (rs.next()) {
            this.trackId = UUID.fromString(rs.getString("trackId"));
          }
        } else if (this.trackId == null && this.friendlyName != null) {
          String sql = "select trackId from tracks where friendlyName = ?";
          PreparedStatement pstmt = conn.prepareStatement(sql);
          pstmt.setString(1, this.friendlyName);
          ResultSet rs = pstmt.executeQuery();
          while (rs.next()) {
            this.trackId = UUID.fromString(rs.getString("trackId"));
          }
        }
        if (this.trackId == null) {
          this.trackId = UUID.randomUUID();
          String sql = "insert into tracks(trackId, isrc, friendlyName) values(?, ?, ?)";
          PreparedStatement pstmt = conn.prepareStatement(sql);
          pstmt.setString(1, this.trackId.toString());
          pstmt.setString(2, this.isrc);
          pstmt.setString(3, this.friendlyName);
          int diff = pstmt.executeUpdate();
        }
      } catch (SQLException e) {
        System.out.println(e.getMessage());
      }
      return new Track(this);
    }
  }
}
