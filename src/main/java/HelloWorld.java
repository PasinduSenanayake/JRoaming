import model.AddressBook;
import model.Person;
import processor.ForeignInterfaceHandler;

import java.util.concurrent.Future;

public class HelloWorld {
    public static void main(String[] args) {
     SlaveInterface slaveInterface = ForeignInterfaceHandler.wireForeignImpl(SlaveInterface.class,"service-signature-2","dataCollectionService");
        try {
           Future<Person> person = slaveInterface.getAsyncPerson(AddressBook.newBuilder().addPeople(Person.newBuilder().setName("test").build()).build());
           System.out.println(person.get().getName());
        } catch (Exception e){
            e.printStackTrace();
        }

    }

}


