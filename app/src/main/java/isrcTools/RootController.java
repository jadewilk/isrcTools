package isrcTools;
import isrcTools.TrackProviders.*;
import isrcTools.Playlist;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class RootController {
  @FXML private ComboBox<String> source;
  @FXML private ComboBox<String> destination;

  @FXML private ListView<Playlist> sourcePlaylistList;
  @FXML private ListView<String> destinationPlaylistList;

  private static HashMap<String, Class<? extends TrackProvider>> sources = new HashMap<String, Class<? extends TrackProvider>>();
  private static HashMap<String, Class<? extends TrackProvider>> destinations = new HashMap<String, Class<? extends TrackProvider>>();

  private HashMap<String, Class<? extends TrackProvider>> authdProviders = new HashMap<String, Class<? extends TrackProvider>>();

  private Connection conn;
  private Sqlite dbOps;
  private Spotify spotify;
  private YT youtube;

  
  @FXML protected void initialize() { 
    sources.put("spotify", Spotify.class);
    destinations.put("spotify", Spotify.class);
    destinations.put("youtube", YT.class);

    ArrayList<String> sourceList = new ArrayList<String>();
    sourceList.addAll(sources.keySet());
    ArrayList<String> destinationList = new ArrayList<String>();
    destinationList.addAll(destinations.keySet());

    addProviders(sourceList, destinationList);

    final StringConverter<Playlist> playlistConverter = new StringConverter<Playlist>() {
      @Override
      public String toString(Playlist playlist) {
        return playlist.friendlyName;
      }

      @Override
      public Playlist fromString(String string) {
        return null;
      }
    };

    sourcePlaylistList.setCellFactory( lv -> {
      TextFieldListCell<Playlist> cell = new TextFieldListCell<Playlist>();
      cell.setConverter(playlistConverter);
      return cell;
    });

    String dbUrl = "jdbc:sqlite:isrcTools.db";
    try {
      conn = DriverManager.getConnection(dbUrl);
      dbOps = new Sqlite(conn);
      dbOps.ensureDbs();
    }
    catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  @FXML protected <T extends TrackProvider> void addProviders( ArrayList<String> sources, ArrayList<String> destinations) {
    source.setItems(FXCollections.observableArrayList(sources));
    source.autosize();
    destination.setItems(FXCollections.observableArrayList(destinations));
    destination.autosize();
  }

  protected void confirmAuthd(String provider, HashMap<String, Class<? extends TrackProvider>> classes) {
    /*if (!authdProviders.containsKey(provider)) {
      Class<? extends TrackProvider> cl = classes.get(provider);
      cl O = cl.getConstructor(Connection.class).newInstance(conn);
    }*/
    // This is not how i want it to work
    // I want to use reflection but i cant figure out how
    switch (provider) {
      case "spotify": {
        if (spotify == null) spotify = new Spotify(dbOps);
        break;
      }
      case "youtube": {
        if (youtube == null) youtube = new YT(dbOps);
        break;
      }
    };
  }

  @FXML protected void changeSource(ActionEvent event) {
    String provider = source.getValue();
    confirmAuthd(provider, sources);
    switch (provider) {
      case "spotify": {
        ArrayList<Playlist> playlists = spotify.fetchPlaylistList();
        sourcePlaylistList.setItems(FXCollections.observableArrayList(playlists));
        sourcePlaylistList.autosize();
      }
    };
   
  }

  @FXML protected void changeSourcePlaylist(MouseEvent event) {
    Playlist playlist = sourcePlaylistList.getSelectionModel().getSelectedItem();
    Clipboard clipboard = Clipboard.getSystemClipboard();
    ClipboardContent content = new ClipboardContent();
    content.putString(playlist.url);
    clipboard.setContent(content);
    // get playlist and process to generic tracks
    spotify.fetchPlaylist(playlist);
  }

  @FXML protected void changeDestination(ActionEvent event) {
    confirmAuthd(destination.getValue(), destinations);
  }

  @FXML protected void executeTransfer(ActionEvent event) {
    ArrayList<String> res = youtube.getPlaylistTrackUrls(sourcePlaylistList.getSelectionModel().getSelectedItem());
    destinationPlaylistList.setItems(FXCollections.observableArrayList(res));
    destinationPlaylistList.autosize();
  }
  
  @FXML protected void changeDestinationTrack(MouseEvent event) {
    Clipboard clipboard = Clipboard.getSystemClipboard();
    ClipboardContent content = new ClipboardContent();
    content.putString(destinationPlaylistList.getSelectionModel().getSelectedItem());
    clipboard.setContent(content);
  }
}

