import annotation.ForeignFunction;
import marker.ForeignServiceImpl;
import model.AddressBook;
import model.Person;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class SlaveImpl implements SlaveInterface, ForeignServiceImpl {
    @Override
    @ForeignFunction(functionSignature = "getAsyncPerson")
    public Future<Person> getAsyncPerson(AddressBook addressBook) {
        CompletableFuture<Person> completableFuture = new CompletableFuture<>();
        completableFuture.complete(Person.newBuilder().setName(addressBook.getPeople(0).getName()+"_updated").build());
        return completableFuture;
    }

    @Override
    @ForeignFunction(functionSignature = "getPerson")
    public Person getPerson(AddressBook addressBook) {
        return Person.newBuilder().setName(addressBook.getPeople(0).getName()+"_updated").build();
    }
}
