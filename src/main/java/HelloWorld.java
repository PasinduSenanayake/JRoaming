import model.AddressBook;
import model.Person;
import processor.ForeignInterfaceHandler;

public class HelloWorld {
    public static void main(String[] args) {
        SlaveInterface slaveInterface = ForeignInterfaceHandler.wireForeignImpl(SlaveInterface.class,"reportEngine","dataCollectionService");
        try {
           Person person = slaveInterface.getPerson(AddressBook.getDefaultInstance());
        } catch (Exception e){
            e.printStackTrace();
        }

    }

}


