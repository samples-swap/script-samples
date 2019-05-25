public class ItemServiceSpec extends Specification {
  ItemService sut
  ItemRepository mockRepo

  def 'setup'() {
    mockRepo = Mock()
    sut = new ItemService(mockRepo)
  }

  def 'get items returns 1 item for #name'() {
    given: 'a user named Jane'
    String name = 'Jane'

    when: 'retrieving her items'
    List<Item> result = sut.getItems(name)

    then: 'the repo is called once'
    1 * mockRepo.getUserItems(name) >> [new Item(id: 1, name: name)]
    
    and: '1 item is returned'
    result.size == 1
  }
}