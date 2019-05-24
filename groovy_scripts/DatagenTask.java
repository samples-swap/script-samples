public class DatagenTask extends SourceTask {
  public void start(Map<String, String> props) {
    // init config variable for poll() logic
  }
  public void stop() {}
  public List<SourceRecord> poll() throws InterruptedException {
    List<SourceRecord> records = new ArrayList<>();
    // load message template and randomize fields 
    // based on config variable
    // and add resulting messages to records
    return records;
  }
}