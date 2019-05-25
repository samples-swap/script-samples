public class ItemService {
  public List<Item> getImportantItems(String name) {
    ItemRepository repo = new ItemRepository()
    Long timestamp = new Date().getTime()
    List<ItemObj> itemObjs = repo.getItems(name, timestamp)
    List<Item> items = MetadataUtil.addMetadata(itemObjs)

    return items
  }
}

@Service
public class ItemServiceV2 {
  private final ItemRepository repo
  private final MetadataService metadataService
  public ImportantServiceV2(ItemRepository repo, MetadataService ms) {
    this.repo = repo
    this.metadataService = ms
  }

  public List<Item> getImportantItems(String name, Date date) {
    List<ItemObj> itemObjs = repo.getItems(name, date.getTime())
    List<Item> items = metadataService.addMetadata(itemObjs)

    return items
  }
}