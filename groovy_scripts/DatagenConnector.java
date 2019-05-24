public class DatagenConnector extends SourceConnector {
  public void start(Map<String, String> props) { this.props = props; }
  public void stop() {}
  public Class<? extends Task> taskClass() { return DatagenTask.class; }
  public ConfigDef config() { return DatagenConnectorConfig.definition(); }
  
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    List<Map<String, String>> configs = new ArrayList<>();
    for (int i = 0; i < maxTasks; i++) { configs.add(this.props); }
    return configs;
  }
}